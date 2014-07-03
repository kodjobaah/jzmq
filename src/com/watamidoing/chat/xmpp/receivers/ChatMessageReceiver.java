package com.watamidoing.chat.xmpp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.watamidoing.reeiver.callbacks.XMPPConnectionController;

public class ChatMessageReceiver extends BroadcastReceiver {

	
	public static final String MESSAGE_RECIEVED = "com-waid-xmpp-message-received";
	private static final String TAG = "ChatMessageReceiver";
	private XMPPConnectionController xmppController;
	
	public ChatMessageReceiver(XMPPConnectionController xmppController) {
		this.xmppController = xmppController;
	}
	
    @Override
    public void onReceive(final Context context, final Intent intent) {
    			
    			if (intent.getAction().equalsIgnoreCase(MESSAGE_RECIEVED)) {
    				String chatMessage = intent.getStringExtra(XMPPConnectionController.CHAT_MESSAGE);
    				xmppController.messageReceived(chatMessage);
    			}
    }
}
