#include <span>
#include <string>
#include <thread>

#include <jni.h>

#include <onnxruntime_cxx_api.h>
#include <opencv2/opencv.hpp>

#include "image_decoder.h"
#include "paddle_ocr_v6/paddle_ocr.h"
#include "root_shell.h"
#include "simple_logger.h"

namespace {

void logPaddleOCRResult(const paddle_ocr::OCRRunResult &runResult) {
  for (std::size_t index = 0; index < runResult.results.size(); ++index) {
    const paddle_ocr::OCRResult &result = runResult.results[index];
    logger::error("ocr: index:{} p0:({},{}) p1:({},{}) p2:({},{}) p3:({},{}) "
                  "text:{} confidence:{}",
                  index, result.box.points[0].x, result.box.points[0].y,
                  result.box.points[1].x, result.box.points[1].y,
                  result.box.points[2].x, result.box.points[2].y,
                  result.box.points[3].x, result.box.points[3].y, result.text,
                  result.confidence);
  }
  logger::error("ocr timing: load:{} total:{} det:{} (pre:{} infer:{} post:{}) "
                "rec:{} (pre:{} infer:{} post:{}) overhead:{} lines:{}",
                runResult.coldLoadTimeMs, runResult.totalTimeMs,
                runResult.detectionTimeMs, runResult.detPreprocessMs,
                runResult.detInferenceMs, runResult.detPostprocessMs,
                runResult.recognitionTimeMs, runResult.recPreprocessMs,
                runResult.recInferenceMs, runResult.recPostprocessMs,
                runResult.pipelineOverheadMs, runResult.lineCount);
}

template <class Image> void runPaddleOCRV6(const Image &image) {
  auto ocr = MeasureTime(paddle_ocr::PaddleOCR::create());
  const auto runResult = MeasureTime(ocr->recognize(image));
  logPaddleOCRResult(runResult);
  ocr->release();
}

} // namespace

void testPaddleOCRV6(std::span<const std::uint8_t> imageBytes) {
  runPaddleOCRV6(imageBytes);
}

void testPaddleOCRV6_2() {
  constexpr char ImagePath[] = "/sdcard/Download/1.jpg";
  const cv::Mat image = cv::imread(ImagePath, cv::IMREAD_COLOR);
  if (image.empty()) {
    logger::error("Failed to read OCR test image: {}", ImagePath);
    return;
  }

  logger::error("OCR test image: path:{} width:{} height:{} channels:{}",
                ImagePath, image.cols, image.rows, image.channels());
  runPaddleOCRV6(image);
}

extern "C" JNIEXPORT void JNICALL
Java_com_sliver_samples_MainActivity_screenCapture(JNIEnv *env, jobject thiz) {
  JavaVM *jvm;
  env->GetJavaVM(&jvm);
  std::thread([=]() {
    RootShell shell;
    std::string ret1 = shell.execute("screencap -p");
    logger::error("screencap -p bytes: {}", ret1.size());

    std::string ret2 = shell.execute("input tap 540 1000");
    logger::error("input tap 540 1000: {} {}", ret2.c_str(), ret2.size());

    ImageDecoder decoder(jvm);
    auto bitmap = decoder.decodeBuffer(ret1.data(), ret1.size());
    logger::error("ImageDecode: width:{} height:{}", bitmap->width,
                  bitmap->height);

    logger::error("OpenCV Version: {}", cv::getVersionString());

    logger::error("onnxruntime Version: {}", Ort::GetVersionString());

    const auto *bytes = reinterpret_cast<const std::uint8_t *>(ret1.data());
      testPaddleOCRV6_2();
  }).detach();
}
