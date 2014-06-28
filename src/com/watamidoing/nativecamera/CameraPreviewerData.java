package com.watamidoing.nativecamera;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.NativeCameraView.OpenCvSizeAccessor;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

public class CameraPreviewerData {
		public CameraPreviewerData() {
	}
	
	public Size calculateCameraFrameSize(int dWidth, int dHeight) {
		
		VideoCapture mCamera = new VideoCapture(Highgui.CV_CAP_ANDROID);
		
		java.util.List<Size> supportedSizes = new ArrayList<Size>();
		try {
			supportedSizes = mCamera.getSupportedPreviewSizes();
			mCamera.release();
		
		} catch (NumberFormatException e) {
			
		}

		OpenCvSizeAccessor accessor = new OpenCvSizeAccessor();
		int calcWidth = Integer.MAX_VALUE;
		int calcHeight = Integer.MAX_VALUE;
		
		
		int maxAllowedWidth = 1024;
		int maxAllowedHeight = 1024;
		
		
		int prevWidth = 0;
		int prevHeight = 0;
		for(Object size: supportedSizes){
			int width = accessor.getWidth(size);
			int height = accessor.getHeight(size);
			
			if ((dWidth >= prevWidth) && (dWidth <= width)) {
			
				calcWidth= (int)width;
				calcHeight = (int)height;
				break;
			}
			
			
			if ((dHeight >= prevHeight) && (dHeight <= height)) {
				
				calcWidth= (int)width;
				calcHeight = (int)height;
				break;
			}
			
			prevWidth = width;
			prevHeight = height;
		}
		return new Size(calcWidth, calcHeight);
	}
	
}