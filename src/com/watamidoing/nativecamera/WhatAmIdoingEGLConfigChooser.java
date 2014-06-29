package com.watamidoing.nativecamera;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import android.opengl.GLSurfaceView.EGLConfigChooser;
public class WhatAmIdoingEGLConfigChooser implements EGLConfigChooser{

	
	private static final int EGL_OPENGL_ES2_BIT = 4;
	@Override
	public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
		
		EGL10 mEgl = (EGL10) EGLContext.getEGL();
		EGLDisplay mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }
		/*
		 * EGL10.EGL_RED_SIZE, 5,
                 EGL10.EGL_GREEN_SIZE, 6,
                 EGL10.EGL_BLUE_SIZE, 5,
                 EGL10.EGL_DEPTH_SIZE, 16,
		 */
		int attribs[] = {
			//	EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
				EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE,8,
                EGL10.EGL_DEPTH_SIZE, 16,
            	EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
			    EGL10.EGL_NONE
			};

		
		EGLConfig[] configs = new EGLConfig[1];
        int[] result = new int[1];
        
        egl.eglChooseConfig(mEglDisplay, attribs, configs, 1, result);
        return configs[0];
     	}

}
