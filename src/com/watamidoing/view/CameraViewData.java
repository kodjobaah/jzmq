package com.watamidoing.view;

import org.opencv.core.Mat;

public class CameraViewData {

	public byte[] data;
	public int width;
	public int height;
	public int imageWidth;
	public int imageHeight;
	public int previewFormat;
	private Mat mat;

	public void setFrame(byte[] data) {
		this.data = data;
		
	}

	public void setWidth(int width) {
		this.width = width;
		
	}

	public void setHeight(int height) {
		this.height = height;
		
	}

	public void setImageWidth(int imageWidth) {
		this.imageWidth = imageWidth;
		
	}

	public void setImageHeight(int imageHeight) {
		this.imageHeight = imageHeight;
		
	}

	public void setPreviewFromat(int previewFormat) {
		this.previewFormat = previewFormat;
	}

	public void setMat(Mat rgb) {
		this.mat = rgb;
		
	}

	public Mat getMat() {
		return mat;
	}

}
