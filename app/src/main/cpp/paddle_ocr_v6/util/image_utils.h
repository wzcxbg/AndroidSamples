#pragma once

#include <cstdint>
#include <span>
#include <string_view>

#include <opencv2/core/mat.hpp>

namespace paddle_ocr {

class BitmapUtils final {
public:
  static cv::Mat imdecodeBGR(std::span<const std::uint8_t> imageBytes);
  static cv::Mat toBGR(const cv::Mat &image);
};

class ImageUtils final {
public:
  static cv::Mat resizeToMultipleOf32(const cv::Mat &source, int limitSideLen,
                                      std::string_view limitType,
                                      int maxSideLimit);
};

} // namespace paddle_ocr
