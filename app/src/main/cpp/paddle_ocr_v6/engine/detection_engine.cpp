#include "engine/detection_engine.h"

#include <chrono>
#include <utility>

#include "postprocess/db_post_processor.h"
#include "preprocess/det_preprocessor.h"

namespace paddle_ocr {
namespace {

using Clock = std::chrono::steady_clock;

std::int64_t elapsedMilliseconds(Clock::time_point start) {
  return std::chrono::duration_cast<std::chrono::milliseconds>(Clock::now() -
                                                               start)
      .count();
}

} // namespace

DetectionEngine::DetectionEngine(ORTSessionManager &sessionManager,
                                 PaddleOCRConfig config)
    : sessionManager_(sessionManager), config_(std::move(config)) {}

DetectionResult DetectionEngine::detect(const cv::Mat &image) const {
  const auto preprocessStart = Clock::now();
  DetPreprocessResult preprocessResult = DetPreprocessor::preprocess(
      image, config_.detLimitSideLen, config_.detLimitType,
      config_.detMaxSideLimit, config_.detImgMode);
  const std::int64_t preprocessMs = elapsedMilliseconds(preprocessStart);

  const auto inferenceStart = Clock::now();
  TensorOutput output = sessionManager_.runDetection(
      preprocessResult.tensorData, preprocessResult.shape);
  const std::int64_t inferenceMs = elapsedMilliseconds(inferenceStart);

  const auto postprocessStart = Clock::now();
  std::vector<OCRBox> boxes = DBPostProcessor::process(
      output.data, output.shape,
      DBPostProcessParams{
          .threshold = config_.detThresh,
          .boxThreshold = config_.detBoxThresh,
          .unclipRatio = config_.detUnclipRatio,
          .maximumCandidates = config_.detMaxCandidates,
          .useDilation = config_.detUseDilation,
          .scoreMode = config_.detScoreMode,
          .boxType = config_.detBoxType,
      },
      cv::Size(preprocessResult.originalWidth,
               preprocessResult.originalHeight));
  const std::int64_t postprocessMs = elapsedMilliseconds(postprocessStart);

  return DetectionResult{
      .boxes = std::move(boxes),
      .preprocessMs = preprocessMs,
      .inferenceMs = inferenceMs,
      .postprocessMs = postprocessMs,
      .timeMs = preprocessMs + inferenceMs + postprocessMs,
      .inputShape =
          {
              1,
              3,
              static_cast<std::int32_t>(preprocessResult.shape[2]),
              static_cast<std::int32_t>(preprocessResult.shape[3]),
          },
  };
}

} // namespace paddle_ocr
