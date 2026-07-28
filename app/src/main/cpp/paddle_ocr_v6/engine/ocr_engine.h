#pragma once

#include <cstdint>
#include <memory>
#include <span>

#include <opencv2/core/mat.hpp>

#include "config.h"
#include "engine/detection_engine.h"
#include "engine/ort_session_manager.h"
#include "engine/recognition_engine.h"
#include "model/model_bundle.h"
#include "model/types.h"

namespace paddle_ocr {

class OCREngine final {
public:
  OCREngine(PaddleOCRConfig config, const EngineConfig &engineConfig,
            const ModelBundle &models);
  ~OCREngine();

  OCREngine(const OCREngine &) = delete;
  OCREngine &operator=(const OCREngine &) = delete;

  OCREngineResult run(const cv::Mat &image) const;
  OCREngineResult run(std::span<const std::uint8_t> imageBytes) const;
  void release() noexcept;

  [[nodiscard]] std::int64_t coldLoadTimeMs() const noexcept {
    return sessionManager_.coldLoadTimeMs();
  }

private:
  OCREngineResult runBGR(const cv::Mat &image) const;

  PaddleOCRConfig config_;
  ORTSessionManager sessionManager_;
  std::unique_ptr<DetectionEngine> detectionEngine_;
  std::unique_ptr<RecognitionEngine> recognitionEngine_;
};

} // namespace paddle_ocr
