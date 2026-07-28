#pragma once

#include <opencv2/core/mat.hpp>

#include "model/types.h"

namespace paddle_ocr {

class QuadTextCrop final {
public:
  static cv::Mat crop(const cv::Mat &source, const OCRBox &box);
};

} // namespace paddle_ocr
