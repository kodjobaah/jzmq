package com.whatamidoing.transport.receivers;

import com.whatamidoing.tasks.callbacks.WebsocketController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotAbleToConnectReceiver extends BroadcastReceiver {
	
	public static final String NOT_ABLE_TO_CONNECT = "com.whatamidoing.not.able.to.connect";
	private WebsocketController websocketController;

	
	public NotAbleToConnectReceiver(WebsocketController websocketController) {
		this.websocketController = websocketController;
	}
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Log.d("NotAbleToConnectReceiver.onReceive","receved message");
		websocketController.websocketProblems(true);
	}

}
