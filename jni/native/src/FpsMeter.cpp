/*
 * FpsMeter.cpp
 *
 *  Created on: 23 Jun 2014
 *      Author: whatamidoing
 */

#include <opencv2/core/core.hpp>
#include <android/log.h>
#include "FpsMeter.h"

#define LOG_TAG    "FPS_METER"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)


FpsMeter::FpsMeter() {
	mFramesCouner = 0;
	mFrequency = 0.0;
	mprevFrameTime = 0;
	currentFps = 0.0;
	STEP = 20;
	mIsInitialized = false;

}

void FpsMeter::measure() {
        if (!mIsInitialized) {
            init();
            mIsInitialized = true;
        } else {
            mFramesCouner++;
            if (mFramesCouner % STEP == 0) {
                long time = cv::getTickCount();
                double fps = STEP * mFrequency / (time - mprevFrameTime);
                mprevFrameTime = time;
                currentFps = fps;
                LOG("CURRENT FPS %f",fps);
            }
        }
    }

void FpsMeter::init() {
        mFramesCouner = 0;
        mFrequency = cv::getTickFrequency();
        mprevFrameTime = cv::getTickCount();

}

FpsMeter::~FpsMeter() {
	// TODO Auto-generated destructor stub
}

