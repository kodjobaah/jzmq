package com.watamidoing.transport.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.watamidoing.tasks.callbacks.XMPPConnectionController;

public class ChatMessageReceiver extends BroadcastReceiver {

	
	public static final String MESSAGE_RECIEVED = "com-waid-xmpp-message-received";
	private XMPPConnectionController xmppController;
	
	public ChatMessageReceiver(XMPPConnectionController xmppController) {
		this.xmppController = xmppController;
	}
	
    @Override
    public void onReceive(final Context context, final Intent intent) {
    			String chatMessage = intent.getStringExtra(XMPPConnectionController.CHAT_MESSAGE);
    			xmppController.messageReceived(chatMessage);
    }
}
