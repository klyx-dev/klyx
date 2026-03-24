cmake_minimum_required(VERSION 3.22)

project("terminal_desktop" C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

find_package(JNI REQUIRED)

include_directories(
    ${JNI_INCLUDE_DIRS}
    "../include"
)

add_library(
    terminal SHARED
    terminal.c
    terminal_jni.c
)

target_link_libraries(terminal ${JNI_LIBRARIES})
