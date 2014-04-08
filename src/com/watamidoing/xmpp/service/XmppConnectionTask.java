package com.watamidoing.xmpp.service;

import java.io.IOException;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.TCPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;

import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.view.WhatAmIdoing;

public class XmppConnectionTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "XmppConnectionTask";
	private String url;
	private WhatAmIdoing context;
	private ConnectionResult results;
	private XmppConnectionNotifiction connectionNotification;
	private XMPPConnection connection;

	public XmppConnectionTask() {

	}

	public XmppConnectionTask(String url, XmppConnectionNotifiction connectionNotification) {
		this.url = url;
	}

	public XmppConnectionTask(XmppConnectionNotifiction connectionNotification) {
		this.connectionNotification = connectionNotification;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		int maxLimit = 3;
		int count =0;
		 Log.i(TAG,"TRYIG TO MAKE CONNECTION TO XMPP");
		 ConnectionConfiguration config = new ConnectionConfiguration("192.168.1.2",5222,"my");
		 SmackConfiguration.setDefaultPacketReplyTimeout(1000);
		  
		 Log.i(TAG,"CONFIG STRING:"+config.toString());
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
				// TODO Auto-generated catch block
				Log.i(TAG,"FAILED TRYING AGAING:"+count+"] reason for failure["+e.getMessage());
				count = count + 1;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.i(TAG,"FAILED TRYING AGAING:"+count+"] reason for failure["+e.getMessage());
				count = count + 1;
			} catch (XMPPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			Log.i(TAG, "--XMPP CONNECTION MADE");
		} else {
			connectionNotification.connected(false,null);
			Log.i(TAG, "-XMPP CONNECTION FAILURE:");

		}
	}

}
