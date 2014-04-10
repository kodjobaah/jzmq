package com.watamidoing.transport.receivers;

import com.watamidoing.reeiver.callbacks.WebsocketController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

	
	private WebsocketController websocketController;
	
	public NetworkChangeReceiver(WebsocketController websocketController) {
		this.websocketController = websocketController;
	}
	
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        final android.net.NetworkInfo wifi = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        final android.net.NetworkInfo mobile = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (wifi.isAvailable() || mobile.isAvailable()) {
        	
        	websocketController.networkStatusChange(true);
            // Do something

            Log.d("Netowk Available ", "Flag No 1");
        } else {
        	websocketController.networkStatusChange(false);
        }
    }
}
