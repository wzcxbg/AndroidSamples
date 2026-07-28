#pragma once

#include <cstdint>
#include <string>

namespace paddle_ocr {

struct PaddleOCRConfig {
  std::string detImgMode = "BGR";
  std::int32_t detLimitSideLen = 64;
  std::string detLimitType = "min";
  std::int32_t detMaxSideLimit = 4000;
  float detThresh = 0.3F;
  float detBoxThresh = 0.6F;
  float detUnclipRatio = 1.5F;
  std::int32_t detMaxCandidates = 3000;
  bool detUseDilation = false;
  std::string detScoreMode = "fast";
  std::string detBoxType = "quad";
  float recScoreThresh = 0.0F;
  std::int32_t recBatchSize = 1;
};

struct EngineConfig {
  std::int32_t numThreads = 4;
};

} // namespace paddle_ocr
