#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include <opencv2/core/types.hpp>

#include "model/types.h"

namespace paddle_ocr {

struct DBPostProcessParams {
  float threshold = 0.3F;
  float boxThreshold = 0.6F;
  float unclipRatio = 1.5F;
  int maximumCandidates = 3000;
  bool useDilation = false;
  std::string scoreMode = "fast";
  std::string boxType = "quad";
};

class DBPostProcessor final {
public:
  static std::vector<OCRBox>
  process(const std::vector<float> &prediction,
          const std::vector<std::int64_t> &predictionShape,
          const DBPostProcessParams &params, cv::Size originalSize);
};

} // namespace paddle_ocr
