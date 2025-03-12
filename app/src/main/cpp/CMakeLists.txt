cmake_minimum_required(VERSION 3.22.1)
project("samples")

include(FetchContent)
FetchContent_Declare(
    spdlog
    GIT_REPOSITORY https://github.com/gabime/spdlog.git
    GIT_TAG v1.15.1
)
set(SPDLOG_BUILD_SHARED OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(spdlog)

add_library(${CMAKE_PROJECT_NAME} SHARED samples.cpp)
target_link_libraries(${CMAKE_PROJECT_NAME} android log)
target_compile_features(${CMAKE_PROJECT_NAME} PRIVATE cxx_std_20)

set(OpenCV_DIR ${CMAKE_CURRENT_SOURCE_DIR}/3rdparty/OpenCV)
find_package(OpenCV REQUIRED imgproc imgcodecs)
target_link_libraries(${CMAKE_PROJECT_NAME} ${OpenCV_LIBS})

set(onnxruntime_DIR ${CMAKE_CURRENT_SOURCE_DIR}/3rdparty/onnxruntime/lib/cmake/onnxruntime)
find_package(onnxruntime REQUIRED)
target_link_libraries(${CMAKE_PROJECT_NAME} onnxruntime::onnxruntime)

add_subdirectory(3rdparty/paddleocr_cpp_infer)
target_link_libraries(${CMAKE_PROJECT_NAME} paddleocr_cpp_infer)

add_subdirectory(3rdparty/cmake-binary-resource)
add_binary_target(models
        models/det/det.onnx
        models/cls/cls.onnx
        models/rec/rec.onnx
        models/rec/ppocr_keys_v1.txt)
target_link_libraries(${CMAKE_PROJECT_NAME} models)

add_subdirectory(3rdparty/unrar)
target_link_libraries(${CMAKE_PROJECT_NAME} unrar)

target_link_libraries(${CMAKE_PROJECT_NAME} spdlog::spdlog)
