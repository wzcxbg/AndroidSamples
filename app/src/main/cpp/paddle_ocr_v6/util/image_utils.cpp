#include "util/image_utils.h"

#include <algorithm>
#include <cctype>
#include <cmath>
#include <string>
#include <vector>

#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>

#include "model/types.h"
#include "util/math_utils.h"

namespace paddle_ocr {
namespace {

std::string lowercase(std::string_view value) {
  std::string result(value);
  std::ranges::transform(result, result.begin(), [](unsigned char character) {
    return static_cast<char>(std::tolower(character));
  });
  return result;
}

} // namespace

cv::Mat BitmapUtils::imdecodeBGR(std::span<const std::uint8_t> imageBytes) {
  if (imageBytes.empty()) {
    throw OCRError(OCRErrorCode::InvalidImage,
                   "Input image is empty or invalid");
  }
  const std::vector<std::uint8_t> encoded(imageBytes.begin(), imageBytes.end());
  cv::Mat decoded = cv::imdecode(encoded, cv::IMREAD_COLOR);
  if (decoded.empty()) {
    throw OCRError(OCRErrorCode::InvalidImage,
                   "Input image is empty or invalid");
  }
  return decoded;
}

cv::Mat BitmapUtils::toBGR(const cv::Mat &image) {
  if (image.empty() || image.rows <= 0 || image.cols <= 0) {
    throw OCRError(OCRErrorCode::InvalidImage,
                   "Input image is empty or invalid");
  }
  if (image.channels() == 3) {
    return image.clone();
  }

  cv::Mat result;
  if (image.channels() == 4) {
    cv::cvtColor(image, result, cv::COLOR_BGRA2BGR);
    return result;
  }
  if (image.channels() == 1) {
    cv::cvtColor(image, result, cv::COLOR_GRAY2BGR);
    return result;
  }
  throw OCRError(OCRErrorCode::InvalidImage,
                 "Unsupported input image channel count");
}

cv::Mat ImageUtils::resizeToMultipleOf32(const cv::Mat &source,
                                         int limitSideLen,
                                         std::string_view limitType,
                                         int maxSideLimit) {
  const int height = source.rows;
  const int width = source.cols;
  const std::string normalizedType = lowercase(limitType);

  double ratio = 1.0;
  if (normalizedType == "max") {
    const int longest = std::max(height, width);
    ratio = longest > limitSideLen ? static_cast<double>(limitSideLen) / longest
                                   : 1.0;
  } else if (normalizedType == "min") {
    const int shortest = std::min(height, width);
    ratio = shortest < limitSideLen
                ? static_cast<double>(limitSideLen) / shortest
                : 1.0;
  } else if (normalizedType == "resize_long") {
    ratio = static_cast<double>(limitSideLen) / std::max(height, width);
  } else {
    throw std::invalid_argument("Unsupported det limit type: " +
                                std::string(limitType));
  }

  int newHeight = static_cast<int>(height * ratio);
  int newWidth = static_cast<int>(width * ratio);
  if (std::max(newHeight, newWidth) > maxSideLimit) {
    ratio = static_cast<double>(maxSideLimit) / std::max(newHeight, newWidth);
    newHeight = static_cast<int>(newHeight * ratio);
    newWidth = static_cast<int>(newWidth * ratio);
  }

  newHeight = std::max(MathUtils::roundHalfToEven(newHeight / 32.0) * 32, 32);
  newWidth = std::max(MathUtils::roundHalfToEven(newWidth / 32.0) * 32, 32);
  cv::Mat result;
  cv::resize(source, result, cv::Size(newWidth, newHeight), 0.0, 0.0,
             cv::INTER_LINEAR);
  return result;
}

} // namespace paddle_ocr
