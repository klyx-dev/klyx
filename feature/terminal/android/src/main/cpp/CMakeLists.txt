cmake_minimum_required(VERSION 3.22.1)

project("terminal" C)

set(SHARED_CPP_DIR "../../../../emulator/src/nativeInterop/cpp")
set(SHARED_INCLUDE_DIR "../../../../emulator/src/nativeInterop/include")

include_directories(${SHARED_INCLUDE_DIR})

add_library(
    terminal SHARED
    ${SHARED_CPP_DIR}/terminal.c
    ${SHARED_CPP_DIR}/terminal_jni.c 
)

find_library(log-lib log)
target_link_libraries(terminal ${log-lib})
