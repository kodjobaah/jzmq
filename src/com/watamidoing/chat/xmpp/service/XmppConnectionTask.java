package com.watamidoing.chat.xmpp.service;

import java.io.IOException;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.TCPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.os.AsyncTask;
import android.util.Log;

import com.waid.R;
import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.view.WhatAmIdoing;

public class XmppConnectionTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "XmppConnectionTask";
	private String url;
	private WhatAmIdoing context;
	private ConnectionResult results;
	private XmppConnectionNotifiction connectionNotification;
	private XMPPConnection connection;
	private XMPPService xmppService;

	public XmppConnectionTask() {

	}

	public XmppConnectionTask(XMPPService xmppService, XmppConnectionNotifiction connectionNotification) {
		this.xmppService = xmppService;
		this.connectionNotification = connectionNotification;
	}

	public XmppConnectionTask(XmppConnectionNotifiction connectionNotification, XMPPService xmppService) {
		this.xmppService = xmppService;
		this.connectionNotification = connectionNotification;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		int maxLimit = 3;
		int count =0;
		 String xmppIp = xmppService.getString(R.string.xmpp_ip);
		 String xmppPort = xmppService.getString(R.string.xmpp_port);
		 String xmppDomain = xmppService.getString(R.string.xmpp_domain);
		 String xmppDefaultTimeout = xmppService.getString(R.string.xmpp_default_timeout);
		 ConnectionConfiguration config = new ConnectionConfiguration(xmppIp,Integer.valueOf(xmppPort),xmppDomain);
		 SmackConfiguration.setDefaultPacketReplyTimeout(Integer.valueOf(xmppDefaultTimeout));
		
		 config.setDebuggerEnabled(true);
		 SASLAuthentication.supportSASLMechanism("PLAIN", 0);
		 config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
		 
		  connection = new TCPConnection(config);
		while(count < maxLimit) {
			
			 try {
				 	connection.connect();			
				 	connection.loginAnonymously();
				 	count = maxLimit;
				 	return true;
			} catch (SmackException e) {
				Log.i(TAG,"FAILED TRYING AGAING:"+count+"] reason for failure["+e.getMessage());
				count = count + 1;
			} catch (IOException e) {
				Log.i(TAG,"FAILED TRYING AGAING:"+count+"] reason for failure["+e.getMessage());
				count = count + 1;
			} catch (XMPPException e) {
				Log.i(TAG,"FAILED TRYING AGAING:"+count+"] reason for failure["+e.getMessage());
				count = count + 1;
			}
		} 
		return false;
	}

	@Override
	protected void onPostExecute(final Boolean success) {

		if (success) {
			connectionNotification.connected(true,connection);
		} else {
			connectionNotification.connected(false,null);
		
		}
	}

}
