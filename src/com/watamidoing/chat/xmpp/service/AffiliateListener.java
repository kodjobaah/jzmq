package com.watamidoing.chat.xmpp.service;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smackx.muc.packet.MUCUser;

import android.content.Intent;
import android.util.Log;

import com.watamidoing.chat.xmpp.receivers.ChatParticipantReceiver;
import com.watamidoing.reeiver.callbacks.XMPPConnectionController;

public class AffiliateListener implements PacketListener {

	private static final String TAG = "AffiliateListner";
	private XMPPService xmppService;

	public AffiliateListener(XMPPService xmppService) {
		this.xmppService = xmppService;
	}

	@Override
	public void processPacket(Packet packet) throws NotConnectedException {
		
		MUCUser user = (MUCUser)packet.getExtension("http://jabber.org/protocol/muc#user");
		
		if (user != null) {
		String nick = user.getItem().getNick();
		Log.i(TAG,"NICK NAME:"+nick);
		Intent broadcastIntent = new Intent();
		
		Presence prescence = (Presence)packet;
		String avail = prescence.getType() != null ? prescence.getType().toString(): Type.available.toString();
		Participant participant = new Participant(avail, nick);
		broadcastIntent.putExtra(XMPPConnectionController.CHAT_PARTICIPANT,
				participant);
		broadcastIntent
				.setAction(ChatParticipantReceiver.CHAT_PARTICIPANT_RECEIVER);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		xmppService.sendBroadcast(broadcastIntent);
		Log.i(TAG,"BROADCAST SENT");
		} else {
			Log.i(TAG,"NOT BROADCAST:"+packet.toString());
		}
		
		
	}

}
