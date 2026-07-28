#pragma once

#include <array>
#include <cstdint>
#include <optional>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

#include <opencv2/core/types.hpp>

namespace paddle_ocr {

enum class OCRErrorCode {
  ModelNotFound,
  ModelLoadFailed,
  ConfigParseFailed,
  InvalidImage,
  InferenceFailed,
  DecodeError,
};

class OCRError final : public std::runtime_error {
public:
  OCRError(OCRErrorCode code, std::string message)
      : std::runtime_error(std::move(message)), code_(code) {}

  [[nodiscard]] OCRErrorCode code() const noexcept { return code_; }

private:
  OCRErrorCode code_;
};

struct OCRBox {
  std::array<cv::Point2f, 4> points{};
};

struct OCRResult {
  OCRBox box;
  std::string text;
  float confidence = 0.0F;
  std::optional<std::vector<OCRBox>> wordBoxes;
};

struct OCRRunResult {
  std::vector<OCRResult> results;
  std::int64_t detectionTimeMs = 0;
  std::int64_t recognitionTimeMs = 0;
  std::int64_t totalTimeMs = 0;
  std::int32_t lineCount = 0;
  std::int64_t detPreprocessMs = 0;
  std::int64_t detInferenceMs = 0;
  std::int64_t detPostprocessMs = 0;
  std::int64_t recPreprocessMs = 0;
  std::int64_t recInferenceMs = 0;
  std::int64_t recPostprocessMs = 0;
  std::int64_t pipelineOverheadMs = 0;
  std::int64_t coldLoadTimeMs = 0;
  std::vector<std::int32_t> detInputShape;
  std::vector<std::vector<std::int32_t>> recInputShapes;
  std::vector<std::int64_t> perLineRecMs;
};

struct OCREngineResult : OCRRunResult {};

} // namespace paddle_ocr
