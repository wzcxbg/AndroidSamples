#pragma once

#include <cstddef>
#include <string_view>

namespace paddle_ocr {

class YamlUtils final {
public:
  static std::size_t leadingSpaces(std::string_view line) noexcept {
    const std::size_t position = line.find_first_not_of(' ');
    return position == std::string_view::npos ? line.size() : position;
  }
};

} // namespace paddle_ocr
