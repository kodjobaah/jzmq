#include <android/log.h>
//#include <boost/lockfree/queue.hpp>
#include <boost/thread/exceptions.hpp>
#include <boost/thread/thread.hpp>
#include <boost/lockfree/spsc_queue.hpp>
#include <boost/interprocess/creation_tags.hpp>
#include <boost/interprocess/ipc/message_queue.hpp>
#include <boost/atomic/atomic.hpp>
#include <EGL/egl.h>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <jni.h>
#include <math.h>
//#include <Math.h>
//#include <opencv/cv.h>
#include <opencv2/core/core.hpp>
#include <opencv2/core/types_c.h>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/highgui/highgui_c.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgproc/types_c.h>
#include <pthread.h>
#include <sched.h>
#include <string.h>
#include <zmq.hpp>
#include <string>
#include <vector>
#include <queue>
#include <b64/encode.hpp>

#include "FpsMeter.h"
#include "ringbuffer.hpp"

//#include <time.h>

//#include "com_watamidoing_nativecamera_CameraPreviewer.h"

// Utility for logging:
#define LOG_TAG    "CAMERA_RENDERER"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define VERTEX_POS_SIZE 3 // x, y and z
#define VERTEX_TEXCOORD0_SIZE 2 // s and t


PFNGLEGLIMAGETARGETTEXTURE2DOESPROC glEGLImageTargetTexture2DOES;

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

ringbuffer<cv::Mat, 200> dataToSend;
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
pthread_mutex_t FGmutex;
pthread_t frameGrabber;
pthread_attr_t attr;

boost::thread_group tgroup;

pthread_t dataSenderThread;
pthread_attr_t dataSenderThreadAttr;
pthread_mutex_t dataToSendMux;
pthread_cond_t dataToSendCond;

struct sched_param dataSenderParam;
struct sched_param param;

static JavaVM *gJavaVM;
static jobject gNativeObject;

typedef struct {
	GLuint renderBuffer;
	GLuint frameBuffer;
	GLuint texture;
	GLint texWidth;
	GLint texHeight;

	GLfloat* vVertices;		// xyst, xyst, ...
	GLuint* indices;
	GLuint vboVertices;
	GLuint vboIndices;

	// Texture handle
	GLuint textureId;

	// Sampler location
	GLint samplerLoc;

	// Offset location
	GLint offsetLoc;

	GLuint programObject;
	GLuint verticesLoc;	// location for a_vertices attribute in vertex shader
	// Attribute locations
	GLint positionLoc;
	GLint texCoordLoc;

	GLuint vboIds[2];

	GLuint baseMapLoc;	// location for s_baseMap sampler2D in fragment shader
} UserData;

UserData userData;

/************* Examples **************************/

GLchar VERTEX_SHADER[] = "attribute vec4 a_position;   \n"
		"attribute vec2 a_texCoord;   \n"
		"varying vec2 v_texCoord;     \n"
		"void main()                  \n"
		"{                            \n"
		"   gl_Position = a_position; \n"
		"   v_texCoord = a_texCoord;  \n"
		"}                            \n";
/*

 "uniform float u_offset;      \n"
 "attribute vec4 a_position;   \n"
 "attribute vec2 a_texCoord;   \n"
 "varying vec2 v_texCoord;     \n"
 "void main()                  \n"
 "{                            \n"
 "   gl_Position = a_position; \n"
 "   gl_Position.x += u_offset;\n"
 "   v_texCoord = a_texCoord;  \n"
 "}                            \n";
 */
GLchar FRAGMENT_SHADER[] =
		"precision mediump float;                            \n"
				"varying vec2 v_texCoord;                            \n"
				"uniform sampler2D s_baseMap;                        \n"
				"void main()                                         \n"
				"{                                                   \n"
				"                                                    \n"
				"  gl_FragColor = texture2D( s_baseMap, v_texCoord );   \n"
				"}                                                   \n";
/*
 "#extension GL_OES_EGL_image_external : require \n"
 "precision highp float;                            \n"
 "varying vec2 v_texCoord;                            \n"
 "uniform samplerExternalOES s_texture;                        \n"
 "void main()                                         \n"
 "{                                                   \n"
 "  gl_FragColor = texture2D( s_texture, v_texCoord );\n"
 "}\n";
 */


GLuint vtxStride = sizeof ( GLfloat ) * ( VERTEX_POS_SIZE + VERTEX_TEXCOORD0_SIZE );
GLuint nVertices = 4;

GLushort indices[] = { 0, 1, 2, 0, 2, 3 };

GLfloat vVertices[] = { -0.5f, 0.5f, 0.0f,  // Position 0
		0.0f, 0.0f,        // TexCoord 0
		-0.5f, -0.5f, 0.0f, // Position 1
		0.0f, 1.0f,        // TexCoord 1
		0.5f, -0.5f, 0.0f, // Position 2
		1.0f, 1.0f,        // TexCoord 2
		0.5f, 0.5f, 0.0f,  // Position 3
		1.0f, 0.0f         // TexCoord 3
		};

EGLContext mEglContext;
EGLDisplay mEglDisplay = EGL_NO_DISPLAY;

/************* EXAMPLES END ********************/
extern "C" {

void drawBackground(bool sendData);
void destroyTexture();
void frameRetriever();
void sendData();
JNIEnv *getJniEnv();
jclass getCallbackInterface(JNIEnv *env);
void ableToConnectZmq();
void unableToConnectZmq();
GLuint LoadTexture(cv::Mat data);
GLuint createShader(GLenum shaderType, const char* src);
GLuint createProgram(const char* vtxSrc, const char* fragSrc);
bool checkGlError(const char* funcName);
void drawImage(cv::Mat data);

/*******************************EXAMPLES BEGIN ****************************/
bool checkGlError(const char* funcName) {
	GLint err = glGetError();
	if (err != GL_NO_ERROR) {
		LOGE("GL error after %s(): 0x%08x\n", funcName, err);
		return true;
	}
	return false;
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
	glClearColor(0.0f, 0.6f, 0.0f, 0.0f);
	//Provides a hint to compiler to release resources used for
	//compiling shaders
	glReleaseShaderCompiler();
	return program;
}

static void printGlString(const char* name, GLenum s) {
	const char* v = (const char*) glGetString(s);
	LOG("GL %s: %s\n", name, v);
}

GLuint createTexture() {
	GLuint texId;
	    glGenTextures(1, &texId);
	    glBindTexture(GL_TEXTURE_2D, texId);
	    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, 1024, 1024, 0, GL_RGB,GL_UNSIGNED_BYTE, NULL);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	    glBindTexture(GL_TEXTURE_2D, 0);
	    return texId;
}

void loadSubTexture(cv::Mat data) {
	   glTexSubImage2D(GL_TEXTURE_2D, 0, (1024-frameWidth)/2, (1024-frameHeight)/2, frameWidth, frameHeight,
	   GL_RGB, GL_UNSIGNED_BYTE, data.ptr());
}

GLuint LoadTexture(cv::Mat data)
{

	GLuint texId;

    glGenTextures(1, &texId);
    glBindTexture(GL_TEXTURE_2D, texId);

    char* rawPtr = new char[data.cols * data.rows * data.channels()];
    memcpy(rawPtr, (char*)data.data, data.cols * data.rows* data.channels());

    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, data.cols, data.rows, 0, GL_RGB,GL_UNSIGNED_BYTE, rawPtr);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    free(rawPtr);
    return texId;
}

GLuint createRenderBuffer() {

	GLuint colorRenderbuffer;

	// Generates the name/id, creates and configures the Color Render Buffer.
	glGenRenderbuffers(1, &colorRenderbuffer);
	glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
	glRenderbufferStorage(GL_RENDERBUFFER, GL_RGB, frameWidth, frameHeight);

	return colorRenderbuffer;
}

GLuint createFrameBuffer() {

	GLuint frameBuffer;

	// Creates a name/id to our frameBuffer.
	glGenFramebuffers(1, &frameBuffer);

	// The real Frame Buffer Object will be created here,
	// at the first time we bind an unused name/id.
	glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

	return frameBuffer;
}

void setupVBO() {


		  GLuint v = sizeof(GLfloat );
		  GLuint s = sizeof(GLushort);
		  LOG("SETTING UP SIZEOFGLOAT=%d SIZEOFGLUSHOR=%d vtxString=%d nVertices=%d",v,s,vtxStride,nVertices);
		  // Only allocate on the first draw
	      glGenBuffers ( 2, userData.vboIds );
	      checkGlError("glGenBuffers");
	      glBindBuffer ( GL_ARRAY_BUFFER, userData.vboIds[0] );
	      checkGlError("glBindBuffer--ARRAY");
	      glBufferData ( GL_ARRAY_BUFFER,nVertices*vtxStride ,  vVertices, GL_STATIC_DRAW );
	      checkGlError("glBindBufferData--ARRAY");
	      glBindBuffer ( GL_ELEMENT_ARRAY_BUFFER, userData.vboIds[1] );
	      checkGlError("glBindBuffer--ELEMENT");
	      glBufferData ( GL_ELEMENT_ARRAY_BUFFER,sizeof (indices),indices, GL_STATIC_DRAW );
	      checkGlError("glBindBufferData--ELEMENT");
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

	if (checkGlError("eglCurrentDisplay")) {
		LOG("------------ THE DISPLAY VALUE IS NOT DEFINED ---------");
	} else {
		LOG("------------- THE DISPLAY VALUE IS DEFINED ------------------ ");
	}

	userData.programObject = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

	LOG("userData.programObject=%d",userData.programObject);
	// Get the attribute locations
	userData.positionLoc = glGetAttribLocation(userData.programObject, "a_position");
	checkGlError("glGetAttribLocation-positionLoc");
	userData.texCoordLoc = glGetAttribLocation(userData.programObject, "a_texCoord");
	checkGlError("glGetAttribLocation-textLoc");
	    // Get the sampler location
	userData.baseMapLoc = glGetUniformLocation(userData.programObject, "s_baseMap");
	checkGlError("glGetUniformatLoction");
	if (userData.baseMapLoc == 0)  {
		LOG("--------------- UNABLE TO LOAD THE BASEMAPLOC------------------------");
	}

	setupVBO();
	userData.textureId = createTexture();

	/*
	userData.renderBuffer = createRenderBuffer();
	userData.frameBuffer = createFrameBuffer();
	//Attach render buffer to frambuffer
	glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_RENDERBUFFER,userData.renderBuffer);
	*/
}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_surfaceChangedNative(
		JNIEnv*, jobject, jint width, jint height, jint orien) {
	LOG("Surface Changed");

	frameHeight = height;
	frameWidth = width;

}

///
// Draw a triangle using the shader pair created in Init()
//
JNIEXPORT void JNICALL
Java_com_watamidoing_nativecamera_Native_draw(JNIEnv* env, jobject obj) {

	GLuint textureId;
	cv::Mat fr;
	GLuint offset = 0;
	LOG("READING FRAME");
//	if (sendToDisplayFrame.pop(fr)) {
	if (capture.isOpened() && capture.read(inframe)) {
		LOG("READ FRAME");
		inframe.copyTo(fr);

		cv::cvtColor(fr, outframe, CV_BGR2RGB);
		cv::flip(outframe, rgbFrame, 1);

	    glClear(GL_COLOR_BUFFER_BIT);
		glBindTexture(GL_TEXTURE_2D, userData.textureId);
		loadSubTexture(rgbFrame);

		//userData.textureId = LoadTexture(rgbFrame);
	    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

	    glViewport(0, 0, frameWidth, frameHeight);

	    // Clear the color buffer

	    // Use the program object
	    glUseProgram(userData.programObject);

	    glBindBuffer(GL_ARRAY_BUFFER, userData.vboIds[0]);
	    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,userData.vboIds[1]);


	    LOG("------userData.positionLoc=%d",userData.positionLoc);
	    glEnableVertexAttribArray(userData.positionLoc);
	    LOG("------userData.textCoordLoc=%d",userData.texCoordLoc);
	    glEnableVertexAttribArray(userData.texCoordLoc);

	    glVertexAttribPointer (userData.positionLoc, VERTEX_POS_SIZE,
	                              GL_FLOAT, GL_FALSE, vtxStride,
	                              ( const void * ) offset );

	    offset += VERTEX_POS_SIZE * sizeof ( GLfloat );
	    glVertexAttribPointer (userData.texCoordLoc,
	    		VERTEX_TEXCOORD0_SIZE,
	                              GL_FLOAT, GL_FALSE, vtxStride,
	                              ( const void * ) offset );

	    glActiveTexture(GL_TEXTURE0);
	    glBindTexture(GL_TEXTURE_2D, userData.textureId);

	    // Set the base map sampler to texture unit to 0
	    glUniform1i(userData.baseMapLoc, 0);

	    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
	} else {
		LOG("UNABLE TO READ FRAME");
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

jclass getCallbackInterface(JNIEnv *env) {

	bool send = true;
	/* Construct a Java string */
	jclass interfaceClass = env->GetObjectClass(gNativeObject);
	if (!interfaceClass) {
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
	jclass interfaceClass;
	if (env) {
		interfaceClass = getCallbackInterface(env);
	}

	bool send = true;
	if (interfaceClass) {
		/* Find the callBack method ID */
		jmethodID method = env->GetMethodID(interfaceClass, "ableToConnect",
				"()V");
		if (!method) {
			LOG("callback_handler: failed to get method ID");
			gJavaVM->DetachCurrentThread();
			send = false;
		}

		if (send) {
			env->CallStaticVoidMethod(interfaceClass, method);
		}

		gJavaVM->DetachCurrentThread();

	}
	LOG("------------ SENDER STOPPED-----------------");
}

void unableToConnectZmq() {

	JNIEnv *env = getJniEnv();
	jclass interfaceClass;
	if (env) {
		interfaceClass = getCallbackInterface(env);
	}

	bool send = true;
	if (interfaceClass) {
		/* Find the callBack method ID */
		jmethodID method = env->GetMethodID(interfaceClass, "unableToConnect",
				"()V");
		if (!method) {
			LOG("callback_handler: failed to get method ID");
			gJavaVM->DetachCurrentThread();
			send = false;
		}

		if (send) {
			env->CallStaticVoidMethod(interfaceClass, method);
		}

		gJavaVM->DetachCurrentThread();

	}
	LOG("------------ SENDER STOPPED-----------------");
}

void sendData() {

	LOG("-------------ZEROMQ THREAD STARTED");

	zmq::context_t context(1);
	zmq::socket_t socket(context, ZMQ_REQ);

	LOG("Connecting to hello world server...");
	socket.connect("tcp://www.whatamidoing.info:12345");
	std::string start = "CONNECT";
	zmq::message_t request(start.size());

	memcpy((void *) request.data(), start.c_str(), start.size());
	LOG("Sending Hello ");
	socket.send(request);

	//  Get the reply.
	zmq::message_t reply;
	socket.recv(&reply);
	std::string expResponse = "ALIVE";

	bool send = true;
	if (expResponse.compare((const char *) reply.data())) {
		connectedZmq();
	} else {
		unableToConnectZmq();
		send = false;
	}
	LOG("Received World %s", reply.data());

	base64::encoder E;
	while (send) {
		try {

			cv::Mat data;
			cv::Mat in;
			if (dataToSend.pop(in)) {
				data = in.clone();

				int cols = data.cols;
				int rows = data.rows;
				int elemSize = (int) data.elemSize();
				int type = data.type();
				const size_t data_size = data.total() * data.elemSize();
				char d[data_size];
				memcpy(d, data.ptr(), data_size);
				const size_t n_data_size = data_size * 2
						+ (data.elemSize()) * 3;
				char *out = (char *) malloc(n_data_size);
				E.encode(d, data_size, out);

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

			}

		} catch (boost::thread_interrupted const&) {
			send = false;
		}
	}

	LOG("------------ SENDER STOPPED-----------------");

}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_startZeromq(
		JNIEnv *, jobject) {
	boost::thread sendDataThread(sendData);
	tgroup.add_thread(&sendDataThread);
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

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_surfaceInit(
		JNIEnv *, jobject) {
	LOG("INTI SURFACE 1");
	glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	LOG("INTI SURFACE 2");
	glClearDepthf(1.0f);
	LOG("INTI SURFACE 4");
//	glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
	LOG("END INTI SURFACE");
}
JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_initCamera(
		JNIEnv*, jobject, jint width, jint height, jint rot, jint camId) {
	LOG("Camera Created");


	frameWidth = width;
	frameHeight = height;
	orientation = rot;
	cameraId = camId;
	//capture.open(CV_CAP_ANDROID + 0);
	LOG("frameWidth = %d", frameWidth);
	LOG("frameHeight = %d", frameHeight);
	capture.open(cameraId);
	capture.set(CV_CAP_PROP_FRAME_WIDTH, frameWidth);
	capture.set(CV_CAP_PROP_FRAME_HEIGHT, frameHeight);


	//boost::thread frameRetrieverThread(frameRetriever);
	//tgroup.add_thread(&frameRetrieverThread);

}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_surfaceChanged(
		JNIEnv*, jobject, jint width, jint height, jint orien) {
	LOG("Surface Changed");
	glViewport(0, 0, width, height);
	if (orien == 1) {
		screenWidth = width;
		screenHeight = height;
		orientation = 1;
	} else {
		screenWidth = height;
		screenHeight = width;
		orientation = 2;
	}

	LOG("screenWidth = %d", screenWidth);
	LOG("screenHeight = %d", screenHeight);
	//glMatrixMode(GL_PROJECTION);
	//LOG("2");
	//glLoadIdentity();
	//LOG("3");
	//float aspect = screenWidth / screenHeight;
	//float bt = (float) tan(double(45 / 2));
	//float lr = bt * aspect;
	//glFrustumf(-lr * 0.1f, lr * 0.1f, -bt * 0.1f, bt * 0.1f, 0.1f, 100.0f);
	//LOG("4");
	//glMatrixMode(GL_MODELVIEW);
	//LOG("5");
	//glLoadIdentity();
	//LOG("6");
	LOG("1");
	glEnable(GL_TEXTURE_2D);
	LOG("2");
	glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	LOG("3");
	glEnable(GL_CULL_FACE);
	LOG("4");
	//glShadeModel(GL_SMOOTH);
	LOG("5");
	glClearDepthf(1.0f);
	LOG("6");
	glEnable(GL_DEPTH_TEST);
	LOG("7");
	glDepthFunc(GL_ALWAYS);
	LOG("BEFORE CREATE TEXTURE");

}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_releaseCamera(
		JNIEnv*, jobject) {
	LOG("Camera Released");
	capture.release();
	//tgroup.interrupt_all();
	destroyTexture();

}

void destroyTexture() {
	LOG("Texture destroyed");
	glDeleteTextures(1, &texture);
}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_renderBackground(
		JNIEnv*, jobject, jboolean sendData) {
	drawBackground(sendData);
}

void drawBackground(bool sendData) {

}

void frameRetriever() {

	bool readFrame = true;
	int count = 0;
	LOG("---------------- STARTING RETREIVING FRAMES---------------------");
	while (capture.isOpened() && readFrame) {
		try {
			fps = capture.get(CV_CAP_PROP_FPS);
			bool res = capture.read(inframe);
			fpsMeter.measure();
			cv::Mat mat;
			if (!inframe.empty() && (res == true)) {
				const cv::Mat fr = inframe.clone();

				//char val[20];
				//sprintf(val,"/sdcard/out%d.png",count);

				//std::string newVal(val);
				//LOG("FileName:%s",newVal.c_str());
				//bool res = cv::imwrite(newVal.c_str(), fr);
				//pthread_mutex_lock(&FGmutex);
				LOG("PUSHING");
				sendToDisplayFrame.push(fr);
				count = count + 1;
				//pthread_mutex_unlock(&FGmutex);

			}

		} catch (boost::thread_interrupted const&) {
			readFrame = false;
		}
	}
	//dataToSend.
	LOG("Camera Closed");
}

}
