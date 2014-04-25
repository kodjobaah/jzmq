package com.watamidoing.transport.service;

import com.watamidoing.reeiver.callbacks.WebsocketController;
import com.watamidoing.utils.UtilsWhatAmIdoing;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

public class WebsocketServiceConnection implements ServiceConnection{

	private Activity activity;
	private WebsocketController websocketController;

	public WebsocketServiceConnection(Activity activity, WebsocketController websocketController) {
		this.activity = activity;
		this.websocketController = websocketController;
	}
	@Override
	public void onServiceConnected(ComponentName className,
			IBinder service) {
		// This is called when the connection with the service has been
		// established, giving us the service object we can use to
		// interact with the service.  We are communicating with our
		// service through an IDL interface, so get a client-side
		// representation of that from the raw service object.
		Messenger mService = new Messenger(service);
		
		websocketController.setMessengerService(mService);
		Log.i("ServiceConnection.onServiceConnection","able to bind to servces");



	}

	public void onServiceDisconnected(ComponentName className) {
		// This is called when the connection with the service has been
		// unexpectedly disconnected -- that is, its process crashed.
		Log.i("ServiceConnection.onServiceDisconnected","disconnected from service");
		websocketController.setMessengerService(null);
	}

}
