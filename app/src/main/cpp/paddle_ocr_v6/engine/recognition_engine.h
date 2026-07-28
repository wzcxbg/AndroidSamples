#pragma once

#include <cstdint>
#include <string>
#include <utility>
#include <vector>

#include <opencv2/core/mat.hpp>

#include "engine/ort_session_manager.h"

namespace paddle_ocr {

struct RecognitionResult {
  std::vector<std::pair<std::string, float>> texts;
  std::int64_t preprocessMs = 0;
  std::int64_t inferenceMs = 0;
  std::int64_t postprocessMs = 0;
  std::int64_t timeMs = 0;
  std::vector<std::int32_t> inputShape;
};

class RecognitionEngine final {
public:
  RecognitionEngine(ORTSessionManager &sessionManager,
                    std::vector<std::string> characterList);

  RecognitionResult recognize(const std::vector<cv::Mat> &crops) const;

private:
  ORTSessionManager &sessionManager_;
  std::vector<std::string> characterList_;
};

} // namespace paddle_ocr
