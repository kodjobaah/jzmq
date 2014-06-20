LOCAL_PATH := $(call my-dir)
 

include $(CLEAR_VARS)
include C:\Users\whatamidoing\OpenCV-2.4.9-android-sdk\sdk\native\jni\OpenCV.mk

OPENGLES_LIB  := -lGLESv1_CM
OPENGLES_DEF  := -DUSE_OPENGL_ES_1_1
LOCAL_MODULE    := NativeCamera
LOCAL_SRC_FILES := src/CameraRenderer.cpp
LOCAL_LDLIBS +=  $(OPENGLES_LIB) -llog -ldl -lEGL
 
include $(BUILD_SHARED_LIBRARY)