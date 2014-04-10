package com.watamidoing.reeiver.callbacks;

import java.io.Serializable;

import android.os.Messenger;

public interface TotalWatchersController extends Serializable {
	
	String CONNECTION_OPEN = "com-waid-total-watchers-connection-open";
	String CONNECTION_CLOSE = "com-waid-total-watchers-connection-close";
	String MESSAGE = "com-waid-total-watchers-message";
	
	/*
	 * Invoked in [[com.watamidoing.total.receivers.TotalWatchersReceiver]]
	 */
	public void updateTotalWatchers(String totalWatchers);
}
