#pragma once

#include <array>

#include <opencv2/core/types.hpp>

namespace paddle_ocr {

class QuadGeometry final {
public:
  static std::array<cv::Point2f, 4>
  orderMinAreaRectPoints(const std::array<cv::Point2f, 4> &points);
};

} // namespace paddle_ocr
