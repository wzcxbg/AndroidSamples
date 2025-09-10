message(STATUS "LIBRARY_NAME:           ${LIBRARY_NAME}")           # 库名称
message(STATUS "LIBRARY_SOURCE_DIR:     ${LIBRARY_SOURCE_DIR}")     # 源码目录
message(STATUS "LIBRARY_BUILD_DIR:      ${LIBRARY_BUILD_DIR}")      # 构建目录
message(STATUS "LIBRARY_INSTALL_DIR:    ${LIBRARY_INSTALL_DIR}")    # 安装目录
message(STATUS "LIBRARY_BUILD_TYPE:     ${LIBRARY_BUILD_TYPE}")     # 编译类型
message(STATUS "LIBRARY_BUILD_SHARED:   ${LIBRARY_BUILD_SHARED}")   # 是否编译动态库
message(STATUS "CMAKE_SYSTEM_NAME:      ${CMAKE_SYSTEM_NAME}")      # 平台名
message(STATUS "CMAKE_SYSTEM_PROCESSOR: ${CMAKE_SYSTEM_PROCESSOR}") # 架构名

message(STATUS "CMAKE_SYSTEM_NAME:              ${CMAKE_SYSTEM_NAME}")
message(STATUS "CMAKE_EXPORT_COMPILE_COMMANDS:  ${CMAKE_EXPORT_COMPILE_COMMANDS}")
message(STATUS "CMAKE_SYSTEM_VERSION:           ${CMAKE_SYSTEM_VERSION}")
message(STATUS "ANDROID_PLATFORM:               ${ANDROID_PLATFORM}")
message(STATUS "ANDROID_ABI:                    ${ANDROID_ABI}")
message(STATUS "CMAKE_ANDROID_ARCH_ABI:         ${CMAKE_ANDROID_ARCH_ABI}")
message(STATUS "ANDROID_NDK:                    ${ANDROID_NDK}")
message(STATUS "CMAKE_ANDROID_NDK:              ${CMAKE_ANDROID_NDK}")
message(STATUS "CMAKE_TOOLCHAIN_FILE:           ${CMAKE_TOOLCHAIN_FILE}")
message(STATUS "CMAKE_MAKE_PROGRAM:             ${CMAKE_MAKE_PROGRAM}")
message(STATUS "CMAKE_BUILD_TYPE:               ${CMAKE_BUILD_TYPE}")

# .\build.bat --update --build
#   --skip_tests
#   --skip_onnx_tests
#   --skip_winml_tests
#   --skip_nodejs_tests
#   --config Debug
#   --build_shared_lib
#   --parallel
#   --compile_no_warning_as_error
find_program(PYTHON_EXECUTABLE NAMES python python3 REQUIRED)

if (LIBRARY_BUILD_SHARED)
    set(ONNXRUNTIME_BUILD_SHARED "--build_shared_lib")
else ()
    set(ONNXRUNTIME_BUILD_SHARED "")
endif ()

# 在Mac上编译Android程序
if (CMAKE_HOST_SYSTEM_NAME STREQUAL "Windows" AND CMAKE_SYSTEM_NAME STREQUAL "Windows")
    execute_process(
            COMMAND build.bat
            --build_dir "${LIBRARY_BUILD_DIR}"
            --update --build
            --skip_tests
            --skip_onnx_tests
            --skip_winml_tests
            --skip_nodejs_tests
            --config ${LIBRARY_BUILD_TYPE}
            ${ONNXRUNTIME_BUILD_SHARED}
            --parallel
            --compile_no_warning_as_error
            WORKING_DIRECTORY "${LIBRARY_SOURCE_DIR}"
            COMMAND_ERROR_IS_FATAL ANY
    )
elseif (CMAKE_HOST_SYSTEM_NAME STREQUAL "Darwin" AND CMAKE_SYSTEM_NAME STREQUAL "Android")
    execute_process(
            COMMAND ./build.sh --android
            --android_sdk_path "${CMAKE_ANDROID_NDK}/../../"
            --android_ndk_path "${CMAKE_ANDROID_NDK}"
            --android_abi ${ANDROID_ABI}
            --android_api ${CMAKE_SYSTEM_VERSION}
            --build_dir "${LIBRARY_BUILD_DIR}"
            --cmake_generator Ninja
            --update --build
            --skip_tests
            --skip_onnx_tests
            --skip_winml_tests
            --skip_nodejs_tests
            --config ${LIBRARY_BUILD_TYPE}
            ${ONNXRUNTIME_BUILD_SHARED}
            --parallel
            --compile_no_warning_as_error
            WORKING_DIRECTORY "${LIBRARY_SOURCE_DIR}"
            COMMAND_ERROR_IS_FATAL ANY
    )
endif ()

execute_process(
        COMMAND ${CMAKE_COMMAND}
        --install "${LIBRARY_BUILD_DIR}/${LIBRARY_BUILD_TYPE}"
        --config ${LIBRARY_BUILD_TYPE}
        --prefix ${LIBRARY_INSTALL_DIR}
        COMMAND_ERROR_IS_FATAL ANY
)