function(convert_path OUTPUT_VAR INPUT_PATH NAMING EXCLUDE_EXT)
    if (EXCLUDE_EXT)
        string(REGEX REPLACE "\\.[^.]*$" "" INPUT_PATH "${INPUT_PATH}")
    endif ()
    string(TOLOWER "${INPUT_PATH}" INPUT_PATH)
    # 按如下内容拆分路径[ :\  :/  /  \  .  _  - ]
    string(REGEX REPLACE "(:\\\\|:/|[/\\._-])" ";" WORD_LIST "${INPUT_PATH}")

    if (NAMING STREQUAL "lowerCamelCase")
        set(RESULT "")
        set(IS_FIRST_WORD TRUE)
        foreach (WORD IN LISTS WORD_LIST)
            if (IS_FIRST_WORD)
                set(RESULT "${WORD}")
                set(IS_FIRST_WORD FALSE)
            else ()
                string(SUBSTRING "${WORD}" 0 1 FIRST_CHAR)
                string(SUBSTRING "${WORD}" 1 -1 REST_OF_WORD)
                string(TOUPPER "${FIRST_CHAR}" FIRST_CHAR)
                set(RESULT "${RESULT}${FIRST_CHAR}${REST_OF_WORD}")
            endif ()
        endforeach ()
        set(${OUTPUT_VAR} "${RESULT}" PARENT_SCOPE)
        return()
    elseif (NAMING STREQUAL "UpperCamelCase")
        set(RESULT "")
        foreach (WORD IN LISTS WORD_LIST)
            string(SUBSTRING "${WORD}" 0 1 FIRST_CHAR)
            string(SUBSTRING "${WORD}" 1 -1 REST_OF_WORD)
            string(TOUPPER "${FIRST_CHAR}" FIRST_CHAR)
            set(RESULT "${RESULT}${FIRST_CHAR}${REST_OF_WORD}")
        endforeach ()
        set(${OUTPUT_VAR} "${RESULT}" PARENT_SCOPE)
        return()
    elseif (NAMING STREQUAL "SCREAMING_SNAKE_CASE")
        string(REPLACE ";" "_" RESULT "${WORD_LIST}")
        string(TOUPPER "${RESULT}" RESULT)
        set(${OUTPUT_VAR} "${RESULT}" PARENT_SCOPE)
        return()
    elseif (NAMING STREQUAL "snake_case")
        string(REPLACE ";" "_" RESULT "${WORD_LIST}")
        set(${OUTPUT_VAR} "${RESULT}" PARENT_SCOPE)
        return()
    elseif (NAMING STREQUAL "kebab-case")
        string(REPLACE ";" "-" RESULT "${WORD_LIST}")
        set(${OUTPUT_VAR} "${RESULT}" PARENT_SCOPE)
        return()
    elseif (NAMING STREQUAL "dot.case")
        string(REPLACE ";" "." RESULT "${WORD_LIST}")
        set(${OUTPUT_VAR} "${RESULT}" PARENT_SCOPE)
        return()
    elseif (NAMING STREQUAL "flatcase")
        string(REPLACE ";" "" RESULT "${WORD_LIST}")
        set(${OUTPUT_VAR} "${RESULT}" PARENT_SCOPE)
        return()
    endif ()
endfunction()


function(add_binary_target_for_android TARGET_NAME)
    # 存在的问题: 只支持arm64-v8a，只支持C++20
    # 将二进制文件编译成库文件
    # objcopy -I binary -O elf64-aarch64 -B aarch64 sample.jpg sample.o
    # ld -shared -o libexample.so example.o
    # ar -rcs libsample.a sample.o
    if (NOT CMAKE_CXX_COMPILER OR NOT CMAKE_OBJCOPY OR NOT CMAKE_LINKER OR NOT CMAKE_AR)
        message(FATAL_ERROR "Toolchain not support!")
    endif ()
    set(RES_NAMESPACE "Res")
    set(RES_FILENAME "${TARGET_NAME}")
    set(OUTPUT_FILE_DIR ${CMAKE_CURRENT_BINARY_DIR}/CMakeFiles/${TARGET_NAME}.dir)
    set(RES_H_FILE_DIR "${OUTPUT_FILE_DIR}/include")
    set(RES_H_FILE_PATH "${OUTPUT_FILE_DIR}/include/${TARGET_NAME}.h")
    set(RES_CPP_FILE_PATH "${OUTPUT_FILE_DIR}/${TARGET_NAME}.cpp")
    set(RES_OBJ_FILE_PATH "${OUTPUT_FILE_DIR}/${TARGET_NAME}.o")
    set(RES_LIB_FILE_PATH "${OUTPUT_FILE_DIR}/${TARGET_NAME}.a")
    message("RES_H_FILE_DIR ${RES_H_FILE_DIR}")
    message("RES_H_FILE_PATH ${RES_H_FILE_PATH}")
    message("RES_LIB_FILE_PATH ${RES_LIB_FILE_PATH}")

    # 使用缓存屏蔽重复生成
    set(CONFIG_CACHE_NAME "${TARGET_NAME}_CACHE")
    if (NOT "${${CONFIG_CACHE_NAME}}" STREQUAL "${ARGN}")
        message("${TARGET_NAME}: Generating ...")
        set(${CONFIG_CACHE_NAME} "${ARGN}" CACHE INTERNAL "inputs of last configured" FORCE)
    else ()
        message("${TARGET_NAME}: Use last generated")
        add_library(${TARGET_NAME} STATIC IMPORTED)
        target_include_directories(${TARGET_NAME} INTERFACE ${RES_H_FILE_DIR})
        set_target_properties(${TARGET_NAME} PROPERTIES LINKER_LANGUAGE CXX)
        set_target_properties(${TARGET_NAME} PROPERTIES IMPORTED_LOCATION ${RES_LIB_FILE_PATH})
        return()
    endif ()

    # 二进制文件生成OBJ
    set(OBJECT_FILE_PATHS)
    foreach (BINARY_FILE ${ARGN})
        get_filename_component(INPUT_FILE ${BINARY_FILE} ABSOLUTE)
        get_filename_component(INPUT_FILE_NAME ${BINARY_FILE} NAME)
        get_filename_component(INPUT_FILE_NAME_WE ${BINARY_FILE} NAME_WE)
        set(OUTPUT_OBJ "${OUTPUT_FILE_DIR}/${INPUT_FILE_NAME_WE}.o")
        file(MAKE_DIRECTORY ${OUTPUT_FILE_DIR})

        execute_process(
                WORKING_DIRECTORY ${OUTPUT_FILE_DIR}
                COMMAND ${CMAKE_COMMAND} -E copy_if_different ${INPUT_FILE} .
                COMMAND_ERROR_IS_FATAL ANY)
        execute_process(
                COMMAND ${CMAKE_OBJCOPY} -I binary -O elf64-aarch64 -B aarch64 ${INPUT_FILE_NAME} ${OUTPUT_OBJ}
                WORKING_DIRECTORY ${OUTPUT_FILE_DIR}
                COMMAND_ERROR_IS_FATAL ANY)

        list(APPEND OBJECT_FILE_PATHS ${OUTPUT_OBJ})
    endforeach ()

    # 生成头文件
    set(RES_H_FILE_CONTENT "#pragma once\n\n")
    string(APPEND RES_H_FILE_CONTENT "#include <span>\n\n")
    string(APPEND RES_H_FILE_CONTENT "namespace Res {\n")
    foreach (BINARY_FILE ${ARGN})
        convert_path(BINARY_FILE_VAR_ID "${BINARY_FILE}" "lowerCamelCase" "FALSE")
        string(APPEND RES_H_FILE_CONTENT "    extern std::span<char> ${BINARY_FILE_VAR_ID}\;\n")
    endforeach ()
    string(APPEND RES_H_FILE_CONTENT "}\n")
    file(WRITE ${RES_H_FILE_PATH} ${RES_H_FILE_CONTENT})

    # 生成源文件
    set(RES_CPP_FILE_CONTENT "#include \"${RES_FILENAME}.h\"\n\n")
    foreach (BINARY_FILE ${ARGN})
        get_filename_component(BINARY_FILE_NAME ${BINARY_FILE} NAME)
        string(REGEX REPLACE "[/\\.]" "_" BINARY_SYMBOL ${BINARY_FILE_NAME})
        set(SYMBOL_START "_binary_${BINARY_SYMBOL}_start")
        set(SYMBOL_END "_binary_${BINARY_SYMBOL}_end")
        set(SYMBOL_SIZE "_binary_${BINARY_SYMBOL}_size")
        string(APPEND RES_CPP_FILE_CONTENT "extern char ${SYMBOL_START}\;\n")
        string(APPEND RES_CPP_FILE_CONTENT "extern char ${SYMBOL_END}\;\n")
        string(APPEND RES_CPP_FILE_CONTENT "extern char ${SYMBOL_SIZE}\;\n")
        string(APPEND RES_CPP_FILE_CONTENT "\n")
    endforeach ()
    string(APPEND RES_CPP_FILE_CONTENT "namespace Res {\n")
    foreach (BINARY_FILE ${ARGN})
        get_filename_component(BINARY_FILE_NAME ${BINARY_FILE} NAME)
        string(REGEX REPLACE "[/\\.]" "_" BINARY_SYMBOL ${BINARY_FILE_NAME})
        set(SYMBOL_START "_binary_${BINARY_SYMBOL}_start")
        set(SYMBOL_END "_binary_${BINARY_SYMBOL}_end")
        set(SYMBOL_SIZE "_binary_${BINARY_SYMBOL}_size")
        convert_path(BINARY_FILE_VAR_ID "${BINARY_FILE}" "lowerCamelCase" "FALSE")
        string(APPEND RES_CPP_FILE_CONTENT "    std::span<char> ${BINARY_FILE_VAR_ID}(&${SYMBOL_START}, &${SYMBOL_END})\;\n")
    endforeach ()
    string(APPEND RES_CPP_FILE_CONTENT "}\n")
    file(WRITE ${RES_CPP_FILE_PATH} ${RES_CPP_FILE_CONTENT})

    # 编译源文件
    execute_process(
            COMMAND ${CMAKE_CXX_COMPILER} --target=aarch64-linux-android21 -std=c++20 -fPIC
            -c ${RES_CPP_FILE_PATH} -o ${RES_OBJ_FILE_PATH} -I ${RES_H_FILE_DIR}
            WORKING_DIRECTORY ${OUTPUT_FILE_DIR}
            COMMAND_ERROR_IS_FATAL ANY)
    list(APPEND OBJECT_FILE_PATHS ${RES_OBJ_FILE_PATH})

    # 生成库文件
    execute_process(
            COMMAND ${CMAKE_AR} -rcs ${RES_LIB_FILE_PATH} ${OBJECT_FILE_PATHS}
            WORKING_DIRECTORY ${OUTPUT_FILE_DIR}
            COMMAND_ERROR_IS_FATAL ANY)

    # 添加库文件
    add_library(${TARGET_NAME} STATIC IMPORTED)
    target_include_directories(${TARGET_NAME} INTERFACE ${RES_H_FILE_DIR})
    set_target_properties(${TARGET_NAME} PROPERTIES LINKER_LANGUAGE CXX)
    set_target_properties(${TARGET_NAME} PROPERTIES IMPORTED_LOCATION ${RES_LIB_FILE_PATH})
endfunction()


function(add_binary_target_for_windows TARGET_NAME)
    # 存在的问题: RES_NAME可能重复、RES_CPP_FILE不够简洁
    set(RES_NAMESPACE "Res")
    set(RES_FILENAME "${TARGET_NAME}")
    set(OUTPUT_FILE_DIR ${CMAKE_CURRENT_BINARY_DIR}/CMakeFiles/${TARGET_NAME}.dir)
    set(RESOURCE_H_FILE_PATH "${OUTPUT_FILE_DIR}/Resource.h")
    set(RESOURCE_RC_FILE_PATH "${OUTPUT_FILE_DIR}/Resource.rc")
    set(RES_H_FILE_DIR "${OUTPUT_FILE_DIR}/include")
    set(RES_LAZY_VAL_H_FILE_PATH "${OUTPUT_FILE_DIR}/include/LazyVal.h")
    set(RES_H_FILE_PATH "${OUTPUT_FILE_DIR}/include/${RES_FILENAME}.h")
    set(RES_CPP_FILE_PATH "${OUTPUT_FILE_DIR}/${RES_FILENAME}.cpp")

    # 使用缓存屏蔽重复生成
    set(CONFIG_CACHE_NAME "${TARGET_NAME}_CACHE")
    if (NOT "${${CONFIG_CACHE_NAME}}" STREQUAL "${ARGN}")
        message(STATUS "${TARGET_NAME}: Generating ...")
        set(${CONFIG_CACHE_NAME} "${ARGN}" CACHE INTERNAL "inputs of last configured" FORCE)
    else ()
        message(STATUS "${TARGET_NAME}: Use last generated")
        add_library(${TARGET_NAME} OBJECT IMPORTED)
        target_sources(${TARGET_NAME} INTERFACE ${RESOURCE_RC_FILE_PATH} ${RES_CPP_FILE_PATH})
        target_include_directories(${TARGET_NAME} INTERFACE ${RES_H_FILE_DIR})
        return()
    endif ()

    # 生成Resource.h
    if (NOT GLOBAL_RESOURCE_ID)
        set(RESOURCE_ID 101)
    else ()
        set(RESOURCE_ID ${GLOBAL_RESOURCE_ID})
    endif ()
    set(RES_FILE_CONTENT "#pragma once\n\n")
    foreach (BINARY_FILE IN LISTS ARGN)
        get_filename_component(BINARY_FILE_ABS_PATH "${BINARY_FILE}" ABSOLUTE)
        convert_path(BINARY_FILE_RES_ID "${BINARY_FILE}" "SCREAMING_SNAKE_CASE" "FALSE")
        string(APPEND RES_FILE_CONTENT "#define ${BINARY_FILE_RES_ID} ${RESOURCE_ID}\n")
        math(EXPR RESOURCE_ID "${RESOURCE_ID} + 1")
    endforeach ()
    set(GLOBAL_RESOURCE_ID "${RESOURCE_ID}" CACHE INTERNAL "global id for resource" FORCE)
    file(WRITE "${RESOURCE_H_FILE_PATH}" "${RES_FILE_CONTENT}")

    # 生成Resource.rc
    set(RC_FILE_CONTENT "#include \"Resource.h\"\n\n")
    foreach (BINARY_FILE IN LISTS ARGN)
        get_filename_component(BINARY_FILE_ABS_PATH "${BINARY_FILE}" ABSOLUTE)
        convert_path(BINARY_FILE_RES_ID "${BINARY_FILE}" "SCREAMING_SNAKE_CASE" "FALSE")
        string(APPEND RC_FILE_CONTENT "${BINARY_FILE_RES_ID} RCDATA \"${BINARY_FILE_ABS_PATH}\"\n")
    endforeach ()
    file(WRITE "${RESOURCE_RC_FILE_PATH}" "${RC_FILE_CONTENT}")

    # 生成头文件LazyVal.h
    set(RES_LAZY_VAL_H_FILE_CONTENT "#ifndef LAZYVAL_H\n#define LAZYVAL_H\n\n")
    string(APPEND RES_LAZY_VAL_H_FILE_CONTENT "#include <optional>\n#include <functional>\n#include <type_traits>\n\n")
    string(APPEND RES_LAZY_VAL_H_FILE_CONTENT "template<class T>\nclass LazyVal {\n    std::function<T()> delegate;\n    std::optional<T> container;\n\npublic:\n    template<class F, std::enable_if_t<\n        std::is_invocable_r_v<T, F>, int>  = 0>\n    LazyVal(F &&callable) /*NOLINT*/\n        : delegate(callable) {\n    }\n\n    operator const T &() const noexcept { /*NOLINT*/\n        if (!container.has_value()) {\n            const_cast<LazyVal *>(this)\n                    ->container = delegate();\n        }\n        return container.value();\n    }\n};\n\n")
    string(APPEND RES_LAZY_VAL_H_FILE_CONTENT "#endif //LAZYVAL_H\n")
    file(WRITE "${RES_LAZY_VAL_H_FILE_PATH}" "${RES_LAZY_VAL_H_FILE_CONTENT}")

    # 生成头文件${RES_FILENAME}.h
    set(RES_H_FILE_CONTENT "#pragma once\n\n#include \"LazyVal.h\"\n\n")
    string(APPEND RES_H_FILE_CONTENT "namespace ${RES_NAMESPACE} {\n")
    foreach (BINARY_FILE IN LISTS ARGN)
        convert_path(BINARY_FILE_VAR_ID "${BINARY_FILE}" "lowerCamelCase" "FALSE")
        string(APPEND RES_H_FILE_CONTENT "    extern const LazyVal<std::vector<uint8_t>> ${BINARY_FILE_VAR_ID};\n")
    endforeach ()
    string(APPEND RES_H_FILE_CONTENT "}\n")
    file(WRITE "${RES_H_FILE_PATH}" "${RES_H_FILE_CONTENT}")

    # 生成源文件${RES_FILENAME}.cpp
    set(RES_CPP_FILE_CONTENT "#include <windows.h>\n\n#include <iostream>\n#include \"Resource.h\"\n#include \"${RES_FILENAME}.h\"\n\n")
    string(APPEND RES_CPP_FILE_CONTENT "inline std::vector<uint8_t> LoadResourceById(const WORD id) {\n    HMODULE hModule = GetModuleHandle(nullptr);\n\n    HRSRC hResource = FindResource(hModule,MAKEINTRESOURCE(id), RT_RCDATA);\n    if (hResource == nullptr) {\n        std::cerr << \"Failed to find resource: \" << GetLastError() << std::endl;\n        exit(-1);\n    }\n\n    HGLOBAL hResourceData = LoadResource(hModule, hResource);\n    if (hResourceData == nullptr) {\n        std::cerr << \"Failed to load resource: \" << GetLastError() << std::endl;\n        exit(-1);\n    }\n\n    LPVOID pResourceData = LockResource(hResourceData);\n    if (pResourceData == nullptr) {\n        std::cerr << \"Failed to lock resource: \" << GetLastError() << std::endl;\n        FreeResource(hResourceData);\n        exit(-1);\n    }\n    DWORD resourceSize = SizeofResource(hModule, hResource);\n\n    std::vector<uint8_t> resourceData(\n        static_cast<uint8_t *>(pResourceData),\n        static_cast<uint8_t *>(pResourceData) + resourceSize);\n\n    FreeResource(hResourceData);\n    return resourceData;\n}\n\n")
    string(APPEND RES_CPP_FILE_CONTENT "class ResourceLoader {\n    WORD resId;\n\npublic:\n    explicit ResourceLoader(WORD resId)\n        : resId(resId) {\n    }\n\n    std::vector<uint8_t> operator()() const {\n        return LoadResourceById(resId);\n    }\n};\n\n")
    string(APPEND RES_CPP_FILE_CONTENT "namespace ${RES_NAMESPACE} {\n")
    foreach (BINARY_FILE IN LISTS ARGN)
        convert_path(BINARY_FILE_RES_ID "${BINARY_FILE}" "SCREAMING_SNAKE_CASE" "FALSE")
        convert_path(BINARY_FILE_VAR_ID "${BINARY_FILE}" "lowerCamelCase" "FALSE")
        string(APPEND RES_CPP_FILE_CONTENT "    const LazyVal<std::vector<uint8_t>> ${BINARY_FILE_VAR_ID} = ResourceLoader(${BINARY_FILE_RES_ID});\n")
    endforeach ()
    string(APPEND RES_CPP_FILE_CONTENT "}\n")
    file(WRITE "${RES_CPP_FILE_PATH}" "${RES_CPP_FILE_CONTENT}")

    # 添加OBJECT库${TARGET_NAME}
    add_library(${TARGET_NAME} OBJECT IMPORTED)
    target_sources(${TARGET_NAME} INTERFACE ${RESOURCE_RC_FILE_PATH} ${RES_CPP_FILE_PATH})
    target_include_directories(${TARGET_NAME} INTERFACE ${RES_H_FILE_DIR})
endfunction()

function(add_binary_target TARGET_NAME)
    if (CMAKE_SYSTEM_NAME STREQUAL "Android")
        message(STATUS "Configuring on/for Android")
        add_binary_target_for_android(${TARGET_NAME} ${ARGN})
    elseif (CMAKE_SYSTEM_NAME STREQUAL "Windows")
        message(STATUS "Configuring on/for Windows")
        add_binary_target_for_windows(${TARGET_NAME} ${ARGN})
    else ()
        message(FATAL_ERROR "unsupported platform")
    endif ()
endfunction()