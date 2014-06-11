package com.watamidoing.transport.receivers;

import com.watamidoing.reeiver.callbacks.ZeroMQController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ZeroMQNotAbleToConnectReceiver extends BroadcastReceiver {
	
	public static final String NOT_ABLE_TO_CONNECT = "com.whatamidoing.not.able.to.connect";
	private ZeroMQController zeroMQController;

	
	public ZeroMQNotAbleToConnectReceiver(ZeroMQController zeroMQController) {
		this.zeroMQController = zeroMQController;
	}
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Log.i("NotAbleToConnectReceiver.onReceive","receved message");
		zeroMQController.zeroMQProblems(true);
	}

}
