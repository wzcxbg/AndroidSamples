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

add_subdirectory(3rdparty/paddleocr_cpp_infer)
target_link_libraries(${CMAKE_PROJECT_NAME} paddleocr_cpp_infer)


function(add_model_target TARGET_NAME)
    add_library(${TARGET_NAME} STATIC)
    get_target_property(TARGET_BINARY_DIR ${TARGET_NAME} BINARY_DIR)
    set(TARGET_OUTPUT_DIR ${TARGET_BINARY_DIR}/CMakeFiles/${TARGET_NAME}.dir)
    message("TARGET_OUTPUT_DIR ${TARGET_OUTPUT_DIR}")

    set(OUTPUT_OBJ_LIST)
    foreach (FILE_TO_COPY ${ARGN})
        get_filename_component(INPUT_FILE ${FILE_TO_COPY} ABSOLUTE)
        get_filename_component(INPUT_FILE_NAME ${FILE_TO_COPY} NAME)
        get_filename_component(INPUT_FILE_NAME_WE ${FILE_TO_COPY} NAME_WE)
        set(OUTPUT_OBJ ${INPUT_FILE_NAME_WE}.o)

        # 将二进制文件为.o文件
        # objcopy -I binary -O elf64-aarch64 -B aarch64 sample.jpg sample.o
        # clang -shared -o libsample.so sample.o
        # ar -rcs libsample.a sample.o
        add_custom_command(
                OUTPUT ${OUTPUT_OBJ}
                COMMAND ${CMAKE_COMMAND} -E copy_if_different ${INPUT_FILE} .
                COMMAND ${CMAKE_OBJCOPY} -I binary -O elf64-aarch64 -B aarch64 ${INPUT_FILE_NAME} ${OUTPUT_OBJ}
                MAIN_DEPENDENCY ${INPUT_FILE})
        list(APPEND OUTPUT_OBJ_LIST ${OUTPUT_OBJ})
    endforeach ()

    target_sources(${TARGET_NAME} PRIVATE ${OUTPUT_OBJ_LIST})
    set_target_properties(${TARGET_NAME} PROPERTIES LINKER_LANGUAGE CXX)
endfunction()

add_model_target(models
        models/det/det.onnx
        models/cls/cls.onnx
        models/rec/rec.onnx)
target_link_libraries(${CMAKE_PROJECT_NAME} models)