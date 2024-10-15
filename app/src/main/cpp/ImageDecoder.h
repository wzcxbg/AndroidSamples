#pragma once

#include <memory>
#include <functional>

#include <jni.h>

#include "Bitmap.h"


std::unique_ptr<JNIEnv, std::function<void(JNIEnv *)>> GetAutoDetachJniEnv(JavaVM *jvm) {
    jboolean isAttachedNewThread = JNI_FALSE;
    JNIEnv *env = nullptr;
    if (jvm->GetEnv(reinterpret_cast<void **>(&env),
                    JNI_VERSION_1_6) != JNI_OK) {
        if (jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return nullptr;
        }
        isAttachedNewThread = JNI_TRUE;
    }
    return {env, [=](JNIEnv *) {
        if (isAttachedNewThread) {
            jvm->DetachCurrentThread();
        }
    }};
}


class ImageDecoder {
    JavaVM *jvm;
public:
    explicit ImageDecoder(JavaVM *jvm) : jvm(jvm) {}

    std::unique_ptr<Bitmap> decodeBuffer(void *imageData, size_t imageDataByteSize) {
        auto env = GetAutoDetachJniEnv(jvm);

        //BitmapFactory.decodeByteArray()
        jbyteArray pngData = env->NewByteArray(jsize(imageDataByteSize));
        env->SetByteArrayRegion(pngData, 0, jsize(imageDataByteSize),
                                reinterpret_cast<const jbyte *>(imageData));
        jmethodID decodeByteArrayMid = env->GetStaticMethodID(
                env->FindClass("android/graphics/BitmapFactory"),
                "decodeByteArray", "([BII)Landroid/graphics/Bitmap;");
        jobject bitmapObj = env->CallStaticObjectMethod(
                env->FindClass("android/graphics/BitmapFactory"),
                decodeByteArrayMid, pngData, 0, env->GetArrayLength(pngData));
        if (env->IsSameObject(bitmapObj, nullptr)) {
            return nullptr;
        }

        //Bitmap.getConfig()
        jmethodID getWidthMid = env->GetMethodID(
                env->FindClass("android/graphics/Bitmap"), "getWidth", "()I");
        jmethodID getHeightMid = env->GetMethodID(
                env->FindClass("android/graphics/Bitmap"), "getHeight", "()I");
        jmethodID getConfigMid = env->GetMethodID(
                env->FindClass("android/graphics/Bitmap"), "getConfig",
                "()Landroid/graphics/Bitmap$Config;");
        jint width = env->CallIntMethod(bitmapObj, getWidthMid);
        jint height = env->CallIntMethod(bitmapObj, getHeightMid);
        jobject config = env->CallObjectMethod(bitmapObj, getConfigMid);

        jfieldID configFid = env->GetStaticFieldID(
                env->FindClass("android/graphics/Bitmap$Config"),
                "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
        jobject argb8888 = env->GetStaticObjectField(
                env->FindClass("android/graphics/Bitmap$Config"),
                configFid);
        if (!env->IsSameObject(config, argb8888)) {
            return nullptr;
        }

        //Bitmap.getPixels()
        jintArray bitmapData = env->NewIntArray(width * height);
        jmethodID getPixelsMid = env->GetMethodID(
                env->FindClass("android/graphics/Bitmap"), "getPixels", "([IIIIIII)V");
        env->CallVoidMethod(bitmapObj, getPixelsMid, bitmapData,
                            0, width, 0, 0, width, height);

        //Read As Native Bitmap
        auto bitmap = std::make_unique<Bitmap>(width, height);
        env->GetIntArrayRegion(bitmapData, 0, width * height,
                               reinterpret_cast<jint *>(bitmap->data));

        //Bitmap.recycle()
        jmethodID recycleMid = env->GetMethodID(
                env->FindClass("android/graphics/Bitmap"), "recycle", "()V");
        env->CallVoidMethod(bitmapObj, recycleMid);

        return bitmap;
    }
};