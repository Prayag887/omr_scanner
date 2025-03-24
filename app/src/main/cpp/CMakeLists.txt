cmake_minimum_required(VERSION 3.10.2)
project("myapplication")

# Set the path to the OpenCVConfig.cmake file
#set(OpenCV_DIR "C:/Users/user/AndroidStudioProjects/MyApplication/app/src/main/opencv/native")
set(OpenCV_DIR "C:/Users/user/AndroidStudioProjects/MyApplication/app/src/main/opencv/native/jni")
set(OpenCV_CONFIG_PATH "${OpenCV_DIR}")

# Find OpenCV
find_package(OpenCV REQUIRED)

# Add the native library
add_library(
        myapplication
        SHARED
        C:/Users/user/AndroidStudioProjects/MyApplication/app/src/main/cpp/native-lib.cpp
)

# Include OpenCV headers
include_directories(${OpenCV_INCLUDE_DIRS})

# Link OpenCV to the native library
target_link_libraries(
        myapplication
        ${OpenCV_LIBS}
)