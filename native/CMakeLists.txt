cmake_minimum_required(VERSION 3.20)
project(astra_native VERSION 0.1.0 LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Export compile commands for tooling
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

include_directories(include)

# Main native shared library
add_library(astra_native SHARED
    src/astra_native.cpp
)

target_compile_definitions(astra_native PRIVATE ASTRA_BUILD_DLL)

# Set library output directory
set_target_properties(astra_native PROPERTIES
    ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)

# Standalone native test
add_executable(test_native_main
    tests/test_native_main.cpp
)

target_link_libraries(test_native_main PRIVATE astra_native)
set_target_properties(test_native_main PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)
