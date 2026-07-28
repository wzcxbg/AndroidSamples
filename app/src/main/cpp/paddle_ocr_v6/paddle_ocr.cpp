#include "paddle_ocr.h"

#include <string_view>
#include <utility>

#include "engine/ocr_engine.h"
#include "paddle_ocr_v6_models.h"

namespace paddle_ocr {
namespace {

ModelBundle defaultModels() {
  return ModelBundle{
      .detectionModel = PaddleOCRV6Resources::ppOcrV6DetOnnx,
      .recognitionModel = PaddleOCRV6Resources::ppOcrV6RecOnnx,
      .recognitionConfig =
          std::string_view(reinterpret_cast<const char *>(
                               PaddleOCRV6Resources::ppOcrV6CharsYml.data()),
                           PaddleOCRV6Resources::ppOcrV6CharsYml.size()),
  };
}

} // namespace

std::unique_ptr<PaddleOCR> PaddleOCR::create() {
  return create(PaddleOCRConfig{}, EngineConfig{});
}

std::unique_ptr<PaddleOCR> PaddleOCR::create(PaddleOCRConfig config,
                                             EngineConfig engineConfig) {
  return create(std::move(config), engineConfig, defaultModels());
}

std::unique_ptr<PaddleOCR> PaddleOCR::create(PaddleOCRConfig config,
                                             EngineConfig engineConfig,
                                             const ModelBundle &models) {
  auto engine =
      std::make_unique<OCREngine>(std::move(config), engineConfig, models);
  return std::unique_ptr<PaddleOCR>(new PaddleOCR(std::move(engine)));
}

PaddleOCR::PaddleOCR(std::unique_ptr<OCREngine> engine)
    : engine_(std::move(engine)) {}

PaddleOCR::~PaddleOCR() { release(); }

OCRRunResult PaddleOCR::recognize(const cv::Mat &image) const {
  return engine_->run(image);
}

OCRRunResult
PaddleOCR::recognize(std::span<const std::uint8_t> imageBytes) const {
  return engine_->run(imageBytes);
}

void PaddleOCR::release() noexcept {
  if (engine_) {
    engine_->release();
  }
}

std::int64_t PaddleOCR::coldLoadTimeMs() const noexcept {
  return engine_ ? engine_->coldLoadTimeMs() : 0;
}

} // namespace paddle_ocr
