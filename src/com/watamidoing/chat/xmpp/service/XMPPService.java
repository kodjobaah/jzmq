package com.watamidoing.chat.xmpp.service;

import java.net.HttpURLConnection;
import java.util.Iterator;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.muc.Affiliate; 

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.waid.R;
import com.watamidoing.chat.xmpp.receivers.ChatEnabledReceiver;
import com.watamidoing.chat.xmpp.receivers.ChatMessageReceiver;
import com.watamidoing.chat.xmpp.receivers.ChatParticipantReceiver;
import com.watamidoing.chat.xmpp.receivers.XMPPServiceStoppedReceiver;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.parser.ParseRoomJid;
import com.watamidoing.reeiver.callbacks.XMPPConnectionController;
import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;

public class XMPPService extends Service implements PacketListener, XmppConnectionNotifiction {
	 

	
	private static final String TAG = "XMPPService";

	private NotificationManager nm;
	

	
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

    private SmackAndroid smackAndroid;

	private XMPPConnection connection;

	private XmppConnectionTask xmmpConnectionTask;

	private ParticipantPacketListener participantPacketListener;

	private ParseRoomJid parserRoomJid;

	private AffiliateListener affiliateInterceptor;

	private AffiliateListener affiliateListener;

	private static MultiUserChat multiUserChat;
	

	@Override
    public void onCreate() {
    	 nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	 start();
    	
    }

	@Override
    public void onDestroy() {

		try {
			if (multiUserChat != null) {
				multiUserChat.leave();
			}
			
			if ((connection != null) && (connection.isConnected())){
				connection.disconnect();
			}
			
		} catch (NotConnectedException e) {
			e.printStackTrace();
		}
		Log.i(TAG,"onDestroy -- should be sending service stopped message");
		sendServiceStoppedMessage();
		smackAndroid.onDestroy();	
		
    }
	
	/**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
    	
		private MultiUserChat chat;
		private XMPPService service;

		public IncomingHandler(MultiUserChat multiUserChat, XMPPService xmppService) {
    		this.chat = multiUserChat;
    		this.service = xmppService;
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
					service.stopSelf();
				} catch (XMPPException e) {
					service.stopSelf();
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
    	
    	
    	return Service.START_NOT_STICKY;
    }
    
	
	private void start() {
		xmmpConnectionTask = new XmppConnectionTask(this,(XmppConnectionNotifiction)this);
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
					stopSelf();
				} else if (results.getStatusCode() != HttpURLConnection.HTTP_OK) {
					Log.i(TAG,"problems logging on");
					stopSelf();
				} else {
					 String res = results.getResult();
					 parserRoomJid = new ParseRoomJid(res);
					 String roomJid = parserRoomJid.getRoomJid();
					 String nickname = parserRoomJid.getNickName();
					 Log.i(TAG,"ROOMJID="+roomJid);
					 Log.i(TAG,"NICKNAME="+nickname);
				
					if (roomJid.length() >= 1) { 				
						multiUserChat = new MultiUserChat(connection, roomJid);
					    DiscussionHistory history = new DiscussionHistory();
					    history.setMaxStanzas(5);
						multiUserChat.join(nickname,"holder",history,SmackConfiguration.getDefaultPacketReplyTimeout());
						multiUserChat.addMessageListener(this);
						multiUserChat.pollMessage();
						affiliateListener = new AffiliateListener(this);
						AffiliatePacketFilter apf = new AffiliatePacketFilter();
						connection.addPacketListener(affiliateListener, apf);
						
						Iterator<String> occupants = multiUserChat.getOccupants();
						while(occupants.hasNext()) {
							
							String nick = StringUtils.parseResource(occupants.next());
							Participant participant = new Participant(Type.available.toString(), nick);
							Intent broadcastIntent = new Intent();
							broadcastIntent.putExtra(XMPPConnectionController.CHAT_PARTICIPANT,
									participant);
							broadcastIntent
									.setAction(ChatParticipantReceiver.CHAT_PARTICIPANT_RECEIVER);
							broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
							this.sendBroadcast(broadcastIntent);
						}
						//participantPacketListener = new ParticipantPacketListener(this);
						//multiUserChat.addParticipantListener(participantPacketListener);
						mMessenger= new Messenger(new IncomingHandler(multiUserChat,this));
						sendChatEnableMessage();
											
					} else {
						connection.disconnect();
						stopSelf();
						
					}
				}

			} catch (SmackException e) {
				e.printStackTrace();
				connection.disconnect();
				stopSelf();
			} catch (XMPPException e) {
				e.printStackTrace();
				stopSelf();
			}
		
	}

	
	@Override
	public void connected(boolean status, XMPPConnection connection) {
		
		if(status) {
			this.connection = connection;
			startChat();
		} else {
			
			if  (multiUserChat != null) {
				try {
					multiUserChat.leave();
				} catch (NotConnectedException e) {
					e.printStackTrace();
				}
			}
			Log.i(TAG,"unable to connect should be stopping the service");
			stopSelf();
		}
		
	}

	public void sendChatEnableMessage() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ChatEnabledReceiver.CHAT_ENABLED_RECEIVER);
	    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
	    	sendBroadcast(broadcastIntent);
	}
	
	public void sendServiceStoppedMessage() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(XMPPServiceStoppedReceiver.XMPP_SERVICE_STOPPED_RECIEVED);
	    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
	    	sendBroadcast(broadcastIntent);
	}
	

	
	@Override
	public void processPacket(Packet packet) throws NotConnectedException {
		
		if (packet instanceof org.jivesoftware.smack.packet.Message) {
			org.jivesoftware.smack.packet.Message mess = (org.jivesoftware.smack.packet.Message) packet;
	
			Intent broadcastIntent = new Intent();			
			String from = mess.getFrom().substring(mess.getFrom().indexOf("/")+1,mess.getFrom().length());
			
			String xmppParticipantDelimeter = this.getString(R.string.xmpp_participant_nick_delimeter);
			
			if (from.equalsIgnoreCase(parserRoomJid.getNickName())) {
				from = "me";
			} else {
				Integer n = from.lastIndexOf(xmppParticipantDelimeter);
				if (n > 0) {
					from = from.substring(n+13,from.length());
				}
			}
			
			String m = "<bold><font color=\"blue\">"+from+"</font></bold> : "+mess.getBody();
			broadcastIntent.putExtra(XMPPConnectionController.CHAT_MESSAGE, m);
		    	broadcastIntent.setAction(ChatMessageReceiver.MESSAGE_RECIEVED);
		    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		    	sendOrderedBroadcast(broadcastIntent, null);
		} else if (packet instanceof org.jivesoftware.smack.packet.Presence) {
			org.jivesoftware.smack.packet.Presence prescene = (org.jivesoftware.smack.packet.Presence) packet;
			MUCUser mucUser = (MUCUser) prescene.getExtension("http://jabber.org/protocol/muc#user");
			Log.i(TAG,"PACKET RECEIVED WAS A PRESCENCE FROM:"+mucUser.getItem().getNick());
		} else {
			Log.i(TAG,"PACKET RECEIVED WAS NOT A MESSAGE:"+packet.toXML());
		}
		
		
	}

}
