#pragma once

#include <cstdint>
#include <vector>

#include <opencv2/core/mat.hpp>

namespace paddle_ocr {

struct RecPreprocessResult {
  std::vector<float> tensorData;
  std::vector<std::int64_t> shape;
};

class RecPreprocessor final {
public:
  static constexpr int FixedHeight = 48;
  static constexpr int MaxImageWidth = 3200;

  static RecPreprocessResult preprocessBatch(const std::vector<cv::Mat> &crops);
};

} // namespace paddle_ocr
