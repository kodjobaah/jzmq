package com.watamidoing.value;

public class FrameData {

	private String frame;
	private String time;

	public FrameData(String frame, String time) {
		this.frame = frame;
		this.time = time;
	}

	public String getTime() {
		return this.time;
	}

	public String getFrame() {
		return this.frame;
	}

}
