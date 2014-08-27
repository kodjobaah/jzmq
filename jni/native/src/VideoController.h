/*
 * VideoController.h
 *
 *  Created on: 24 Aug 2014
 *      Author: whatamidoing
 */


#include "global.h"
#include "FrameBufferData.h"
#include <android/log.h>

#ifndef VIDEOCONTROLLER_H_
#define VIDEOCONTROLLER_H_



namespace waid {




class VideoController {

private:
	static int VERTEX_POS_SIZE; // x, y and z
	static int VERTEX_TEXCOORD0_SIZE; // s and t

	GLuint texture;

	int frameWidth;
	int frameHeight;
	int screenWidth;
	int screenHeight;



	/************* Examples **************************/
	//GLchar *VERTEX_SHADER;
	GLchar *FRAGMENT_SHADER;
	GLchar *VERTEX_SHADER;
	std::string vertexShader;
	std::string fragmentShader;
	GLuint vtxStride;

	GLuint nVertices;

	GLushort indices[6];

	GLfloat vVertices[20];
	EGLContext mEglContext;
	EGLDisplay mEglDisplay;
	FrameBufferData frameBufferData;

	bool checkGlError(const char* funcName);
	void printGlString(const char* name, GLenum s);
	GLuint createShader(GLenum shaderType, const char* src);
	GLuint createProgram(const char* vtxSrc, const char* fragSrc);
	void setupVBO();
	GLuint createTexture();
	void loadSubTexture(cv::Mat data);
	void renderToTexture(cv::Mat rgbFrame);
	GLuint createRenderBuffer();
	void checkBufferStatus();

public:
	VideoController();
	void setupSurface(int width, int height);
	void surfaceChanged(int width, int height);
	void updateDimensions(int width, int height);
	void draw(cv::Mat rgbFrame);
	virtual ~VideoController();
};


} /* namespace waid */

#endif /* VIDEOCONTROLLER_H_ */
