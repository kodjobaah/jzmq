package com.watamidoing.nativecamera;

import android.util.Log;

import com.watamidoing.transport.service.ZeroMQServiceNative;
import com.watamidoing.view.WhatAmIdoing;

public class NativeCommunicator implements NativeCallback {

	private static final String TAG = NativeCommunicator.class.getName();
	private WhatAmIdoing activity;
	private ZeroMQServiceNative zeroMQServiceNative;

	public NativeCommunicator(WhatAmIdoing activity) {
		this.activity = activity;
	}

	public NativeCommunicator(ZeroMQServiceNative zeroMQServiceNative) {
		this.zeroMQServiceNative = zeroMQServiceNative;
	}

	@Override
	public void ableToConnect() {
		
		Log.d(TAG,"---- ABLE TO CONNECT");
		
		zeroMQServiceNative.sendServiceStartNotification();
		
	}

	@Override
	public void unableToConnect() {
		Log.d(TAG,"---- UN - ABLE TO CONNECT");
		zeroMQServiceNative.sendServiceStopNotification();
	}

	@Override
	public void connectionDropped() {
		Log.d(TAG,"---- CONNECTION DROPPED");
		zeroMQServiceNative.sendServiceStopNotification();
		
	}

	@Override
	public void updateMessagesSent(long messageCount) {
		Log.d(TAG,"---- MESSAGE COUNT");
		zeroMQServiceNative.updateMessagesSent(messageCount);
	}

}
