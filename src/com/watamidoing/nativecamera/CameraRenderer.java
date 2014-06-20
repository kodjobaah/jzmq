package com.watamidoing.nativecamera;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.opencv.core.Size;
import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;

public class CameraRenderer implements Renderer{

	private Size size;
	private Context context;
	public CameraRenderer(Context c, Size size) {
		super();
		context=  c;
		this.size = size;
	}

	@Override
	public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		Native.initCamera((int)size.width,(int)size.height);
	}

	@Override
	public void onDrawFrame(GL10 arg0) {
		Native.renderBackground();
	}

	@Override
	public void onSurfaceChanged(GL10 arg0, int width, int height) {
		Native.surfaceChanged(width,height,context.getResources().getConfiguration().orientation);
	}


}
