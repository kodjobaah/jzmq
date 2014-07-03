package com.watamidoing.nativecamera;

import org.opencv.core.Size;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class WhatAmIDoingSurfaceView extends GLSurfaceView {
	private static final String TAG = WhatAmIDoingSurfaceView.class.getName();
	private CameraRenderer mRenderer;
	private boolean firstTap = true;
	private Long thisTime;
	private Long prevTime;
	protected static final long DOUBLE_CLICK_MAX_DELAY = 1000L;

	public WhatAmIDoingSurfaceView(Context context) {
	    super(context);
	    setZOrderMediaOverlay(true);
	    setZOrderOnTop(true);
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
	
	@Override
    public boolean onTouchEvent(MotionEvent event)
    {
	    if(firstTap){
            thisTime = SystemClock.uptimeMillis();
            firstTap = false;
        }else{
            prevTime = thisTime;
            thisTime = SystemClock.uptimeMillis();

            //Check that thisTime is greater than prevTime
            //just incase system clock reset to zero
            if(thisTime > prevTime){

                //Check if times are within our max delay
                if((thisTime - prevTime) <= DOUBLE_CLICK_MAX_DELAY){

                	 queueEvent(new Runnable()
                     {
                         @Override
                         public void run()
                         {
                        	    //We have detected a double tap!
                         	Log.d(TAG,"------------------------ DOUBLE CLICK--------------------------");
                             //PUT YOUR LOGIC HERE!!!
                         }
                     });
                
                    firstTap = true;
                }else{
                    //Otherwise Reset firstTap
                    firstTap = true;
                }
            }else{
                firstTap = true;
            }
        }
        return false;
    }
	
	 // Hides superclass method.
    public void setRenderer(CameraRenderer renderer)
    {
        mRenderer = renderer;
        super.setRenderer(renderer);
    }
 
}

