#include <android/log.h>
//#include <boost/lockfree/queue.hpp>

#include <EGL/egl.h>

#define GLM_FORCE_RADIANS
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <jni.h>
#include <math.h>
//#include <Math.h>
//#include <opencv/cv.h>
#include <sched.h>
#include <string.h>
#include <zmq.hpp>
#include <string>
#include <vector>
#include <queue>
#include <b64/encode.hpp>
#include <msgpack.h>


#include <android/native_activity.h>
#include <turbojpeg.h>

#include <android/bitmap.h>

#include "FpsMeter.h"
#include "global.h"
#include "SharedData.h"


//#include <time.h>

//#include "com_watamidoing_nativecamera_CameraPreviewer.h"

// Utility for logging:
#define LOG_TAG    "CAMERA_RENDERER"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

//Used for zeromq
#define REQUEST_TIMEOUT     2500
#define REQUEST_RETRIES     3

#define VERTEX_POS_SIZE 3 // x, y and z
#define VERTEX_TEXCOORD0_SIZE 2 // s and t

GLuint texture;
cv::VideoCapture capture;
cv::Mat buffer[3000];
cv::Mat rgbFrame;
cv::Mat inframe;
cv::Mat outframe;
//std::queue<cv::Mat> dataToSend;
//std::queue<cv::Mat> sendToDisplayFrame;
//typedef boost::heap::priority_queue<cv::Mat *> MyPriQue;
//MyPriQue dataToSend;

ringbuffer<cv::Mat, 200> sendToDisplayFrame;
FpsMeter fpsMeter;

int bufferIndex;
int rgbIndex;
int frameWidth;
int frameHeight;
int screenWidth;
int screenHeight;
int orientation;
int cameraId;
double fps;

boost::mutex muxSendData;
waid::SharedData sharedData;
bool waid::SharedData::sendMatData = false;


pthread_mutex_t FGmutex;
pthread_t frameGrabber;
pthread_attr_t attr;

pthread_t dataSenderThread;
pthread_attr_t dataSenderThreadAttr;

pthread_cond_t dataToSendCond;

struct sched_param dataSenderParam;
struct sched_param param;

static JavaVM *gJavaVM;
static jobject gNativeObject;

typedef struct {
	GLuint renderBuffer;
	GLuint frameBuffer;
	GLuint texture;

	// Texture handle
	GLuint textureId;

	// Offset location
	GLint offsetLoc;

	GLuint programObject;

	// Attribute locations
	GLint positionLoc;
	GLint texCoordLoc;

	GLuint vboIds[2];

	GLuint baseMapLoc;
} UserData;

UserData userData;

/************* Examples **************************/
GLchar VERTEX_SHADER[] = "attribute vec4 a_position;   \n"
		"attribute vec2 a_texCoord;   \n"
		"varying vec2 v_texCoord;     \n"
		"void main()                  \n"
		"{                            \n"
		"    gl_Position =  a_position; \n"
		"   v_texCoord = a_texCoord;  \n"
		"}                            \n";
GLchar FRAGMENT_SHADER[] =
		"precision mediump float;                            \n"
				"varying vec2 v_texCoord;                            \n"
				"uniform sampler2D s_baseMap;                        \n"
				"void main()                                         \n"
				"{                                                   \n"
				"                                                    \n"
				"  gl_FragColor = texture2D( s_baseMap, v_texCoord );   \n"
				"}                                                   \n";

GLuint vtxStride = sizeof(GLfloat) * ( VERTEX_POS_SIZE + VERTEX_TEXCOORD0_SIZE);
GLuint nVertices = 4;

GLushort indices[] = { 0, 1, 2, 0, 2, 3 };

GLfloat vVertices[] = {
		// X, Y, Z, U, V
		-1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f, -1.0f,
		1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, };

EGLContext mEglContext;
EGLDisplay mEglDisplay = EGL_NO_DISPLAY;

/************* EXAMPLES END ********************/
extern "C" {

void drawBackground(bool sendData);
void destroyTexture();
void frameRetriever();
void sendData(const char *urlPath, const char *authToken, waid::SharedData *sharedData);
JNIEnv *getJniEnv();
jobject getCallbackInterface(JNIEnv *env);
void ableToConnectZmq();
void unableToConnectZmq();
GLuint LoadTexture(cv::Mat data);
GLuint createShader(GLenum shaderType, const char* src);
GLuint createProgram(const char* vtxSrc, const char* fragSrc);
bool checkGlError(const char* funcName);
void drawImage(cv::Mat data);
void translateM(float m[], int mOffset, float x, float y, float z);
void printMatrix(float m[]);
void checkBufferStatus();
void sendCVMat(waid::SharedData *sharedData);
/*******************************EXAMPLES BEGIN ****************************/
bool checkGlError(const char* funcName) {
	GLint err = glGetError();
	if (err != GL_NO_ERROR) {
		LOGE("GL error after %s(): 0x%08x\n", funcName, err);
		return true;
	}
	return false;
}

static void printGlString(const char* name, GLenum s) {
	const char* v = (const char*) glGetString(s);
	LOG("GL %s: %s\n", name, v);
}

///
// Create a shader object, load the shader source, and
// compile the shader.
//
GLuint createShader(GLenum shaderType, const char* src) {
	GLuint shader = glCreateShader(shaderType);
	if (!shader) {
		checkGlError("glCreateShader");
		return 0;
	}
	glShaderSource(shader, 1, &src, NULL);

	GLint compiled = GL_FALSE;
	glCompileShader(shader);
	glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
	if (!compiled) {
		GLint infoLogLen = 0;
		glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLogLen);
		if (infoLogLen > 0) {
			GLchar* infoLog = (GLchar*) malloc(infoLogLen);
			if (infoLog) {
				glGetShaderInfoLog(shader, infoLogLen, NULL, infoLog);
				LOGE("Could not compile %s shader:\n%s\n",
						shaderType == GL_VERTEX_SHADER ? "vertex" : "fragment",
						infoLog);
				free(infoLog);
			}
		}
		glDeleteShader(shader);
		return 0;
	}

	return shader;
}

GLuint createProgram(const char* vtxSrc, const char* fragSrc) {
	GLuint vtxShader = 0;
	GLuint fragShader = 0;
	GLuint program = 0;
	GLint linked = GL_FALSE;

	vtxShader = createShader(GL_VERTEX_SHADER, vtxSrc);
	if (!vtxShader)
		goto exit;

	fragShader = createShader(GL_FRAGMENT_SHADER, fragSrc);
	if (!fragShader)
		goto exit;

	program = glCreateProgram();
	if (!program) {
		checkGlError("glCreateProgram");
		goto exit;
	}
	glAttachShader(program, vtxShader);
	glAttachShader(program, fragShader);

	glLinkProgram(program);
	glGetProgramiv(program, GL_LINK_STATUS, &linked);
	if (!linked) {
		LOGE("Could not link program");
		GLint infoLogLen = 0;
		glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infoLogLen);
		if (infoLogLen) {
			GLchar* infoLog = (GLchar*) malloc(infoLogLen);
			if (infoLog) {
				glGetProgramInfoLog(program, infoLogLen, NULL, infoLog);
				LOGE("Could not link program:\n%s\n", infoLog);
				free(infoLog);
			}
		}
		glDeleteProgram(program);
		program = 0;
	}

	exit: glDeleteShader(vtxShader);
	glDeleteShader(fragShader);
	glReleaseShaderCompiler();
	return program;
}

void setupVBO() {

	GLuint v = sizeof(GLfloat);
	GLuint s = sizeof(GLushort);
	// Only allocate on the first draw
	glGenBuffers(2, userData.vboIds);
	glBindBuffer( GL_ARRAY_BUFFER, userData.vboIds[0]);
	glBufferData( GL_ARRAY_BUFFER, nVertices * vtxStride, vVertices,
	GL_STATIC_DRAW);
	glBindBuffer( GL_ELEMENT_ARRAY_BUFFER, userData.vboIds[1]);
	glBufferData( GL_ELEMENT_ARRAY_BUFFER, sizeof(indices), indices,
	GL_STATIC_DRAW);
}

GLuint createTexture() {
	GLuint texId;
	glGenTextures(1, &texId);
	glBindTexture(GL_TEXTURE_2D, texId);
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, frameWidth, frameHeight, 0, GL_RGB,
	GL_UNSIGNED_BYTE, NULL);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
	GL_NEAREST_MIPMAP_NEAREST);
	/*
	 glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	 glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	 */
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	glGenerateMipmap(GL_TEXTURE_2D);
	glBindTexture(GL_TEXTURE_2D, 0);
	return texId;
}

void loadSubTexture(cv::Mat data) {
	glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, screenWidth, screenHeight,
	GL_RGB, GL_UNSIGNED_BYTE, data.ptr());
}

void renderToTexture(cv::Mat rgbFrame) {
	GLuint offset = 0.0;
	glBindFramebuffer(GL_FRAMEBUFFER, userData.frameBuffer);
	glUseProgram(userData.programObject);

	glActiveTexture(GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, userData.textureId);

	glViewport(0, 0, frameWidth, frameHeight);
	glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

	loadSubTexture(rgbFrame);

	glBindBuffer(GL_ARRAY_BUFFER, userData.vboIds[0]);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, userData.vboIds[1]);

	glEnableVertexAttribArray(userData.positionLoc);
	glEnableVertexAttribArray(userData.texCoordLoc);

	glVertexAttribPointer(userData.positionLoc, VERTEX_POS_SIZE,
	GL_FLOAT, GL_FALSE, vtxStride, (const void *) offset);

	offset += VERTEX_POS_SIZE * sizeof(GLfloat);
	glVertexAttribPointer(userData.texCoordLoc,
	VERTEX_TEXCOORD0_SIZE,
	GL_FLOAT, GL_FALSE, vtxStride, (const void *) offset);
	glUniform1i(userData.baseMapLoc, 0);

	glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	glBindFramebuffer(GL_FRAMEBUFFER, 0);

}
GLuint createRenderBuffer() {

	// create a framebuffer object, you need to delete them when program exits.
	glGenFramebuffers(1, &userData.frameBuffer);
	glBindFramebuffer(GL_FRAMEBUFFER, userData.frameBuffer);

	glGenRenderbuffers(1, &userData.renderBuffer);
	glBindRenderbuffer(GL_RENDERBUFFER, userData.renderBuffer);
	glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, frameWidth,
			frameHeight);

	// attach a renderbuffer to depth attachment point
	glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
	GL_RENDERBUFFER, userData.renderBuffer);
	glBindRenderbuffer(GL_RENDERBUFFER, 0);

	userData.textureId = createTexture();

	//glActiveTexture(GL_TEXTURE0);
	//glBindTexture(GL_TEXTURE_2D, userData.textureId);
	// attach a texture to FBO color attachement point
	glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
			userData.textureId, 0);

	glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
	GL_RENDERBUFFER, userData.renderBuffer);

	checkBufferStatus();
	glBindTexture(GL_TEXTURE_2D, 0);
	glBindFramebuffer(GL_FRAMEBUFFER, 0);
}

void checkBufferStatus() {
	GLuint returned = (glCheckFramebufferStatus(GL_FRAMEBUFFER));

	if (returned != GL_FRAMEBUFFER_COMPLETE) {
		LOG("ERROR CODE RETURNED FROM CHECKING BUFFER STATUS %d -->", returned);
		switch (returned) {
		case GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
			LOG("Incomplete: Dimensions");
			break;

		case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE_IMG:
			LOG("Incomplete: Formats");
			break;

		case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
			LOG("Incomplete: Missing Attachment");
			break;

		case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
			LOG("Incomplete: Attachment");
			break;

		default:
			LOG("-- NOTHING Complete");
			break;
		}
	}

}

JNIEXPORT void JNICALL
Java_com_watamidoing_nativecamera_Native_init(JNIEnv* env, jobject obj,
		jint width, jint height) {

	printGlString("Version", GL_VERSION);
	printGlString("Vendor", GL_VENDOR);
	printGlString("Renderer", GL_RENDERER);
	printGlString("Extensions", GL_EXTENSIONS);
	frameWidth = width;
	frameHeight = height;

	const char* versionStr = (const char*) glGetString(GL_VERSION);
	mEglContext = eglGetCurrentContext();
	mEglDisplay = eglGetCurrentDisplay();
	glClearDepthf(1.0f);

	if (checkGlError("eglCurrentDisplay")) {
		LOG("------------ THE DISPLAY VALUE IS NOT DEFINED ---------");
	} else {
		LOG("------------- THE DISPLAY VALUE IS DEFINED ------------------ ");
	}

	userData.programObject = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

	// Get the attribute locations
	userData.positionLoc = glGetAttribLocation(userData.programObject,
			"a_position");
	checkGlError("glGetAttribLocation-positionLoc");
	userData.texCoordLoc = glGetAttribLocation(userData.programObject,
			"a_texCoord");
	checkGlError("glGetAttribLocation-textLoc");
	// Get the sampler location
	userData.baseMapLoc = glGetUniformLocation(userData.programObject,
			"s_baseMap");

	if (userData.baseMapLoc == 0) {
		LOG(
				"--------------- UNABLE TO LOAD THE BASEMAPLOC------------------------");
	}

	setupVBO();
	//userData.textureId = createTexture();
	createRenderBuffer();

	boost::thread sendCVMatThread(sendCVMat,&sharedData);
	tgroup.add_thread(&sendCVMatThread);
}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_initCamera(
		JNIEnv*, jobject, jint width, jint height, jint rot, jint camId) {
	LOG("Camera Created");

	orientation = rot;
	cameraId = camId;
	LOG("cameraPictureWidth = %d", width);
	LOG("cameraPictureHeight = %d", height);
	capture.open(CV_CAP_ANDROID + cameraId);
	capture.set(CV_CAP_PROP_FRAME_WIDTH, width);
	capture.set(CV_CAP_PROP_FRAME_HEIGHT, height);
	screenWidth = width;
	screenHeight = height;

}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_releaseCamera(
		JNIEnv*, jobject) {
	LOG("Camera Released");
	capture.release();
	//tgroup.interrupt_all();
}
JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_ReleaseProgram(
		JNIEnv*, jobject) {

	glDeleteFramebuffers(1, &userData.frameBuffer);
	glDeleteTextures(1, &userData.textureId);
	glDeleteProgram(userData.programObject);

}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_surfaceChangedNative(
		JNIEnv*, jobject, jint width, jint height, jint orien) {
	LOG("Surface Changed");

	frameHeight = height;
	frameWidth = width;

	glEnable(GL_DEPTH_TEST);
	glDepthFunc(GL_LEQUAL);

	glViewport(0, 0, width, height);
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

}

JNIEXPORT void JNICALL
Java_com_watamidoing_nativecamera_Native_draw(JNIEnv* env, jobject obj) {

	GLuint textureId;
	cv::Mat fr;
	GLuint offset = 0;
//	LOG("READING FRAME frameWidth=%d frameHeight=%d", frameWidth, frameHeight);
//	if (sendToDisplayFrame.pop(fr)) {
	if (capture.isOpened() && capture.read(inframe)) {
		//	LOG("READ FRAME");
		inframe.copyTo(fr);
		cv::cvtColor(fr, outframe, CV_BGR2RGB);
		cv::flip(outframe, rgbFrame, 1);

		renderToTexture(rgbFrame);

		//setProjection(frameWidth,frameHeight);
		glViewport(0, 0, frameWidth, frameHeight);
		glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		// Use the program object
		glUseProgram(userData.programObject);

		//glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, userData.textureId);

		loadSubTexture(rgbFrame);
		glBindBuffer(GL_ARRAY_BUFFER, userData.vboIds[0]);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, userData.vboIds[1]);

		glEnableVertexAttribArray(userData.positionLoc);
		glEnableVertexAttribArray(userData.texCoordLoc);

		glVertexAttribPointer(userData.positionLoc, VERTEX_POS_SIZE,
		GL_FLOAT, GL_FALSE, vtxStride, (const void *) offset);

		offset += VERTEX_POS_SIZE * sizeof(GLfloat);
		glVertexAttribPointer(userData.texCoordLoc,
		VERTEX_TEXCOORD0_SIZE,
		GL_FLOAT, GL_FALSE, vtxStride, (const void *) offset);

		// Set the base map sampler to texture unit to 0
		glUniform1i(userData.baseMapLoc, 0);

		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

		glDisableVertexAttribArray(userData.positionLoc);
		glDisableVertexAttribArray(userData.texCoordLoc);

		cv::Mat frameToSend;
		rgbFrame.copyTo(frameToSend);
		dataToSend.push(frameToSend);
	} else {
		//	LOG("UNABLE TO READ FRAME");
	}
}

/****************************** EXAMPLES END *****************************/
JNIEnv *getJniEnv() {
	JNIEnv *env;
	bool isAttached = false;
	bool send = true;
	int status = gJavaVM->GetEnv((void **) &env, JNI_VERSION_1_6);
	if (status < 0) {
		LOG("callback_handler: failed to get JNI environment, "
				"assuming native thread");
		status = gJavaVM->AttachCurrentThread(&env, NULL);
		if (status < 0) {
			LOG("callback_handler: failed to attach "
					"current thread");
			send = false;
		}
		isAttached = true;
	}

	if (send) {
		return env;
	} else {
		return NULL;
	}
}

jobject getCallbackInterface(JNIEnv *env) {

	bool send = true;
	/* Construct a Java string */
	LOG("-Z3");
	jobject interfaceClass = env->NewWeakGlobalRef(gNativeObject);
	LOG("-Z4");
	if (!interfaceClass) {
		LOG("-Z5");
		LOG("callback_handler: failed to get class reference");
		gJavaVM->DetachCurrentThread();
		send = false;
	}

	if (send) {
		return interfaceClass;
	} else {
		return NULL;
	}

}

void connectedZmq() {

	JNIEnv *env = getJniEnv();
	LOG("-ZC1");
	jobject obj;
	if (env) {
		LOG("-ZC2");
		obj = getCallbackInterface(env);
	}

	bool send = true;
	if (obj) {
		LOG("-ZC3");
		/* Find the callBack method ID */
		jclass interfaceClass = env->GetObjectClass(obj);
		LOG("-ZC4");
		jmethodID method = env->GetMethodID(interfaceClass, "ableToConnect",
				"()V");
		LOG("-ZC5");
		if (!method) {
			LOG("callback_handler: failed to get method ID (ableToConnect)");
			gJavaVM->DetachCurrentThread();
			send = false;
		}

		if (send) {
			LOG("-ZC6");
			env->CallVoidMethod(obj, method);
			LOG("-ZC7");
		}

		gJavaVM->DetachCurrentThread();

	}
	LOG("------------ SENDT ABLET TO CONNECT MESSAGE-----------------");
}

void unableToConnectZmq() {

	JNIEnv *env = getJniEnv();
	LOG("-ZNC1");
	jobject obj;
	if (env) {
		LOG("-ZNC2");
		obj = getCallbackInterface(env);
	}

	bool send = true;
	if (obj) {
		/* Find the callBack method ID */
		jclass interfaceClass = env->GetObjectClass(obj);
		jmethodID method = env->GetMethodID(interfaceClass, "unableToConnect",
				"()V");
		if (!method) {
			LOG("callback_handler: failed to get method ID (unableToConnect)");
			gJavaVM->DetachCurrentThread();
			send = false;
		}

		if (send) {
			env->CallVoidMethod(obj, method);
		}

		gJavaVM->DetachCurrentThread();

	}
	LOG("------------ SENT UNABLE TO CONNECT MESSAGE----------------");
}

void zmqConnectionDropped() {

	JNIEnv *env = getJniEnv();
	LOG("-ZNC1");
	jobject obj;
	if (env) {
		LOG("-ZNC2");
		obj = getCallbackInterface(env);
	}

	bool send = true;
	if (obj) {
		/* Find the callBack method ID */
		jclass interfaceClass = env->GetObjectClass(obj);
		jmethodID method = env->GetMethodID(interfaceClass, "connectionDropped",
				"()V");
		if (!method) {
			LOG("callback_handler: failed to get method ID (connectionDropped)");
			gJavaVM->DetachCurrentThread();
			send = false;
		}

		if (send) {
			env->CallVoidMethod(obj, method);
		}

		gJavaVM->DetachCurrentThread();

	}
	LOG("------------ CONNECTION DROPPED----------------");
}

void updateMessageSent(long messageSent) {

	JNIEnv *env = getJniEnv();
	LOG("-UMSC1");
	jobject obj;
	if (env) {
		LOG("-UMSC2");
		obj = getCallbackInterface(env);
	}

	bool send = true;
	if (obj) {
		/* Find the callBack method ID */
		jclass interfaceClass = env->GetObjectClass(obj);
		jmethodID method = env->GetMethodID(interfaceClass, "updateMessagesSent",
				"(J)V");
		if (!method) {
			LOG("callback_handler: failed to get method ID (updateMessagesSent)");
			gJavaVM->DetachCurrentThread();
			send = false;
		}

		if (send) {
			env->CallVoidMethod(obj, method);
		}

		gJavaVM->DetachCurrentThread();

	}
	LOG("------------ Messages Sent----------------");
}

void sendCVMat(waid::SharedData *sharedData) {
	LOG(
			"---------------------------------------------- SEND CVMAT -----------------------");
	bool done = false;
	LOG("-1");
	zmq::context_t context(1);
	LOG("-2");
	zmq::socket_t socket(context, ZMQ_PUSH);
	LOG("-3");
	socket.bind("tcp://127.0.0.1:5555");
	int linger = 0;
	socket.setsockopt(ZMQ_LINGER, &linger, sizeof(linger));
	LOG("=== WAITING FOR MAT=====");
	int fileCount = 0;
	tjhandle _jpegCompressor = tjInitCompress();

	while (!done) {

		cv::Mat in;
		CvMat *out;
		cv::Mat cl;
		if (dataToSend.pop(in)) {


			muxSendData.lock();
			LOG("send data %i",unsigned(sharedData->sendMatData));
			if (sharedData->sendMatData) {
				muxSendData.unlock();
				cv::Mat newSize;
				int _width = 352;
				int _height = 288;

				cv::Size dsize(_width,_height);
				cv::resize(in,newSize,dsize,0,0,cv::INTER_AREA);

				unsigned char *outdata = (uchar *) newSize.datastart;
				int flags  = TJFLAG_BOTTOMUP;
				const int JPEG_QUALITY = 75;
				long unsigned int _jpegSize = 0;
				unsigned char* _compressedImage = NULL; //!< Memory is allocated by tjCompress2 if _jpegSize == 0


				int compResults = tjCompress2(_jpegCompressor,outdata, _width, 0, _height, TJPF_RGB,
			          &_compressedImage, &_jpegSize, TJSAMP_440, JPEG_QUALITY,
			          flags);

				LOG("RESULTS FROM COMPRESSION: %i",compResults);
				zmq::message_t messData(_jpegSize);
				memcpy((void *) messData.data(), _compressedImage, _jpegSize);
				LOG("JUST BEFORE SENDING");
				socket.send(messData,0);
				LOG("--sent");

			 			//to free the memory allocated by TurboJPEG (either by tjAlloc(),
			//or by the Compress/Decompress) after you are done working on it:
				tjFree(_compressedImage);
				fileCount = fileCount + 1;
			} else {
				muxSendData.unlock();
			}

			/* -- PACK -- */
			/*
			 char *buf = NULL;
			 msgpack_sbuffer sbuf;
			 msgpack_sbuffer_init(&sbuf);
			 msgpack_packer pck;
			 msgpack_packer_init(&pck, &sbuf, msgpack_sbuffer_write);
			 msgpack_pack_raw(&pck, data_size);
			 msgpack_pack_raw_body(&pck, in.ptr(), data_size);
			 socket.send(sbuf.data, sbuf.size, 0);
			 LOG("SENT MESSAGE: DATA_SIZE=%d, BUF_SIZE=%d",data_size,sbuf.size);
			 msgpack_sbuffer_destroy(&sbuf);
			 */
			//socket.send(in.ptr(), data_size);

			//LOG("SENT MESSAGE: DATA_SIZE=%d ", data_size);

		}
	}
	tjDestroy(_jpegCompressor);

}

static zmq::socket_t * s_client_socket(zmq::context_t & context,
		const char *urlPath, const char *authToken) {

	LOG("Connecting to hello world server...urlPath=%s..........authToken=%s",
				urlPath, authToken);

	LOG("-1A");
	zmq::socket_t *socketExternal = new zmq::socket_t(context, ZMQ_DEALER);
	LOG("-1B");
	std::string auth(authToken);
	int linger = 0;

	socketExternal->setsockopt(ZMQ_LINGER, &linger, sizeof(linger));
	LOG("-1C");
	socketExternal->setsockopt(ZMQ_IDENTITY, auth.c_str(),(long) auth.length());
	LOG("-1D");
	socketExternal->connect(urlPath);
	LOG("-1E");
	return socketExternal;
}

void sendData(const char *urlPath, const char *authToken, waid::SharedData *sharedData) {

	LOG("-------------ZEROMQ THREAD STARTED");

	zmq::context_t contextExternal(1);
	zmq::socket_t * socketExternal = s_client_socket(contextExternal, urlPath,
			authToken);

	std::string start = "CONNECT";
	zmq::message_t request(start.size());
	LOG("-1F");
	memcpy((void *) request.data(), start.c_str(), start.size());
	socketExternal->send(request);
	LOG("-1G");
	std::string expResponse = "NOT_ALIVE";
	int retries_left = REQUEST_RETRIES;
	bool connected = false;
	LOG("-1H");
	zmq::message_t reply;
	LOG("WAITING FOR CONNECTIONG REPLY");
	socketExternal->recv(&reply);
	LOG("-1K");
	std::string res((const char *) reply.data());
	expResponse = res;
	LOG("RECEIVED RESPONSE: %s", res.c_str());


	int count = 0;
	zmq::context_t contextInternal(1);
	zmq::socket_t socketInternal(contextInternal, ZMQ_PULL);
	if (expResponse.compare("ALIVE")) {
		connectedZmq();
		socketInternal.connect("tcp://127.0.0.1:5555");
		LOG("WAITING TO RECEIVE MAT");
		sharedData->sendMatData = true;
	} else {
		LOG("unable to connect");
		unableToConnectZmq();
		sharedData->sendMatData = false;
	}

	base64::encoder E;
	bool retry = false;
	muxSendData.lock();
	while (sharedData->sendMatData) {
	muxSendData.unlock();
		try {

			int64_t more = 0;
			size_t more_size = sizeof(more);

			zmq::message_t reply;
			socketInternal.recv(&reply);
			LOG("----------------------------------------------------------------------");


			long unsigned int _jpegSize = reply.size();

			unsigned char *dataReceived = (unsigned char*)reply.data();
			char numstr[65]; // enough to hold all numbers up to 64-bits
			sprintf(numstr, "%d", count);
			std::string loc = "/sdcard/image";
			 std::string ext = ".jpg";
			 std::string configFile = loc + numstr + ext;
			 FILE* appConfigFile = fopen(configFile.c_str(), "w+");
			 int res =  0;
			 LOG("App config file created successfully. Writing config data ...%s...image size=%i\n",configFile.c_str(),reply.size());
			 res = fwrite(dataReceived, sizeof(char),_jpegSize, appConfigFile);
			 fclose(appConfigFile);
			 count = count + 1;

			//updateMessageSent(count);

			/* -- PACK -- */
			/*
			 char *buf = NULL;
			 msgpack_sbuffer sbuf;
			 msgpack_sbuffer_init(&sbuf);
			 msgpack_packer pck;
			 msgpack_packer_init(&pck, &sbuf, msgpack_sbuffer_write);
			 msgpack_pack_raw(&pck, data_size);
			 msgpack_pack_raw_body(&pck, in.ptr(), data_size);
			 socket.send(sbuf.data, sbuf.size, 0);
			 LOG("SENT MESSAGE: DATA_SIZE=%d, BUF_SIZE=%d",data_size,sbuf.size);
			 msgpack_sbuffer_destroy(&sbuf);
			 */

			//E.encode(d, data_size, out);
			//memcpy (d, data.ptr(), data_size);
			//LOG("Connecting to hello world server...");
			//socket.connect ("tcp://localhost:5555");
			//zmq::message_t request (6);
			//	memcpy ((void *) request.data (), d, imageSize);
			//	LOG("Sending Hello ");
			//	socket.send (request);
			//  Get the reply.
			//zmq::message_t reply;
			//socket.recv (&reply);
			//LOG("Received World");
		} catch (boost::thread_interrupted const&) {
			LOG("========================= THREAD INTERRUPTED ======================");
			muxSendData.lock();
			sharedData->sendMatData = false;
			muxSendData.unlock();
		}
		muxSendData.lock();
	}

	contextExternal.close();
	contextInternal.close();
	LOG(
			"=================== SENDER THREAD STOPPED  ============================");

}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_stopZeromq(
		JNIEnv *env, jclass clz) {
	LOG("---- SHOULD HAVE INTERUPTED THREAD");
	sharedData.sendMatData = false;

}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_startZeromq(
		JNIEnv *env, jclass clz, jstring path, jstring token) {

	const char *urlPath = env->GetStringUTFChars(path, 0);
	const char *authToken = env->GetStringUTFChars(token, 0);

	sharedData.sendMatData = true;
	boost::thread sendDataThread(sendData, urlPath, authToken, &sharedData);
	tgroup.add_thread(&sendDataThread);
	muxSendData.lock();

	LOG("VALUE OF SENDMATADATA %i",sharedData.sendMatData);
	muxSendData.unlock();
	//boost::thread receiveDataThread(zeroMQDataReceiver);
	//tgroup.add_thread(&receiveDataThread);
	//env->ReleaseStringUTFChars(path, urlPath);
	//env->ReleaseStringUTFChars(token, authToken);
	LOG("------- ZEROMQ THREAD CALLED TO START");

}
JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_storeMessenger(
		JNIEnv * env, jclass c, jobject jc) {
	LOG("STORING MESSENGER");
	gNativeObject = env->NewGlobalRef(jc);
	LOG("FINNISHED STORING MESSENGER");
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {

	JNIEnv *env;
	gJavaVM = vm;
	LOG("JNI_OnLoad called");
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
		LOG("Failed to get the environment using GetEnv()");
		return -1;
	}
	LOG("JNIOnLoad");

	return JNI_VERSION_1_6; /* the required JNI version */
}

}