package com.watamidoing.chat;

import java.net.HttpURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.utils.UtilsWhatAmIdoing;
import com.watamidoing.view.WhatAmIdoing;
import com.waid.R;

import android.os.AsyncTask;
import android.util.Log;

public class GetGroupIdTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "GetGroupIdTask";
	private String url;
	private WhatAmIdoing context;
	private ConnectionResult results;

	public GetGroupIdTask() {

	}

	public GetGroupIdTask(String url, WhatAmIdoing context) {
		this.url = url;
		this.context = context;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper();
		results = httpConnectionHelper.connect(url);
		if (results == null) {
			return false;
		} else if (results.getStatusCode() != HttpURLConnection.HTTP_OK) {
			return false;
		}

		return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {

		if (success) {

			
			if (context.isVideoSharing() && context.isVideoStart()) {
				try {
					JSONObject json = new JSONObject(results.getResult());
					Log.i(TAG,"----resulst:"+json.toString());
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
				
				
			} else {
				String message = context.getString(R.string.camera_not_started);
				UtilsWhatAmIdoing.displayGenericMessageDialog(context, message);
			}

			Log.d(TAG, "succes:");
		} else {
			UtilsWhatAmIdoing.displayNetworkProblemsForInvitesDialog(context);
			Log.d(TAG, "failure:");

		}
	}

}
