/*
 * FrameBufferData.cpp
 *
 *  Created on: 26 Aug 2014
 *      Author: whatamidoing
 */

#include "FrameBufferData.h"

namespace waid {

FrameBufferData::FrameBufferData() {
	renderBuffer =0;
	frameBuffer = 0;
	texture  = 0;
	// Texture handle
	textureId = 0;
	// Offset location
	offsetLoc =0;
	programObject =0;
	// Attribute locations
	positionLoc =0;
	texCoordLoc =0;
	baseMapLoc =0;

}

FrameBufferData::~FrameBufferData() {

}

} /* namespace waid */
