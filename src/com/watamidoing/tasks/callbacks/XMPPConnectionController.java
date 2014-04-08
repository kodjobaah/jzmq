package com.watamidoing.tasks.callbacks;

import java.io.Serializable;

import android.os.Messenger;

public interface XMPPConnectionController extends Serializable {
	
	String CONNECTION_OPEN = "xmpp-connection-open";
	String CONNECTION_CLOSE = "`xmpp-connection-close";
	String MESSAGE = "xmpp-message";
	String CHAT_MESSAGE = "xmpp-chat-message";
	String CHAT_PARTICIPANT = "xmpp-chat-participant";
	
	public void xmppConnection(boolean status, Messenger mService);

	public void messageReceived(String chatMessage);

	public void enableChat(boolean b);

	public void newParticipant(String participant);

}
