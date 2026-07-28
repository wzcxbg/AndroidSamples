#include "engine/ort_session_manager.h"

#include <chrono>
#include <functional>
#include <numeric>
#include <string>

#include "model/types.h"

namespace paddle_ocr {
namespace {

using Clock = std::chrono::steady_clock;

std::int64_t elapsedMilliseconds(Clock::time_point start) {
  return std::chrono::duration_cast<std::chrono::milliseconds>(Clock::now() -
                                                               start)
      .count();
}

std::string firstInputName(const Ort::Session &session) {
  Ort::AllocatorWithDefaultOptions allocator;
  const auto name = session.GetInputNameAllocated(0, allocator);
  return name.get();
}

} // namespace

ORTSessionManager::ORTSessionManager(const EngineConfig &config)
    : config_(config),
      environment_(ORT_LOGGING_LEVEL_WARNING, "paddle_ocr_v6") {}

ORTSessionManager::~ORTSessionManager() { release(); }

void ORTSessionManager::loadModels(const ModelBundle &models) {
  if (models.detectionModel.empty()) {
    throw OCRError(OCRErrorCode::ModelNotFound, "Model not found: detection");
  }
  if (models.recognitionModel.empty()) {
    throw OCRError(OCRErrorCode::ModelNotFound, "Model not found: recognition");
  }

  const auto start = Clock::now();
  Ort::SessionOptions options;
  options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);
  options.SetIntraOpNumThreads(config_.numThreads);

  try {
    detectionSession_ = std::make_unique<Ort::Session>(
        environment_, models.detectionModel.data(),
        models.detectionModel.size(), options);
  } catch (const Ort::Exception &exception) {
    throw OCRError(OCRErrorCode::ModelLoadFailed,
                   "Failed to load detection model: " +
                       std::string(exception.what()));
  }

  try {
    recognitionSession_ = std::make_unique<Ort::Session>(
        environment_, models.recognitionModel.data(),
        models.recognitionModel.size(), options);
  } catch (const Ort::Exception &exception) {
    detectionSession_.reset();
    throw OCRError(OCRErrorCode::ModelLoadFailed,
                   "Failed to load recognition model: " +
                       std::string(exception.what()));
  }

  try {
    detectionInputName_ = firstInputName(*detectionSession_);
    recognitionInputName_ = firstInputName(*recognitionSession_);
  } catch (const Ort::Exception &exception) {
    release();
    throw OCRError(OCRErrorCode::ModelLoadFailed,
                   "Failed to inspect OCR model inputs: " +
                       std::string(exception.what()));
  }
  coldLoadTimeMs_ = elapsedMilliseconds(start);
}

TensorOutput
ORTSessionManager::runDetection(const std::vector<float> &input,
                                const std::vector<std::int64_t> &shape) const {
  if (!detectionSession_) {
    throw OCRError(
        OCRErrorCode::ModelLoadFailed,
        "Failed to load detection model: session is not initialized");
  }
  return runSession(*detectionSession_, detectionInputName_, input, shape,
                    "detection");
}

TensorOutput ORTSessionManager::runRecognition(
    const std::vector<float> &input,
    const std::vector<std::int64_t> &shape) const {
  if (!recognitionSession_) {
    throw OCRError(
        OCRErrorCode::ModelLoadFailed,
        "Failed to load recognition model: session is not initialized");
  }
  return runSession(*recognitionSession_, recognitionInputName_, input, shape,
                    "recognition");
}

TensorOutput
ORTSessionManager::runSession(Ort::Session &session, std::string_view inputName,
                              const std::vector<float> &input,
                              const std::vector<std::int64_t> &shape,
                              std::string_view stage) const {
  try {
    const Ort::MemoryInfo memoryInfo =
        Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
    Ort::Value tensor = Ort::Value::CreateTensor<float>(
        memoryInfo, const_cast<float *>(input.data()), input.size(),
        shape.data(), shape.size());
    const char *inputNames[] = {inputName.data()};
    Ort::AllocatorWithDefaultOptions allocator;
    const auto outputName = session.GetOutputNameAllocated(0, allocator);
    const char *outputNames[] = {outputName.get()};
    auto outputs = session.Run(Ort::RunOptions{nullptr}, inputNames, &tensor, 1,
                               outputNames, 1);
    if (outputs.empty() || !outputs.front().IsTensor()) {
      throw OCRError(OCRErrorCode::InferenceFailed,
                     "Inference failed at stage '" + std::string(stage) +
                         "': no output tensor found");
    }

    const auto tensorInfo = outputs.front().GetTensorTypeAndShapeInfo();
    std::vector<std::int64_t> outputShape = tensorInfo.GetShape();
    const std::size_t elementCount = tensorInfo.GetElementCount();
    const float *outputData = outputs.front().GetTensorData<float>();
    return TensorOutput{
        .data = std::vector<float>(outputData, outputData + elementCount),
        .shape = std::move(outputShape),
    };
  } catch (const Ort::Exception &exception) {
    throw OCRError(OCRErrorCode::InferenceFailed,
                   "Inference failed at stage '" + std::string(stage) +
                       "': " + exception.what());
  }
}

void ORTSessionManager::release() noexcept {
  recognitionSession_.reset();
  detectionSession_.reset();
}

} // namespace paddle_ocr
