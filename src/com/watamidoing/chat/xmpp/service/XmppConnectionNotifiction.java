package com.watamidoing.chat.xmpp.service;

import org.jivesoftware.smack.XMPPConnection;

public interface XmppConnectionNotifiction {

	public void connected(boolean status, XMPPConnection connection);
}
