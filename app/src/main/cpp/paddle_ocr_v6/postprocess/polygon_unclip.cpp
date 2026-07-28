#include "postprocess/polygon_unclip.h"

#include <algorithm>
#include <cmath>
#include <numbers>
#include <vector>

namespace paddle_ocr {
namespace {

constexpr double Epsilon = 1e-6;
constexpr double ArcTolerance = 0.25;

double signedArea(const std::vector<cv::Point2f> &points) {
  double sum = 0.0;
  for (std::size_t index = 0; index < points.size(); ++index) {
    const cv::Point2f &first = points[index];
    const cv::Point2f &second = points[(index + 1) % points.size()];
    sum += first.x * second.y - second.x * first.y;
  }
  return sum / 2.0;
}

double perimeter(const std::vector<cv::Point2f> &points) {
  double length = 0.0;
  for (std::size_t index = 0; index < points.size(); ++index) {
    const cv::Point2f &first = points[index];
    const cv::Point2f &second = points[(index + 1) % points.size()];
    length += std::hypot(second.x - first.x, second.y - first.y);
  }
  return length;
}

double arcStepAngle(double distance) {
  const double ratio = std::clamp(1.0 - ArcTolerance / distance, -1.0, 1.0);
  const double step = 2.0 * std::acos(ratio);
  return std::isfinite(step) && step > Epsilon ? step : std::numbers::pi / 8.0;
}

void appendPoint(std::vector<cv::Point2f> &output, const cv::Point2f &point) {
  if (output.empty()) {
    output.push_back(point);
    return;
  }
  const cv::Point2f &last = output.back();
  if (std::hypot(point.x - last.x, point.y - last.y) > Epsilon) {
    output.push_back(point);
  }
}

void appendRoundJoin(std::vector<cv::Point2f> &output,
                     const cv::Point2f &center, const cv::Point2f &fromNormal,
                     const cv::Point2f &toNormal, double distance,
                     bool clockwiseInImageCoordinates) {
  double startAngle = std::atan2(fromNormal.y, fromNormal.x);
  double endAngle = std::atan2(toNormal.y, toNormal.x);
  if (clockwiseInImageCoordinates) {
    while (endAngle < startAngle) {
      endAngle += 2.0 * std::numbers::pi;
    }
  } else {
    while (endAngle > startAngle) {
      endAngle -= 2.0 * std::numbers::pi;
    }
  }

  const double sweep = endAngle - startAngle;
  const int steps = std::max(
      static_cast<int>(std::ceil(std::abs(sweep) / arcStepAngle(distance))), 1);
  for (int step = 0; step <= steps; ++step) {
    const double angle = startAngle + sweep * step / steps;
    appendPoint(
        output,
        cv::Point2f(static_cast<float>(center.x + std::cos(angle) * distance),
                    static_cast<float>(center.y + std::sin(angle) * distance)));
  }
}

} // namespace

std::vector<cv::Point2f>
PolygonUnclip::unclip(const std::vector<cv::Point2f> &points,
                      float unclipRatio) {
  if (points.size() < 3) {
    return points;
  }

  const double polygonSignedArea = signedArea(points);
  const double area = std::abs(polygonSignedArea);
  const double polygonPerimeter = perimeter(points);
  if (!std::isfinite(area) || !std::isfinite(polygonPerimeter) ||
      area <= Epsilon || polygonPerimeter <= Epsilon) {
    return points;
  }

  const double distance = area * unclipRatio / polygonPerimeter;
  if (!std::isfinite(distance) || distance <= Epsilon) {
    return points;
  }

  const bool clockwiseInImageCoordinates = polygonSignedArea > 0.0;
  std::vector<cv::Point2f> normals;
  normals.reserve(points.size());
  for (std::size_t index = 0; index < points.size(); ++index) {
    const cv::Point2f &start = points[index];
    const cv::Point2f &end = points[(index + 1) % points.size()];
    const double deltaX = end.x - start.x;
    const double deltaY = end.y - start.y;
    const double length = std::hypot(deltaX, deltaY);
    if (!std::isfinite(length) || length <= Epsilon) {
      return points;
    }
    normals.emplace_back(
        clockwiseInImageCoordinates ? deltaY / length : -deltaY / length,
        clockwiseInImageCoordinates ? -deltaX / length : deltaX / length);
  }

  std::vector<cv::Point2f> expanded;
  for (std::size_t index = 0; index < points.size(); ++index) {
    appendRoundJoin(expanded, points[index],
                    normals[(index + normals.size() - 1) % normals.size()],
                    normals[index], distance, clockwiseInImageCoordinates);
  }
  return expanded.size() >= 3 ? expanded : points;
}

} // namespace paddle_ocr
