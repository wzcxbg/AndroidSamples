cmake_minimum_required(VERSION 3.22.1)
project("paddleocr_cpp_infer")

add_library(${PROJECT_NAME} STATIC
        src/postprocess_op.cpp
        src/preprocess_op.cpp
        src/clipper.cpp
        src/utility.cpp
        include/postprocess_op.h
        include/preprocess_op.h
        include/clipper.h
        include/utility.h)

set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/3rdparty/OpenCV)
find_package(OpenCV REQUIRED imgproc imgcodecs)
target_link_libraries(${PROJECT_NAME} ${OpenCV_LIBS})
target_include_directories(${PROJECT_NAME} PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

