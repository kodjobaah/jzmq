package com.watamidoing.chat.xmpp.service;

import java.util.ArrayList;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smackx.muc.packet.MUCUser;

import com.watamidoing.chat.xmpp.receivers.ChatMessageReceiver;
import com.watamidoing.chat.xmpp.receivers.ChatParticipantReceiver;
import com.watamidoing.reeiver.callbacks.XMPPConnectionController;

import android.content.Intent;
import android.util.Log;

public class ParticipantPacketListener implements PacketListener {

	private static final String TAG = "ParticipantPacketListener";
	private XMPPService service;
	private ArrayList<String> participants;

	public ParticipantPacketListener(XMPPService xmppService) {
		this.service = xmppService;
		this.participants = new ArrayList<String>();
	}

	@Override
	public void processPacket(Packet packet) throws NotConnectedException {
		if (packet instanceof org.jivesoftware.smack.packet.Presence) {
			org.jivesoftware.smack.packet.Presence prescence = (org.jivesoftware.smack.packet.Presence) packet;
			MUCUser mucUser = (MUCUser) prescence
					.getExtension("http://jabber.org/protocol/muc#user");

			Intent broadcastIntent = new Intent();

			String nick = mucUser.getItem().getNick();
			Participant participant = new Participant(prescence.getType()
					.toString(), nick);
			broadcastIntent.putExtra(XMPPConnectionController.CHAT_PARTICIPANT,
					participant);
			broadcastIntent
					.setAction(ChatParticipantReceiver.CHAT_PARTICIPANT_RECEIVER);
			broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
			// broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
			service.sendBroadcast(broadcastIntent);
			Log.i(TAG, "PACKET RECEIVED WAS A PRESCENCE FROM:" + nick);
		}

	}

}
