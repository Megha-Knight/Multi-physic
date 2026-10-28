cmake_minimum_required(VERSION 3.20)

# Locate local LLVM toolchain if not explicitly provided or invalid
if(NOT DEFINED CMAKE_C_COMPILER OR NOT EXISTS "${CMAKE_C_COMPILER}")
    if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/../src/main/cpp/bin/clang.exe")
        set(CMAKE_C_COMPILER "${CMAKE_CURRENT_SOURCE_DIR}/../src/main/cpp/bin/clang.exe" CACHE FILEPATH "C Compiler" FORCE)
    elseif(EXISTS "E:/LLVM/bin/clang.exe")
        set(CMAKE_C_COMPILER "E:/LLVM/bin/clang.exe" CACHE FILEPATH "C Compiler" FORCE)
    endif()
endif()

if(NOT DEFINED CMAKE_CXX_COMPILER OR NOT EXISTS "${CMAKE_CXX_COMPILER}")
    if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/../src/main/cpp/bin/clang++.exe")
        set(CMAKE_CXX_COMPILER "${CMAKE_CURRENT_SOURCE_DIR}/../src/main/cpp/bin/clang++.exe" CACHE FILEPATH "CXX Compiler" FORCE)
    elseif(EXISTS "E:/LLVM/bin/clang++.exe")
        set(CMAKE_CXX_COMPILER "E:/LLVM/bin/clang++.exe" CACHE FILEPATH "CXX Compiler" FORCE)
    endif()
endif()

set(CMAKE_C_COMPILER_TARGET x86_64-w64-windows-gnu CACHE STRING "Target" FORCE)
set(CMAKE_CXX_COMPILER_TARGET x86_64-w64-windows-gnu CACHE STRING "Target" FORCE)

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
    src/astra_geometry.cpp
)

target_compile_definitions(astra_native PRIVATE ASTRA_BUILD_DLL)

# Set library output directory
set_target_properties(astra_native PROPERTIES
    ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)

# Standalone native test (math & raycast)
add_executable(test_native_main
    tests/test_native_main.cpp
)

target_link_libraries(test_native_main PRIVATE astra_native)
set_target_properties(test_native_main PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)

# Standalone native test (geometry ABI contract)
add_executable(test_geometry_abi_contract
    tests/test_geometry_abi_contract.c
)

target_link_libraries(test_geometry_abi_contract PRIVATE astra_native)
set_target_properties(test_geometry_abi_contract PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)
