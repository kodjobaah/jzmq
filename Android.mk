LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
#openCV
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on

include /Users/kodjobaah/software/opencv/OpenCV-2.4.9-android-sdk/sdk/native/jni/OpenCV.mk

