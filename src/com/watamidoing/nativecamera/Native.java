package com.watamidoing.nativecamera;

import android.os.Message;
import android.util.Log;

public class Native {

	private static final String TAG = Native.class.getName();

	public static void loadlibs() {
	//	System.loadLibrary("opencv_java");
		System.loadLibrary("NativeCamera");
	}

	public static native void initCamera(int width, int height,int rot, int camId);
	public static native void releaseCamera();
	public static native void renderBackground(boolean transmitFrame);
	public static native void surfaceChanged(int width, int height, int orientation);
	public static native void storeMessenger(NativeCallback messenger);
	public static native void startZeromq();
	public static native void init(int width, int height);
	public static native void draw();
	public static native void surfaceInit();
	public static native void surfaceChangedNative(int width, int height);

}
