#include <string>
#include <thread>

#include <jni.h>
#include <android/log.h>

#include <opencv2/opencv.hpp>
#include <onnxruntime_cxx_api.h>

#include "ImageDecoder.h"
#include "Shell.h"

cv::Mat preprocess(const cv::Mat& img, int tar_w = 960, int tar_h = 960) {
    // 1. Resize
    cv::Mat resized_img;
    cv::resize(img, resized_img, cv::Size(tar_w, tar_h));

    // 2. Normalize
    resized_img.convertTo(resized_img, CV_32F, 1.0 / 255.0);

    std::vector<float> mean = {0.485, 0.456, 0.406};
    std::vector<float> std = {0.229, 0.224, 0.225};

    // Normalize each channel
    for (int c = 0; c < resized_img.channels(); ++c) {
        cv::Mat channel;
        cv::extractChannel(resized_img, channel, c);
        channel = (channel - mean[c]) / std[c];
        cv::insertChannel(channel, resized_img, c);
    }

    // 3. Transpose to BCHW
    cv::Mat transposed_img;
    cv::transpose(resized_img, transposed_img);

    // Add batch dimension
    std::vector<cv::Mat> channels;
    for (int c = 0; c < transposed_img.channels(); ++c) {
        channels.push_back(transposed_img.row(c));
    }

    cv::Mat batched_img;
    cv::merge(channels, batched_img);

    return batched_img;
}

std::vector<float> preprocess2(const cv::Mat& source, int tar_w = 960, int tar_h = 960) {
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
    return mat_data;
}

std::string getMatShape(const cv::Mat& mat) {
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
        cv::Mat img = cv::imread("/sdcard/Download/model_test.png");
        cv::cvtColor(img, img, cv::COLOR_BGR2BGRA);
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND",
                            "image: width:%d, height:%d, channels:%d, shape:%s",
                            img.cols, img.rows, img.channels(), getMatShape(img).c_str());

        // Preprocess image
        std::vector<float> processed_data = preprocess2(img);
//        __android_log_print(ANDROID_LOG_ERROR, "COMMAND",
//                            "preprocess: width:%d, height:%d, channels:%d, shape:%s",
//                            preprocessed_img.cols, preprocessed_img.rows,
//                            preprocessed_img.channels(), getMatShape(preprocessed_img).c_str());

        // 创建 ONNX Runtime 环境
        Ort::Env env(ORT_LOGGING_LEVEL_WARNING, "test");

        // 创建 ONNX Runtime 会话
        Ort::SessionOptions session_options;
        Ort::Session session(env, "/sdcard/Download/det.onnx", session_options);

        // 获取输入和输出的名称和维度
        Ort::AllocatorWithDefaultOptions allocator;
        Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(OrtDeviceAllocator, OrtMemTypeDefault);

        std::vector<const char*> input_node_names = {"x"};
        std::vector<const char*> output_node_names = {"sigmoid_0.tmp_0"};

        // 创建输入张量
        // 动态维度的具体值
        int64_t dynamic_dim_0 = 1;  // 例如，p2o.DynamicDimension.0 = 2
        int64_t dynamic_dim_1 = 960;  // 例如，p2o.DynamicDimension.1 = 4
        int64_t dynamic_dim_2 = 960;  // 例如，p2o.DynamicDimension.2 = 5

        // 创建输入张量
        //std::vector<float> input_data(dynamic_dim_0 * 3 * dynamic_dim_1 * dynamic_dim_2);
        std::vector<float> input_data(processed_data.begin(), processed_data.end());
        std::vector<int64_t> input_shape = {dynamic_dim_0, 3, dynamic_dim_1, dynamic_dim_2};

        Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
                memory_info,
                input_data.data(), input_data.size(),
                input_shape.data(), input_shape.size()
        );

        // 运行模型
        std::vector<Ort::Value> output_tensors = session.Run(
                Ort::RunOptions{nullptr},
                input_node_names.data(), &input_tensor, 1,
                output_node_names.data(), 1
        );

        // 获取输出张量
        float* output_data = output_tensors[0].GetTensorMutableData<float>();
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
    }).detach();
}
