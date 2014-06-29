package com.watamidoing.nativecamera;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import android.opengl.GLSurfaceView.EGLContextFactory;
import android.util.Log;

public class WhatAmIdoingEglContextFactory implements EGLContextFactory{

	private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
	private static final String TAG = WhatAmIdoingEglContextFactory.class.getName();
	
	@Override
	public EGLContext createContext(EGL10 egl, EGLDisplay display,
			EGLConfig eglConfig) {
		
		EGL10 mEgl = (EGL10) EGLContext.getEGL();
	    EGLDisplay mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }
        
        String res = mEgl.eglQueryString(mEglDisplay,EGL10.EGL_EXTENSIONS);

        Log.i(TAG,"---EGL EXTENSIONS:"+res);
        int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };     
		EGLContext context = egl.eglCreateContext(
		mEglDisplay, 
		 eglConfig,  
		 EGL10.EGL_NO_CONTEXT
		 ,attrib_list);     
		
		return context;   
		
	}

	@Override
	public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
		// TODO Auto-generated method stub
		
	}

}
