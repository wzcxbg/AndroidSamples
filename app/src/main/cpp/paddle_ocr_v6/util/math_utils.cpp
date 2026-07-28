#include "util/math_utils.h"

#include <cmath>

namespace paddle_ocr {

int MathUtils::roundHalfToEven(double value) noexcept {
  const double floored = std::floor(value);
  const double difference = value - floored;
  const int integer = static_cast<int>(floored);
  if (difference < 0.5) {
    return integer;
  }
  if (difference > 0.5) {
    return integer + 1;
  }
  return integer % 2 == 0 ? integer : integer + 1;
}

} // namespace paddle_ocr
