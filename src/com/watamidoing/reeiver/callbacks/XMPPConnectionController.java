package com.watamidoing.reeiver.callbacks;

import java.io.Serializable;

import com.watamidoing.chat.xmpp.service.Participant;

import android.os.Messenger;

public interface XMPPConnectionController extends Serializable {
	
	String CONNECTION_OPEN = "xmpp-connection-open";
	String CONNECTION_CLOSE = "`xmpp-connection-close";
	String MESSAGE = "xmpp-message";
	String CHAT_MESSAGE = "xmpp-chat-message";
	String CHAT_PARTICIPANT = "xmpp-chat-participant";
	
	/*
	 * This is invoked in [[com.watamidoing.tasks.callbacks.XMPPServiceConnection]] when trying to bind 
	 * to the service
	 */
	public void xmppConnection(boolean status, Messenger mService);

	/*
	 * This is invoked in [[com.watamidoing.xmpp.receivers.ChatMessageReceived]]
	 */
	public void messageReceived(String chatMessage);

	/*
	 * This is invoked in [[com.watamidoing.xmpp.receivers.ChatEnabledReceiver]]
	 */
	public void enableChat(boolean status);

	/*
	 * This is invoked in [[com.watamidoing.xmpp.receivers.ChatParticipantReceiver]]
	 */
	public void newParticipant(Participant participant);

	/*
	 * This is invoked in [[com.watamidoing.xmpp.receivers.XMPPServiceStoppedReceiver]]
	 */
	public void xmppServiceStopped(boolean status);

}
