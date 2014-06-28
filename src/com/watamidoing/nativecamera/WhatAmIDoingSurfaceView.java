package com.watamidoing.nativecamera;

import org.opencv.core.Size;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

public class WhatAmIDoingSurfaceView extends GLSurfaceView {
	private static final String TAG = WhatAmIDoingSurfaceView.class.getName();

	public WhatAmIDoingSurfaceView(Context context) {
	    super(context);
	    Log.d(TAG,"---------------------------------CONSTRUCTOR 1 ---------------------------------");

	}

	public WhatAmIDoingSurfaceView(Context context, AttributeSet attribs) {
	    super(context, attribs);
	    setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
	    Log.d(TAG,"--------------------------------- CONSTRUCTOR 2 ---------------------------------");
	}

	public WhatAmIDoingSurfaceView(Context context, Size size) {
		super(context);
		Log.d(TAG,"--------------------------------- CONSTRUCTOR 3 ---------------------------------");
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG,"PAUSE-should be stoppinng camera");
		Native.releaseCamera();
	}	
}

