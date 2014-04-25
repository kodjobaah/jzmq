package com.watamidoing.transport.service;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.waid.R;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.transport.receivers.NotAbleToConnectReceiver;
import com.watamidoing.transport.receivers.ServiceConnectionCloseReceiver;
import com.watamidoing.transport.receivers.ServiceStartedReceiver;
import com.watamidoing.transport.receivers.ServiceStoppedReceiver;
import com.watamidoing.utils.UtilsWhatAmIdoing;

import de.tavendo.autobahn.WebSocket.ConnectionHandler;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;

public class WebsocketService extends Service {

	NotificationManager nm;

	static private volatile Boolean isRunning = false;

	/** Keeps track of all current registered clients. */
	ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	/** Holds last value set by a client. */
	int mValue = 0;

	/**
	 * Command to service to push message
	 */
	static public final int PUSH_MESSAGE_TO_QUEUE = 1;

	/**
	 * Command to the service to register a client, receiving callbacks from the
	 * service. The Message's replyTo field must be a Messenger of the client
	 * where callbacks should be sent.
	 */
	static final int MSG_REGISTER_CLIENT = 1;

	/**
	 * Command to the service to unregister a client, ot stop receiving
	 * callbacks from the service. The Message's replyTo field must be a
	 * Messenger of the client as previously given with MSG_REGISTER_CLIENT.
	 */
	static final int MSG_UNREGISTER_CLIENT = 2;

	/**
	 * Command to service to set a new value. This can be sent to the service to
	 * supply a new value, and will be sent by the service to any registered
	 * clients with the new value.
	 */
	static final int MSG_SET_VALUE = 3;

	private static final String TAG = "WebSocketService";

	private static WebSocketConnection mConnection = null;

	
	/**
	 * Handler of incoming messages from clients.
	 */
	static class IncomingHandler extends Handler {
		long start_time =0;
		double difference =0;
		static {
			Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

		}

		private WebsocketService websocketService;

		public IncomingHandler(WebsocketService websocketService) {
			this.websocketService = websocketService;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PUSH_MESSAGE_TO_QUEUE:

			    if (start_time == 0)
			    		start_time = System.nanoTime();
			    else {
			    	long end_time = System.nanoTime();
			    	 difference = (end_time - start_time)/1e6;
			    	start_time = end_time;
			   }
				if (mConnection != null) {
					Boolean stopSelf = msg.getData().getBoolean("stopSelf");
					if ((stopSelf != null) && (stopSelf.equals(true))) {
						UtilsWhatAmIdoing.displayGenericToast(websocketService,
								"receive message to stop service["+mConnection.isConnected());
						mConnection.disconnect();
						websocketService.stopSelf();
						
					} else {

						byte[] frame = msg.getData().getByteArray("frame");
						Log.d(TAG,
								"------------------RECEIVED SHOUDL BE BEFORE COMPRESS TRANSMITTING----:"
										+ frame.length+" DIFFERENCE TIME["+difference+"]");
						String res = Base64.encodeToString(frame,
								Base64.DEFAULT);

						if (frame != null) {
							if ((res != null) && (mConnection != null)) {

								if (mConnection.isConnected()) {
									mConnection.sendTextMessage(res);
								} else {
									websocketService.stopSelf();
								}
							}
						}
						Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
					}
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		WebsocketService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return WebsocketService.this;
		}
	}

	public WebsocketService() {
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler(this));

	@Override
	public void onCreate() {
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int res = createSockectContext();

	}

	@Override
	public void onDestroy() {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

		StrictMode.setThreadPolicy(policy); 
		super.onDestroy();
		sendServiceStopNotification();
		if ((mConnection != null) && (mConnection.isConnected())) {
			mConnection.sendTextMessage("SERVICE_STOPPED");	
			//Use to forcible close the connection
			//CloseConnectionTask dlt = new CloseConnectionTask(this);
			//dlt.execute((Void)null);
			

			mConnection.disconnect();
		}
		mConnection = null;
		setRunning(false);
		// Cancel the persistent notification.
		// mNM.cancel(R.string.remote_service_started);

		// Tell the user we stopped.
		// Toast.makeText(this, R.string.remote_service_stopped,
		// Toast.LENGTH_SHORT).show();
	}

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the serv ice.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	@Override
	public int onStartCommand(android.content.Intent intent, int flags,
			int startId) {

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_cable_connect)
				.setContentTitle("What Am I doing").setContentText("sharing");

		Notification notification = mBuilder.build();
		startForeground(1337, notification);
		// //// will do all my stuff here on in the method onStart() or
		// onCreat()?

		setRunning(true);
		return Service.START_NOT_STICKY;
	}

	private void start(final String wsuri) {

		try {

			mConnection = new WebSocketConnection();
			mConnection.connect(wsuri, new ConnectionHandler() {

				@Override
				public void onOpen() {
					Log.i(TAG, "Status: Connected to " + wsuri);
					// mConnection.sendTextMessage("Hello, world!");
				}

				@Override
				public void onTextMessage(String payload) {

					sendServiceStartedNotification();
					Log.i(TAG, "Got echo: " + payload);
				}

				@Override
				public void onClose(int code, String reason) {
					sendServiceConnectionCloseNotification();
					Log.i(TAG, "Connection lost.");
					stopSelf();
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
			Log.i(TAG, e.toString());
		}
	}

	private void setRunning(boolean running) {
		// SharedPreferences pref =
		// PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		// SharedPreferences.Editor editor = pref.edit();

		// editor.putBoolean(PREF_IS_RUNNING, running);
		// editor.apply();
		isRunning = running;
	}

	public static boolean isRunning() {
		return isRunning;
	}

	private int createSockectContext() {

		Authentication auth = DatabaseHandler.getInstance(this)
				.getDefaultAuthentication();
		String pUrl = this.getResources().getString(R.string.publish_url);
		String pubUrl = pUrl + "?token=" + auth.getToken();
		start(pubUrl);
		return 1;

	}

	private void sendNotAbleToConnectNotification() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(NotAbleToConnectReceiver.NOT_ABLE_TO_CONNECT);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		// broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
		sendBroadcast(broadcastIntent);
	}

	private void sendServiceStartedNotification() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ServiceStartedReceiver.SERVICE_STARTED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		// broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
		sendBroadcast(broadcastIntent);
	}

	protected void sendServiceStopNotification() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ServiceStoppedReceiver.SERVICE_STOPED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		// broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
		sendBroadcast(broadcastIntent);

	}

	protected void sendServiceConnectionCloseNotification() {
		Intent broadcastIntent = new Intent();
		broadcastIntent
				.setAction(ServiceConnectionCloseReceiver.SERVICE_CONNECTION_CLOSED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		// broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
		sendBroadcast(broadcastIntent);

	}

}
