package com.watamidoing.total.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.watamidoing.reeiver.callbacks.MessagesSentController;
import com.watamidoing.reeiver.callbacks.TotalWatchersController;

public class MessagesSentReceiver extends BroadcastReceiver {

	
	public static final String MESSAGES_SENT_RECEIVER = "com-waid-messages-sent-receiver";
	private MessagesSentController messagesSentController;
	
	public MessagesSentReceiver(MessagesSentController messagesSentController) {
		this.messagesSentController = messagesSentController;
	}
	
    @Override
    public void onReceive(final Context context, final Intent intent) {
    		String messagesSent = intent.getStringExtra(MessagesSentController.MESSAGE);
    		messagesSentController.updateMessagesSent(messagesSent);
    }
}
