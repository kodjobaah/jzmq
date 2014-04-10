package com.watamidoing.chat.xmpp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.watamidoing.reeiver.callbacks.XMPPConnectionController;

public class XMPPServiceStoppedReceiver extends BroadcastReceiver {

	
	public static final String XMPP_SERVICE_STOPPED_RECIEVED = "com-waid-xmpp-service-stopped-received";
	private XMPPConnectionController xmppController;
	
	public XMPPServiceStoppedReceiver(XMPPConnectionController xmppController) {
		this.xmppController = xmppController;
	}
	
    @Override
    public void onReceive(final Context context, final Intent intent) {
    			xmppController.xmppServiceStopped(true);
    }
}
