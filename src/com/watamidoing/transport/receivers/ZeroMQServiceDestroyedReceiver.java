package com.watamidoing.transport.receivers;

import com.watamidoing.reeiver.callbacks.ZeroMQController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ZeroMQServiceDestroyedReceiver extends BroadcastReceiver {

	private static final String TAG = ZeroMQServiceDestroyedReceiver.class.getName();
	public static final String SERVICE_DESTROYED = "com.whatamidoing.service.destroyed";
	private ZeroMQController zeroMQController;
	
	public ZeroMQServiceDestroyedReceiver(ZeroMQController zeroMQController) {
		this.zeroMQController = zeroMQController;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG,"receved message destroy ");
		zeroMQController.zeroMQServiceDestroyed(true);
		
	}

}
