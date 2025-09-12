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

set(OpenCV_INCLUDE_DIR "/Users/bohong.wu/AndroidStudioProjects/AndroidSamples/app/src/main/cpp/3rdparty/OpenCV/android/arm64/debug_shared/sdk/native/jni/include")

file(WRITE "${LIBRARY_SOURCE_DIR}/${LIBRARY_NAME}Config.cmake.in"
"\@PACKAGE_INIT@
include(\${CMAKE_CURRENT_LIST_DIR}/${LIBRARY_NAME}Targets.cmake)
")

file(WRITE "${LIBRARY_SOURCE_DIR}/CMakeLists.txt"
"cmake_minimum_required(VERSION 3.22.1)
project(${LIBRARY_NAME})
add_library(${LIBRARY_NAME} STATIC
        \${CMAKE_CURRENT_SOURCE_DIR}/deploy/cpp_infer/src/postprocess_op.cpp
        \${CMAKE_CURRENT_SOURCE_DIR}/deploy/cpp_infer/src/preprocess_op.cpp
        \${CMAKE_CURRENT_SOURCE_DIR}/deploy/cpp_infer/src/clipper.cpp
        \${CMAKE_CURRENT_SOURCE_DIR}/deploy/cpp_infer/src/utility.cpp)
target_sources(${LIBRARY_NAME} PUBLIC
        FILE_SET public_headers TYPE HEADERS
        BASE_DIRS
        \${CMAKE_CURRENT_SOURCE_DIR}/deploy/cpp_infer
        FILES
        \${CMAKE_CURRENT_SOURCE_DIR}/deploy/cpp_infer/include/postprocess_op.h
        \${CMAKE_CURRENT_SOURCE_DIR}/deploy/cpp_infer/include/preprocess_op.h
        \${CMAKE_CURRENT_SOURCE_DIR}/deploy/cpp_infer/include/clipper.h
        \${CMAKE_CURRENT_SOURCE_DIR}/deploy/cpp_infer/include/utility.h
)
target_include_directories(${LIBRARY_NAME}
        PUBLIC $<BUILD_INTERFACE:\${CMAKE_CURRENT_SOURCE_DIR}/deploy/cpp_infer>
        PRIVATE ${OpenCV_INCLUDE_DIR})

include(GNUInstallDirs)
install(
        FILES
        \${CMAKE_CURRENT_BINARY_DIR}/${LIBRARY_NAME}Config.cmake
        \${CMAKE_CURRENT_BINARY_DIR}/${LIBRARY_NAME}ConfigVersion.cmake
        DESTINATION \${CMAKE_INSTALL_LIBDIR}/cmake/${LIBRARY_NAME}
)
install(TARGETS ${LIBRARY_NAME}
        EXPORT ${LIBRARY_NAME}Targets
        LIBRARY DESTINATION \${CMAKE_INSTALL_LIBDIR}
        ARCHIVE DESTINATION \${CMAKE_INSTALL_LIBDIR}
        FILE_SET public_headers DESTINATION \${CMAKE_INSTALL_INCLUDEDIR}
)

include(CMakePackageConfigHelpers)
configure_package_config_file(
        \${CMAKE_CURRENT_SOURCE_DIR}/${LIBRARY_NAME}Config.cmake.in
        \${CMAKE_CURRENT_BINARY_DIR}/${LIBRARY_NAME}Config.cmake
        INSTALL_DESTINATION ${CMAKE_INSTALL_LIBDIR}/cmake/${LIBRARY_NAME}
)
write_basic_package_version_file(
        \${CMAKE_CURRENT_BINARY_DIR}/${LIBRARY_NAME}ConfigVersion.cmake
        VERSION 1.0.0
        COMPATIBILITY AnyNewerVersion
)
install(
        EXPORT ${LIBRARY_NAME}Targets
        FILE ${LIBRARY_NAME}Targets.cmake
        NAMESPACE ${LIBRARY_NAME}::
        DESTINATION \${CMAKE_INSTALL_LIBDIR}/cmake/${LIBRARY_NAME}
)
")
MESSAGE(STATUS "Configuring ${LIBRARY_NAME}...")
MESSAGE(STATUS "${CMAKE_COMMAND} \
        -S ${LIBRARY_SOURCE_DIR} \
        -B ${LIBRARY_BUILD_DIR} \
        -G Ninja \
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE} \
        -DANDROID_PLATFORM=${ANDROID_PLATFORM} \
        -DANDROID_ABI=${ANDROID_ABI} \
        -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE} \
        -DCMAKE_INSTALL_PREFIX=${LIBRARY_INSTALL_DIR}")
EXECUTE_PROCESS(
        COMMAND ${CMAKE_COMMAND}
        -S ${LIBRARY_SOURCE_DIR}
        -B ${LIBRARY_BUILD_DIR}
        -G Ninja
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
        -DANDROID_PLATFORM=${ANDROID_PLATFORM}
        -DANDROID_ABI=${ANDROID_ABI}
        -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}
        -DCMAKE_INSTALL_PREFIX=${LIBRARY_INSTALL_DIR}
        COMMAND_ERROR_IS_FATAL ANY
)
MESSAGE(STATUS "Building ${LIBRARY_NAME}...")
EXECUTE_PROCESS(
        COMMAND ${CMAKE_COMMAND} --build ${LIBRARY_BUILD_DIR} --config ${LIBRARY_BUILD_TYPE}
        COMMAND_ERROR_IS_FATAL ANY
)
MESSAGE(STATUS "Installing ${LIBRARY_NAME}...")
EXECUTE_PROCESS(
        COMMAND ${CMAKE_COMMAND} --install ${LIBRARY_BUILD_DIR} --config ${LIBRARY_BUILD_TYPE}
        COMMAND_ERROR_IS_FATAL ANY
)