package com.whatamidoing.view;

import java.io.Serializable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.whatamidoing.tasks.callbacks.WebsocketController;

import de.tavendo.autobahn.WebSocket.ConnectionHandler;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;

public class WebSocketActivity extends Activity implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7224900346759671627L;
	public static final String WEBSOCKET_CONTROLLER = "websocketcontroller";
	public static final String PUBLISH_VIDEO_URL = "publishvideourl";
	public static final String CAMERA_BRIDGE_BASEVIEW = "camerabridgebaseview";
	private WebSocketConnection mConnection;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      
        
        Intent intent = getIntent();
        final String pubUrl = intent.getStringExtra(PUBLISH_VIDEO_URL);
        mConnection = new WebSocketConnection();
		try{
		mConnection.connect(pubUrl, new ConnectionHandler() {

			@Override
			public void onOpen() {
				Log.d("WebSocketActivity.startTransmission", "Status: Connected to " + pubUrl);
				Intent intent = new Intent(getApplicationContext(), WhatAmIdoing.class);
				intent.putExtra(WebsocketController.CONNECTION_OPEN, true);
				startActivity(intent);
				// mConnection.sendTextMessage("Hello, world!");
			}

			@Override
			public void onTextMessage(String payload) {
				Log.d("WebSocketActivity.startTransmission", "Got echo: " + payload);
			}

			@Override
			public void onClose(int code, String reason) {
				Log.d("WebSocketActivity.startTransmission", "Connection lost.");
				Intent intent = new Intent(getApplicationContext(), WhatAmIdoing.class);
				intent.putExtra(WebsocketController.CONNECTION_CLOSE, true);
				startActivity(intent);

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
		Log.d("WhatAmIdoing.startTransmission", e.toString());
		Intent whatAmIdoingIntent = new Intent(getApplicationContext(), WhatAmIdoing.class);
		whatAmIdoingIntent.putExtra(WebsocketController.CONNECTION_CLOSE, true);
		startActivity(whatAmIdoingIntent);

	}

	
        // The activity is being created.
    }
	
	
	protected void onNewIntent(Intent intent) {
		  super.onNewIntent(intent);
		  setIntent(intent);//must store the new intent unless getIntent() will return the old one
	
		  
		  
		 String frame =   intent.getStringExtra(WebsocketController.FRAME);
		
		 if ((frame != null) && (mConnection != null) && (mConnection.isConnected())){
			 mConnection.sendTextMessage(frame);
		 }
	}
	
	
    @Override
    protected void onStart() {
        super.onStart();
        mConnection.reconnect();
        // The activity is about to become visible.
    }
    @Override
    protected void onResume() {
        super.onResume();
        mConnection.reconnect();
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Another activity is taking focus (this activity is about to be "paused").
    }
    @Override
    protected void onStop() {
        super.onStop();
       // mConnection.disconnect();
        // The activity is no longer visible (it is now "stopped")
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
      //  mConnection.disconnect();
        mConnection = null;
        // The activity is about to be destroyed.
    }

}
