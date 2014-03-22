package com.watamidoing.tasks;

import java.net.HttpURLConnection;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.waid.R;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.parser.ParseInviteList;
import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.utils.ScreenDimension;
import com.watamidoing.utils.UtilsWhatAmIdoing;
import com.watamidoing.view.WhatAmIdoing;
import com.watamidoing.view.adapter.InviteListExpandableAdapter;
import com.watamidoing.view.adapter.WhosNotWatchingAdapter;
import com.watamidoing.view.adapter.WhosWatchingAdapter;

public class TwitterPinTask extends AsyncTask<Void, Void, Boolean> {

	
	private static final String TWITTER_CONSUMER_KEY ="zafDiHrQppvYEnzrMlzXQ";
	private static final String TWITTER_CONSUMER_SECRET="hX97yXn9znhVQTBniZDOaS2ECmJA0KL9wvL3tRiMfs";
	
	protected static final String TAG = "TwitterPinTask";
	private String inviteList = null;
	private WhatAmIdoing mContext;
	private View mInviteStatusView;
	private View mInviteFormView;
	private Dialog dialog;
	private InviteListExpandableAdapter expListAdapter;
	private RequestToken requestToken;
	public static final String  TWITTER_CALLBACK_URL = "oob";

	public TwitterPinTask(final WhatAmIdoing context) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		this.mContext= context;
	}
	
	@Override
	protected Boolean doInBackground(Void... arg0) {
     
		return true;

	}

	@Override
	protected void onPostExecute(final Boolean success) {
  
        
		if (success) {
			
			ParseInviteList parseInviteList = new ParseInviteList(inviteList);
			
			//Creating a list of those thqt have accepted
			ListView invitedList = (ListView)dialog.findViewById(R.id.invited_list);
			TextView tv = new TextView(mContext);
	        tv.setText(R.string.invited_accepted);
	        invitedList.addHeaderView(tv);
			WhosWatchingAdapter whosWatchingAdapter = new WhosWatchingAdapter(mContext,parseInviteList.getAccepted());
			invitedList.setAdapter(whosWatchingAdapter);
	
			//Creating list of those that have not accepted
			
			
			ListView notAcceptedList = (ListView)dialog.findViewById(R.id.invited_list_not_accepted);
			TextView not = new TextView(mContext);
			not.setText(R.string.invited_not_accepted);
			notAcceptedList.addHeaderView(not);
			WhosNotWatchingAdapter whosWatchingAdapterNotAccepted = new WhosNotWatchingAdapter(mContext,parseInviteList.getNotAccepted());
			notAcceptedList.setAdapter(whosWatchingAdapterNotAccepted);
			
		    Log.d("TwitterPinTask.onpostexecute","succes:"+inviteList);
		    showProgress(false);
		} else {
			showProgress(false);
			Log.d("TwitterPinTask.onpostexecute","failute:");
		}
	}
	
	
	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = mContext.getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mInviteStatusView.setVisibility(View.VISIBLE);
			mInviteStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mInviteStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mInviteFormView.setVisibility(View.VISIBLE);
			mInviteFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mInviteFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mInviteStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mInviteFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}



	@Override
	protected void onCancelled() {
	}

}
