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
build_package(
        NAME OpenCV
        URL https://github.com/opencv/opencv/archive/refs/tags/4.12.0.zip
        BUILD_SCRIPT ${CMAKE_CURRENT_SOURCE_DIR}/cmake/BuildOpenCV.cmake
        BUILD_TYPE Debug
        BUILD_SHARED ON
)
build_package(
        NAME paddleocr_cpp_infer
        GIT_REPOSITORY "https://github.com/PaddlePaddle/PaddleOCR"
        GIT_TAG v2.10.0
        BUILD_SCRIPT ${CMAKE_CURRENT_SOURCE_DIR}/cmake/BuildOcrCppInfer.cmake
        BUILD_TYPE Debug
        BUILD_SHARED OFF
)
add_library(${CMAKE_PROJECT_NAME} SHARED samples.cpp)
target_link_libraries(${CMAKE_PROJECT_NAME} android log)
target_compile_features(${CMAKE_PROJECT_NAME} PRIVATE cxx_std_20)

set(OpenCV_DIR ${OpenCV_ROOT}/sdk/native/jni)
find_package(OpenCV COMPONENTS imgproc imgcodecs REQUIRED)
target_link_libraries(${CMAKE_PROJECT_NAME} ${OpenCV_LIBS})

find_package(onnxruntime NO_CMAKE_FIND_ROOT_PATH REQUIRED)
target_link_libraries(${CMAKE_PROJECT_NAME} onnxruntime::onnxruntime)

find_package(paddleocr_cpp_infer NO_CMAKE_FIND_ROOT_PATH REQUIRED)
target_link_libraries(${CMAKE_PROJECT_NAME} paddleocr_cpp_infer::paddleocr_cpp_infer)

include(FetchContent)
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

target_link_libraries(${CMAKE_PROJECT_NAME} models)
