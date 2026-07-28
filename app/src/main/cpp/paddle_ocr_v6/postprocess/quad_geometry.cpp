#include "postprocess/quad_geometry.h"

#include <algorithm>

namespace paddle_ocr {

std::array<cv::Point2f, 4>
QuadGeometry::orderMinAreaRectPoints(const std::array<cv::Point2f, 4> &points) {
  std::array<cv::Point2f, 4> sorted = points;
  std::stable_sort(sorted.begin(), sorted.end(),
                   [](const cv::Point2f &left, const cv::Point2f &right) {
                     return left.x < right.x;
                   });

  const cv::Point2f topLeft = sorted[1].y > sorted[0].y ? sorted[0] : sorted[1];
  const cv::Point2f bottomLeft =
      sorted[1].y > sorted[0].y ? sorted[1] : sorted[0];
  const cv::Point2f topRight =
      sorted[3].y > sorted[2].y ? sorted[2] : sorted[3];
  const cv::Point2f bottomRight =
      sorted[3].y > sorted[2].y ? sorted[3] : sorted[2];
  return {topLeft, topRight, bottomRight, bottomLeft};
}

} // namespace paddle_ocr
