#include "ringbuffer.hpp"
#include <boost/thread/exceptions.hpp>
#include <boost/thread/pthread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include <boost/lockfree/spsc_queue.hpp>
#include <boost/interprocess/creation_tags.hpp>
#include <boost/interprocess/ipc/message_queue.hpp>
#include <boost/atomic/atomic.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/core/types_c.h>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/highgui/highgui_c.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgproc/types_c.h>
#include <pthread.h>
#include <EGL/egl.h>
#define GLM_FORCE_RADIANS
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#ifndef GLOBAL_H // header guards
#define GLOBAL_H

extern ringbuffer<cv::Mat, 200> dataToSend;
extern pthread_mutex_t dataToSendMux;
//extern boost::atomic<bool> sendMatData;
extern boost::thread_group tgroup;
extern ringbuffer<cv::Mat, 200> dataToSendToServer;
#endif
