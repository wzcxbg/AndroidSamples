cmake_minimum_required(VERSION 3.22.1)
project("samples")

include(cmake/BuildPackage.cmake)
build_package(
        NAME onnxruntime
        URL https://github.com/microsoft/onnxruntime/archive/refs/tags/v1.22.2.zip
        BUILD_SCRIPT ${CMAKE_CURRENT_SOURCE_DIR}/cmake/BuildOnnxRuntime.cmake
        BUILD_TYPE Release
        BUILD_SHARED ON
)
add_library(${CMAKE_PROJECT_NAME} SHARED samples.cpp)
target_link_libraries(${CMAKE_PROJECT_NAME} android log)
target_compile_features(${CMAKE_PROJECT_NAME} PRIVATE cxx_std_20)

set(OpenCV_DIR ${CMAKE_CURRENT_SOURCE_DIR}/3rdparty/OpenCV)
find_package(OpenCV REQUIRED imgproc imgcodecs)
target_link_libraries(${CMAKE_PROJECT_NAME} ${OpenCV_LIBS})

find_package(onnxruntime NO_CMAKE_FIND_ROOT_PATH REQUIRED)
target_link_libraries(${CMAKE_PROJECT_NAME} onnxruntime::onnxruntime)


include(FetchContent)
FetchContent_Populate(
        paddleocr_cpp_infer
        GIT_REPOSITORY "https://github.com/PaddlePaddle/PaddleOCR"
        GIT_TAG v2.10.0
        GIT_SHALLOW TRUE
        SOURCE_DIR ${paddleocr_cpp_infer_SOURCE_DIR})
add_library(paddleocr_cpp_infer STATIC
        ${paddleocr_cpp_infer_SOURCE_DIR}/deploy/cpp_infer/include/postprocess_op.h
        ${paddleocr_cpp_infer_SOURCE_DIR}/deploy/cpp_infer/include/preprocess_op.h
        ${paddleocr_cpp_infer_SOURCE_DIR}/deploy/cpp_infer/include/clipper.h
        ${paddleocr_cpp_infer_SOURCE_DIR}/deploy/cpp_infer/include/utility.h
        ${paddleocr_cpp_infer_SOURCE_DIR}/deploy/cpp_infer/src/postprocess_op.cpp
        ${paddleocr_cpp_infer_SOURCE_DIR}/deploy/cpp_infer/src/preprocess_op.cpp
        ${paddleocr_cpp_infer_SOURCE_DIR}/deploy/cpp_infer/src/clipper.cpp
        ${paddleocr_cpp_infer_SOURCE_DIR}/deploy/cpp_infer/src/utility.cpp)
target_include_directories(paddleocr_cpp_infer PUBLIC
        ${paddleocr_cpp_infer_SOURCE_DIR}/deploy/cpp_infer)
target_link_libraries(paddleocr_cpp_infer PUBLIC ${OpenCV_LIBS})


FetchContent_Populate(
        cmake-binary-resource
        GIT_REPOSITORY "https://github.com/wzcxbg/cmake-binary-resource"
        GIT_TAG main
        GIT_SHALLOW TRUE
        SOURCE_DIR ${cmake-binary-resource_SOURCE_DIR})
include(${cmake-binary-resource_SOURCE_DIR}/CMakeLists.txt)
add_binary_target(
        TARGET_NAME         models
        TARGET_NAMESPACE    Res
        TARGET_BINARY_FILES models/det/det.onnx
                            models/cls/cls.onnx
                            models/rec/rec.onnx
                            models/rec/ppocr_keys_v1.txt)


target_link_libraries(${CMAKE_PROJECT_NAME} models paddleocr_cpp_infer)
