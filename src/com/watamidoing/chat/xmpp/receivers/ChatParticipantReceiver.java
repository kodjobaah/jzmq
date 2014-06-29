package com.watamidoing.chat.xmpp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.watamidoing.chat.xmpp.service.Participant;
import com.watamidoing.reeiver.callbacks.XMPPConnectionController;

public class ChatParticipantReceiver extends BroadcastReceiver {

	
	public static final String CHAT_PARTICIPANT_RECEIVER = "com-waid-chat-participant-receiver";
	private XMPPConnectionController xmppController;
	
	public ChatParticipantReceiver(XMPPConnectionController xmppController) {
		this.xmppController = xmppController;
	}
	
    @Override
    public void onReceive(final Context context, final Intent intent) {
    		Participant participant = (Participant)intent.getParcelableExtra(XMPPConnectionController.CHAT_PARTICIPANT);
		xmppController.newParticipant(participant);
    }
}
