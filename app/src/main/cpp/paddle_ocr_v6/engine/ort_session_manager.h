#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <string_view>
#include <vector>

#include <onnxruntime_cxx_api.h>

#include "config.h"
#include "model/model_bundle.h"

namespace paddle_ocr {

struct TensorOutput {
  std::vector<float> data;
  std::vector<std::int64_t> shape;
};

class ORTSessionManager final {
public:
  explicit ORTSessionManager(const EngineConfig &config);
  ~ORTSessionManager();

  ORTSessionManager(const ORTSessionManager &) = delete;
  ORTSessionManager &operator=(const ORTSessionManager &) = delete;

  void loadModels(const ModelBundle &models);
  TensorOutput runDetection(const std::vector<float> &input,
                            const std::vector<std::int64_t> &shape) const;
  TensorOutput runRecognition(const std::vector<float> &input,
                              const std::vector<std::int64_t> &shape) const;
  void release() noexcept;

  [[nodiscard]] std::int64_t coldLoadTimeMs() const noexcept {
    return coldLoadTimeMs_;
  }

private:
  TensorOutput runSession(Ort::Session &session, std::string_view inputName,
                          const std::vector<float> &input,
                          const std::vector<std::int64_t> &shape,
                          std::string_view stage) const;

  EngineConfig config_;
  Ort::Env environment_;
  std::unique_ptr<Ort::Session> detectionSession_;
  std::unique_ptr<Ort::Session> recognitionSession_;
  std::string detectionInputName_ = "x";
  std::string recognitionInputName_ = "x";
  std::int64_t coldLoadTimeMs_ = 0;
};

} // namespace paddle_ocr
