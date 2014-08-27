/*
 * VideoController.cpp
 *
 *  Created on: 24 Aug 2014
 *      Author: whatamidoing
 */



#include "FrameBufferData.h"
#include "VideoController.h"


// Utility for logging:
#define LOG_TAG    "CAMERA_RENDERER"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


namespace waid {

int VideoController::VERTEX_POS_SIZE=3;
int VideoController::VERTEX_TEXCOORD0_SIZE=2;

VideoController::VideoController() {


	/************* Examples **************************/

	vertexShader = std::string("attribute vec4 a_position;   \n"
			"attribute vec2 a_texCoord;   \n"
			"varying vec2 v_texCoord;     \n"
			"void main()                  \n"
			"{                            \n"
			"    gl_Position =  a_position; \n"
			"   v_texCoord = a_texCoord;  \n"
			"}                            \n");

	VERTEX_SHADER = const_cast<GLchar* >(vertexShader.c_str());

	fragmentShader = std::string("precision mediump float;                            \n"
					"varying vec2 v_texCoord;                            \n"
					"uniform sampler2D s_baseMap;                        \n"
					"void main()                                         \n"
					"{                                                   \n"
					"                                                    \n"
					"  gl_FragColor = texture2D( s_baseMap, v_texCoord );   \n"
					"}                                                   \n");

	FRAGMENT_SHADER = const_cast<GLchar *>(fragmentShader.c_str());

	vtxStride = sizeof(GLfloat) * ( VERTEX_POS_SIZE + VERTEX_TEXCOORD0_SIZE);
	nVertices = 4;

	indices[0] = 0;
	indices[1] = 1;
	indices[2] = 2;
	indices[3] = 0;
	indices[4] = 2;
	indices[5] = 3;


	vVertices[0] = -1.0f;
	vVertices[1] = -1.0f;
	vVertices[2] = 0.0f;
	vVertices[3] = 0.0f;
	vVertices[4] = 0.0f;
	vVertices[5] = 1.0f;
	vVertices[6] = 0.0f;
	vVertices[7] = -1.0f;
	vVertices[8] = 0.0f;
	vVertices[9] =  1.0f;
	vVertices[10] = 0.0f;
	vVertices[11] = -1.0f;
	vVertices[12] = 1.0f;
	vVertices[13] = 0.0f;
	vVertices[14] = 1.0f;
	vVertices[15] = 1.0f;
	vVertices[16] = 1.0f;
	vVertices[17] = 0.0f;
	vVertices[18] = 1.0f;
	vVertices[19] = 1.0f;

	EGLContext mEglContext;
	EGLDisplay mEglDisplay = EGL_NO_DISPLAY;


	printGlString("Version", GL_VERSION);
	printGlString("Vendor", GL_VENDOR);
	printGlString("Renderer", GL_RENDERER);
	printGlString("Extensions", GL_EXTENSIONS);

}

VideoController::~VideoController() {
	// TODO Auto-generated destructor stub
	glDeleteFramebuffers(1, &frameBufferData.frameBuffer);
	glDeleteTextures(1, &frameBufferData.textureId);
	glDeleteProgram(frameBufferData.programObject);

}

void VideoController::setupSurface(int width, int height) {
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

	frameBufferData.programObject = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

	// Get the attribute locations
	frameBufferData.positionLoc = glGetAttribLocation(frameBufferData.programObject,
			"a_position");
	checkGlError("glGetAttribLocation-positionLoc");
	frameBufferData.texCoordLoc = glGetAttribLocation(frameBufferData.programObject,
			"a_texCoord");
	checkGlError("glGetAttribLocation-textLoc");
	// Get the sampler location
	frameBufferData.baseMapLoc = glGetUniformLocation(frameBufferData.programObject,
			"s_baseMap");
	checkGlError("glGetUniformLocation-s_basemap");

	if (frameBufferData.baseMapLoc == 0) {
		LOG(
				"--------------- UNABLE TO LOAD THE BASEMAPLOC------------------------");
		frameBufferData.baseMapLoc = 2;
	}

	setupVBO();
	//frameBufferData.textureId = createTexture();
	createRenderBuffer();

}
void VideoController::surfaceChanged(int width, int height) {

	glEnable(GL_DEPTH_TEST);
	glDepthFunc(GL_LEQUAL);
	glViewport(0, 0, width, height);
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

}

void VideoController::updateDimensions(int width, int height) {
	screenWidth = width;
	screenHeight = height;
}

void VideoController::draw(cv::Mat rgbFrame) {

	GLuint textureId;
	GLuint offset = 0;

	renderToTexture(rgbFrame);

	//setProjection(frameWidth,frameHeight);
	glViewport(0, 0, frameWidth, frameHeight);
	glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

	// Use the program object
	glUseProgram(frameBufferData.programObject);

	//glActiveTexture(GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, frameBufferData.textureId);

	loadSubTexture(rgbFrame);
	glBindBuffer(GL_ARRAY_BUFFER, frameBufferData.vboIds[0]);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, frameBufferData.vboIds[1]);

	glEnableVertexAttribArray(frameBufferData.positionLoc);
	glEnableVertexAttribArray(frameBufferData.texCoordLoc);

	glVertexAttribPointer(frameBufferData.positionLoc, VERTEX_POS_SIZE,
	GL_FLOAT, GL_FALSE, vtxStride, (const void *) offset);

	offset += VERTEX_POS_SIZE * sizeof(GLfloat);
	glVertexAttribPointer(frameBufferData.texCoordLoc,
	VERTEX_TEXCOORD0_SIZE,
	GL_FLOAT, GL_FALSE, vtxStride, (const void *) offset);

	// Set the base map sampler to texture unit to 0
	glUniform1i(frameBufferData.baseMapLoc, 0);

	glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

	glDisableVertexAttribArray(frameBufferData.positionLoc);
	glDisableVertexAttribArray(frameBufferData.texCoordLoc);


}
bool VideoController::checkGlError(const char* funcName) {
	GLint err = glGetError();
	if (err != GL_NO_ERROR) {
		LOGE("GL error after %s(): 0x%08x\n", funcName, err);
		return true;
	}
	return false;
}

void VideoController::printGlString(const char* name, GLenum s) {
	const char* v = (const char*) glGetString(s);
	LOG("GL %s: %s\n", name, v);
}

///
// Create a shader object, load the shader source, and
// compile the shader.
//
GLuint VideoController::createShader(GLenum shaderType, const char* src) {
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

GLuint VideoController::createProgram(const char* vtxSrc, const char* fragSrc) {
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

void VideoController::setupVBO() {

	GLuint v = sizeof(GLfloat);
	GLuint s = sizeof(GLushort);
	// Only allocate on the first draw
	glGenBuffers(2, frameBufferData.vboIds);
	glBindBuffer( GL_ARRAY_BUFFER, frameBufferData.vboIds[0]);
	glBufferData( GL_ARRAY_BUFFER, nVertices * vtxStride, vVertices,
	GL_STATIC_DRAW);
	glBindBuffer( GL_ELEMENT_ARRAY_BUFFER, frameBufferData.vboIds[1]);
	glBufferData( GL_ELEMENT_ARRAY_BUFFER, sizeof(indices), indices,
	GL_STATIC_DRAW);
}

GLuint VideoController::createTexture() {
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

void VideoController::loadSubTexture(cv::Mat data) {
	glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, screenWidth, screenHeight,
	GL_RGB, GL_UNSIGNED_BYTE, data.ptr());
}

void VideoController::renderToTexture(cv::Mat rgbFrame) {
	GLuint offset = 0.0;
	glBindFramebuffer(GL_FRAMEBUFFER, frameBufferData.frameBuffer);
	glUseProgram(frameBufferData.programObject);

	glActiveTexture(GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, frameBufferData.textureId);

	glViewport(0, 0, frameWidth, frameHeight);
	glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

	loadSubTexture(rgbFrame);

	glBindBuffer(GL_ARRAY_BUFFER, frameBufferData.vboIds[0]);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, frameBufferData.vboIds[1]);

	glEnableVertexAttribArray(frameBufferData.positionLoc);
	glEnableVertexAttribArray(frameBufferData.texCoordLoc);

	glVertexAttribPointer(frameBufferData.positionLoc, VERTEX_POS_SIZE,
	GL_FLOAT, GL_FALSE, vtxStride, (const void *) offset);

	offset += VERTEX_POS_SIZE * sizeof(GLfloat);
	glVertexAttribPointer(frameBufferData.texCoordLoc,
	VERTEX_TEXCOORD0_SIZE,
	GL_FLOAT, GL_FALSE, vtxStride, (const void *) offset);
	glUniform1i(frameBufferData.baseMapLoc, 0);

	glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	glBindFramebuffer(GL_FRAMEBUFFER, 0);

}
GLuint VideoController::createRenderBuffer() {

	// create a framebuffer object, you need to delete them when program exits.
	glGenFramebuffers(1, &frameBufferData.frameBuffer);
	glBindFramebuffer(GL_FRAMEBUFFER, frameBufferData.frameBuffer);

	glGenRenderbuffers(1, &frameBufferData.renderBuffer);
	glBindRenderbuffer(GL_RENDERBUFFER, frameBufferData.renderBuffer);
	glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, frameWidth,
			frameHeight);

	// attach a renderbuffer to depth attachment point
	glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
	GL_RENDERBUFFER, frameBufferData.renderBuffer);
	glBindRenderbuffer(GL_RENDERBUFFER, 0);

	frameBufferData.textureId = createTexture();

	//glActiveTexture(GL_TEXTURE0);
	//glBindTexture(GL_TEXTURE_2D, frameBufferData.textureId);
	// attach a texture to FBO color attachement point
	glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
			frameBufferData.textureId, 0);

	glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
	GL_RENDERBUFFER, frameBufferData.renderBuffer);

	checkBufferStatus();
	glBindTexture(GL_TEXTURE_2D, 0);
	glBindFramebuffer(GL_FRAMEBUFFER, 0);
}

void VideoController::checkBufferStatus() {
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


} /* namespace waid */
