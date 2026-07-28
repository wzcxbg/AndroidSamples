#pragma once

#include <vector>

#include <opencv2/core/types.hpp>

namespace paddle_ocr {

class PolygonUnclip final {
public:
  static std::vector<cv::Point2f> unclip(const std::vector<cv::Point2f> &points,
                                         float unclipRatio);
};

} // namespace paddle_ocr
