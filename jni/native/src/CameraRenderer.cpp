#include <android/log.h>
//#include <boost/lockfree/queue.hpp>
#include <boost/thread/exceptions.hpp>
#include <boost/thread/thread.hpp>
#include <boost/lockfree/spsc_queue.hpp>
#include <boost/interprocess/creation_tags.hpp>
#include <boost/interprocess/ipc/message_queue.hpp>
#include <boost/atomic/atomic.hpp>
#include <EGL/egl.h>

#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>

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
	GLuint projectionLoc;
	GLuint modelLoc;
	GLuint viewLoc;
	GLfloat projectionMatrix[16];
	GLfloat viewMatrix[16];
	GLfloat projectionViewMatrix[16];

	glm::mat4 Model, View, Projection;
	GLuint modelViewLoc;
	GLuint baseMapLoc;	// location for s_baseMap sampler2D in fragment shader
} UserData;

UserData userData;

/************* Examples **************************/
// gl_Position =  u_projection * u_view * u_model * a_position;
GLchar VERTEX_SHADER[] = "uniform mat4 u_projection; \n"
		"uniform mat4 u_model; \n"
		"uniform mat4 u_view; \n"
		"attribute vec4 a_position;   \n"
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
                -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
                 1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
                -1.0f,  1.0f, 0.0f, 0.0f, 1.0f,
                 1.0f,  1.0f, 0.0f, 1.0f, 1.0f,
};

/*
GLfloat vVertices[] = {
     -1.0f, -1.0f,0.0f,
     1.0f, 1.0f,
     1.0f, -1.0f,0.0f,
     1.0f, 0.0f,
     -1.0f,  1.0f,0.0f,
     0.0f,  1.0f,
     1.0f,  1.0f,0.0f,
     0.0f,  0.0f,
 };
 */
/*
GLfloat vVertices[] = { -0.5f, 0.5f, 0.0f,  // Position 0
		0.0f, 0.0f,        // TexCoord 0
		-0.5f, -0.5f, 0.0f, // Position 1
		0.0f, 1.0f,        // TexCoord 1
		0.5f, -0.5f, 0.0f, // Position 2
		1.0f, 1.0f,        // TexCoord 2
		0.5f, 0.5f, 0.0f,  // Position 3
		1.0f, 0.0f         // TexCoord 3
		};
*/
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
void translateM(float m[], int mOffset, float x, float y, float z);
void printMatrix(float m[]);
void checkBufferStatus();
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
	glClearColor(0.0f, 0.3f, 0.0f, 0.0f);
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
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, frameWidth, frameHeight, 0, GL_RGB,
			GL_UNSIGNED_BYTE, NULL);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

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

	// Set the base map sampler to texture unit to 0
	/*
	glUniform1i(userData.baseMapLoc, 0);

	glUniformMatrix4fv(userData.modelLoc, 1, GL_FALSE,
			glm::value_ptr(userData.View));
	glUniformMatrix4fv(userData.viewLoc, 1, GL_FALSE,
			glm::value_ptr(userData.View));
	glUniformMatrix4fv(userData.projectionLoc, 1, GL_FALSE,
			glm::value_ptr(userData.Projection));

	 */
	//glDrawElements(GL_TRIANGLE_STRIP, 6, GL_UNSIGNED_SHORT, 0);
	glDrawArrays(GL_TRIANGLE_STRIP,0,4);
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

	//userData.textureId = createTexture();
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

	/*
	userData.projectionLoc = glGetUniformLocation(userData.programObject,
			"u_projection");
	userData.modelLoc = glGetUniformLocation(userData.programObject, "u_model");
	userData.viewLoc = glGetUniformLocation(userData.programObject, "u_view");
	LOG("projectionLoc=%d, modelLoc=%d viewLoc=%d a_postion=%d, a_textCord=%d",
			userData.projectionLoc, userData.modelLoc, userData.viewLoc,
			userData.positionLoc, userData.texCoordLoc);
	*/
	if (userData.baseMapLoc == 0) {
		LOG(
				"--------------- UNABLE TO LOAD THE BASEMAPLOC------------------------");
	}


	setupVBO();
	userData.textureId = createTexture();
	createRenderBuffer();
}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_initCamera(
		JNIEnv*, jobject, jint width, jint height, jint rot, jint camId) {
	LOG("Camera Created");

	orientation = rot;
	cameraId = camId;
	//capture.open(CV_CAP_ANDROID + 0);
	LOG("cameraPictureWidth = %d", width);
	LOG("cameraPictureHeight = %d", height);
	capture.open(cameraId);
	capture.set(CV_CAP_PROP_FRAME_WIDTH, width);
	capture.set(CV_CAP_PROP_FRAME_HEIGHT, height);
	screenWidth = width;
	screenHeight = height;

	// Projection matrix : 45° Field of View, 4:3 ratio, display range : 0.1 unit <-> 100 units
	//glm::mat4 Projection = glm::perspective(45.0f, float(frameWidth / frameHeight), 0.1f, 50.0f);
	glm::mat4 Projection = glm::ortho<GLfloat>( 0.0, frameWidth, frameHeight, 0.0, 1.0, -1.0 );
	glm::mat4 View = glm::mat4();
	/*
	glm::mat4 View       = glm::lookAt(
								glm::vec3(0,-3,1), // Camera is at (4,3,-3), in World Space
								glm::vec3(0,0,0), // and looks at the origin
								glm::vec3(1,1,0)  // Head is up (set to 0,-1,0 to look upside-down)
						   );*/
	// Model matrix : an identity matrix (model will be at the origin)
	glm::mat4 Model      = glm::mat4();
	// Our ModelViewProjection : multiplication of our 3 matrices
	//userData.Projection = Projection * View * Model; // Remember, matrix multiplication is the other way around
	userData.Projection = glm::ortho(0.1f, float(frameWidth), float(frameHeight), 0.1f);
	//userData.Projection=glm::perspective(100.0f, (float)screenWidth/screenHeight, 1.0f, 100.0f);
	//boost::thread frameRetrieverThread(frameRetriever);
	//tgroup.add_thread(&frameRetrieverThread);

}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_surfaceChangedNative(
		JNIEnv*, jobject, jint width, jint height, jint orien) {
	LOG("Surface Changed");

	frameHeight = height;
	frameWidth = width;


	glEnable(GL_DEPTH_TEST);
	glDepthFunc(GL_LEQUAL);


//	userData.Projection = glm::perspective(20.1f, aspect, 0.1f, 1.0f);
//	userData.Projection = glm::perspective(45.0f, aspect, 1.0f, 100.0f);
//	userData.Projection = glm::ortho(0, frameWidth, 0, frameHeight);
	glViewport(0, 0, width, height);

	//userData.Projection = glm::ortho(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);
	//userData.Model = glm::translate(userData.Model, glm::vec3(0.1f, 0.2f, 0.5f));

}

void printMatrix(float m[]) {

	int i;
	LOG("---matix begin--");
	for (i = 0; i < 16; i++) {
		LOG("%.2f", m[i]);
	}
	LOG("--matrix-end");
}
///
// Draw a triangle using the shader pair created in Init()
//
JNIEXPORT void JNICALL
Java_com_watamidoing_nativecamera_Native_draw(JNIEnv* env, jobject obj) {

	GLuint textureId;
	cv::Mat fr;
	GLuint offset = 0;
	LOG("READING FRAME frameWidth=%d frameHeight=%d", frameWidth, frameHeight);
//	if (sendToDisplayFrame.pop(fr)) {
	if (capture.isOpened() && capture.read(inframe)) {
		LOG("READ FRAME");
		inframe.copyTo(fr);
		cv::cvtColor(fr, outframe, CV_BGR2RGB);
		cv::flip(outframe, rgbFrame, 1);

		renderToTexture(rgbFrame);

		//setProjection(frameWidth,frameHeight);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		// Use the program object
		glUseProgram(userData.programObject);

		//glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, userData.textureId);

		/// glBindFramebuffer(GL_FRAMEBUFFER, userData.frameBuffer);
		//glViewport(0, 0, frameWidth, frameHeight);
		//glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
		//userData.textureId = LoadTexture(rgbFrame);

		//loadSubTexture(rgbFrame);
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

		glUniformMatrix4fv(userData.modelLoc, 1, GL_FALSE,
				glm::value_ptr(userData.View));
		glUniformMatrix4fv(userData.viewLoc, 1, GL_FALSE,
				glm::value_ptr(userData.View));
		glUniformMatrix4fv(userData.projectionLoc, 1, GL_FALSE,
				glm::value_ptr(userData.Projection));

		//glFrontFace(GL_CW);
		//glDrawElements(GL_TRIANGLE_STRIP, 6, GL_UNSIGNED_SHORT, 0);
		glDrawArrays(GL_TRIANGLE_STRIP,0,4);

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
	glEnable(GL_TEXTURE_2D);
	glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	glEnable(GL_CULL_FACE);
	glClearDepthf(1.0f);
	glEnable(GL_DEPTH_TEST);
	glDepthFunc(GL_ALWAYS);
}

JNIEXPORT void JNICALL Java_com_watamidoing_nativecamera_Native_releaseCamera(
		JNIEnv*, jobject) {
	LOG("Camera Released");
	capture.release();
	//tgroup.interrupt_all();
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
