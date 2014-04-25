package com.watamidoing.total.service;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.watamidoing.reeiver.callbacks.TotalWatchersController;
import com.watamidoing.total.receivers.TotalWatchersReceiver;
import com.watamidoing.transport.service.WebsocketService;


public class TotalWatchersService extends Service {
	 

	
	private static final String TAG = "TotalWatchersService";

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

	private TotalUsersWatchingTask totalUsersWatchingTask;

	private IncomingHandler incomingHandler;


	@Override
    public void onCreate() {
    	 nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	 start();
    	
    }

	@Override
    public void onDestroy() {
		super.onDestroy();
		setRunning(false);
    }
	
	/**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {

		private TotalWatchersService totalWatchersService;

		public IncomingHandler() {
    		}
       	
		public IncomingHandler(TotalWatchersService totalWatchersService) {
			this.totalWatchersService = totalWatchersService;
		}

		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PUSH_MESSAGE_TO_QUEUE:
                	
                		String message = msg.getData().getString("totalViewers");
                		sendTotalWatchers(message);
				try {
					Thread.sleep(3000);
					if (isServiceRunning(WebsocketService.class.getName())) {
						getTotalUsersWatching();
					} else {
						totalWatchersService.stopSelf();
					}
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                		
                	
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
    	/*
    	NotificationCompat.Builder mBuilder =
    		    new NotificationCompat.Builder(this)
    		    .setSmallIcon(R.drawable.chat)
    		    .setContentTitle("Chat Started")
    		    .setContentText("Chating");
    	
    	Notification notification = mBuilder.build();
    	startForeground(1338, notification);
      */
                                   ////// will do all my stuff here on in the method onStart() or onCreat()?
    	
    	
    	setRunning(true);
    	start();
    	return Service.START_NOT_STICKY;
    }
    
    public boolean isServiceRunning(String serviceClassName) {
		final ActivityManager activityManager = (ActivityManager) this
				.getSystemService(Context.ACTIVITY_SERVICE);
		final List<android.app.ActivityManager.RunningServiceInfo> services = activityManager
				.getRunningServices(Integer.MAX_VALUE);

		for (android.app.ActivityManager.RunningServiceInfo runningServiceInfo : services) {
			if (runningServiceInfo.service.getClassName().equals(
					serviceClassName)) {
				return true;
			}
		}
		return false;
	}
    
    private void start() {
    		Log.i(TAG,"------------ STARTED TOTAL WATCHERS SERVICE");
    	    incomingHandler = new IncomingHandler(this);
    		mMessenger= new Messenger(incomingHandler);
    		totalUsersWatchingTask = new TotalUsersWatchingTask(this);
		totalUsersWatchingTask.execute((Void)null);
    }
    
    private void getTotalUsersWatching() {
    		if (isRunning) {
    		totalUsersWatchingTask = new TotalUsersWatchingTask(this);
		totalUsersWatchingTask.execute((Void)null);
    		}
    	}

	private void setRunning(boolean running) {
    	isRunning = running;
		
	}

	public void updateTotalViews(String totalWatchers) {
		Message message = Message.obtain(incomingHandler, PUSH_MESSAGE_TO_QUEUE);
		
		Bundle b = new Bundle();
		b.putString("totalViewers", totalWatchers);
		message.setData(b);
		try {
			if (mMessenger != null)
				mMessenger.send(message);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void sendTotalWatchers(String totalWatchers) {
			Intent broadcastIntent = new Intent();
			broadcastIntent.putExtra(TotalWatchersController.MESSAGE, totalWatchers);
			broadcastIntent.setAction(TotalWatchersReceiver.TOTAL_WATCHERS_RECEIVER);
		    	broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		    	sendBroadcast(broadcastIntent);
	}
	
}
