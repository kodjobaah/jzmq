package com.watamidoing.transport.receivers;

import com.watamidoing.reeiver.callbacks.ZeroMQController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ZeroMQServiceStoppedReceiver extends BroadcastReceiver {

	public static final String SERVICE_STOPED = "com.whatamidoing.service.stoped";
	private ZeroMQController zeroMQController;
	
	public ZeroMQServiceStoppedReceiver(ZeroMQController zeroMQController) {
		this.zeroMQController = zeroMQController;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("ServiceStoppedReceiver.onReceive","receved message");
		zeroMQController.zeroMQServiceStop(true);
		
	}

}
