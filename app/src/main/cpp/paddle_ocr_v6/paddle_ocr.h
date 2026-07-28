#pragma once

#include <cstdint>
#include <memory>
#include <span>

#include <opencv2/core/mat.hpp>

#include "config.h"
#include "model/model_bundle.h"
#include "model/types.h"

namespace paddle_ocr {

class OCREngine;

class PaddleOCR final {
public:
  static std::unique_ptr<PaddleOCR> create();
  static std::unique_ptr<PaddleOCR> create(PaddleOCRConfig config,
                                           EngineConfig engineConfig = {});
  static std::unique_ptr<PaddleOCR> create(PaddleOCRConfig config,
                                           EngineConfig engineConfig,
                                           const ModelBundle &models);

  ~PaddleOCR();

  PaddleOCR(const PaddleOCR &) = delete;
  PaddleOCR &operator=(const PaddleOCR &) = delete;

  OCRRunResult recognize(const cv::Mat &image) const;
  OCRRunResult recognize(std::span<const std::uint8_t> imageBytes) const;
  void release() noexcept;

  [[nodiscard]] std::int64_t coldLoadTimeMs() const noexcept;

private:
  explicit PaddleOCR(std::unique_ptr<OCREngine> engine);

  std::unique_ptr<OCREngine> engine_;
};

} // namespace paddle_ocr
