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

extern unsigned char _binary_det_onnx_start[];
extern unsigned char _binary_det_onnx_end[];
extern unsigned char _binary_det_onnx_size[];

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
    Ort::Session session(env, "/sdcard/Download/det.onnx", session_options);

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

void testOnnx2() {
    auto startTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();

    // Read image
    cv::Mat img = cv::imread("/sdcard/Download/1.jpg");
    log("image: width:{}, height:{}, channels:{}, shape:{}",
        img.cols, img.rows, img.channels(), getMatShape(img));

    float ratio_h{};
    float ratio_w{};

    cv::Mat srcimg;
    cv::Mat resize_img;
    img.copyTo(srcimg);

    std::string limit_type_ = "max";
    int limit_side_len_ = 960;
    bool use_tensorrt_ = false;
    PaddleOCR::ResizeImgType0().Run(img, resize_img, limit_type_,
                                    limit_side_len_, ratio_h, ratio_w,
                                    use_tensorrt_);

    std::vector<float> mean_ = {0.485f, 0.456f, 0.406f};
    std::vector<float> scale_ = {1 / 0.229f, 1 / 0.224f, 1 / 0.225f};
    bool is_scale_ = true;
    PaddleOCR::Normalize().Run(&resize_img, mean_, scale_, is_scale_);

    std::vector<float> input(1 * 3 * resize_img.rows * resize_img.cols, 0.0f);
    PaddleOCR::Permute().Run(&resize_img, input.data());


    // 创建 ONNX Runtime 环境
    Ort::Env env(ORT_LOGGING_LEVEL_WARNING, "test");

    // 创建 ONNX Runtime 会话
    Ort::SessionOptions session_options;
    size_t model_size = _binary_det_onnx_end - _binary_det_onnx_start;
    log("model_size: {} {}", model_size, *_binary_det_onnx_size);
    __android_log_print(ANDROID_LOG_ERROR, "TAG", "model: %p %p %p", _binary_det_onnx_start,
                        _binary_det_onnx_end, _binary_det_onnx_size);
    Ort::Session session(env, _binary_det_onnx_start, model_size, session_options);

    // 获取输入和输出的名称和维度
    Ort::AllocatorWithDefaultOptions allocator;
    Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(
            OrtDeviceAllocator, OrtMemTypeDefault);

    std::vector<const char *> input_node_names = {"x"};
    std::vector<const char *> output_node_names = {"sigmoid_0.tmp_0"};

    // 创建输入张量
    std::vector<int64_t> input_shape = {1, 3, resize_img.rows, resize_img.cols};
    std::vector<float> input_data(input);

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
    float *output_data = output_tensors[0].GetTensorMutableData<float>();
    std::vector<int64_t> output_shape = output_tensors[0].GetTensorTypeAndShapeInfo().GetShape();
    int out_num = std::accumulate(output_shape.begin(), output_shape.end(), 1,
                                  std::multiplies<int64_t>());
    std::vector<float> out_data(output_data, output_data + out_num);

    int n2 = output_shape[2];
    int n3 = output_shape[3];
    int n = n2 * n3;

    std::vector<float> pred(n, 0.0);
    std::vector<unsigned char> cbuf(n, ' ');

    for (int i = 0; i < n; i++) {
        pred[i] = float(out_data[i]);
        cbuf[i] = (unsigned char)((out_data[i]) * 255);
    }

    cv::Mat cbuf_map(n2, n3, CV_8UC1, (unsigned char *)cbuf.data());
    cv::Mat pred_map(n2, n3, CV_32F, (float *)pred.data());

    double det_db_thresh_ = 0.3;
    const double threshold = det_db_thresh_ * 255;
    const double maxvalue = 255;

    cv::Mat bit_map;
    cv::threshold(cbuf_map, bit_map, threshold, maxvalue, cv::THRESH_BINARY);

    double det_db_box_thresh_ = 0.5;
    double det_db_unclip_ratio_ = 2.0;
    std::string det_db_score_mode_ = "slow";
    std::vector<std::vector<std::vector<int>>> boxes;
    boxes = PaddleOCR::DBPostProcessor().BoxesFromBitmap(
            pred_map, bit_map,
            det_db_box_thresh_,
            det_db_unclip_ratio_,
            det_db_score_mode_);

    boxes = PaddleOCR::DBPostProcessor().FilterTagDetRes(
            boxes, ratio_h, ratio_w, srcimg);

    std::vector<PaddleOCR::OCRPredictResult> ocr_results;
    for (int i = 0; i < boxes.size(); i++) {
        PaddleOCR::OCRPredictResult res;
        res.box = boxes[i];
        ocr_results.push_back(res);
    }
    // sort boex from top to bottom, from left to right
    PaddleOCR::Utility::sorted_boxes(ocr_results);

    log("dec: {}", ocr_results.size());

    // crop image
    std::vector<cv::Mat> img_list;
    for (int j = 0; j < ocr_results.size(); j++) {
        cv::Mat crop_img;
        crop_img = PaddleOCR::Utility::GetRotateCropImage(img, ocr_results[j].box);
        img_list.push_back(crop_img);
    }

    std::vector<int> cls_labels(img_list.size(), 0);
    std::vector<float> cls_scores(img_list.size(), 0);
    std::vector<double> cls_times;


    //cls
    int cls_batch_num_ = 1;

    int img_num = img_list.size();
    std::vector<int> cls_image_shape = {3, 48, 192};
    for (int beg_img_no = 0; beg_img_no < img_num;
         beg_img_no += cls_batch_num_) {
        auto preprocess_start = std::chrono::steady_clock::now();
        int end_img_no = std::min(img_num, beg_img_no + cls_batch_num_);
        int batch_num = end_img_no - beg_img_no;
        // preprocess
        std::vector<cv::Mat> norm_img_batch;
        for (int ino = beg_img_no; ino < end_img_no; ino++) {
            cv::Mat srcimg;
            img_list[ino].copyTo(srcimg);
            cv::Mat resize_img;
            PaddleOCR::ClsResizeImg().Run(srcimg, resize_img, use_tensorrt_,
                                          cls_image_shape);

            std::vector<float> mean_ = {0.5f, 0.5f, 0.5f};
            std::vector<float> scale_ = {1 / 0.5f, 1 / 0.5f, 1 / 0.5f};
            bool is_scale_ = true;
            PaddleOCR::Normalize().Run(&resize_img, mean_, scale_, is_scale_);

            if (resize_img.cols < cls_image_shape[2]) {
                cv::copyMakeBorder(resize_img, resize_img, 0, 0, 0,
                                   cls_image_shape[2] - resize_img.cols,
                                   cv::BORDER_CONSTANT, cv::Scalar(0, 0, 0));
            }
            norm_img_batch.push_back(resize_img);
        }
        std::vector<float> input(batch_num * cls_image_shape[0] *
                                 cls_image_shape[1] * cls_image_shape[2],
                                 0.0f);
        PaddleOCR::PermuteBatch().Run(norm_img_batch, input.data());

        // inference.
        {
            // 创建 ONNX Runtime 环境
            Ort::Env env(ORT_LOGGING_LEVEL_WARNING, "cls");

            // 创建 ONNX Runtime 会话
            Ort::SessionOptions session_options;
            Ort::Session session(env, "/sdcard/Download/cls.onnx", session_options);

            // 获取输入和输出的名称和维度
            Ort::AllocatorWithDefaultOptions allocator;
            Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(
                    OrtDeviceAllocator, OrtMemTypeDefault);

            std::vector<const char *> input_node_names = {"x"};
            std::vector<const char *> output_node_names = {"softmax_0.tmp_0"};

            // 创建输入张量
            std::vector<int64_t> input_shape = {batch_num,
                                                cls_image_shape[0],
                                                cls_image_shape[1],
                                                cls_image_shape[2]};

            std::vector<float> input_data(input);

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
            float *output_data = output_tensors[0].GetTensorMutableData<float>();
            std::vector<int64_t> predict_shape = output_tensors[0].GetTensorTypeAndShapeInfo().GetShape();

            int out_num = std::accumulate(predict_shape.begin(), predict_shape.end(), 1,
                                          std::multiplies<int64_t>());
            std::vector<float> predict_batch(output_data, output_data + out_num);


            // postprocess
            auto postprocess_start = std::chrono::steady_clock::now();
            for (int batch_idx = 0; batch_idx < predict_shape[0]; batch_idx++) {
                int label = int(
                        PaddleOCR::Utility::argmax(&predict_batch[batch_idx * predict_shape[1]],
                                        &predict_batch[(batch_idx + 1) * predict_shape[1]]));
                float score = float(*std::max_element(
                        &predict_batch[batch_idx * predict_shape[1]],
                        &predict_batch[(batch_idx + 1) * predict_shape[1]]));
                cls_labels[beg_img_no + batch_idx] = label;
                cls_scores[beg_img_no + batch_idx] = score;
            }
        }
    }


    // output cls results
    for (int i = 0; i < cls_labels.size(); i++) {
        ocr_results[i].cls_label = cls_labels[i];
        ocr_results[i].cls_score = cls_scores[i];
        log("cls: index:{} label:{} score:{}", i, cls_labels[i], cls_scores[i]);
    }


    std::vector<std::string> rec_texts(img_list.size(), "");
    std::vector<float> rec_text_scores(img_list.size(), 0);
    //rec
    std::vector<std::string> label_list_ = PaddleOCR::Utility::ReadDict(
            "/sdcard/Download/ppocr_keys_v1.txt");
    label_list_.insert(label_list_.begin(),"#"); // blank char for ctc
    label_list_.push_back(" ");

    {
        int rec_batch_num_ = 6;
        int rec_img_h_ = 48;
        int rec_img_w_ = 320;
        std::vector<int> rec_image_shape_ = {3, rec_img_h_, rec_img_w_};
        bool use_tensorrt_ = false;
        std::vector<float> mean_ = {0.5f, 0.5f, 0.5f};
        std::vector<float> scale_ = {1 / 0.5f, 1 / 0.5f, 1 / 0.5f};
        bool is_scale_ = true;

        int img_num = img_list.size();
        std::vector<float> width_list;
        for (int i = 0; i < img_num; i++) {
            width_list.push_back(float(img_list[i].cols) / img_list[i].rows);
        }
        std::vector<int> indices = PaddleOCR::Utility::argsort(width_list);

        for (int beg_img_no = 0; beg_img_no < img_num;
             beg_img_no += rec_batch_num_) {
            int end_img_no = std::min(img_num, beg_img_no + rec_batch_num_);
            int batch_num = end_img_no - beg_img_no;
            int imgH = rec_image_shape_[1];
            int imgW = rec_image_shape_[2];
            float max_wh_ratio = imgW * 1.0 / imgH;
            for (int ino = beg_img_no; ino < end_img_no; ino++) {
                int h = img_list[indices[ino]].rows;
                int w = img_list[indices[ino]].cols;
                float wh_ratio = w * 1.0 / h;
                max_wh_ratio = std::max(max_wh_ratio, wh_ratio);
            }

            int batch_width = imgW;
            std::vector<cv::Mat> norm_img_batch;
            for (int ino = beg_img_no; ino < end_img_no; ino++) {
                cv::Mat srcimg;
                img_list[indices[ino]].copyTo(srcimg);
                cv::Mat resize_img;
                PaddleOCR::CrnnResizeImg().Run(
                        srcimg, resize_img, max_wh_ratio,
                        use_tensorrt_, rec_image_shape_);
                PaddleOCR::Normalize().Run(&resize_img, mean_, scale_,
                                        is_scale_);
                norm_img_batch.push_back(resize_img);
                batch_width = std::max(resize_img.cols, batch_width);
            }

            std::vector<float> input(batch_num * 3 * imgH * batch_width, 0.0f);
            PaddleOCR::PermuteBatch().Run(norm_img_batch, input.data());

            // Inference.
            {
                // 创建 ONNX Runtime 环境
                Ort::Env env(ORT_LOGGING_LEVEL_WARNING, "rec");

                // 创建 ONNX Runtime 会话
                Ort::SessionOptions session_options;
                Ort::Session session(env, "/sdcard/Download/rec.onnx", session_options);

                // 获取输入和输出的名称和维度
                Ort::AllocatorWithDefaultOptions allocator;
                Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(
                        OrtDeviceAllocator, OrtMemTypeDefault);

                std::vector<const char *> input_node_names = {"x"};
                std::vector<const char *> output_node_names = {"softmax_11.tmp_0"};

                // 创建输入张量
                std::vector<int64_t> input_shape = {batch_num, 3, imgH, batch_width};

                std::vector<float> input_data(input);

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
                float *output_data = output_tensors[0].GetTensorMutableData<float>();
                std::vector<int64_t> predict_shape = output_tensors[0].GetTensorTypeAndShapeInfo().GetShape();
                int out_num = std::accumulate(predict_shape.begin(), predict_shape.end(), 1,
                                              std::multiplies<int64_t>());

                std::vector<float> predict_batch(output_data, output_data + out_num);

                // ctc decode
                auto postprocess_start = std::chrono::steady_clock::now();
                for (int m = 0; m < predict_shape[0]; m++) {
                    std::string str_res;
                    int argmax_idx;
                    int last_index = 0;
                    float score = 0.f;
                    int count = 0;
                    float max_value = 0.0f;

                    for (int n = 0; n < predict_shape[1]; n++) {
                        // get idx
                        argmax_idx = int(PaddleOCR::Utility::argmax(
                                &predict_batch[(m * predict_shape[1] + n) * predict_shape[2]],
                                &predict_batch[(m * predict_shape[1] + n + 1) * predict_shape[2]]));
                        // get score
                        max_value = float(*std::max_element(
                                &predict_batch[(m * predict_shape[1] + n) * predict_shape[2]],
                                &predict_batch[(m * predict_shape[1] + n + 1) * predict_shape[2]]));

                        if (argmax_idx > 0 && (!(n > 0 && argmax_idx == last_index))) {
                            score += max_value;
                            count += 1;
                            str_res += label_list_[argmax_idx];
                        }
                        last_index = argmax_idx;
                    }
                    score /= count;
                    if (std::isnan(score)) {
                        continue;
                    }
                    rec_texts[indices[beg_img_no + m]] = str_res;
                    rec_text_scores[indices[beg_img_no + m]] = score;
                }
            }
        }
    }

    // output rec results
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

        testOnnx2();
    }).detach();
}
