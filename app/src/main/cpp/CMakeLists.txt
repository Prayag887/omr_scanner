cmake_minimum_required(VERSION 3.10.2)
project("myapplication")

# Set the path to the OpenCVConfig.cmake file
#set(OpenCV_DIR "C:/Users/user/AndroidStudioProjects/MyApplication/app/src/main/opencv/native")
# Assume CMAKE_CURRENT_LIST_DIR is the current directory of the CMakeLists.txt
get_filename_component(PARENT_DIR "${CMAKE_CURRENT_LIST_DIR}" DIRECTORY)
set(OpenCV_DIR "${PARENT_DIR}/opencv/native/jni")

#set(OpenCV_DIR "/Users/prayag/projects/omr_scanner/app/src/main/opencv/native/jni")
#set(OpenCV_CONFIG_PATH "${OpenCV_DIR}")

# Find OpenCV
find_package(OpenCV REQUIRED)

# Add the native library
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