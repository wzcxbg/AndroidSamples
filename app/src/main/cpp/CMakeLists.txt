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
    # 将二进制文件为.o文件
    # objcopy -I binary -O elf64-aarch64 -B aarch64 sample.jpg sample.o
    # ld -shared -o libexample.so example.o
    # ar -rcs libsample.a sample.o
    if (NOT CMAKE_OBJCOPY OR NOT CMAKE_LINKER OR NOT CMAKE_AR)
        message(FATAL_ERROR "Toolchain not support!")
    endif ()

    set(TARGET_OUTPUT_DIR ${CMAKE_CURRENT_BINARY_DIR}/CMakeFiles/${TARGET_NAME}.dir)
    message("TARGET_OUTPUT_DIR ${TARGET_OUTPUT_DIR}")

    # 生成OBJ
    set(OUTPUT_OBJ_LIST)
    foreach (FILE_TO_COPY ${ARGN})
        get_filename_component(INPUT_FILE ${FILE_TO_COPY} ABSOLUTE)
        get_filename_component(INPUT_FILE_NAME ${FILE_TO_COPY} NAME)
        get_filename_component(INPUT_FILE_NAME_WE ${FILE_TO_COPY} NAME_WE)
        set(OUTPUT_OBJ "${TARGET_OUTPUT_DIR}/${INPUT_FILE_NAME_WE}.o")

        add_custom_command(
                OUTPUT ${OUTPUT_OBJ}
                COMMAND ${CMAKE_COMMAND} -E copy_if_different ${INPUT_FILE} .
                COMMAND ${CMAKE_OBJCOPY} -I binary -O elf64-aarch64 -B aarch64 ${INPUT_FILE_NAME} ${OUTPUT_OBJ}
                MAIN_DEPENDENCY ${INPUT_FILE})
        list(APPEND OUTPUT_OBJ_LIST ${OUTPUT_OBJ})
    endforeach ()
    message("OUTPUT_OBJ_LIST ${OUTPUT_OBJ_LIST}")

    # 生成库文件
#    set(OUTPUT_LIBRARY "${TARGET_OUTPUT_DIR}/models.a")
#    add_custom_command(
#            OUTPUT ${OUTPUT_LIBRARY}
#            COMMAND ${CMAKE_AR} -rcs ${OUTPUT_LIBRARY} ${OUTPUT_OBJ_LIST}
#            DEPENDS ${OUTPUT_OBJ_LIST}
#            MAIN_DEPENDENCY ${OUTPUT_OBJ_LIST})
#    message("OUTPUT_LIBRARY ${OUTPUT_LIBRARY}")

    # 生成头文件
    set(HEADER_FILE_PATH "${TARGET_OUTPUT_DIR}/${TARGET_NAME}.h")
    string(APPEND HEADER_FILE_CONTENT "#pragma once\n")
    string(APPEND HEADER_FILE_CONTENT "#include <span>\n")
    foreach (BINARY_FILE ${ARGN})
        get_filename_component(BINARY_FILE_NAME ${BINARY_FILE} NAME)
        string(REGEX REPLACE "[/\\.]" "_" BINARY_SYMBOL ${BINARY_FILE_NAME})
        set(SYMBOL_START "_binary_${BINARY_SYMBOL}_start")
        set(SYMBOL_END "_binary_${BINARY_SYMBOL}_end")
        set(SYMBOL_SIZE "_binary_${BINARY_SYMBOL}_size")
        string(APPEND HEADER_FILE_CONTENT "extern char ${SYMBOL_START}\;\n")
        string(APPEND HEADER_FILE_CONTENT "extern char ${SYMBOL_END}\;\n")
        string(APPEND HEADER_FILE_CONTENT "extern char ${SYMBOL_SIZE}\;\n")
        string(APPEND HEADER_FILE_CONTENT "std::span<char> ${BINARY_SYMBOL}(&${SYMBOL_START}, &${SYMBOL_END})\;\n")
    endforeach ()
    FILE(WRITE ${HEADER_FILE_PATH} ${HEADER_FILE_CONTENT})

    add_library(${TARGET_NAME} STATIC ${HEADER_FILE_PATH} ${OUTPUT_OBJ_LIST})
    target_include_directories(${TARGET_NAME} PUBLIC ${TARGET_OUTPUT_DIR})
    set_target_properties(${TARGET_NAME} PROPERTIES LINKER_LANGUAGE CXX)
    #set_target_properties(${TARGET_NAME} PROPERTIES IMPORTED_LOCATION ${OUTPUT_LIBRARY})
endfunction()

add_model_target(models
        models/det/det.onnx
        models/cls/cls.onnx
        models/rec/rec.onnx)
target_link_libraries(${CMAKE_PROJECT_NAME} models)