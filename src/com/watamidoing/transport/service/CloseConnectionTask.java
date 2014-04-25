package com.watamidoing.transport.service;
import java.net.HttpURLConnection;

import android.os.AsyncTask;
import android.util.Log;

import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.view.WhatAmIdoing;

public class CloseConnectionTask extends AsyncTask<Void, Void, Boolean> {

	

	private WebsocketService websocketService;


	public CloseConnectionTask(WebsocketService websocketService) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		this.websocketService = websocketService;
			}
	
	@Override
	protected Boolean doInBackground(Void... arg0) {
		return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {
  
        
		if (success) {
			Authentication auth =  DatabaseHandler.getInstance(websocketService).getDefaultAuthentication();

			HttpConnectionHelper connectionHelper = new HttpConnectionHelper();
			try {
				String urlVal = "http://www.whatamidoing.info:9000/closeChannel?token="+auth.getToken();
				ConnectionResult connectionResult = connectionHelper.connect(urlVal);

				if ((connectionResult != null) && (connectionResult.getStatusCode() ==  HttpURLConnection.HTTP_OK)) {
					Log.i("DOLATERTASK","****************** RESULTS FROM CLOSING:"+connectionResult.getResult());
				}

			} finally {
				connectionHelper.closeConnection();
			}	

		}
	}
	

	@Override
	protected void onCancelled() {
	}

}
