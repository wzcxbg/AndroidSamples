#pragma once

#include <vector>
#include <fstream>

#include <memory>
#include <functional>

#include <jni.h>


struct Color {
    uint8_t r, g, b, a;
};

enum class BitmapFmt {
    BGRA,
};

class Bitmap final {
    std::vector<uint8_t> pixels;

public:
    int width;
    int height;
    BitmapFmt format;
    uint8_t *data;

    explicit Bitmap(int width, int height,
                    BitmapFmt format = BitmapFmt::BGRA)
            : width(width), height(height), format(format) {
        pixels.resize(width * height * 4);
        data = pixels.data();
    }

    void FillPixels(void *srcData, int srcRowPitch) {
        uint8_t *dstData = pixels.data();
        int dstRowPitch = width * 4;
        for (int y = 0; y < height; y++) {
            std::copy_n(static_cast<uint8_t *>(srcData) + y * srcRowPitch,
                        dstRowPitch,
                        dstData + y * dstRowPitch);
        }
    }

    Color GetColor(int x, int y) {
        uint8_t *colorData = data + (y * width * 4) + (x * 4);
        const uint8_t b = colorData[0];
        const uint8_t g = colorData[1];
        const uint8_t r = colorData[2];
        const uint8_t a = colorData[3];
        return Color{r, g, b, a};
    }

    void SetColor(int x, int y, Color color) {
        uint8_t *colorData = data + (y * width * 4) + (x * 4);
        colorData[0] = color.b;
        colorData[1] = color.g;
        colorData[2] = color.r;
        colorData[3] = color.a;
    }

    void WriteToFile(std::string &&filePath) {
        std::ofstream file(filePath, std::ios::binary);
        file.write(reinterpret_cast<char *>(&pixels[0]),
                   width * height * 4);
    }
};


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

private:
    static std::unique_ptr<JNIEnv, std::function<void(JNIEnv *)>> GetAutoDetachJniEnv(JavaVM *jvm) {
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
};