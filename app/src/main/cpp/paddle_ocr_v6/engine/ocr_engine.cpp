#include "engine/ocr_engine.h"

#include <algorithm>
#include <chrono>
#include <utility>
#include <vector>

#include "model/model_config.h"
#include "postprocess/box_sorter.h"
#include "postprocess/quad_text_crop.h"
#include "util/image_utils.h"

namespace paddle_ocr {
namespace {

using Clock = std::chrono::steady_clock;

std::int64_t elapsedMilliseconds(Clock::time_point start) {
  return std::chrono::duration_cast<std::chrono::milliseconds>(Clock::now() -
                                                               start)
      .count();
}

OCREngineResult emptyResult(const DetectionResult &detection,
                            std::int64_t totalTimeMs,
                            std::int64_t coldLoadTimeMs) {
  OCREngineResult result;
  result.detectionTimeMs = detection.timeMs;
  result.totalTimeMs = totalTimeMs;
  result.detPreprocessMs = detection.preprocessMs;
  result.detInferenceMs = detection.inferenceMs;
  result.detPostprocessMs = detection.postprocessMs;
  result.detInputShape = detection.inputShape;
  result.coldLoadTimeMs = coldLoadTimeMs;
  return result;
}

} // namespace

OCREngine::OCREngine(PaddleOCRConfig config, const EngineConfig &engineConfig,
                     const ModelBundle &models)
    : config_(std::move(config)), sessionManager_(engineConfig) {
  sessionManager_.loadModels(models);
  const ModelConfig modelConfig = ModelConfig::parse(models.recognitionConfig);
  detectionEngine_ =
      std::make_unique<DetectionEngine>(sessionManager_, config_);
  recognitionEngine_ = std::make_unique<RecognitionEngine>(
      sessionManager_, modelConfig.characterList);
}

OCREngine::~OCREngine() { release(); }

OCREngineResult OCREngine::run(const cv::Mat &image) const {
  const cv::Mat bgr = BitmapUtils::toBGR(image);
  return runBGR(bgr);
}

OCREngineResult OCREngine::run(std::span<const std::uint8_t> imageBytes) const {
  const cv::Mat bgr = BitmapUtils::imdecodeBGR(imageBytes);
  return runBGR(bgr);
}

OCREngineResult OCREngine::runBGR(const cv::Mat &image) const {
  const auto totalStart = Clock::now();
  const DetectionResult detection = detectionEngine_->detect(image);
  if (detection.boxes.empty()) {
    return emptyResult(detection, elapsedMilliseconds(totalStart),
                       sessionManager_.coldLoadTimeMs());
  }

  const std::vector<OCRBox> sortedBoxes =
      BoxSorter::sortInReadingOrder(detection.boxes);
  const int batchSize = std::max(config_.recBatchSize, 1);
  std::int64_t recognitionPreprocessMs = 0;
  std::int64_t recognitionInferenceMs = 0;
  std::int64_t recognitionPostprocessMs = 0;
  std::int64_t recognitionTimeMs = 0;
  std::vector<OCRResult> results;
  std::vector<std::vector<std::int32_t>> inputShapes;
  std::vector<std::int64_t> perLineTimes;

  std::size_t index = 0;
  while (index < sortedBoxes.size()) {
    std::vector<cv::Mat> crops;
    std::vector<std::size_t> boxIndices;
    std::size_t next = index;
    while (next < sortedBoxes.size() &&
           crops.size() < static_cast<std::size_t>(batchSize)) {
      cv::Mat crop = QuadTextCrop::crop(image, sortedBoxes[next]);
      if (!crop.empty() && crop.rows > 0 && crop.cols > 0) {
        crops.push_back(std::move(crop));
        boxIndices.push_back(next);
      }
      ++next;
    }

    if (!crops.empty()) {
      RecognitionResult recognition = recognitionEngine_->recognize(crops);
      recognitionPreprocessMs += recognition.preprocessMs;
      recognitionInferenceMs += recognition.inferenceMs;
      recognitionPostprocessMs += recognition.postprocessMs;
      recognitionTimeMs += recognition.timeMs;
      inputShapes.push_back(std::move(recognition.inputShape));
      if (batchSize == 1) {
        perLineTimes.push_back(recognition.timeMs);
      }

      const std::size_t recognizedCount =
          std::min(recognition.texts.size(), boxIndices.size());
      for (std::size_t resultIndex = 0; resultIndex < recognizedCount;
           ++resultIndex) {
        auto &[text, confidence] = recognition.texts[resultIndex];
        if (confidence < config_.recScoreThresh) {
          continue;
        }
        results.push_back(OCRResult{
            .box = sortedBoxes[boxIndices[resultIndex]],
            .text = std::move(text),
            .confidence = confidence,
            .wordBoxes = std::nullopt,
        });
      }
    }
    index = next;
  }

  const std::int64_t totalTimeMs = elapsedMilliseconds(totalStart);
  OCREngineResult result;
  result.results = std::move(results);
  result.detectionTimeMs = detection.timeMs;
  result.recognitionTimeMs = recognitionTimeMs;
  result.totalTimeMs = totalTimeMs;
  result.lineCount = static_cast<std::int32_t>(result.results.size());
  result.detPreprocessMs = detection.preprocessMs;
  result.detInferenceMs = detection.inferenceMs;
  result.detPostprocessMs = detection.postprocessMs;
  result.recPreprocessMs = recognitionPreprocessMs;
  result.recInferenceMs = recognitionInferenceMs;
  result.recPostprocessMs = recognitionPostprocessMs;
  result.pipelineOverheadMs =
      totalTimeMs - detection.timeMs - recognitionTimeMs;
  result.coldLoadTimeMs = sessionManager_.coldLoadTimeMs();
  result.detInputShape = detection.inputShape;
  result.recInputShapes = std::move(inputShapes);
  result.perLineRecMs = std::move(perLineTimes);
  return result;
}

void OCREngine::release() noexcept { sessionManager_.release(); }

} // namespace paddle_ocr
