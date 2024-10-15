#include <string>
#include <thread>

#include <jni.h>
#include <android/log.h>

#include <opencv2/opencv.hpp>
#include <onnxruntime_cxx_api.h>

#include "ImageDecoder.h"
#include "Shell.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_sliver_samples_MainActivity_screenCapture(JNIEnv *env, jobject thiz) {
    JavaVM *jvm;
    env->GetJavaVM(&jvm);
    std::thread([=]() {
        Shell shell;
        std::string ret1 = shell.execute("screencap -p");
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "ret1: %s %zu", ret1.c_str(), ret1.size());

        std::string ret2 = shell.execute("input tap 540 1000");
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "ret2: %s %zu", ret2.c_str(), ret2.size());

        ImageDecoder decoder(jvm);
        auto bitmap = decoder.decodeBuffer(ret1.data(), ret1.size());
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "ret3: %d %d", bitmap->width,
                            bitmap->height);

        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "ret4: OpenCV %s",
                            cv::getVersionString().c_str());

        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "ret5: onnxruntime %s",
                            Ort::GetVersionString().c_str());
    }).detach();
}
