package com.watamidoing.tasks;

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
	private WhatAmIdoing context;
	private Object totalWatchingUrl;
	private String token;
	private String watchers;

	public TotalUsersWatchingTask(WhatAmIdoing context) {

		this.context = context;
		totalWatchingUrl = context.getString(R.string.total_watching_url);
		Authentication auth = DatabaseHandler.getInstance(context)
				.getDefaultAuthentication();
		token = auth.getToken();
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (context.isVideoSharing() && context.isVideoStart()) {

			HttpConnectionHelper connectionHelper = new HttpConnectionHelper();
			try {
				String urlVal = totalWatchingUrl + "=" + token;
				ConnectionResult connectionResult = connectionHelper
						.connect(urlVal);
				if ((connectionResult != null)
						&& (connectionResult.getStatusCode() == HttpURLConnection.HTTP_OK)) {
					String result = connectionResult.getResult();
					int myNum = 0;

					try {
						myNum = Integer.parseInt(result);
						String message = context.getString(R.string.total_views_message);
						watchers = message+"(" + myNum + ")";

					} catch (NumberFormatException nfe) {
						nfe.printStackTrace(); // Handle parse error.
					}

				}

			} finally {
				connectionHelper.closeConnection();
			}
			return true;
		}
		return false;

	}

	@Override
	protected void onPostExecute(final Boolean success) {

		 ExecutorService executor = Executors.newFixedThreadPool(1);
		 executor.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(1000);

					if (success) {
						context.updateTotalViews(watchers);
					} else {
						context.updateTotalViews(null);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			}
		});
	
	}

}
