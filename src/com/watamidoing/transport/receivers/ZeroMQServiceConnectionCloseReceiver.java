package com.watamidoing.transport.receivers;

import com.watamidoing.reeiver.callbacks.ZeroMQController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ZeroMQServiceConnectionCloseReceiver extends BroadcastReceiver {

	public static final String SERVICE_CONNECTION_CLOSED = "com.whatamidoing.service.connection.close";
	private ZeroMQController zeroMQController;
	
	public ZeroMQServiceConnectionCloseReceiver(ZeroMQController zeroMQController) {
		this.zeroMQController = zeroMQController;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("ServiceConnectionCloseReceiver.onReceive","receved message");
		zeroMQController.zeroMQServiceConnectionClose(true);
		
	}

}
