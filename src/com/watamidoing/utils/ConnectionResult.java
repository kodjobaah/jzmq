package com.watamidoing.utils;

public class ConnectionResult {

	private String result;
	private int statusCode;

	public ConnectionResult(String result, int statusCode) {
		this.result = result;
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getResult() {
		return result;
	}

}
