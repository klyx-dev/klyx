cmake_minimum_required(VERSION 3.22.1)

project("klyx")
set(CPP_DIR src/main/cpp)

add_library(${CMAKE_PROJECT_NAME} SHARED
        ${CPP_DIR}/os.c
)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
)
