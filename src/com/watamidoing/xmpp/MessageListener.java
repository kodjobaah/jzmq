package com.watamidoing.xmpp;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;

class MyMessageListener implements MessageListener {
		 
		@Override
		public void processMessage(Chat chat, Message message) {
            String from = message.getFrom();
            String body = message.getBody();
            System.out.println(String.format("Received message " + body + " from " + from));
			
		}
 }
