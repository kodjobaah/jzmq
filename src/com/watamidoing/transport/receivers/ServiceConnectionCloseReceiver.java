package com.watamidoing.transport.receivers;

import com.watamidoing.tasks.callbacks.WebsocketController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceConnectionCloseReceiver extends BroadcastReceiver {

	public static final String SERVICE_CONNECTION_CLOSED = "com.whatamidoing.service.connection.close";
	private WebsocketController websocketController;
	
	public ServiceConnectionCloseReceiver(WebsocketController websocketController) {
		this.websocketController = websocketController;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("ServiceConnectionCloseReceiver.onReceive","receved message");
		websocketController.websocketServiceConnectionClose(true);
		
	}

}
