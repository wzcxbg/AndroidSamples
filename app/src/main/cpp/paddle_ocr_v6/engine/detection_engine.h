#pragma once

#include <cstdint>
#include <vector>

#include <opencv2/core/mat.hpp>

#include "config.h"
#include "engine/ort_session_manager.h"
#include "model/types.h"

namespace paddle_ocr {

struct DetectionResult {
  std::vector<OCRBox> boxes;
  std::int64_t preprocessMs = 0;
  std::int64_t inferenceMs = 0;
  std::int64_t postprocessMs = 0;
  std::int64_t timeMs = 0;
  std::vector<std::int32_t> inputShape;
};

class DetectionEngine final {
public:
  DetectionEngine(ORTSessionManager &sessionManager, PaddleOCRConfig config);

  DetectionResult detect(const cv::Mat &image) const;

private:
  ORTSessionManager &sessionManager_;
  PaddleOCRConfig config_;
};

} // namespace paddle_ocr
