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
		Log.i("ScreenDimension","getWidthPixels=["+widthPixels+"]");
		return widthPixels;
	}

	public int getHeightPixels() {
		Log.i("ScreenDimension","getHeightPixels=["+heightPixels+"]");
		return heightPixels;
	}
	
	public int getDpWidthPixels() {
		
		Log.i("ScreenDimension","getDpWidthPixels=["+Float.valueOf(widthPixels/density).intValue()+"]");
		return Float.valueOf(widthPixels/density).intValue();
	}
	
	public int getDpHeightPixels() {
		Log.i("ScreenDimension","getDpHeightPixels=["+Float.valueOf(heightPixels/density).intValue()+"]");
		return Float.valueOf(heightPixels/density).intValue();
	}

}
