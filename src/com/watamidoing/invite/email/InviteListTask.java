package com.watamidoing.invite.email;

import java.net.HttpURLConnection;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.waid.R;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.invite.email.callback.InviteDialogInteraction;
import com.watamidoing.invite.email.task.ContactsListTask;
import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.view.WhatAmIdoing;

public class InviteListTask extends AsyncTask<Void, Void, Boolean> implements InviteDialogInteraction {


	private String inviteList = null;
	private WhatAmIdoing mContext;
	private View mInviteStatusView;
	private TextView mInviteStatusMessageView;
	private View mInviteFormView;
	
	private InviteEmailFragment inviteEmailFragment;

	public InviteListTask(final WhatAmIdoing context) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		this.mContext = context;
		
		
		FragmentTransaction ft = mContext.getSupportFragmentManager().beginTransaction();
	    Fragment prev =  mContext.getSupportFragmentManager().findFragmentByTag("whoHasAcceptedFragment");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);

	  	inviteEmailFragment = InviteEmailFragment.newInstance("Whos Watching?",context,this);
		inviteEmailFragment.show(ft, "whoHasAcceptedFragment");
		ContactsListTask contactsListTask = new ContactsListTask(context,inviteEmailFragment);
		contactsListTask.execute((Void)null);
		
	}
	
	@Override
	public void showInviteProgress(boolean state) {
		showProgress(state);
	}
	
	@Override
	public void setInviteForm(View view) {
		mInviteFormView = view;;
	}

	@Override
	public void setStatusView(View view) {
		mInviteStatusView = view;
		
	}
	
	@Override
	public void setInviteStatusMessage(TextView view) {
		mInviteStatusMessageView = view;
	}
	
	
	@Override
	protected Boolean doInBackground(Void... arg0) {

		String inviteUrl = mContext.getString(R.string.findallinvites_url);


		Authentication auth =  DatabaseHandler.getInstance(mContext).getDefaultAuthentication();

		HttpConnectionHelper connectionHelper = new HttpConnectionHelper();
		try {
			String urlVal = inviteUrl+"?token="+auth.getToken();
			ConnectionResult connectionResult = connectionHelper.connect(urlVal);

			if ((connectionResult != null) && (connectionResult.getStatusCode() ==  HttpURLConnection.HTTP_OK)) {
				inviteList = connectionResult.getResult();
			}

		} finally {
			connectionHelper.closeConnection();
		}
		return inviteList == null ? false: true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {


		if (success) {

			inviteEmailFragment.populate(inviteList);
			showProgress(false);
			Log.i("invitelist.onpostexecute","succes:"+inviteList);
		} else {
			showProgress(false);
			Log.i("invitelist.onpostexecute","failute:");
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


			if (mInviteStatusMessageView != null ) {
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

			}

			if (mInviteFormView != null) {
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
			}

		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.

			if (mInviteStatusView != null) {
				mInviteStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			}

			if (mInviteFormView != null) {
				mInviteFormView.setVisibility(show ? View.GONE : View.VISIBLE);
			}
		}
	}

	@Override
	protected void onCancelled() {
	}

}
