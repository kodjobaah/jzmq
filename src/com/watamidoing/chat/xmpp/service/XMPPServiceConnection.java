package com.watamidoing.chat.xmpp.service;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

import com.waid.R;
import com.watamidoing.reeiver.callbacks.XMPPConnectionController;
import com.watamidoing.utils.UtilsWhatAmIdoing;

public class XMPPServiceConnection implements ServiceConnection{

	private static final String TAG = "XMPPServiceConnection";
	private Activity activity;
	private XMPPConnectionController xmppServiceConnection;


	public XMPPServiceConnection(Activity activity, XMPPConnectionController xmppServiceConnection) {
		this.activity = activity;
		this.xmppServiceConnection = xmppServiceConnection;
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
		xmppServiceConnection.xmppConnection(true,mService);
	}

	public void onServiceDisconnected(ComponentName className) {
		// This is called when the connection with the service has been
		// unexpectedly disconnected -- that is, its process crashed.
		xmppServiceConnection.xmppConnection(false, null);
	}

}
