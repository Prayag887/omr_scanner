cmake_minimum_required(VERSION 3.10.2)
project("myapplication")

# Dynamically get the path to the parent of CMakeLists.txt
get_filename_component(PARENT_DIR "${CMAKE_CURRENT_LIST_DIR}" DIRECTORY)

# Set the OpenCV config directory dynamically
set(OpenCV_DIR "${PARENT_DIR}/opencv/native/jni")

# Find OpenCV (must be defined before this call)
find_package(OpenCV REQUIRED)

# Add native library
add_library(
        myapplication
        SHARED
        ${PARENT_DIR}/cpp/native-lib.cpp
)

# Include OpenCV headers
include_directories(${OpenCV_INCLUDE_DIRS})

# Link OpenCV to the native library
target_link_libraries(
        myapplication
        ${OpenCV_LIBS}
)
