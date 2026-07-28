#include "postprocess/db_post_processor.h"

#include <algorithm>
#include <array>
#include <cctype>
#include <cmath>
#include <string>
#include <vector>

#include <opencv2/imgproc.hpp>

#include "postprocess/polygon_unclip.h"
#include "postprocess/quad_geometry.h"
#include "util/math_utils.h"

namespace paddle_ocr {
namespace {

std::string lowercase(std::string value) {
  std::ranges::transform(value, value.begin(), [](unsigned char character) {
    return static_cast<char>(std::tolower(character));
  });
  return value;
}

float computeBoxScore(const cv::Mat &probabilityMap,
                      const std::vector<cv::Point2f> &points) {
  if (points.empty()) {
    return 0.0F;
  }

  float minimumX = points.front().x;
  float maximumX = points.front().x;
  float minimumY = points.front().y;
  float maximumY = points.front().y;
  for (const cv::Point2f &point : points) {
    minimumX = std::min(minimumX, point.x);
    maximumX = std::max(maximumX, point.x);
    minimumY = std::min(minimumY, point.y);
    maximumY = std::max(maximumY, point.y);
  }
  const int xMin = std::clamp(static_cast<int>(std::floor(minimumX)), 0,
                              probabilityMap.cols - 1);
  const int xMax = std::clamp(static_cast<int>(std::ceil(maximumX)), 0,
                              probabilityMap.cols - 1);
  const int yMin = std::clamp(static_cast<int>(std::floor(minimumY)), 0,
                              probabilityMap.rows - 1);
  const int yMax = std::clamp(static_cast<int>(std::ceil(maximumY)), 0,
                              probabilityMap.rows - 1);

  cv::Mat mask(yMax - yMin + 1, xMax - xMin + 1, CV_8UC1, cv::Scalar(0));
  std::vector<cv::Point> localPoints;
  localPoints.reserve(points.size());
  for (const cv::Point2f &point : points) {
    localPoints.emplace_back(static_cast<int>(point.x - xMin),
                             static_cast<int>(point.y - yMin));
  }
  cv::fillPoly(mask, std::vector<std::vector<cv::Point>>{localPoints},
               cv::Scalar(1));
  const cv::Mat region =
      probabilityMap(cv::Range(yMin, yMax + 1), cv::Range(xMin, xMax + 1));
  return static_cast<float>(cv::mean(region, mask)[0]);
}

cv::Point2f scalePoint(const cv::Point2f &point, double scaleX, double scaleY,
                       cv::Size originalSize) {
  return cv::Point2f(
      static_cast<float>(std::clamp(
          MathUtils::roundHalfToEven(point.x * scaleX), 0, originalSize.width)),
      static_cast<float>(
          std::clamp(MathUtils::roundHalfToEven(point.y * scaleY), 0,
                     originalSize.height)));
}

std::vector<cv::Point2f> toPoint2f(const std::vector<cv::Point> &points) {
  std::vector<cv::Point2f> result;
  result.reserve(points.size());
  for (const cv::Point &point : points) {
    result.emplace_back(static_cast<float>(point.x),
                        static_cast<float>(point.y));
  }
  return result;
}

} // namespace

std::vector<OCRBox>
DBPostProcessor::process(const std::vector<float> &prediction,
                         const std::vector<std::int64_t> &predictionShape,
                         const DBPostProcessParams &params,
                         cv::Size originalSize) {
  if (params.boxType != "quad") {
    throw std::invalid_argument(
        "Only DBPostProcess box_type=quad is supported");
  }
  if (predictionShape.size() != 4) {
    throw std::invalid_argument(
        "Detection output must have shape [N, C, H, W]");
  }

  const int predictionHeight = static_cast<int>(predictionShape[2]);
  const int predictionWidth = static_cast<int>(predictionShape[3]);
  const std::size_t mapSize =
      static_cast<std::size_t>(predictionHeight) * predictionWidth;
  if (prediction.size() < mapSize || predictionHeight <= 0 ||
      predictionWidth <= 0) {
    throw std::invalid_argument(
        "Detection output size does not match its shape");
  }

  const cv::Mat probabilityMap(predictionHeight, predictionWidth, CV_32FC1,
                               const_cast<float *>(prediction.data()));
  cv::Mat thresholdMap;
  cv::threshold(probabilityMap, thresholdMap, params.threshold, 255.0,
                cv::THRESH_BINARY);
  cv::Mat mask;
  thresholdMap.convertTo(mask, CV_8UC1);
  if (params.useDilation) {
    cv::dilate(mask, mask, cv::Mat::ones(2, 2, CV_8UC1));
  }

  std::vector<std::vector<cv::Point>> contours;
  cv::findContours(mask, contours, cv::RETR_LIST, cv::CHAIN_APPROX_SIMPLE);
  const std::size_t contourCount =
      std::min(contours.size(),
               static_cast<std::size_t>(std::max(params.maximumCandidates, 0)));
  const double scaleX =
      static_cast<double>(originalSize.width) / predictionWidth;
  const double scaleY =
      static_cast<double>(originalSize.height) / predictionHeight;
  const bool slowScore = lowercase(params.scoreMode) == "slow";

  std::vector<OCRBox> boxes;
  for (std::size_t index = 0; index < contourCount; ++index) {
    const cv::RotatedRect rectangle = cv::minAreaRect(contours[index]);
    if (std::min(rectangle.size.width, rectangle.size.height) < 3.0F) {
      continue;
    }

    std::array<cv::Point2f, 4> rectanglePoints{};
    rectangle.points(rectanglePoints.data());
    const auto ordered = QuadGeometry::orderMinAreaRectPoints(rectanglePoints);
    const std::vector<cv::Point2f> scorePoints =
        slowScore ? toPoint2f(contours[index])
                  : std::vector<cv::Point2f>(ordered.begin(), ordered.end());
    if (computeBoxScore(probabilityMap, scorePoints) < params.boxThreshold) {
      continue;
    }

    const std::vector<cv::Point2f> expanded = PolygonUnclip::unclip(
        std::vector<cv::Point2f>(ordered.begin(), ordered.end()),
        params.unclipRatio);
    const cv::RotatedRect expandedRectangle = cv::minAreaRect(expanded);
    if (std::min(expandedRectangle.size.width, expandedRectangle.size.height) <
        5.0F) {
      continue;
    }

    std::array<cv::Point2f, 4> expandedPoints{};
    expandedRectangle.points(expandedPoints.data());
    const auto expandedOrdered =
        QuadGeometry::orderMinAreaRectPoints(expandedPoints);
    OCRBox box;
    for (std::size_t pointIndex = 0; pointIndex < box.points.size();
         ++pointIndex) {
      box.points[pointIndex] =
          scalePoint(expandedOrdered[pointIndex], scaleX, scaleY, originalSize);
    }
    const double boxWidth = cv::norm(box.points[1] - box.points[0]);
    const double boxHeight = cv::norm(box.points[3] - box.points[0]);
    if (boxWidth <= 3.0 || boxHeight <= 3.0) {
      continue;
    }
    boxes.push_back(std::move(box));
  }
  return boxes;
}

} // namespace paddle_ocr
