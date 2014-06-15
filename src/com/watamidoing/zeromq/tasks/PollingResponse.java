package com.watamidoing.zeromq.tasks;

public class PollingResponse {

	public static final int NOT_ABLE_TO_CONNECT = 1;
	public static final int SENT = 2;
	public static final int RETRY = 3;
	
	private int status;

	public void setStatus(int status) {
		this.status = status;
	}
	
	public int getStatus() {
		return this.status;
	}

}
