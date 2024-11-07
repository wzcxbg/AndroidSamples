#include <string>
#include <thread>

#include <jni.h>

#include <opencv2/opencv.hpp>
#include <onnxruntime_cxx_api.h>

#include "ImageDecoder.h"
#include "Shell.h"
#include "Logger.h"
#include "include/postprocess_op.h"
#include "include/preprocess_op.h"
#include "models.h"
#include "rar.hpp"
#include "Util.h"

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

std::string formatShape(std::vector<int64_t> shape) {
    std::ostringstream oss;
    oss << "[";
    for (size_t i = 0; i < shape.size(); ++i) {
        oss << shape[i];
        if (i < shape.size() - 1) oss << ", ";
    }
    oss << "]";
    return oss.str();
}

template<class T>
void printCompareMemory(T *data1, T *data2, size_t size,
                        bool printDiff, T printThreshold) {
    int cmp_ret = std::memcmp(data1, data2, size);
    log("result: {}", cmp_ret);

    if (cmp_ret == 0) return;

    int same_count = 0;
    for (int i = 0; i < size; ++i) {
        if (data1[i] == data2[i]) {
            same_count++;
            continue;
        }
        if (printDiff && std::abs(data1[i] - data2[i]) > printThreshold) {
            log("data: {} {}", data1[i], data2[i]);
        }
    }
    log("same: {} total: {}", same_count, size);
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
    for (int i = 0; i < net_h; i++) {
        const uchar *current = frame.ptr<uchar>(i);                    // 指向每行首地址
        for (int j = 0; j < net_w; j++) {
            mat_data[data_index] =
                    ((current[3 * j + 0] / 255.0) - 0.485) / 0.229;     // R
            mat_data[net_w * net_h + data_index] =
                    ((current[3 * j + 1] / 255.0) - 0.456) / 0.224;     // G
            mat_data[2 * net_w * net_h + data_index] =
                    ((current[3 * j + 2] / 255.0) - 0.406) / 0.225;     // B
            data_index++;
        }
    }
    auto endTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
    log("Elapsed time: {}", endTime - startTime);
    return mat_data;
}


cv::Mat preprocess3(const cv::Mat &image, int tar_w = 960, int tar_h = 960) {
    auto startTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
    //只支持BGR
    std::vector<cv::Mat> channels;
    cv::split(image, channels);
    std::reverse(channels.begin(), channels.end());
    std::vector<float> mean = {0.485, 0.456, 0.406};
    std::vector<float> std = {0.229, 0.224, 0.225};
    std::vector<float> alpha(3);
    std::vector<float> beta(3);
    for (int i = 0; i < 3; ++i) {
        alpha[i] = 1.0f / 255.0f / std[i];
        beta[i] = -mean[i] / std[i];
    }
    std::vector<int> dims = {3, tar_h, tar_w};
    cv::Mat result(int(dims.size()), dims.data(), CV_32FC1);
    u_long channelBytes = tar_h * tar_w * result.elemSize();
    for (int i = 0; i < 3; ++i) {
        cv::Mat resultCh(tar_h, tar_w, CV_32FC1, result.data + channelBytes * i);
        channels[i].convertTo(channels[i], CV_32FC1, alpha[i], +beta[i]);
        cv::resize(channels[i], resultCh, {tar_w, tar_h});
    }
    auto endTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
    log("Elapsed time: {}", endTime - startTime);
    return result;
}

void testOnnx() {
    auto startTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();

    // Read image
    cv::Mat img = cv::imread("/sdcard/Download/1.jpg");
    log("image: width:{}, height:{}, channels:{}, shape:{}",
        img.cols, img.rows, img.channels(), getMatShape(img));

    cv::Size targetSize{960, 960};

    // Preprocess image
    cv::Mat processed_data = preprocess3(img, targetSize.width, targetSize.height);

    // 创建 ONNX Runtime 环境
    Ort::Env env(ORT_LOGGING_LEVEL_WARNING, "test");

    // 创建 ONNX Runtime 会话
    Ort::SessionOptions session_options;
    Ort::Session session(env, det_onnx.data(), det_onnx.size_bytes(), session_options);

    // 获取输入和输出的名称和维度
    Ort::AllocatorWithDefaultOptions allocator;
    Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(
            OrtDeviceAllocator, OrtMemTypeDefault);

    std::vector<const char *> input_node_names = {"x"};
    std::vector<const char *> output_node_names = {"sigmoid_0.tmp_0"};

    // 创建输入张量
    int64_t dynamic_dim_0 = 1;  // 例如，p2o.DynamicDimension.0 = 2
    int64_t dynamic_dim_1 = targetSize.height;  // 例如，p2o.DynamicDimension.1 = 4
    int64_t dynamic_dim_2 = targetSize.width;  // 例如，p2o.DynamicDimension.2 = 5

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
    log("Output shape: {}", formatShape(output_shape));
    log("Output data: {}", output_data[0]);

    auto endTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
    log("Spent time: {}", endTime - startTime);

    //结果输出
    cv::Mat gray(targetSize.height, targetSize.width, CV_32FC1, output_data);
    cv::Mat result;
    gray.convertTo(result, -1, 255.0);
    cv::imwrite("/sdcard/Download/model_test_ret.png", result);
}

class TimeMeasurer {
    std::string tag;
    std::chrono::time_point<std::chrono::system_clock> startTime =
            std::chrono::system_clock::now();
public:
    explicit TimeMeasurer(const std::string &&tag) : tag(tag) {}
    ~TimeMeasurer(){
        auto totalTime = std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::system_clock::now() - startTime).count();
        log("{} elapsed time {}", tag, totalTime);
    };
};

void testOnnx2() {
    // Read image
    cv::Mat img = cv::imread("/sdcard/Download/1.jpg");
    log("image: width:{}, height:{}, channels:{}, shape:{}",
        img.cols, img.rows, img.channels(), getMatShape(img));


    // det
    std::vector<PaddleOCR::OCRPredictResult> ocr_results;
    TextDetector detector;
    {
        TimeMeasurer measurer("det:");
        detector.Run(img, ocr_results);
    }
    log("dec: {}", ocr_results.size());

    // crop image
    std::vector<cv::Mat> img_list;
    for (int j = 0; j < ocr_results.size(); j++) {
        cv::Mat crop_img;
        crop_img = PaddleOCR::Utility::GetRotateCropImage(img, ocr_results[j].box);
        img_list.push_back(crop_img);
    }


    // cls
    std::vector<int> cls_labels(img_list.size(), 0);
    std::vector<float> cls_scores(img_list.size(), 0);

    TextClassifier classifier;
    {
        TimeMeasurer measurer("cls:");
        classifier.Run(img_list, cls_labels, cls_scores);
    }

    for (int i = 0; i < cls_labels.size(); i++) {
        ocr_results[i].cls_label = cls_labels[i];
        ocr_results[i].cls_score = cls_scores[i];
        log("cls: index:{} label:{} score:{}", i, cls_labels[i], cls_scores[i]);
    }

    // rec
    std::vector<std::string> rec_texts(img_list.size(), "");
    std::vector<float> rec_text_scores(img_list.size(), 0);

    TextRecognizer recognizer;
    {
        TimeMeasurer measurer("rec:");
        recognizer.Run(img_list, rec_texts, rec_text_scores);
    }

    for (int i = 0; i < rec_texts.size(); i++) {
        ocr_results[i].text = rec_texts[i];
        ocr_results[i].score = rec_text_scores[i];
        log("rec: index:{} text:{} score:{}", i, rec_texts[i], rec_text_scores[i]);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_sliver_samples_MainActivity_screenCapture(JNIEnv *env, jobject thiz) {
    JavaVM *jvm;
    env->GetJavaVM(&jvm);
    std::thread([=]() {
        Shell shell;
        std::string ret1 = shell.execute("screencap -p");
        log("screencap -p: {} {}", ret1.c_str(), ret1.size());

        std::string ret2 = shell.execute("input tap 540 1000");
        log("input tap 540 1000: {} {}", ret2.c_str(), ret2.size());

        ImageDecoder decoder(jvm);
        auto bitmap = decoder.decodeBuffer(ret1.data(), ret1.size());
        log("ImageDecode: width:{} height:{}", bitmap->width, bitmap->height);

        log("OpenCV Version: {}", cv::getVersionString());

        log("onnxruntime Version: {}", Ort::GetVersionString());

        log("UnRAR Version: {}.{}.{}", RARVER_MAJOR, RARVER_MINOR, RARVER_BETA);

        auto startTime = std::chrono::time_point_cast<std::chrono::milliseconds>(
                std::chrono::system_clock::now()).time_since_epoch().count();
        testOnnx2();
        auto endTime = std::chrono::time_point_cast<std::chrono::milliseconds>(
                std::chrono::system_clock::now()).time_since_epoch().count();
        log("OCR elapsed time: {}", endTime - startTime);
    }).detach();
}
