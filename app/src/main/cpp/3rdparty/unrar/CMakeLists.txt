cmake_minimum_required(VERSION 3.22.1)

option(BUILD_SHARED "Build the shared library" ON)
option(BUILD_STATIC "Build the static library" ON)

# 官方代码适配Android有点问题，需要手动修改consio.cpp ulinks.cpp
set(UNRAR_SOURCE src/filestr.cpp src/recvol.cpp src/rs.cpp src/scantree.cpp src/qopen.cpp)
set(LIBRARY_SOURCE src/filestr.cpp src/scantree.cpp src/dll.cpp src/qopen.cpp)
set(COMMON_SOURCE src/rar.cpp src/strlist.cpp src/strfn.cpp src/pathfn.cpp src/smallfn.cpp
        src/global.cpp src/file.cpp src/filefn.cpp src/filcreat.cpp src/archive.cpp
        src/arcread.cpp src/unicode.cpp src/system.cpp src/crypt.cpp src/crc.cpp
        src/rawread.cpp src/encname.cpp src/resource.cpp src/match.cpp src/timefn.cpp
        src/rdwrfn.cpp src/consio.cpp src/options.cpp src/errhnd.cpp src/rarvm.cpp
        src/secpassword.cpp src/rijndael.cpp src/getbits.cpp src/sha1.cpp src/sha256.cpp
        src/blake2s.cpp src/hash.cpp src/extinfo.cpp src/extract.cpp src/volume.cpp
        src/list.cpp src/find.cpp src/unpack.cpp src/headers.cpp src/threadpool.cpp
        src/rs16.cpp src/cmddata.cpp src/ui.cpp)

if (BUILD_SHARED OR BUILD_STATIC)
    if (BUILD_SHARED)
        add_library(unrar SHARED ${LIBRARY_SOURCE} ${COMMON_SOURCE})
    else ()
        add_library(unrar STATIC ${LIBRARY_SOURCE} ${COMMON_SOURCE})
    endif ()
    target_include_directories(unrar PUBLIC src)
    target_compile_definitions(unrar PUBLIC RARDLL)
else ()
    add_executable(unrar ${UNRAR_SOURCE} ${COMMON_SOURCE})
endif ()

target_compile_features(unrar PRIVATE cxx_std_11)
target_compile_options(unrar PRIVATE -Wno-logical-op-parentheses -Wno-switch -Wno-dangling-else)
target_compile_definitions(unrar PRIVATE _FILE_OFFSET_BITS=64 _LARGEFILE_SOURCE RAR_SMP)