package com.watamidoing.transport.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.watamidoing.tasks.callbacks.WebsocketController;

public class ServiceStartedReceiver extends BroadcastReceiver {

	public static final String SERVICE_STARTED = "com.whatamidoing.service.started";
	private final WebsocketController websocketController;
	
	public ServiceStartedReceiver(WebsocketController websocketController) {
		this.websocketController = websocketController;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("ServiceStartedReceiver.onReceive","receved message");
		websocketController.websocketConnectionCompleted(true);
	}

}
