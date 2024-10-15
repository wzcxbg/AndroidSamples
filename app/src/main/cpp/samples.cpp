#include <string>
#include <thread>

#include <jni.h>
#include <android/log.h>

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
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "ret1: %s %d", ret1.c_str(), ret1.size());

        std::string ret2 = shell.execute("input tap 540 1000");
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "ret2: %s %d", ret2.c_str(), ret2.size());

        ImageDecoder decoder(jvm);
        auto bitmap = decoder.decodeBuffer(ret1.data(), ret1.size());
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "ret3: %d %d", bitmap->width,
                            bitmap->height);
    }).detach();
}
