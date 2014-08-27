/*
 * FrameBufferData.h
 *
 *  Created on: 26 Aug 2014
 *      Author: whatamidoing
 */

#ifndef FRAMEBUFFERDATA_H_
#define FRAMEBUFFERDATA_H_

#include "global.h"

namespace waid {

class FrameBufferData {

public:
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

	FrameBufferData();
	virtual ~FrameBufferData();
};

} /* namespace waid */

#endif /* FRAMEBUFFERDATA_H_ */
