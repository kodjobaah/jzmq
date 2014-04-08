package com.watamidoing.transport.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.watamidoing.tasks.callbacks.XMPPConnectionController;

public class ChatEnabledReceiver extends BroadcastReceiver {

	
	public static final String CHAT_ENABLED_RECEIVER = "com-waid-chat-enabled-receiver";
	private XMPPConnectionController xmppController;
	
	public ChatEnabledReceiver(XMPPConnectionController xmppController) {
		this.xmppController = xmppController;
	}
	
    @Override
    public void onReceive(final Context context, final Intent intent) {
    	
		xmppController.enableChat(true);
    }
}
