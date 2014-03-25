package com.watamidoing.invite.facebook;

import java.net.HttpURLConnection;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;
import com.watamidoing.invite.twitter.TwitterAuthorization;
import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.utils.UtilsWhatAmIdoing;
import com.watamidoing.view.WhatAmIdoing;
import com.waid.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class FacebookPostTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "FacebookPostTask";
	private String url;
	private WhatAmIdoing context;
	private ConnectionResult results;
	private Session facebookSession;
	private WebDialog requestsDialog;
	private FacebookPostTask facebookPostTask;

	public FacebookPostTask(String url, WhatAmIdoing context,
			Session facebookSession) {
		this.url = url;
		this.context = context;
		this.facebookSession = facebookSession;
		this.facebookPostTask = this;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		Log.i(TAG, "------------------------- trying to connect[" + url + "] ");
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
				String url = context.getString(R.string.invite_location_url)
						+ "=" + results.getResult();
				Bundle params = new Bundle();
				params.putString("description", context
						.getString(R.string.facebook_feed_dialog_description));
				params.putString("link", url);
				params.putString("name",
						context.getString(R.string.facebook_feed_dialog_name));
				params.putString("picture", context
						.getString(R.string.facebook_feed_dialog_picture));
				params.putString("display", context
						.getString(R.string.facebook_feed_dialong_display));

				requestsDialog = (new WebDialog.FeedDialogBuilder(context,
						facebookSession, params)).setOnCompleteListener(
						new OnCompleteListener() {

							@Override
							public void onComplete(Bundle values,
									FacebookException error) {

								if (error != null) {
									if (error instanceof FacebookOperationCanceledException) {
										Log.i(TAG,
												"request cancelled["
														+ error.getMessage()
														+ "]");
									} else {
										UtilsWhatAmIdoing
												.displayNetworkProblemsForInvitesDialog(context);
										Log.i(TAG,
												"network error["
														+ error.getMessage()
														+ "]");

									}
								} else if ((values != null)
										&& (values.getString("post_id") != null)) {
									Log.i(TAG, "Request Sent");
									UtilsWhatAmIdoing
											.displaySuccessInvitesFacebbokDialog(context);
									CloseFacebookFeedDialogTask cffdt = new CloseFacebookFeedDialogTask(
											facebookPostTask);
									cffdt.execute((Void) null);
								}
							}

						}).build();
				requestsDialog.show();
			} else {
				String message = context.getString(R.string.camera_not_started);
				UtilsWhatAmIdoing.displayGenericMessageDialog(context, message);
			}
		} else {
			UtilsWhatAmIdoing.displayNetworkProblemsForInvitesDialog(context);
			Log.d(TAG + ".onpostexecute", "failure:");

		}
	}

	public void cancelDialog() {
		requestsDialog.dismiss();
	}

}
