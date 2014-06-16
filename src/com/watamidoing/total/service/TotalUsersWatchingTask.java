package com.watamidoing.total.service;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.waid.R;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.view.WhatAmIdoing;

public class TotalUsersWatchingTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "TotalUsersWatchingTask";
	private TotalWatchersService context;
	private String totalWatchingUrl;
	private String token;
	private String watchers;
	private boolean problems = false;

	public TotalUsersWatchingTask(TotalWatchersService context) {

		this.context = context;
		totalWatchingUrl = context.getString(R.string.total_watching_url);
		Authentication auth = DatabaseHandler.getInstance(context)
				.getDefaultAuthentication();
		if (auth != null) { 
			token = auth.getToken();
		} else {
			problems  = true;
		}
		
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		
		if (problems) {
			return false;
		}
			HttpConnectionHelper connectionHelper = new HttpConnectionHelper();
			try {
				String urlVal = totalWatchingUrl + "=" + token;
				ConnectionResult connectionResult = connectionHelper
						.connect(urlVal);
				if ((connectionResult != null)
						&& (connectionResult.getStatusCode() == HttpURLConnection.HTTP_OK)) {
					String result = connectionResult.getResult();
					int myNum = 0;
					String message = context.getString(R.string.total_views_message);
					try {
						myNum = Integer.parseInt(result);
						watchers = message+" " + myNum + " ";

					} catch (NumberFormatException nfe) {
						Log.e(TAG,nfe.getMessage()); // Handle parse error.
						watchers = message+" " + myNum + " ";
					}

				}

			} finally {
				connectionHelper.closeConnection();
			}
			return true;
		

	}

	@Override
	protected void onPostExecute(final Boolean success) {

		
		if (success) {
			context.updateTotalViews(watchers);
		} else {
			context.updateTotalViews(null);
		}
			
	}

}
