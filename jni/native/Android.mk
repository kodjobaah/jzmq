LOCAL_PATH := $(call my-dir)


#include C:\Users\whatamidoing\OpenCV-2.4.9-android-sdk\sdk\native\jni\OpenCV.mk

OPENCV_SDK := C:/Users/whatamidoing/OpenCV-2.4.9-android-sdk

include $(CLEAR_VARS)
# OpenCV
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
#WITH_TBB=ON
#OPENCV_LIB_TYPE:=STATIC
include $(OPENCV_SDK)/sdk/native/jni/OpenCV.mk

ANDROID_NDK :=  C:/Users/whatamidoing/android-ndk-r9b
CPP_STATIC := C:/Users/whatamidoing/android-ndk-r9b/sources/cxx-stl/gnu-libstdc++/4.8/libs/armeabi-v7a/libgnustl_static.a

OPENGLES_LIB  := -lGLESv1_CM
OPENGLES_DEF  := -DUSE_OPENGL_ES_1_1
LOCAL_MODULE    := NativeCamera
LOCAL_SRC_FILES := src/CameraRenderer.cpp \
                   src/FpsMeter.cpp \
                   src/cdecode.c \
                   src/cencode.c \
                   src/global.cpp \
                   src/msgpacktest.cpp

LOCAL_STATIC_LIBRARIES := boost_system boost_thread libzmq msgpack 
LOCAL_C_INCLUDES       += $(LOCAL_PATH)/include \
						  ../zeromq/include \
						  C:/Users/whatamidoing/android-ndk-r9b/sources/boost/include/boost-1_55


LOCAL_LDLIBS +=  $(OPENGLES_LIB) -llog -ldl -lEGL  -lGLESv2 \
			$(CPP_STATIC)
include $(BUILD_SHARED_LIBRARY)
 $(call import-module,boost) 