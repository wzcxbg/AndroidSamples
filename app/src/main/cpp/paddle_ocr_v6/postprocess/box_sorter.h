#pragma once

#include <vector>

#include "model/types.h"

namespace paddle_ocr {

class BoxSorter final {
public:
  static std::vector<OCRBox>
  sortInReadingOrder(const std::vector<OCRBox> &boxes);
};

} // namespace paddle_ocr
