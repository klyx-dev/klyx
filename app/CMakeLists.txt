cmake_minimum_required(VERSION 3.22.1)

project(klyx C)

set(CPP_DIR src/main/cpp)

add_library(klyx SHARED
        ${CPP_DIR}/os.c
)

target_link_libraries(klyx
        android
        log
)
