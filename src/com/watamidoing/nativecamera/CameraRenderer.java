package com.watamidoing.nativecamera;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.opencv.core.Size;

import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

import com.watamidoing.view.WhatAmIdoing;


public class CameraRenderer implements Renderer{

	private static final String TAG =  CameraRenderer.class.getName();
	private Size size;
	private WhatAmIdoing context;
	private int rotation;
	private int cameraId;
	private volatile boolean opencvopened = false;
	private volatile boolean startCamera = false;
	private volatile boolean transmitFrame = false;
	private volatile boolean initCalled = false;
	private NativeCallback mes;
	private int imageWidth;
	private int imageHeight;
	private NativeCommunicator nativeCommunicator;
	private CameraPreviewerData cameraPreviewData;
	public CameraRenderer(WhatAmIdoing whatAmIdoing, Size size,int rotation,int cameraId) {
		super();
		context=  whatAmIdoing;
		this.size = size;
		this.rotation = rotation;
		this.cameraId = cameraId;
		cameraPreviewData = new CameraPreviewerData();
	}

	@Override
	public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		size = cameraPreviewData.calcualteCameraFrameSize(size);
		Native.init((int)size.width,(int) size.height);
		Log.i(TAG,"----------------- SURFACE CREATED: width="+size.width+"----height="+size.height);
	}

	@Override
	public void onDrawFrame(GL10 arg0) {
		Native.draw();
	}

	@Override
	public void onSurfaceChanged(GL10 arg0, int width, int height) {
		Native.surfaceChangedNative(width, height);
	
	}

	public void setOrientation(int rotation, int cameraId, int imageWidth, int imageHeight) {
		this.rotation = rotation;
		this.cameraId = cameraId;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		
	}

	public void setSize(org.opencv.core.Size size) {
		this.size = size;
		Native.surfaceChangedNative((int)size.width,(int)size.height);
	}

	public void opencvOpen() {
		opencvopened  = true;
		
	}

	public boolean startCamera() {
		
	    Log.d(TAG,"------------- SIZE ==:"+size.toString());
		if(!initCalled && opencvopened) { 
			Native.initCamera((int)size.width,(int)size.height,rotation,cameraId);
			initCalled = true;
		} else {
			return false;
		}

		startCamera = true;
		return true;
		
	}

	public void stopCamera() {
		initCalled = false;
		startCamera = false;
		Native.releaseCamera();
		
	}

	public void opencvOpen(NativeCommunicator nativeCommunicator) {
		this.nativeCommunicator = nativeCommunicator;
		Native.loadlibs();
		Native.storeMessenger(nativeCommunicator);
		opencvOpen();
	}


}