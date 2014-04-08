package com.watamidoing.xmpp.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.TCPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.packet.MUCUser;

import com.waid.R;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.parser.ParseRoomJid;
import com.watamidoing.tasks.callbacks.XMPPConnectionController;
import com.watamidoing.transport.receivers.ChatEnabledReceiver;
import com.watamidoing.transport.receivers.ChatMessageReceiver;
import com.watamidoing.transport.receivers.NotAbleToConnectReceiver;
import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.utils.UtilsWhatAmIdoing;
import com.watamidoing.view.WhatAmIdoing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

public class XMPPService extends Service implements PacketListener, XmppConnectionNotifiction {
	 

	
	private static final String TAG = "XMPPService";

	private NotificationManager nm;
	
	/** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;
    
    /**
     * Command to service to push message
     */
    static public final int PUSH_MESSAGE_TO_QUEUE = 1;

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    static final int MSG_SET_VALUE = 3;
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger mMessenger;

	private boolean isRunning;

	private SmackAndroid smackAndroid;

	private XMPPConnection connection;

	private XmppConnectionTask xmmpConnectionTask;

	private ParticipantPacketListener participantPacketListener;

	private static MultiUserChat multiUserChat;
	

	@Override
    public void onCreate() {
    	 nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	 start();
    	
    }

	@Override
    public void onDestroy() {
		smackAndroid.onDestroy();
	    // Cancel the persistent notification.
       // mNM.cancel(R.string.remote_service_started);

        // Tell the user we stopped.
       // Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    }
	
	/**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
    	
		private MultiUserChat chat;

		public IncomingHandler(MultiUserChat multiUserChat) {
    		this.chat = multiUserChat;
    	}
    	static {
    		Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

    	}
    	
		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PUSH_MESSAGE_TO_QUEUE:
                	
                		String message = msg.getData().getString("chatMessage");
				try {
					chat.sendMessage(message);
				} catch (NotConnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (XMPPException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
					Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                	/*
                	int result = NativeMethods.pushFrame(frame, frame.length);
                	if (result == -1) {
                		notDone = false;
                	}
                	*/
                    break;
               default:
                    super.handleMessage(msg);
            }
        }
    }

     /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the serv        ice.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    
    @Override
    public int onStartCommand(android.content.Intent intent, int flags, int startId){
    	
    smackAndroid =	SmackAndroid.init(this);
    	NotificationCompat.Builder mBuilder =
    		    new NotificationCompat.Builder(this)
    		    .setSmallIcon(R.drawable.chat)
    		    .setContentTitle("Chat Started")
    		    .setContentText("Chating");
    	
    	Notification notification = mBuilder.build();
    	startForeground(1338, notification);
                                   ////// will do all my stuff here on in the method onStart() or onCreat()?
    	
    	
    	setRunning(true);
    	return Service.START_NOT_STICKY;
    }
    

	private void setRunning(boolean running) {
    	isRunning = running;
		
	}

	
	private void start() {
		xmmpConnectionTask = new XmppConnectionTask(this);
		xmmpConnectionTask.execute((Void)null);
	}
	
	private void startChat() {

		 try {
			
			 StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			 StrictMode.setThreadPolicy(policy);
			 Authentication auth = DatabaseHandler.getInstance(this).getDefaultAuthentication();
				String token = auth.getToken();
				
				String url  = this.getString(R.string.xmmp_roomjid_url)+"="+token;
			    HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper();
				ConnectionResult results = httpConnectionHelper.connect(url);
				if (results == null) {
					Log.i(TAG,"problems logging on");
				} else if (results.getStatusCode() != HttpURLConnection.HTTP_OK) {
					Log.i(TAG,"problems logging on");
				} else {
					 String res = results.getResult();
					 ParseRoomJid parserRoomJid = new ParseRoomJid(res);
					 String roomJid = parserRoomJid.getRoomJid();
					 String nickname = parserRoomJid.getNickName();
					 Log.i(TAG,"ROOMJID="+roomJid);
					 Log.i(TAG,"NICKNAME="+nickname);
				
					if (roomJid.length() >= 1) { 				
						multiUserChat = new MultiUserChat(connection, roomJid);
						multiUserChat.join(nickname);
						multiUserChat.addMessageListener(this);
						participantPacketListener = new ParticipantPacketListener(this);
						multiUserChat.addParticipantListener(participantPacketListener);
						mMessenger= new Messenger(new IncomingHandler(multiUserChat));
						sendChatEnableMessage();
						Log.i(TAG,"--CHAT ENABLED MESSAGE SENT");
			
					} else {
						Log.i(TAG,"--NOT ROOOM JID");
					}
				}

			} catch (SmackException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (XMPPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}

	
	@Override
	public void connected(boolean status, XMPPConnection connection) {
		
		if(status) {
			setRunning(true);
			this.connection = connection;
			startChat();
		} else {
			stopSelf();
		}
		
	}

	public void sendChatEnableMessage() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ChatEnabledReceiver.CHAT_ENABLED_RECEIVER);
	    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
	    	sendBroadcast(broadcastIntent);
	}
	
	@Override
	public void processPacket(Packet packet) throws NotConnectedException {
		
		Log.i(TAG,"PACKET RECEIVED"+packet.toXML());
		if (packet instanceof org.jivesoftware.smack.packet.Message) {
			org.jivesoftware.smack.packet.Message mess = (org.jivesoftware.smack.packet.Message) packet;
	
			Intent broadcastIntent = new Intent();
			String from = mess.getFrom().substring(mess.getFrom().indexOf("/")+1,mess.getFrom().length());
			String m = from+":"+mess.getBody();
			broadcastIntent.putExtra(XMPPConnectionController.CHAT_MESSAGE, m);
		    	broadcastIntent.setAction(ChatMessageReceiver.MESSAGE_RECIEVED);
		    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		    	//broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
		    	sendBroadcast(broadcastIntent);
			
			Log.i(TAG,"PACKET RECEIVE WAS A MESSAGE:"+mess.getBody());
		} else if (packet instanceof org.jivesoftware.smack.packet.Presence) {
			org.jivesoftware.smack.packet.Presence prescene = (org.jivesoftware.smack.packet.Presence) packet;
			MUCUser mucUser = (MUCUser) prescene.getExtension("http://jabber.org/protocol/muc#user");
			Log.i(TAG,"PACKET RECEIVED WAS A PRESCENCE FROM:"+mucUser.getItem().getNick());
		} else {
			Log.i(TAG,"PACKET RECEIVED WAS NOT A MESSAGE:"+packet.toXML());
		}
		
		
	}

}
