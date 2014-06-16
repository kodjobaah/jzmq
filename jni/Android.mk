include $(CLEAR_VARS)

# OpenCV
#OPENCV_CAMERA_MODULES:=on
#OPENCV_INSTALL_MODULES:=on
include C:\Users\whatamidoing\OpenCV-2.4.9-android-sdk\sdk\native\jni\OpenCV.mk

# ZeroMQ
include $(CLEAR_VARS)

include  C:\Users\whatamidoing\hey\whatamidoingffmpeg\jni\zeromq\Android.mk


include $(CLEAR_VARS)
include  C:\Users\whatamidoing\hey\whatamidoingffmpeg\jni\jzmq\src\main\c++\Android.mk