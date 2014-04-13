package com.watamidoing.tasks;

import java.net.HttpURLConnection;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Dialog;
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

public class GetInvitedTask extends AsyncTask<Void, Void, Boolean> {

	
	private String inviteList = null;
	private WhatAmIdoing mContext;
	private View mInviteStatusView;
	private View mInviteFormView;
	private Dialog dialog;
	private InviteListExpandableAdapter expListAdapter;

	public GetInvitedTask(final WhatAmIdoing context) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		this.mContext = context;
		dialog = new Dialog(mContext, R.style.ThemeWithCorners);
		dialog.setContentView(R.layout.invited);
        dialog.setTitle("Whos Watching?");
        dialog.setCancelable(true);
        mInviteFormView = dialog.findViewById(R.id.invited_form);
 		mInviteStatusView = dialog.findViewById(R.id.invited_status);
		
	    ScreenDimension sm = UtilsWhatAmIdoing.getScreenDimensions(mContext);
	    LayoutParams params = dialog.getWindow().getAttributes();
	    
	    dialog.show();
        showProgress(true);
        //Add event listeners
        Button cancel = (Button) dialog.findViewById(R.id.cancel_invited);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               Log.i("InviteListTask.InviteListTask","Cancel Button was clicked");
               dialog.cancel();
            }
        });
       
	}
	
	@Override
	protected Boolean doInBackground(Void... arg0) {

		String inviteUrl = mContext.getString(R.string.getInviteList_url);
		
	    Authentication auth =  DatabaseHandler.getInstance(mContext).getDefaultAuthentication();
		HttpConnectionHelper connectionHelper = new HttpConnectionHelper();
		try {
			String urlVal = inviteUrl+"?token="+auth.getToken();
			ConnectionResult connectionResult = connectionHelper.connect(urlVal);
		
			Log.i("GetInvitedTask","doInBackground["+connectionResult+"]");
			if ((connectionResult != null) && (connectionResult.getStatusCode() ==  HttpURLConnection.HTTP_OK)) {
				inviteList = connectionResult.getResult();
				Log.i("GetInvitedTask","doInBackground["+inviteList+"]");
			}
		
		} finally {
			connectionHelper.closeConnection();
		}
		return inviteList == null ? false: true;
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
	
			
			
		    Log.i("GetInvitelist.onpostexecute","succes:"+inviteList);
		    showProgress(false);
		} else {
			showProgress(false);
			Log.i("GetInvitelist.onpostexecute","failute:");
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
