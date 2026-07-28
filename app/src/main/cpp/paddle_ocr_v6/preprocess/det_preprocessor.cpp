#include "preprocess/det_preprocessor.h"

#include <algorithm>
#include <array>
#include <cctype>
#include <cstring>
#include <string>
#include <vector>

#include <opencv2/imgproc.hpp>

#include "util/image_utils.h"

namespace paddle_ocr {
namespace {

bool isRgb(std::string_view imageMode) {
  std::string normalized(imageMode);
  std::ranges::transform(normalized, normalized.begin(),
                         [](unsigned char character) {
                           return static_cast<char>(std::toupper(character));
                         });
  return normalized == "RGB";
}

} // namespace

DetPreprocessResult DetPreprocessor::preprocess(const cv::Mat &source,
                                                int limitSideLen,
                                                std::string_view limitType,
                                                int maxSideLimit,
                                                std::string_view imageMode) {
  cv::Mat input;
  if (isRgb(imageMode)) {
    cv::cvtColor(source, input, cv::COLOR_BGR2RGB);
  } else {
    input = source;
  }

  const cv::Mat resized = ImageUtils::resizeToMultipleOf32(
      input, limitSideLen, limitType, maxSideLimit);
  cv::Mat floatImage;
  resized.convertTo(floatImage, CV_32FC3);

  std::vector<cv::Mat> channels;
  cv::split(floatImage, channels);
  constexpr std::array<double, 3> means{0.485, 0.456, 0.406};
  constexpr std::array<double, 3> deviations{0.229, 0.224, 0.225};

  const int height = resized.rows;
  const int width = resized.cols;
  const std::size_t channelSize = static_cast<std::size_t>(height) * width;
  std::vector<float> tensorData(channelSize * 3);
  for (std::size_t channel = 0; channel < channels.size(); ++channel) {
    channels[channel].convertTo(channels[channel], CV_32F,
                                1.0 / 255.0 / deviations[channel],
                                -means[channel] / deviations[channel]);
    const cv::Mat continuous = channels[channel].isContinuous()
                                   ? channels[channel]
                                   : channels[channel].clone();
    std::memcpy(tensorData.data() + channel * channelSize,
                continuous.ptr<float>(), channelSize * sizeof(float));
  }

  return DetPreprocessResult{
      .tensorData = std::move(tensorData),
      .shape = {1, 3, height, width},
      .originalHeight = source.rows,
      .originalWidth = source.cols,
  };
}

} // namespace paddle_ocr
