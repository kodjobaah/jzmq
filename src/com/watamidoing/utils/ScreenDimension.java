package com.watamidoing.utils;

import android.util.Log;

public class ScreenDimension {

	private int widthPixels;
	private int heightPixels;
	private float density;

	public ScreenDimension(int heightPixels, int widthPixels, float density) {
		this.heightPixels = heightPixels;
		this.widthPixels = widthPixels;
		this.density = density;
	}

	public int getWidthPixels() {
		Log.d("ScreenDimension","getWidthPixels=["+widthPixels+"]");
		return widthPixels;
	}

	public int getHeightPixels() {
		Log.d("ScreenDimension","getHeightPixels=["+heightPixels+"]");
		return heightPixels;
	}
	
	public int getDpWidthPixels() {
		
		Log.d("ScreenDimension","getDpWidthPixels=["+Float.valueOf(widthPixels/density).intValue()+"]");
		return Float.valueOf(widthPixels/density).intValue();
	}
	
	public int getDpHeightPixels() {
		Log.d("ScreenDimension","getDpHeightPixels=["+Float.valueOf(heightPixels/density).intValue()+"]");
		return Float.valueOf(heightPixels/density).intValue();
	}

}
