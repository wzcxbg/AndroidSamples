#include "engine/recognition_engine.h"

#include <chrono>
#include <utility>

#include "postprocess/ctc_decoder.h"
#include "preprocess/rec_preprocessor.h"

namespace paddle_ocr {
namespace {

using Clock = std::chrono::steady_clock;

std::int64_t elapsedMilliseconds(Clock::time_point start) {
  return std::chrono::duration_cast<std::chrono::milliseconds>(Clock::now() -
                                                               start)
      .count();
}

} // namespace

RecognitionEngine::RecognitionEngine(ORTSessionManager &sessionManager,
                                     std::vector<std::string> characterList)
    : sessionManager_(sessionManager),
      characterList_(std::move(characterList)) {}

RecognitionResult
RecognitionEngine::recognize(const std::vector<cv::Mat> &crops) const {
  const auto preprocessStart = Clock::now();
  RecPreprocessResult preprocessResult =
      RecPreprocessor::preprocessBatch(crops);
  const std::int64_t preprocessMs = elapsedMilliseconds(preprocessStart);

  const auto inferenceStart = Clock::now();
  TensorOutput output = sessionManager_.runRecognition(
      preprocessResult.tensorData, preprocessResult.shape);
  const std::int64_t inferenceMs = elapsedMilliseconds(inferenceStart);

  const auto postprocessStart = Clock::now();
  auto decoded = CTCDecoder::decode(output.data, output.shape, characterList_);
  const std::int64_t postprocessMs = elapsedMilliseconds(postprocessStart);

  std::vector<std::int32_t> inputShape;
  inputShape.reserve(preprocessResult.shape.size());
  for (const std::int64_t dimension : preprocessResult.shape) {
    inputShape.push_back(static_cast<std::int32_t>(dimension));
  }
  return RecognitionResult{
      .texts = std::move(decoded),
      .preprocessMs = preprocessMs,
      .inferenceMs = inferenceMs,
      .postprocessMs = postprocessMs,
      .timeMs = preprocessMs + inferenceMs + postprocessMs,
      .inputShape = std::move(inputShape),
  };
}

} // namespace paddle_ocr
