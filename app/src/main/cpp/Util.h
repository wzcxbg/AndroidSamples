#pragma once

#include <opencv2/opencv.hpp>
#include <onnxruntime_cxx_api.h>
#include <algorithm>

#include "include/postprocess_op.h"
#include "include/preprocess_op.h"
#include "models.h"

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

    void Run(std::vector<cv::Mat> img_list,
             std::vector<int> &cls_labels,
             std::vector<float> &cls_scores) {

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
            Ort::AllocatorWithDefaultOptions allocator;
            auto input_name = session->GetInputNameAllocated(0, allocator);
            auto output_name = session->GetOutputNameAllocated(0, allocator);

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
                cls_labels[beg_img_no + batch_idx] = label;
                cls_scores[beg_img_no + batch_idx] = score;
            }
        }
    }
};

class TextRectDetector {
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
    explicit TextRectDetector() {
        // 创建 ONNX Runtime 会话
        Ort::SessionOptions session_options;
        session = std::make_unique<Ort::Session>(
                env, det_onnx.data(), det_onnx.size_bytes(),
                session_options);
        this->det_db_box_thresh_ = 0.6;
        this->det_db_unclip_ratio_ = 1.5;
        this->use_dilation_ = true;
    }

    void Run(const cv::Mat &img, std::vector<PaddleOCR::OCRPredictResult> &ocr_results) {
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
        // 获取输入和输出的名称和维度
        Ort::AllocatorWithDefaultOptions allocator;
        auto input_name = session->GetInputNameAllocated(0, allocator);
        auto output_name = session->GetOutputNameAllocated(0, allocator);
//        auto input_shape_str = formatShape(session->GetInputTypeInfo(0)
//                                                   .GetTensorTypeAndShapeInfo().GetShape());
//        auto output_shape_str = formatShape(session->GetOutputTypeInfo(0)
//                                                    .GetTensorTypeAndShapeInfo().GetShape());
//        log("input: {} shape: {}", input_name.get(), input_shape_str);
//        log("output: {} shape: {}", output_name.get(), output_shape_str);


        // 设置输入获取输出
        Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(
                OrtDeviceAllocator, OrtMemTypeDefault);
        std::vector<int64_t> input_shape = {1, 3, resize_img.rows, resize_img.cols};
        Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
                memory_info,
                input.data(), input.size(),
                input_shape.data(), input_shape.size());

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
            cv::Mat dila_ele =
                    cv::getStructuringElement(cv::MORPH_RECT, cv::Size(2, 2));
            cv::dilate(bit_map, bit_map, dila_ele);
        }

        std::vector<std::vector<std::vector<int>>> boxes = post_processor_.BoxesFromBitmap(
                pred_map, bit_map, det_db_box_thresh_, det_db_unclip_ratio_,
                det_db_score_mode_);

        boxes = post_processor_.FilterTagDetRes(boxes, ratio_h, ratio_w, srcimg);

        for (int i = 0; i < boxes.size(); i++) {
            PaddleOCR::OCRPredictResult res;
            res.box = boxes[i];
            ocr_results.push_back(res);
        }
        // sort boex from top to bottom, from left to right
        PaddleOCR::Utility::sorted_boxes(ocr_results);
    }

    static std::string formatShape(std::vector<int64_t> shape) {
        std::ostringstream oss;
        oss << "[";
        for (size_t i = 0; i < shape.size(); ++i) {
            oss << shape[i];
            if (i < shape.size() - 1) oss << ", ";
        }
        oss << "]";
        return oss.str();
    }
};