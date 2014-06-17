LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

JZMQ_JAVA_FILES := \
    src/main/java/org/zeromq/EmbeddedLibraryTools.java \
    src/main/java/org/zeromq/ZAuth.java \
    src/main/java/org/zeromq/ZContext.java \
    src/main/java/org/zeromq/ZDispatcher.java \
    src/main/java/org/zeromq/ZFrame.java \
    src/main/java/org/zeromq/ZLoop.java \
    src/main/java/org/zeromq/ZMQ.java \
    src/main/java/org/zeromq/ZMQException.java \
    src/main/java/org/zeromq/ZMQForwarder.java \
    src/main/java/org/zeromq/ZMQQueue.java \
    src/main/java/org/zeromq/ZMQStreamer.java \
    src/main/java/org/zeromq/ZMsg.java \
    src/main/java/org/zeromq/ZThread.java

JZMQ_CPP_FILES := \
    src/main/c++/ZMQ.cpp \
    src/main/c++/Context.cpp \
    src/main/c++/Socket.cpp \
    src/main/c++/Poller.cpp \
    src/main/c++/util.cpp

JZMQ_H_FILES := \
    src/main/c++/org_zeromq_ZMQ.h \
    src/main/c++/org_zeromq_ZMQ_Context.h \
    src/main/c++/org_zeromq_ZMQ_Error.h \
    src/main/c++/org_zeromq_ZMQ_PollItem.h \
    src/main/c++/org_zeromq_ZMQ_Poller.h \
    src/main/c++/org_zeromq_ZMQ_Socket.h

JZMQ_HPP_FILES = \
    util.hpp

LOCAL_SRC_FILES := $(JZMQ_JAVA_FILES)

LOCAL_DEX_PREOPT := false 

LOCAL_MODULE := zmq

include $(BUILD_STATIC_JAVA_LIBRARY)

include $(CLEAR_VARS)

# Building headers using javah 

ifneq (,$(findstring /cygdrive/,$(PATH)))
    UNAME := Cygwin
else
ifneq (,$(findstring windows,$(PATH)))
    UNAME := Windows
else
    UNAME := $(shell uname -s)
endif
endif

ifeq ($(UNAME), Linux)
java_files := $(shell ls $(LOCAL_PATH)/src/main/java/org/zeromq)
$(shell $(foreach java_file,$(java_files), $(shell javac -classpath $(LOCAL_PATH)/src/main/c++ -sourcepath $(LOCAL_PATH)/src/main/java -d $(LOCAL_PATH)/src/main/c++ $(LOCAL_PATH)/src/main/java/org/zeromq/$(java_file))))
$(shell pushd $(LOCAL_PATH)/src/main/c++ > /dev/null; javah -jni org.zeromq.ZMQ; popd > /dev/null)
endif
ifeq ($(UNAME), Darwin)
java_files := $(shell ls $(LOCAL_PATH)/src/main/java/org/zeromq)
$(shell $(foreach java_file,$(java_files), $(shell javac -classpath $(LOCAL_PATH)/src/main/c++ -sourcepath $(LOCAL_PATH)/src/main/java -d $(LOCAL_PATH)/src/main/c++ $(LOCAL_PATH)/src/main/java/org/zeromq/$(java_file))))
$(shell pushd $(LOCAL_PATH)/src/main/c++ > /dev/null; javah -jni org.zeromq.ZMQ; popd > /dev/null)
endif

ifeq ($(UNAME), Windows)
java_files := $(shell dir /B /O-N $(LOCAL_PATH)\src\main\java\org\zeromq)
$(shell $(foreach java_file,$(java_files), $(shell javac -verbose -classpath $(LOCAL_PATH)\src\main\c++ -sourcepath $(LOCAL_PATH)\src\main\java -d $(LOCAL_PATH)\src\main\c++ $(LOCAL_PATH)\src\main\java\org\zeromq\$(java_file))))
$($(shell cd $(LOCAL_PATH)\src\main\c++), $(shell javah -jni org.zeromq.ZMQ))
endif



# $(info $(shell echo $(ZEROMQ_PATH)))

LOCAL_SRC_FILES := $(JZMQ_CPP_FILES)

LOCAL_C_INCLUDES := \
    $(JZMQ_HPP_FILES) \
    $(JZMQ_H_FILES) \
    C:\Users\whatamidoing\hey\whatamidoingffmpeg\jni\zeromq\include 

LOCAL_MODULE := libjzmq

LOCAL_STATIC_LIBRARIES := libzmq

LOCAL_MODULE_TAGS := optional

LOCAL_NDK_STL_VARIANT := stlport_static

include $(BUILD_SHARED_LIBRARY)