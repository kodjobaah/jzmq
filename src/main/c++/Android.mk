LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES= C:\Users\whatamidoing\hey\whatamidoingffmpeg\jni\zeromq\include

LOCAL_CPP_EXTENSION := .cxx .cpp .cc
LOCAL_CPPFLAGS := -g -O2 -D_GNU_SOURCE -D_REENTRANT -D_THREAD_SAFE -fPIC -DZMQ_USE_EPOLL
LOCAL_LIBS := -lrt -lpthread -lgcc
LOCAL_MODULE := jzmq
LOCAL_SRC_FILES := Event.cpp \
	Context.cpp \
	Event.cpp \
	Poller.cpp \
	Socket.cpp \
	ZMQ.cpp \
	util.cpp

LOCAL_LDLIBS := -ldl
LOCAL_SHARED_LIBRARIES := libzmq
include $(BUILD_SHARED_LIBRARY)