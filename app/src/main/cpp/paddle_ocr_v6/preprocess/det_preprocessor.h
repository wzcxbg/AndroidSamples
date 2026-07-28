#pragma once

#include <cstdint>
#include <string_view>
#include <vector>

#include <opencv2/core/mat.hpp>

namespace paddle_ocr {

struct DetPreprocessResult {
  std::vector<float> tensorData;
  std::vector<std::int64_t> shape;
  int originalHeight = 0;
  int originalWidth = 0;
};

class DetPreprocessor final {
public:
  static DetPreprocessResult preprocess(const cv::Mat &source, int limitSideLen,
                                        std::string_view limitType,
                                        int maxSideLimit,
                                        std::string_view imageMode);
};

} // namespace paddle_ocr
