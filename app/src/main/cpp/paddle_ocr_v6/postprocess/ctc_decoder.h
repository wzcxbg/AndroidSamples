#pragma once

#include <cstdint>
#include <string>
#include <utility>
#include <vector>

namespace paddle_ocr {

class CTCDecoder final {
public:
  static std::vector<std::pair<std::string, float>>
  decode(const std::vector<float> &output,
         const std::vector<std::int64_t> &shape,
         const std::vector<std::string> &characterList);
};

} // namespace paddle_ocr
