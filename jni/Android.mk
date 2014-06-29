LOCAL_PATH := $(call my-dir)
#OPENCV_SDK := /Users/kodjobaah/software/opencv/OpenCV-2.4.9-android-sdk
OPENCV_SDK := C:/Users/whatamidoing/OpenCV-2.4.9-android-sdk
include $(CLEAR_VARS)

# OpenCV
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
include $(OPENCV_SDK)/sdk/native/jni/OpenCV.mk

# ZeroMQ
include $(CLEAR_VARS)
#include  C:\Users\whatamidoing\hey\whatamidoingffmpeg\jni\zeromq\Android.mk

include  C:/Users/whatamidoing/hey/whatamidoingffmpeg/jni/zeromq/Android.mk


include $(CLEAR_VARS)
include C:/Users/whatamidoing/hey/whatamidoingffmpeg/jni/native/Android.mk

include $(CLEAR_VARS)
include  C:/Users/whatamidoing/hey/whatamidoingffmpeg/jni/jzmq/Android.mk

