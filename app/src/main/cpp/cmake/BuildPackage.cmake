# 使用 /<library>/<platform>/<architecture>/<config> 的预构建库目录结构
# 例如:
#       3rdparty/prebuilt/spdlog/windows/x86_64/debug
#       3rdparty/sources/googletest
# prebuilt/
#├── spdlog/
#│   ├── windows/
#│   │   ├── x86_64/
#│   │   │   ├── release/
#│   │   │   │   ├── include/          # 头文件 (对于所有配置都一样，可以放在上层)
#│   │   │   │   ├── lib/              # .lib 文件
#│   │   │   │   └── bin/              # .dll 文件
#│   │   │   └── debug/
#│   │   │       ├── lib/              # spdlogd.lib (调试版)
#│   │   │       └── bin/              # spdlogd.dll
#│   │   └── x86/
#│   │       └── ... (32位版本)
#│   ├── linux/
#│   │   └── x86_64/
#│   │       ├── include/
#│   │       ├── lib/
#│   │       │   ├── libspdlog.a       # Release静态库
#│   │       │   └── libspdlog_d.a     # Debug静态库
#│   │       └── share/                # （可选）
#│   │           └── cmake/spdlog/
#│   │               └── spdlogConfig.cmake
#│   └── macos/
#│       ├── x86_64/
#│       │   └── ...
#│       └── arm64/
#│           └── ...
#│
#└── zlib/
#    ├── windows/
#    │   └── x86_64/
#    │       └── ...
#    └── linux/
#        └── x86_64/
#            └── ...
function(get_library_store_path RETURN_VAR LIB_NAME BUILD_TYPE BUILD_SHARED)
    # 确定平台名称
    if (CMAKE_SYSTEM_NAME STREQUAL "Windows")
        set(platform "windows")
    elseif (CMAKE_SYSTEM_NAME STREQUAL "Linux")
        set(platform "linux")
    elseif (CMAKE_SYSTEM_NAME STREQUAL "Darwin")
        set(platform "macos")
    else ()
        string(TOLOWER "${CMAKE_SYSTEM_NAME}" platform)
    endif ()

    # 确定架构名称
    if (CMAKE_SYSTEM_PROCESSOR MATCHES "AMD64|x86_64")
        set(arch "x86_64")
    elseif (CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64|arm64")
        set(arch "arm64")
    elseif (CMAKE_SYSTEM_PROCESSOR MATCHES "x86|i[3-6]86")
        set(arch "x86")
    else ()
        string(TOLOWER "${CMAKE_SYSTEM_PROCESSOR}" arch)
    endif ()

    # 确定构建配置
    set(config "debug")
    if (DEFINED BUILD_TYPE)
        string(TOLOWER "${BUILD_TYPE}" config)
    else ()
        string(TOLOWER "${CMAKE_BUILD_TYPE}" config)
    endif ()

    # 确定库类型
    set(linkage "static")
    if (DEFINED BUILD_SHARED)
        if (BUILD_SHARED)
            set(linkage "shared")
        endif ()
    elseif (DEFINED BUILD_SHARED_LIBS)
        if (BUILD_SHARED_LIBS)
            set(linkage "shared")
        endif ()
    endif ()

    # 组合成最终路径
    set(result_path "${CMAKE_SOURCE_DIR}/3rdparty/${LIB_NAME}/${platform}/${arch}/${config}_${linkage}")

    # 将结果返回给调用者
    set(${RETURN_VAR} "${result_path}" PARENT_SCOPE)
endfunction()


# 用法示例：
# build_package(
#        NAME spdlog
#        GIT_REPOSITORY https://github.com/gabime/spdlog.git
#        GIT_TAG v1.15.3
#        CONFIGURE_ARGS
#            -DSPDLOG_BUILD_SHARED=OFF
#            -DSPDLOG_INSTALL=ON
#)
function(build_package)
    cmake_parse_arguments(
            LIB
            ""
            "NAME;GIT_REPOSITORY;GIT_TAG;URL;BUILD_SCRIPT;BUILD_TYPE;BUILD_SHARED"
            "CONFIGURE_ARGS"
            ${ARGN}
    )

    # 打印参数
    message(STATUS "build_package called with:")
    message(STATUS "  NAME: ${LIB_NAME}")
    if (LIB_GIT_REPOSITORY)
        message(STATUS "  GIT_REPOSITORY: ${LIB_GIT_REPOSITORY}")
    endif ()
    if (LIB_GIT_TAG)
        message(STATUS "  GIT_TAG: ${LIB_GIT_TAG}")
    endif ()
    if (LIB_URL)
        message(STATUS "  URL: ${LIB_URL}")
    endif ()
    if (LIB_BUILD_SCRIPT)
        message(STATUS "  BUILD_SCRIPT: ${LIB_BUILD_SCRIPT}")
    endif ()
    if (LIB_CONFIGURE_ARGS)
        message(STATUS "  CONFIGURE_ARGS: ${LIB_CONFIGURE_ARGS}")
    endif ()
    if (LIB_BUILD_TYPE)
        message(STATUS "  BUILD_TYPE: ${LIB_BUILD_TYPE}")
    endif ()
    if (DEFINED LIB_BUILD_SHARED)
        message(STATUS "  BUILD_SHARED: ${LIB_BUILD_SHARED}")
    endif ()


    # 必须配置 NAME
    if (NOT LIB_NAME)
        message(FATAL_ERROR "Error: NAME is required.")
    endif ()
    # 不能同时配置 URL 和 GIT_REPOSITORY
    if (DEFINED LIB_URL AND DEFINED LIB_GIT_REPOSITORY)
        message(FATAL_ERROR "Error: Cannot specify both URL and GIT_REPOSITORY.")
    endif ()
    # 不能同时配置 CONFIGURE_ARGS 和 BUILD_SCRIPT
    if (DEFINED LIB_CONFIGURE_ARGS AND DEFINED LIB_BUILD_SCRIPT)
        message(FATAL_ERROR "Error: Cannot specify both CONFIGURE_ARGS and BUILD_SCRIPT.")
    endif ()
    if (NOT DEFINED LIB_BUILD_TYPE)
        set(LIB_BUILD_TYPE Debug)
    endif ()
    if (NOT DEFINED LIB_BUILD_SHARED)
        set(LIB_BUILD_SHARED TRUE)
    endif ()


    # 组合 FetchContent 参数 FETCH_CONTENT_ARGS
    set(LIB_FETCH_CONTENT_ARGS)
    if (DEFINED LIB_URL)
        list(APPEND LIB_FETCH_CONTENT_ARGS URL ${LIB_URL})
        list(APPEND LIB_FETCH_CONTENT_ARGS DOWNLOAD_EXTRACT_TIMESTAMP TRUE)
    elseif (DEFINED LIB_GIT_REPOSITORY)
        list(APPEND LIB_FETCH_CONTENT_ARGS GIT_REPOSITORY ${LIB_GIT_REPOSITORY})
        if (DEFINED LIB_GIT_TAG)
            list(APPEND LIB_FETCH_CONTENT_ARGS GIT_TAG ${LIB_GIT_TAG})
        endif ()
        list(APPEND LIB_FETCH_CONTENT_ARGS GIT_SHALLOW TRUE)
        list(APPEND LIB_FETCH_CONTENT_ARGS GIT_PROGRESS TRUE)
    endif ()

    _build_package_impl(
            "${LIB_NAME}"
            "${LIB_FETCH_CONTENT_ARGS}"
            "${LIB_CONFIGURE_ARGS}"
            "${LIB_BUILD_SCRIPT}"
            "${LIB_BUILD_TYPE}"
            "${LIB_BUILD_SHARED}"
    )
    set(${LIB_NAME}_ROOT "${${LIB_NAME}_ROOT}" PARENT_SCOPE)
endfunction()


function(_build_package_impl _NAME _FETCH_CONTENT_ARGS _CONFIGURE_ARGS _BUILD_SCRIPT _BUILD_TYPE _BUILD_SHARED)
    set(LIB_NAME ${_NAME})
    set(PRJ_NAME "${LIB_NAME}_project")

    # 1. 获取期望的预编译路径，并尝试查找已编译的库
    get_library_store_path(PREBUILT_PATH ${LIB_NAME} ${_BUILD_TYPE} ${_BUILD_SHARED})
    if (EXISTS "${PREBUILT_PATH}")
        set(${LIB_NAME}_ROOT "${PREBUILT_PATH}" PARENT_SCOPE)
        message(STATUS "Found prebuilt ${LIB_NAME} at: ${PREBUILT_PATH}")
        return()
    endif ()

    # 2. 如果没找到，进入自动构建和安装流程
    set(${PRJ_NAME}_SOURCE_DIR "${CMAKE_SOURCE_DIR}/3rdparty/sources/${PRJ_NAME}-src")
    set(${PRJ_NAME}_BINARY_DIR "${CMAKE_SOURCE_DIR}/3rdparty/sources/${PRJ_NAME}-build")
    if (NOT EXISTS ${PRJ_NAME}_SOURCE_DIR OR NOT EXISTS ${PRJ_NAME}_BINARY_DIR)
        include(FetchContent)
        message(STATUS "${PRJ_NAME}")
        message(STATUS "${_FETCH_CONTENT_ARGS}")
        FetchContent_Populate(
                "${PRJ_NAME}"
                "${_FETCH_CONTENT_ARGS}"
                SOURCE_DIR ${${PRJ_NAME}_SOURCE_DIR}
                BINARY_DIR ${${PRJ_NAME}_BINARY_DIR}
        )
        message(STATUS "${PRJ_NAME}_SOURCE_DIR: ${${PRJ_NAME}_SOURCE_DIR}")
        message(STATUS "${PRJ_NAME}_BINARY_DIR: ${${PRJ_NAME}_BINARY_DIR}")
    endif ()

    # 3. 优先使用构建脚本构建
    if (_BUILD_SCRIPT)
        MESSAGE(STATUS "Configuring ${LIB_NAME}...")
        EXECUTE_PROCESS(
                COMMAND ${CMAKE_COMMAND}
                # 通用部分
                -DLIBRARY_NAME=${_NAME}
                -DLIBRARY_SOURCE_DIR=${${PRJ_NAME}_SOURCE_DIR}
                -DLIBRARY_BUILD_DIR=${${PRJ_NAME}_BINARY_DIR}
                -DLIBRARY_INSTALL_DIR=${PREBUILT_PATH}
                -DLIBRARY_BUILD_TYPE=${_BUILD_TYPE}
                -DLIBRARY_BUILD_SHARED=${_BUILD_SHARED}
                -DCMAKE_SYSTEM_NAME=${CMAKE_SYSTEM_NAME}
                -DCMAKE_SYSTEM_PROCESSOR=${CMAKE_SYSTEM_PROCESSOR}
                # Android部分
                -DCMAKE_SYSTEM_NAME=${CMAKE_SYSTEM_NAME}
                -DCMAKE_EXPORT_COMPILE_COMMANDS=${CMAKE_EXPORT_COMPILE_COMMANDS}
                -DCMAKE_SYSTEM_VERSION=${CMAKE_SYSTEM_VERSION}
                -DANDROID_PLATFORM=${ANDROID_PLATFORM}
                -DANDROID_ABI=${ANDROID_ABI}
                -DCMAKE_ANDROID_ARCH_ABI=${CMAKE_ANDROID_ARCH_ABI}
                -DANDROID_NDK=${ANDROID_NDK}
                -DCMAKE_ANDROID_NDK=${CMAKE_ANDROID_NDK}
                -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
                -DCMAKE_MAKE_PROGRAM=${CMAKE_MAKE_PROGRAM}
                -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}
                -P ${_BUILD_SCRIPT}
                COMMAND_ERROR_IS_FATAL ANY
        )
        set(${LIB_NAME}_ROOT "${PREBUILT_PATH}" PARENT_SCOPE)
        message(STATUS "Prebuilt ${LIB_NAME} successfully built and installed.")
        return()
    endif ()

    set(LIB_CONFIGURE_ARGS
            -S ${${PRJ_NAME}_SOURCE_DIR}
            -B ${${PRJ_NAME}_BINARY_DIR}
            -G ${CMAKE_GENERATOR}
            -DCMAKE_INSTALL_PREFIX=${PREBUILT_PATH}
            -DCMAKE_BUILD_TYPE=${_BUILD_TYPE}
            -DCMAKE_POSITION_INDEPENDENT_CODE=ON
            -DBUILD_SHARED_LIBS=${_BUILD_SHARED}
            ${_CONFIGURE_ARGS}
    )

    # 4. 用cmake编译项目
    MESSAGE(STATUS "Configuring ${LIB_NAME}...")
    EXECUTE_PROCESS(
            COMMAND ${CMAKE_COMMAND} ${LIB_CONFIGURE_ARGS}
            OUTPUT_QUIET COMMAND_ERROR_IS_FATAL ANY
    )
    MESSAGE(STATUS "Building ${LIB_NAME}...")
    EXECUTE_PROCESS(
            COMMAND ${CMAKE_COMMAND} --build ${${PRJ_NAME}_BINARY_DIR} --config ${_BUILD_TYPE}
            OUTPUT_QUIET COMMAND_ERROR_IS_FATAL ANY
    )
    MESSAGE(STATUS "Installing ${LIB_NAME}...")
    EXECUTE_PROCESS(
            COMMAND ${CMAKE_COMMAND} --install ${${PRJ_NAME}_BINARY_DIR} --config ${_BUILD_TYPE}
            OUTPUT_QUIET COMMAND_ERROR_IS_FATAL ANY
    )
    MESSAGE(STATUS "Cleaning ${LIB_NAME}...")
    EXECUTE_PROCESS(
            COMMAND ${CMAKE_COMMAND} -E remove_directory ${${PRJ_NAME}_BINARY_DIR}
            OUTPUT_QUIET COMMAND_ERROR_IS_FATAL ANY
    )

    # 5. 重新查找配置库
    if (EXISTS "${PREBUILT_PATH}")
        set(${LIB_NAME}_ROOT "${PREBUILT_PATH}" PARENT_SCOPE)
        message(STATUS "Prebuilt ${LIB_NAME} successfully built and installed.")
        return()
    endif ()
endfunction()