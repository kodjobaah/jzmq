package com.watamidoing.reeiver.callbacks;

import java.io.Serializable;

import android.os.Messenger;

public interface MessagesSentController extends Serializable {

	String MESSAGE = "com-waid-messages-sent-message";
	
	/*
	 * Invoked in [[com.watamidoing.total.receivers.MessagesSentReceiver]]
	 */
	public void updateMessagesSent(String messagesSent);
}
