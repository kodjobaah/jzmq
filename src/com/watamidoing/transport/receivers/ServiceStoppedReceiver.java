package com.watamidoing.transport.receivers;

import com.watamidoing.tasks.callbacks.WebsocketController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceStoppedReceiver extends BroadcastReceiver {

	public static final String SERVICE_STOPED = "com.whatamidoing.service.stoped";
	private WebsocketController websocketController;
	
	public ServiceStoppedReceiver(WebsocketController websocketController) {
		this.websocketController = websocketController;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("ServiceStoppedReceiver.onReceive","receved message");
		websocketController.websocketServiceStop(true);
		
	}

}
