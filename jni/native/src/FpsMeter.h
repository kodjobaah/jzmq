/*
 * FpsMeter.h
 *
 *  Created on: 23 Jun 2014
 *      Author: whatamidoing
 */

#ifndef FPSMETER_H_
#define FPSMETER_H_

class FpsMeter {

	int STEP;
	int mFramesCouner;
	double mFrequency;
	long mprevFrameTime;
	bool mIsInitialized;
	double currentFps;

public:
	FpsMeter();
	virtual ~FpsMeter();
	void init();
	void measure();

};

#endif /* FPSMETER_H_ */
