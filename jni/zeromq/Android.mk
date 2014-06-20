LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES= $(LOCAL_PATH)include

LOCAL_CPPFLAGS := -g -O2 -D_GNU_SOURCE -D_REENTRANT -D_THREAD_SAFE -fPIC -DZMQ_USE_EPOLL
LOCAL_LIBS := -lrt -lpthread -lgcc
LOCAL_MODULE := zmq
LOCAL_SRC_FILES := src/address.cpp \
	src/clock.cpp \
	src/ctx.cpp \
	src/curve_client.cpp \
	src/curve_server.cpp \
	src/dealer.cpp \
	src/devpoll.cpp \
	src/dist.cpp \
	src/epoll.cpp \
	src/err.cpp \
	src/fq.cpp \
	src/io_object.cpp \
	src/io_thread.cpp \
	src/ip.cpp \
	src/ipc_address.cpp \
	src/ipc_connecter.cpp \
	src/ipc_listener.cpp \
	src/kqueue.cpp \
	src/lb.cpp \
	src/mailbox.cpp \
	src/mechanism.cpp \
	src/msg.cpp \
	src/mtrie.cpp \
	src/null_mechanism.cpp \
	src/object.cpp \
	src/options.cpp \
	src/own.cpp \
	src/pair.cpp \
	src/pgm_receiver.cpp \
	src/pgm_sender.cpp \
	src/pgm_socket.cpp \
	src/pipe.cpp \
	src/plain_mechanism.cpp \
	src/poll.cpp \
	src/poller_base.cpp \
	src/precompiled.cpp \
	src/proxy.cpp \
	src/pub.cpp \
	src/pull.cpp \
	src/push.cpp \
	src/random.cpp \
	src/raw_decoder.cpp \
	src/raw_encoder.cpp \
	src/reaper.cpp \
	src/rep.cpp \
	src/req.cpp \
	src/router.cpp \
	src/select.cpp \
	src/session_base.cpp \
	src/signaler.cpp \
	src/socket_base.cpp \
	src/stream.cpp \
	src/stream_engine.cpp \
	src/sub.cpp \
	src/tcp.cpp \
	src/tcp_address.cpp \
	src/tcp_connecter.cpp \
	src/tcp_listener.cpp \
	src/thread.cpp \
	src/trie.cpp \
	src/v1_decoder.cpp \
	src/v1_encoder.cpp \
	src/v2_decoder.cpp \
	src/v2_encoder.cpp \
	src/xpub.cpp \
	src/xsub.cpp \
	src/zmq.cpp \
	src/zmq_utils.cpp
	
 LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include

 include $(BUILD_SHARED_LIBRARY)
