#include <string>
#include <thread>

#include <jni.h>

#include <opencv2/opencv.hpp>
#include <onnxruntime_cxx_api.h>

#include "simple_logger.h"
#include "root_shell.h"
#include "image_decoder.h"
#include "paddle_ocr.h"

void testOnnx2() {
    cv::Mat img = cv::imread("/sdcard/Download/1.jpg");
    logger::error("image: width:{}, height:{}, channels:{}",
                  img.cols, img.rows, img.channels());

    PPOCR ppocr = MeasureTime(PPOCR());
    auto ocrResults = MeasureTime(ppocr.ocr(img));
    for (int i = 0; i < ocrResults.size(); ++i) {
        auto &det_ret = ocrResults[i].det_ret;
        auto &cls_ret = ocrResults[i].cls_ret;
        auto &rec_ret = ocrResults[i].rec_ret;
        logger::error("dec: index:{} left:({},{}) top:({},{}) right:({},{}) bottom:({},{})", i,
                      det_ret.left().x, det_ret.left().y,
                      det_ret.top().x, det_ret.top().y,
                      det_ret.right().x, det_ret.right().y,
                      det_ret.bottom().x, det_ret.bottom().y);
        logger::error("cls: index:{} label:{} score:{}", i, cls_ret.label, cls_ret.score);
        logger::error("rec: index:{} text:{} score:{}", i, rec_ret.text, rec_ret.score);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_sliver_samples_MainActivity_screenCapture(JNIEnv *env, jobject thiz) {
    JavaVM *jvm;
    env->GetJavaVM(&jvm);
    std::thread([=]() {
        RootShell shell;
        std::string ret1 = shell.execute("screencap -p");
        logger::error("screencap -p: {} {}", ret1.c_str(), ret1.size());

        std::string ret2 = shell.execute("input tap 540 1000");
        logger::error("input tap 540 1000: {} {}", ret2.c_str(), ret2.size());

        ImageDecoder decoder(jvm);
        auto bitmap = decoder.decodeBuffer(ret1.data(), ret1.size());
        logger::error("ImageDecode: width:{} height:{}", bitmap->width, bitmap->height);

        logger::error("OpenCV Version: {}", cv::getVersionString());

        logger::error("onnxruntime Version: {}", Ort::GetVersionString());

        auto startTime = std::chrono::time_point_cast<std::chrono::milliseconds>(
                std::chrono::system_clock::now()).time_since_epoch().count();
        testOnnx2();
        auto endTime = std::chrono::time_point_cast<std::chrono::milliseconds>(
                std::chrono::system_clock::now()).time_since_epoch().count();
        logger::error("OCR elapsed time: {}", endTime - startTime);
    }).detach();
}
