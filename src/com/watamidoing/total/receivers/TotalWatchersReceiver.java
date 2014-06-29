package com.watamidoing.total.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.watamidoing.reeiver.callbacks.TotalWatchersController;

public class TotalWatchersReceiver extends BroadcastReceiver {

	
	public static final String TOTAL_WATCHERS_RECEIVER = "com-waid-total-watchers-receiver";
	private TotalWatchersController totalWatchersController;
	
	public TotalWatchersReceiver(TotalWatchersController totalWatchersController) {
		this.totalWatchersController = totalWatchersController;
	}
	
    @Override
    public void onReceive(final Context context, final Intent intent) {
    		String totalWatchers = intent.getStringExtra(TotalWatchersController.MESSAGE);
    		totalWatchersController.updateTotalWatchers(totalWatchers);
    }
}
