#include "postprocess/ctc_decoder.h"

#include <algorithm>
#include <cstddef>
#include <stdexcept>

namespace paddle_ocr {

std::vector<std::pair<std::string, float>>
CTCDecoder::decode(const std::vector<float> &output,
                   const std::vector<std::int64_t> &shape,
                   const std::vector<std::string> &characterList) {
  if (shape.size() != 3) {
    throw std::invalid_argument("Recognition output must have shape [N, T, C]");
  }
  const std::size_t batchSize = static_cast<std::size_t>(shape[0]);
  const std::size_t timeSteps = static_cast<std::size_t>(shape[1]);
  const std::size_t classCount = static_cast<std::size_t>(shape[2]);
  if (output.size() != batchSize * timeSteps * classCount || classCount == 0) {
    throw std::invalid_argument(
        "Recognition output size does not match its shape");
  }

  std::vector<std::pair<std::string, float>> results;
  results.reserve(batchSize);
  for (std::size_t batch = 0; batch < batchSize; ++batch) {
    std::string text;
    float probabilitySum = 0.0F;
    std::size_t keptCount = 0;
    std::size_t previousIndex = classCount;
    for (std::size_t time = 0; time < timeSteps; ++time) {
      const std::size_t offset = (batch * timeSteps + time) * classCount;
      const auto begin = output.begin() + static_cast<std::ptrdiff_t>(offset);
      const auto maximum = std::max_element(
          begin, begin + static_cast<std::ptrdiff_t>(classCount));
      const std::size_t index =
          static_cast<std::size_t>(std::distance(begin, maximum));
      if (index != 0 && index != previousIndex) {
        const std::size_t characterIndex = index - 1;
        if (characterIndex < characterList.size()) {
          text += characterList[characterIndex];
          probabilitySum += *maximum;
          ++keptCount;
        }
      }
      previousIndex = index;
    }
    const float confidence = keptCount == 0 ? 0.0F : probabilitySum / keptCount;
    results.emplace_back(std::move(text), confidence);
  }
  return results;
}

} // namespace paddle_ocr
