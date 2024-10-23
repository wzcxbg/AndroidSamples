cmake_minimum_required(VERSION 3.22.1)
project("samples")

add_library(${CMAKE_PROJECT_NAME} SHARED samples.cpp)
target_link_libraries(${CMAKE_PROJECT_NAME} android log)
target_compile_features(${CMAKE_PROJECT_NAME} PRIVATE cxx_std_20)

set(OpenCV_DIR ${CMAKE_CURRENT_SOURCE_DIR}/3rdparty/OpenCV)
find_package(OpenCV REQUIRED imgproc imgcodecs)
target_link_libraries(${CMAKE_PROJECT_NAME} ${OpenCV_LIBS})

set(onnxruntime_DIR ${CMAKE_CURRENT_SOURCE_DIR}/3rdparty/onnxruntime/lib/cmake/onnxruntime)
find_package(onnxruntime REQUIRED)
target_link_libraries(${CMAKE_PROJECT_NAME} onnxruntime::onnxruntime)