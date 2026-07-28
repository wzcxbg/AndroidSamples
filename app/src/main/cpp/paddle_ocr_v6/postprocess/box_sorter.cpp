#include "postprocess/box_sorter.h"

#include <algorithm>
#include <cmath>

namespace paddle_ocr {

std::vector<OCRBox>
BoxSorter::sortInReadingOrder(const std::vector<OCRBox> &boxes) {
  if (boxes.size() <= 1) {
    return boxes;
  }

  std::vector<OCRBox> sorted = boxes;
  std::stable_sort(sorted.begin(), sorted.end(),
                   [](const OCRBox &left, const OCRBox &right) {
                     if (left.points[0].y == right.points[0].y) {
                       return left.points[0].x < right.points[0].x;
                     }
                     return left.points[0].y < right.points[0].y;
                   });

  for (std::size_t index = 0; index + 1 < sorted.size(); ++index) {
    std::size_t current = index;
    while (true) {
      const OCRBox &left = sorted[current];
      const OCRBox &right = sorted[current + 1];
      if (std::abs(right.points[0].y - left.points[0].y) >= 10.0F ||
          right.points[0].x >= left.points[0].x) {
        break;
      }
      std::swap(sorted[current], sorted[current + 1]);
      if (current == 0) {
        break;
      }
      --current;
    }
  }
  return sorted;
}

} // namespace paddle_ocr
