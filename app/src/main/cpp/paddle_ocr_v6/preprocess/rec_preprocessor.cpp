#include "preprocess/rec_preprocessor.h"

#include <algorithm>
#include <cmath>
#include <cstring>
#include <stdexcept>
#include <vector>

#include <opencv2/imgproc.hpp>

namespace paddle_ocr {

RecPreprocessResult
RecPreprocessor::preprocessBatch(const std::vector<cv::Mat> &crops) {
  if (crops.empty()) {
    throw std::invalid_argument("Recognition crops must not be empty");
  }

  std::vector<cv::Mat> normalizedImages;
  normalizedImages.reserve(crops.size());
  int maximumWidth = 0;
  for (const cv::Mat &crop : crops) {
    cv::Mat rgb;
    cv::cvtColor(crop, rgb, cv::COLOR_BGR2RGB);
    const double aspectRatio =
        rgb.rows > 0 ? static_cast<double>(rgb.cols) / rgb.rows : 1.0;
    const int width = std::min(
        static_cast<int>(std::ceil(FixedHeight * aspectRatio)), MaxImageWidth);

    cv::Mat resized;
    cv::resize(rgb, resized, cv::Size(width, FixedHeight), 0.0, 0.0,
               cv::INTER_LINEAR);
    cv::Mat normalized;
    resized.convertTo(normalized, CV_32FC3, 1.0 / 127.5, -1.0);
    maximumWidth = std::max(maximumWidth, width);
    normalizedImages.push_back(std::move(normalized));
  }

  const std::size_t channelSize =
      static_cast<std::size_t>(FixedHeight) * maximumWidth;
  std::vector<float> tensorData(crops.size() * 3 * channelSize, 0.0F);
  for (std::size_t batch = 0; batch < normalizedImages.size(); ++batch) {
    std::vector<cv::Mat> channels;
    cv::split(normalizedImages[batch], channels);
    for (std::size_t channel = 0; channel < channels.size(); ++channel) {
      const cv::Mat &source = channels[channel];
      float *destination =
          tensorData.data() + (batch * 3 + channel) * channelSize;
      for (int row = 0; row < FixedHeight; ++row) {
        std::memcpy(destination + static_cast<std::size_t>(row) * maximumWidth,
                    source.ptr<float>(row),
                    static_cast<std::size_t>(source.cols) * sizeof(float));
      }
    }
  }

  return RecPreprocessResult{
      .tensorData = std::move(tensorData),
      .shape =
          {
              static_cast<std::int64_t>(crops.size()),
              3,
              FixedHeight,
              maximumWidth,
          },
  };
}

} // namespace paddle_ocr
