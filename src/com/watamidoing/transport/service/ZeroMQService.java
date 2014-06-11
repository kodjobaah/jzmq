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
import com.watamidoing.tasks.ZeroMQTask;
import com.watamidoing.transport.receivers.ZeroMQNotAbleToConnectReceiver;
import com.watamidoing.transport.receivers.ZeroMQServiceConnectionCloseReceiver;
import com.watamidoing.transport.receivers.ZeroMQServiceStartedReceiver;
import com.watamidoing.transport.receivers.ZeroMQServiceStoppedReceiver;
import com.watamidoing.utils.UtilsWhatAmIdoing;

import de.tavendo.autobahn.WebSocket.ConnectionHandler;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;

import org.zeromq.ZMQ;

public class ZeroMQService extends Service {

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

	private static final String TAG = "ZeroMQService";

	/**
	 * Handler of incoming messages from clients.
	 */
	static class IncomingHandler extends Handler {

		long start_time = 0;
		double difference = 0;
		boolean connected = false;
		static {
			Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

		}

		private ZeroMQTask task;
		private ZeroMQService zeroMQService;

		public IncomingHandler(ZeroMQService zeroMQService) {
			this.zeroMQService = zeroMQService;

		}

		public void connectToZeroMQ(String pUrl) {
			task = new ZeroMQTask(pUrl,
					zeroMQService.getAuthenticatonToken());
			task.execute((Void) null);
			connected = true;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PUSH_MESSAGE_TO_QUEUE:
				if (connected) {
					System.gc();
					String frame = msg.getData().getString("frame");
					int diff = msg.getData().getInt("timeStamp");
					task.sendMessge(frame, String.valueOf(diff));
					frame = null;
					msg = null;
					System.gc();
					Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	IncomingHandler handler;
	Messenger mMessenger;

	@Override
	public void onCreate() {
		Log.d(TAG, "-------------- CREATE---------");
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String pUrl = this.getResources().getString(R.string.publish_url);
		handler = new IncomingHandler(this);
		mMessenger = new Messenger(handler);
		handler.connectToZeroMQ(pUrl);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		setRunning(false);
		if (handler != null) {
			handler.task.disconnect();
		}
	}

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the serv ice.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "----------bingind---------");
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

	private String getAuthenticatonToken() {
		Authentication auth = DatabaseHandler.getInstance(this)
				.getDefaultAuthentication();
		return auth.getToken();
	}

	private void sendNotAbleToConnectNotification() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ZeroMQNotAbleToConnectReceiver.NOT_ABLE_TO_CONNECT);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		sendBroadcast(broadcastIntent);
	}

	private void sendServiceStartedNotification() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ZeroMQServiceStartedReceiver.SERVICE_STARTED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		sendBroadcast(broadcastIntent);
	}

	protected void sendServiceStopNotification() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ZeroMQServiceStoppedReceiver.SERVICE_STOPED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		sendBroadcast(broadcastIntent);

	}

	protected void sendServiceConnectionCloseNotification() {
		Intent broadcastIntent = new Intent();
		broadcastIntent
				.setAction(ZeroMQServiceConnectionCloseReceiver.SERVICE_CONNECTION_CLOSED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		sendBroadcast(broadcastIntent);

	}

}
