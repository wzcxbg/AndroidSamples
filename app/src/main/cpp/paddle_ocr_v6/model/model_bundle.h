#pragma once

#include <cstdint>
#include <span>
#include <string_view>

namespace paddle_ocr {

struct ModelBundle {
  std::span<const std::uint8_t> detectionModel;
  std::span<const std::uint8_t> recognitionModel;
  std::string_view recognitionConfig;
};

} // namespace paddle_ocr
