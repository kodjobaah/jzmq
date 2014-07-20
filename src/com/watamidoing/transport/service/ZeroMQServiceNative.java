package com.watamidoing.transport.service;

import java.util.ArrayList;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.waid.R;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.nativecamera.Native;
import com.watamidoing.nativecamera.NativeCommunicator;
import com.watamidoing.reeiver.callbacks.MessagesSentController;
import com.watamidoing.reeiver.callbacks.TotalWatchersController;
import com.watamidoing.total.receivers.MessagesSentReceiver;
import com.watamidoing.total.receivers.TotalWatchersReceiver;
import com.watamidoing.transport.receivers.ZeroMQServiceStartedReceiver;
import com.watamidoing.transport.receivers.ZeroMQServiceStoppedReceiver;

public class ZeroMQServiceNative extends Service {

	private static final int NOTIFICATION_ID = 1337;
	NotificationManager nm;
	private static final String TAG = ZeroMQServiceNative.class.getName();
	static private volatile Boolean isRunning = false;

	int mValue = 0;
	@Override
	public void onCreate() {
		Log.d(TAG, "-------------- CREATE---------");
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG,"DESTROYING:");
	}

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the serv ice.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "----------bingind---------");
		return null;
	}

	@Override
	public int onStartCommand(android.content.Intent intent, int flags,
			int startId) {

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this)
				.setContentTitle("What Am I doing").setContentText("connecting...");

		Notification notification = mBuilder.build();
		startForeground(NOTIFICATION_ID, notification);


		String pUrl = this.getResources().getString(R.string.publish_url);
		
		Log.d(TAG,"pUrl="+pUrl+":---:authToken="+getAuthenticatonToken());
		
		NativeCommunicator nativeCommunicator = new NativeCommunicator(this);
		Native.loadlibs();
		Native.storeMessenger(nativeCommunicator);
		Native.loadlibs();
		Native.startZeromq(pUrl, getAuthenticatonToken());
		Log.d(TAG,"should have started zerom service");
		return Service.START_NOT_STICKY;
	}

	private String getAuthenticatonToken() {
		Authentication auth = DatabaseHandler.getInstance(this)
				.getDefaultAuthentication();
		return auth.getToken();
	}
	
	public void sendServiceStopNotification() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ZeroMQServiceStoppedReceiver.SERVICE_STOPED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		sendBroadcast(broadcastIntent);
		Log.d(TAG,"---SHOULD HAVE BROADCAST SERVICE STOPPED");
	}
	
	
	public void sendServiceStartNotification() {
		
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_cable_connect)
				.setContentTitle("What Am I doing").setContentText("sharing");
		
		nm.notify(NOTIFICATION_ID, mBuilder.build());
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ZeroMQServiceStartedReceiver.SERVICE_STARTED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		sendBroadcast(broadcastIntent);
		Log.d(TAG,"---SHOULD HAVE BROADCAST SERVICE STARTED");
	}

	public void updateMessagesSent(long messageCount) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.putExtra(MessagesSentController.MESSAGE, Long.valueOf(messageCount).toString());
		broadcastIntent.setAction(MessagesSentReceiver.MESSAGES_SENT_RECEIVER);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		sendBroadcast(broadcastIntent);
		
	}

}
