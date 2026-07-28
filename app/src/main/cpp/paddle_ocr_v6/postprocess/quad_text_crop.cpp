#include "postprocess/quad_text_crop.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <vector>

#include <opencv2/imgproc.hpp>

#include "postprocess/quad_geometry.h"

namespace paddle_ocr {

cv::Mat QuadTextCrop::crop(const cv::Mat &source, const OCRBox &box) {
  const std::vector<cv::Point2f> points(box.points.begin(), box.points.end());
  const cv::RotatedRect boundingBox = cv::minAreaRect(points);
  std::array<cv::Point2f, 4> rectanglePoints{};
  boundingBox.points(rectanglePoints.data());
  const auto ordered = QuadGeometry::orderMinAreaRectPoints(rectanglePoints);

  const double widthTop = cv::norm(ordered[0] - ordered[1]);
  const double widthBottom = cv::norm(ordered[2] - ordered[3]);
  const double heightLeft = cv::norm(ordered[0] - ordered[3]);
  const double heightRight = cv::norm(ordered[1] - ordered[2]);
  const int destinationWidth =
      std::max(static_cast<int>(std::max(widthTop, widthBottom)), 1);
  const int destinationHeight =
      std::max(static_cast<int>(std::max(heightLeft, heightRight)), 1);

  const std::array<cv::Point2f, 4> destinationPoints{
      cv::Point2f(0.0F, 0.0F),
      cv::Point2f(static_cast<float>(destinationWidth), 0.0F),
      cv::Point2f(static_cast<float>(destinationWidth),
                  static_cast<float>(destinationHeight)),
      cv::Point2f(0.0F, static_cast<float>(destinationHeight)),
  };
  const cv::Mat transform =
      cv::getPerspectiveTransform(ordered, destinationPoints);
  cv::Mat result;
  cv::warpPerspective(source, result, transform,
                      cv::Size(destinationWidth, destinationHeight),
                      cv::INTER_CUBIC, cv::BORDER_REPLICATE);

  if (static_cast<double>(result.rows) / result.cols < 1.5) {
    return result;
  }
  cv::Mat rotated;
  cv::rotate(result, rotated, cv::ROTATE_90_COUNTERCLOCKWISE);
  return rotated;
}

} // namespace paddle_ocr
