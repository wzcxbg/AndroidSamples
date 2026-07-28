#pragma once

#include <string>
#include <string_view>
#include <vector>

namespace paddle_ocr {

struct ModelConfig {
  std::vector<std::string> characterList;

  static ModelConfig parse(std::string_view content);
};

} // namespace paddle_ocr
