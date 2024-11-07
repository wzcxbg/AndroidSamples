#pragma once

#include <opencv2/opencv.hpp>
#include <onnxruntime_cxx_api.h>
#include <algorithm>

#include "include/postprocess_op.h"
#include "include/preprocess_op.h"
#include "models.h"

struct RecognizeResult {
    std::string text{};
    float score{};
};

struct ClassifyResult {
    int label{};
    float score{};
};

struct DetectResult {
    //std::array<std::array<int, 2>, 4> boxes;
    std::vector<std::vector<int>> box;
    struct Point {int x, y;};
    const Point left() const { return Point{box[0][0], box[0][1]}; }
    const Point top() const { return Point{box[1][0], box[1][1]}; }
    const Point right() const { return Point{box[2][0], box[2][1]}; }
    const Point bottom() const { return Point{box[3][0], box[3][1]}; }
};

struct OcrResult {
    DetectResult det_ret;
    ClassifyResult cls_ret;
    RecognizeResult rec_ret;
};

namespace Utility {

    void sorted_boxes(std::vector<DetectResult> &ocr_result) {
        //将文本框按从左往右，从上往下的顺序排列
        //当Y轴位置相近时，优先按X轴坐标排序
        std::sort(ocr_result.begin(), ocr_result.end(), [](
                const DetectResult &r1,
                const DetectResult &r2) -> bool {
            if (r1.left().y != r2.left().y &&
                std::abs(r1.left().y - r2.left().y) >= 10) {
                return (r1.left().y < r2.left().y);
            } else {
                return (r1.left().x < r2.left().x);
            }
        });
    }

    cv::Mat resize(const cv::Mat &image, int tar_w = 960, int tar_h = 960) {
        // 将图片数据预处理（只支持BGR），并转换为 [batch, channel, height, width]
        // 形状的内存连续的cv::Mat
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
        return result;
    }

    template<class T>
    long compare_same_count(std::span<T> data1, std::span<T> data2) {
        int same_count = 0;
        for (int i = 0; i < std::min(data1.size(), data2.size()); ++i) {
            if (data1[i] == data2[i]) {
                same_count++;
                continue;
            }
        }
        return same_count;
    }

    std::string format_shape(const std::vector<int64_t> shape) {
        std::ostringstream oss;
        oss << "[";
        for (size_t i = 0; i < shape.size(); ++i) {
            oss << shape[i];
            if (i < shape.size() - 1) oss << ", ";
        }
        oss << "]";
        return oss.str();
    }

    std::string format_shape(const cv::Mat &mat) {
        std::ostringstream oss;
        oss << "[";
        for (int i = 0; i < mat.dims; ++i) {
            oss << mat.size[i];
            if (i < mat.dims - 1) oss << " x ";
        }
        oss << "]";
        return oss.str();
    }
}

class TextRecognizer {
    std::vector<float> mean_ = {0.5f, 0.5f, 0.5f};
    std::vector<float> scale_ = {1 / 0.5f, 1 / 0.5f, 1 / 0.5f};
    bool is_scale_ = true;
    std::string precision_ = "fp32";
    int rec_batch_num_ = 6;
    int rec_img_h_ = 32;        //48
    int rec_img_w_ = 320;       //320
    std::vector<int> rec_image_shape_ = {3, rec_img_h_, rec_img_w_};
    // pre-process
    PaddleOCR::CrnnResizeImg resize_op_;
    PaddleOCR::Normalize normalize_op_;
    PaddleOCR::PermuteBatch permute_op_;

    Ort::Env env = Ort::Env(ORT_LOGGING_LEVEL_WARNING, "rec");
    std::unique_ptr<Ort::Session> session = {nullptr};

    std::vector<std::string> label_list_;

public:
    explicit TextRecognizer() {
        Ort::SessionOptions session_options;
        session = std::make_unique<Ort::Session>(
                env, rec_onnx.data(), rec_onnx.size_bytes(),
                session_options);
        this->rec_img_h_ = 48;
        this->rec_img_w_ = 320;
        this->rec_image_shape_ = {3, 48, 320};
        this->label_list_ = ReadDict();
    }

    std::vector<RecognizeResult> Run(const std::vector<cv::Mat> &img_list) {
        std::vector<RecognizeResult> result;
        result.reserve(img_list.size());
        result.resize(img_list.size());

        int img_num = img_list.size();
        std::vector<float> width_list;
        for (int i = 0; i < img_num; i++) {
            width_list.push_back(float(img_list[i].cols) / img_list[i].rows);
        }
        std::vector<int> indices = PaddleOCR::Utility::argsort(width_list);

        for (int beg_img_no = 0; beg_img_no < img_num;
             beg_img_no += this->rec_batch_num_) {


            int end_img_no = std::min(img_num, beg_img_no + this->rec_batch_num_);
            int batch_num = end_img_no - beg_img_no;
            int imgH = this->rec_image_shape_[1];
            int imgW = this->rec_image_shape_[2];
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
                this->resize_op_.Run(srcimg, resize_img, max_wh_ratio,
                                     false, this->rec_image_shape_);
                this->normalize_op_.Run(&resize_img, this->mean_, this->scale_,
                                        this->is_scale_);
                norm_img_batch.push_back(resize_img);
                batch_width = std::max(resize_img.cols, batch_width);
            }

            std::vector<float> input(batch_num * 3 * imgH * batch_width, 0.0f);
            this->permute_op_.Run(norm_img_batch, input.data());


            // Inference.
            Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(
                    OrtDeviceAllocator, OrtMemTypeDefault);
            std::vector<int64_t> input_shape = {batch_num, 3, imgH, batch_width};
            Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
                    memory_info,
                    input.data(), input.size(),
                    input_shape.data(), input_shape.size());

            Ort::AllocatorWithDefaultOptions allocator;
            auto input_name = session->GetInputNameAllocated(0, allocator);
            auto output_name = session->GetOutputNameAllocated(0, allocator);
            std::vector<const char *> input_names = {input_name.get()};
            std::vector<const char *> output_names = {output_name.get()};
            std::vector<Ort::Value> output_tensors = session->Run(
                    Ort::RunOptions{nullptr},
                    input_names.data(), &input_tensor, 1,
                    output_names.data(), 1);

            auto *output_data = output_tensors[0].GetTensorMutableData<float>();
            std::vector<int64_t> output_shape = output_tensors[0].GetTensorTypeAndShapeInfo().GetShape();
            int out_num = std::accumulate(output_shape.begin(), output_shape.end(),
                                          1, std::multiplies<int64_t>());
            std::vector<float> out_data(output_data, output_data + out_num);

            std::vector<int64_t> predict_shape = output_shape;
            std::vector<float> predict_batch = out_data;


            // ctc decode
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
                result[indices[beg_img_no + m]] = {str_res, score};
            }
        }
        return result;
    }

    static std::vector<std::string> ReadDict() {
        std::vector<std::string> result;
        result.emplace_back("#");

        std::istringstream iss;
        iss.str(std::string(ppocr_keys_v1_txt.data(), ppocr_keys_v1_txt.size_bytes()));
        std::string line;
        while (getline(iss, line)) {
            result.push_back(line);
        }

        result.emplace_back(" ");
        return result;
    };
};

class TextClassifier {
    std::vector<float> mean_ = {0.5f, 0.5f, 0.5f};
    std::vector<float> scale_ = {1 / 0.5f, 1 / 0.5f, 1 / 0.5f};
    bool is_scale_ = true;
    std::string precision_ = "fp32";
    double cls_thresh = 0.9;
    int cls_batch_num_ = 1;
    // pre-process
    PaddleOCR::ClsResizeImg resize_op_;
    PaddleOCR::Normalize normalize_op_;
    PaddleOCR::PermuteBatch permute_op_;

    Ort::Env env = Ort::Env(ORT_LOGGING_LEVEL_WARNING, "cls");
    std::unique_ptr<Ort::Session> session = {nullptr};

public:
    explicit TextClassifier() {
        Ort::SessionOptions session_options;
        session = std::make_unique<Ort::Session>(
                env, cls_onnx.data(), cls_onnx.size_bytes(),
                session_options);
    }

    std::vector<ClassifyResult> Run(const std::vector<cv::Mat> &img_list) {
        std::vector<ClassifyResult> result;
        result.reserve(img_list.size());
        result.resize(img_list.size());

        int img_num = img_list.size();
        std::vector<int> cls_image_shape = {3, 48, 192};
        for (int beg_img_no = 0; beg_img_no < img_num;
             beg_img_no += this->cls_batch_num_) {

            int end_img_no = std::min(img_num, beg_img_no + this->cls_batch_num_);
            int batch_num = end_img_no - beg_img_no;

            // preprocess
            std::vector<cv::Mat> norm_img_batch;
            for (int ino = beg_img_no; ino < end_img_no; ino++) {
                cv::Mat srcimg;
                img_list[ino].copyTo(srcimg);
                cv::Mat resize_img;
                this->resize_op_.Run(srcimg, resize_img, false,
                                     cls_image_shape);

                this->normalize_op_.Run(&resize_img, this->mean_, this->scale_,
                                        this->is_scale_);
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
            this->permute_op_.Run(norm_img_batch, input.data());

            // inference.
            Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(
                    OrtDeviceAllocator, OrtMemTypeDefault);
            std::vector<int64_t> input_shape = {batch_num,
                                                cls_image_shape[0],
                                                cls_image_shape[1],
                                                cls_image_shape[2]};
            Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
                    memory_info,
                    input.data(), input.size(),
                    input_shape.data(), input_shape.size());

            Ort::AllocatorWithDefaultOptions allocator;
            auto input_name = session->GetInputNameAllocated(0, allocator);
            auto output_name = session->GetOutputNameAllocated(0, allocator);
            std::vector<const char *> input_names = {input_name.get()};
            std::vector<const char *> output_names = {output_name.get()};
            std::vector<Ort::Value> output_tensors = session->Run(
                    Ort::RunOptions{nullptr},
                    input_names.data(), &input_tensor, 1,
                    output_names.data(), 1);

            auto *output_data = output_tensors[0].GetTensorMutableData<float>();
            std::vector<int64_t> output_shape = output_tensors[0].GetTensorTypeAndShapeInfo().GetShape();
            int out_num = std::accumulate(output_shape.begin(), output_shape.end(),
                                          1, std::multiplies<int64_t>());
            std::vector<float> out_data(output_data, output_data + out_num);

            std::vector<int64_t> predict_shape = output_shape;
            std::vector<float> predict_batch = out_data;

            // postprocess
            for (int batch_idx = 0; batch_idx < predict_shape[0]; batch_idx++) {
                int label = int(
                        PaddleOCR::Utility::argmax(&predict_batch[batch_idx * predict_shape[1]],
                                                   &predict_batch[(batch_idx + 1) *
                                                                  predict_shape[1]]));
                float score = float(*std::max_element(
                        &predict_batch[batch_idx * predict_shape[1]],
                        &predict_batch[(batch_idx + 1) * predict_shape[1]]));
                result[beg_img_no + batch_idx] = {label, score};
            }
        }
        return result;
    }
};

class TextDetector {
    std::string limit_type_ = "max";            //max
    int limit_side_len_ = 960;                  //960

    double det_db_thresh_ = 0.3;                //0.3
    double det_db_box_thresh_ = 0.5;            //0.6
    double det_db_unclip_ratio_ = 2.0;          //1.5
    std::string det_db_score_mode_ = "slow";    //slow
    bool use_dilation_ = false;                 //true

    std::string precision_ = "fp32";            //fp32

    std::vector<float> mean_ = {0.485f, 0.456f, 0.406f};
    std::vector<float> scale_ = {1 / 0.229f, 1 / 0.224f, 1 / 0.225f};
    bool is_scale_ = true;

    // pre-process
    PaddleOCR::ResizeImgType0 resize_op_;
    PaddleOCR::Normalize normalize_op_;
    PaddleOCR::Permute permute_op_;

    // post-process
    PaddleOCR::DBPostProcessor post_processor_;

    Ort::Env env = Ort::Env(ORT_LOGGING_LEVEL_WARNING, "det");
    std::unique_ptr<Ort::Session> session = {nullptr};

public:
    explicit TextDetector() {
        // 创建 ONNX Runtime 会话
        Ort::SessionOptions session_options;
        session = std::make_unique<Ort::Session>(
                env, det_onnx.data(), det_onnx.size_bytes(),
                session_options);
        this->det_db_box_thresh_ = 0.6;
        this->det_db_unclip_ratio_ = 1.5;
        this->use_dilation_ = true;
    }

    std::vector<DetectResult> Run(const cv::Mat &img) {
        float ratio_h{};
        float ratio_w{};

        cv::Mat srcimg;
        cv::Mat resize_img;
        img.copyTo(srcimg);


        // pre-process
        this->resize_op_.Run(img, resize_img, limit_type_,
                             limit_side_len_, ratio_h, ratio_w, false);
        this->normalize_op_.Run(&resize_img, mean_, scale_,
                                is_scale_);
        std::vector<float> input(1 * 3 * resize_img.rows * resize_img.cols, 0.0f);
        this->permute_op_.Run(&resize_img, input.data());


        // inference.
        // 设置输入获取输出
        Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(
                OrtDeviceAllocator, OrtMemTypeDefault);
        std::vector<int64_t> input_shape = {1, 3, resize_img.rows, resize_img.cols};
        Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
                memory_info,
                input.data(), input.size(),
                input_shape.data(), input_shape.size());

        // 获取输入和输出的名称和维度
        Ort::AllocatorWithDefaultOptions allocator;
        auto input_name = session->GetInputNameAllocated(0, allocator);
        auto output_name = session->GetOutputNameAllocated(0, allocator);
        std::vector<const char *> input_names = {input_name.get()};
        std::vector<const char *> output_names = {output_name.get()};
        std::vector<Ort::Value> output_tensors = session->Run(
                Ort::RunOptions{nullptr},
                input_names.data(), &input_tensor, 1,
                output_names.data(), 1);

        auto *output_data = output_tensors[0].GetTensorMutableData<float>();
        std::vector<int64_t> output_shape = output_tensors[0].GetTensorTypeAndShapeInfo().GetShape();
        int out_num = std::accumulate(output_shape.begin(), output_shape.end(),
                                      1, std::multiplies<int64_t>());
        std::vector<float> out_data(output_data, output_data + out_num);


        // post-process
        int n2 = output_shape[2];
        int n3 = output_shape[3];
        int n = n2 * n3;

        std::vector<float> pred(n, 0.0);
        std::vector<unsigned char> cbuf(n, ' ');

        for (int i = 0; i < n; i++) {
            pred[i] = float(out_data[i]);
            cbuf[i] = (unsigned char) ((out_data[i]) * 255);
        }

        cv::Mat cbuf_map(n2, n3, CV_8UC1, (unsigned char *) cbuf.data());
        cv::Mat pred_map(n2, n3, CV_32F, (float *) pred.data());

        const double threshold = this->det_db_thresh_ * 255;
        const double maxvalue = 255;
        cv::Mat bit_map;
        cv::threshold(cbuf_map, bit_map, threshold, maxvalue, cv::THRESH_BINARY);
        if (use_dilation_) {
            cv::Mat dila_ele = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(2, 2));
            cv::dilate(bit_map, bit_map, dila_ele);
        }

        std::vector<std::vector<std::vector<int>>> boxes = post_processor_.BoxesFromBitmap(
                pred_map, bit_map, det_db_box_thresh_, det_db_unclip_ratio_,
                det_db_score_mode_);

        boxes = post_processor_.FilterTagDetRes(boxes, ratio_h, ratio_w, srcimg);

        std::vector<DetectResult> results;
        results.reserve(boxes.size());
        for (auto &box: boxes) {
            results.emplace_back(box);
        }
        Utility::sorted_boxes(results);
        return results;
    }
};

class PPOCR {
    TextDetector detector;
    TextClassifier classifier;
    TextRecognizer recognizer;

public:
    std::vector<DetectResult> det(const cv::Mat &img) {
        return detector.Run(img);
    }

    std::vector<ClassifyResult> cls(const std::vector<cv::Mat> &img_list) {
        return classifier.Run(img_list);
    }

    std::vector<RecognizeResult> rec(const std::vector<cv::Mat> &img_list) {
        return recognizer.Run(img_list);
    }

    std::vector<OcrResult> ocr(const cv::Mat &img) {
        std::vector<OcrResult> results;
        std::vector<DetectResult> detectResults;
        std::vector<ClassifyResult> classifyResults;
        std::vector<RecognizeResult> recognizeResults;

        detectResults = det(img);
        std::vector<cv::Mat> img_list;
        for (auto &detectResult: detectResults) {
            cv::Mat crop_img;
            crop_img = PaddleOCR::Utility::GetRotateCropImage(img, detectResult.box);
            img_list.push_back(crop_img);
        }
        classifyResults = cls(img_list);
        recognizeResults = rec(img_list);

        results.reserve(img_list.size());
        for (int i = 0; i < img_list.size(); ++i) {
            results.emplace_back(detectResults[i],
                                 classifyResults[i],
                                 recognizeResults[i]);
        }
        return results;
    }
};