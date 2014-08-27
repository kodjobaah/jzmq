#include "global.h"

ringbuffer<cv::Mat, 200> dataToSend;
ringbuffer<cv::Mat, 200> dataToSendToServer;

pthread_mutex_t dataToSendMux;
boost::thread_group tgroup;
boost::atomic<bool> send(true);

