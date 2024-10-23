#include <string>
#include <thread>

#include <jni.h>
#include <android/log.h>

#include <opencv2/opencv.hpp>
#include <onnxruntime_cxx_api.h>

#include "ImageDecoder.h"
#include "Shell.h"

std::string getMatShape(const cv::Mat &mat) {
    std::ostringstream oss;
    oss << "Matrix dimensions: " << mat.dims;
    oss << " Matrix shape: ";
    for (int i = 0; i < mat.dims; ++i) {
        oss << mat.size[i];
        if (i < mat.dims - 1) {
            oss << " x ";
        }
    }
    return oss.str();
}

template<class T>
void printCompareMemory(T *data1, T *data2, size_t size,
                        bool printDiff, T printThreshold) {
    int cmp_ret = std::memcmp(data1, data2, size);
    std::ostringstream oss;
    oss.str("");
    oss << "result: " << cmp_ret;
    __android_log_print(ANDROID_LOG_ERROR, "compareMemory", "%s", oss.str().c_str());

    if (cmp_ret == 0) return;

    int same_count = 0;
    for (int i = 0; i < size; ++i) {
        if (data1[i] == data2[i]) {
            same_count++;
            continue;
        }
        if (printDiff && std::abs(data1[i] - data2[i]) > printThreshold) {
            oss.str("");
            oss << "data: " << data1[i] << " " << data2[i];
            __android_log_print(ANDROID_LOG_ERROR, "compareMemory", "%s", oss.str().c_str());
        }
    }
    oss.str("");
    oss << "same: " << same_count << " total: " << size;
    __android_log_print(ANDROID_LOG_ERROR, "compareMemory", "%s", oss.str().c_str());
}

std::vector<float> preprocess2(const cv::Mat &source, int tar_w = 960, int tar_h = 960) {
    auto startTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
    cv::Mat frame;
    int batchsize = 1;
    int net_w = tar_w;
    int net_h = tar_h;
    cv::cvtColor(source, frame, cv::COLOR_BGR2RGB);    // 通道转换
    cv::resize(frame, frame, cv::Size(net_w, net_h));  // resize
    std::vector<float> mat_data(batchsize * net_w * net_h * 3);
    int data_index = 0;
    // 开启图像预处理
    for(int i = 0; i < net_h; i++)
    {
        const uchar* current = frame.ptr<uchar>(i);                    // 指向每行首地址
        for(int j = 0; j < net_w; j++)
        {
            mat_data[data_index] = ((current[3*j + 0] / 255.0) - 0.485) / 0.229;                    // R
            mat_data[net_w*net_h + data_index] = ((current[3*j + 1] / 255.0) - 0.456) / 0.224;      // G
            mat_data[2*net_w * net_h + data_index] = ((current[3*j + 2] / 255.0) - 0.406) / 0.225;  // B
            data_index++;
        }
    }
    auto endTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "Et: %lld", endTime - startTime);
    return mat_data;
}


cv::Mat preprocess3(const cv::Mat &source, int tar_w = 960, int tar_h = 960) {
    auto startTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();

    cv::Mat frame = source.clone();

    std::vector<cv::Mat> channels;
    cv::split(frame, channels);
    std::reverse(channels.begin(), channels.end());
    std::vector<float> mean = {0.485, 0.456, 0.406};
    std::vector<float> std = {0.229, 0.224, 0.225};

    for (int i = 0; i < 3; ++i) {
        float meanVal = mean[i];
        float stdVal = std[i];
        mean[i] = 1.0f / 255.0f / stdVal;
        std[i] = -meanVal / stdVal;
    }
    std::vector<int> dims = {3, tar_h, tar_w};
    cv::Mat result(int(dims.size()), dims.data(), CV_32FC1);
    u_long channelBytes = tar_h * tar_w * result.elemSize();
    for (int i = 0; i < 3; ++i) {
        cv::Mat resultCh(tar_h, tar_w, CV_32FC1, result.data + channelBytes * i);
        channels[i].convertTo(channels[i], CV_32FC1, mean[i], +std[i]);
        cv::resize(channels[i], resultCh, {tar_w, tar_h});
    }
    auto endTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "Et: %lld", endTime - startTime);
    return result;
}


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

        auto startTime = std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::steady_clock::now().time_since_epoch()).count();

        // Read image
        cv::Mat img = cv::imread("/sdcard/Download/1.jpg");
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND",
                            "image: width:%d, height:%d, channels:%d, shape:%s",
                            img.cols, img.rows, img.channels(), getMatShape(img).c_str());

        // Preprocess image
        std::vector<float> processed_data2 = preprocess2(img);
        cv::Mat processed_data = preprocess3(img);
        printCompareMemory<float>(processed_data2.data(),
                                  reinterpret_cast<float *>(processed_data.data),
                                  processed_data.total(), false, 0.0000007);

        // 创建 ONNX Runtime 环境
        Ort::Env env(ORT_LOGGING_LEVEL_WARNING, "test");

        // 创建 ONNX Runtime 会话
        Ort::SessionOptions session_options;
        Ort::Session session(env, "/sdcard/Download/det.onnx", session_options);

        // 获取输入和输出的名称和维度
        Ort::AllocatorWithDefaultOptions allocator;
        Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(
                OrtDeviceAllocator, OrtMemTypeDefault);

        std::vector<const char *> input_node_names = {"x"};
        std::vector<const char *> output_node_names = {"sigmoid_0.tmp_0"};

        // 创建输入张量
        int64_t dynamic_dim_0 = 1;  // 例如，p2o.DynamicDimension.0 = 2
        int64_t dynamic_dim_1 = 960;  // 例如，p2o.DynamicDimension.1 = 4
        int64_t dynamic_dim_2 = 960;  // 例如，p2o.DynamicDimension.2 = 5

        // 创建输入张量
        //std::vector<float> input_data(dynamic_dim_0 * 3 * dynamic_dim_1 * dynamic_dim_2);
        std::vector<int64_t> input_shape = {dynamic_dim_0, 3, dynamic_dim_1, dynamic_dim_2};

        Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
                memory_info,
                reinterpret_cast<float *>(processed_data.data), processed_data.total(),
                input_shape.data(), input_shape.size()
        );

        // 运行模型
        std::vector<Ort::Value> output_tensors = session.Run(
                Ort::RunOptions{nullptr},
                input_node_names.data(), &input_tensor, 1,
                output_node_names.data(), 1
        );

        // 获取输出张量
        float *output_data = output_tensors[0].GetTensorMutableData<float>();
        std::vector<int64_t> output_shape = output_tensors[0].GetTensorTypeAndShapeInfo().GetShape();

        // 打印输出
        std::ostringstream oss;
        oss << "Output shape: [";
        for (size_t i = 0; i < output_shape.size(); ++i) {
            oss << output_shape[i];
            if (i < output_shape.size() - 1) oss << ", ";
        }
        oss << "]" << std::endl;

        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "%s", oss.str().c_str());
        oss.str("");

        oss << "Output data: [";
        for (size_t i = 0; i < output_shape[1]; ++i) {
            oss << output_data[i];
            if (i < output_shape[1] - 1) oss << ", ";
        }
        oss << "]" << std::endl;
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "%s", oss.str().c_str());

        auto endTime = std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::steady_clock::now().time_since_epoch()).count();

        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "Spent time: %lld", endTime - startTime);

        //结果输出
        cv::Mat gray(960, 960, CV_32FC1, output_data);
        cv::Mat result;
        gray.convertTo(result, -1, 255.0);
        cv::imwrite("/sdcard/Download/model_test_ret.png", result);
    }).detach();
}
