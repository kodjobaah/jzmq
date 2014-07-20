package com.watamidoing.transport.receivers;

import com.watamidoing.reeiver.callbacks.ZeroMQController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ZeroMQServiceStartedReceiver extends BroadcastReceiver {

	public static final String SERVICE_STARTED = "com.whatamidoing.service.started";
	private ZeroMQController zeroMQController;
	
	public ZeroMQServiceStartedReceiver(ZeroMQController zeroMQController) {
		this.zeroMQController = zeroMQController;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("ServiceStartedReceiver.onReceive","receved message");
		zeroMQController.zeroMQServiceStarted(true);
		
	}

}
