package com.watamidoing.transport.service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.watamidoing.R;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.transport.receivers.NotAbleToConnectReceiver;
import com.watamidoing.transport.receivers.ServiceConnectionCloseReceiver;
import com.watamidoing.transport.receivers.ServiceStartedReceiver;
import com.watamidoing.transport.receivers.ServiceStoppedReceiver;

import de.tavendo.autobahn.WebSocket.ConnectionHandler;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;

public class WebsocketService  extends Service {
	
	 NotificationManager nm;

	private Thread websocketPoller;
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

	private static final String TAG = null;

    private static  WebSocketConnection mConnection = null;


    private volatile boolean notDone = true;;

    /**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
    	
    	static {
    		Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

    	}
    	
		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PUSH_MESSAGE_TO_QUEUE:
                	
                	if (mConnection != null) {
                		byte[] frame = msg.getData().getByteArray("frame");
                		String res = Base64.encodeToString(frame, Base64.DEFAULT);
                		mConnection.sendTextMessage(res);
                		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                	}
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
    
    public WebsocketService(){}

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
    	
    	 nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	int res = createSockectContext();
    
    }

	@Override
    public void onDestroy() {
		sendServiceStopNotification();
		mConnection.disconnect();
		mConnection = null;
        // Cancel the persistent notification.
       // mNM.cancel(R.string.remote_service_started);

        // Tell the user we stopped.
       // Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
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
    	
    	
    	NotificationCompat.Builder mBuilder =
    		    new NotificationCompat.Builder(this)
    		    .setSmallIcon(R.drawable.custom_btn_arsenic)
    		    .setContentTitle("What Am I doing")
    		    .setContentText("sharing");
    	
    	Notification notification = mBuilder.build();
    	startForeground(1337, notification);
                                   ////// will do all my stuff here on in the method onStart() or onCreat()?
    	return Service.START_NOT_STICKY;
    }

    private void start(final String wsuri) {
     
       try {
    	 
    	  mConnection = new WebSocketConnection();
          mConnection.connect(wsuri, new ConnectionHandler() {
     
             @Override
             public void onOpen() {
            	 sendServiceStartedNotification();
            	 
                Log.d(TAG, "Status: Connected to " + wsuri);
               // mConnection.sendTextMessage("Hello, world!");
             }
     
             @Override
             public void onTextMessage(String payload) {
                Log.d(TAG, "Got echo: " + payload);
             }
     
             @Override
             public void onClose(int code, String reason) {
            	 sendServiceConnectionCloseNotification();
                Log.d(TAG, "Connection lost.");
             }

			@Override
			public void onBinaryMessage(byte[] arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onRawTextMessage(byte[] arg0) {
				// TODO Auto-generated method stub
				
			}
          });
       } catch (WebSocketException e) {
    	   sendNotAbleToConnectNotification();
    	   stopSelf();
          Log.d(TAG, e.toString());
       }
    }
    

	private int createSockectContext() {
		
		 Authentication auth =  DatabaseHandler.getInstance(this).getDefaultAuthentication();
	     String pUrl = this.getResources().getString(R.string.publish_url);
	     String pubUrl = pUrl+"?token="+auth.getToken();
	     start(pubUrl);
	     return 1;
		
	}
	
	
	
	private void sendNotAbleToConnectNotification() {
    	Intent broadcastIntent = new Intent();
    	broadcastIntent.setAction(NotAbleToConnectReceiver.NOT_ABLE_TO_CONNECT);
    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    	//broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
    	sendBroadcast(broadcastIntent);
	}

	private void sendServiceStartedNotification() {
		Intent broadcastIntent = new Intent();
    	broadcastIntent.setAction(ServiceStartedReceiver.SERVICE_STARTED);
    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    	//broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
    	sendBroadcast(broadcastIntent);	
	}

	protected void sendServiceStopNotification() {
		Intent broadcastIntent = new Intent();
    	broadcastIntent.setAction(ServiceStoppedReceiver.SERVICE_STOPED);
    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    	//broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
    	sendBroadcast(broadcastIntent);
		
	}
	
    
    protected void sendServiceConnectionCloseNotification() {
    	Intent broadcastIntent = new Intent();
    	broadcastIntent.setAction(ServiceConnectionCloseReceiver.SERVICE_CONNECTION_CLOSED);
    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    	//broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
    	sendBroadcast(broadcastIntent);
		
	}

	
}