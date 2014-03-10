package com.watamidoing.tasks;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.waid.R;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.utils.ScreenDimension;
import com.watamidoing.utils.UtilsWhatAmIdoing;
import com.watamidoing.view.WhatAmIdoing;
import com.watamidoing.view.adapter.InviteListExpandableAdapter;

public class InviteListTask extends AsyncTask<Void, Void, Boolean> {


	private String inviteList = null;
	private WhatAmIdoing mContext;
	private View mInviteStatusView;
	private TextView mInviteStatusMessageView;
	private View mInviteFormView;
	private Dialog dialog;
	private InviteListExpandableAdapter expListAdapter;

	public InviteListTask(final WhatAmIdoing context) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		this.mContext = context;
		dialog = new Dialog(mContext, R.style.ThemeWithCorners);
		dialog.setContentView(R.layout.invite);
		dialog.setTitle("Invite List");
		dialog.setCancelable(true);
		mInviteFormView = dialog.findViewById(R.id.invite_form);
		mInviteStatusView = dialog.findViewById(R.id.invite_status);
		mInviteStatusMessageView = (TextView) dialog.findViewById(R.id.invite_status_message);

		ScreenDimension sm = UtilsWhatAmIdoing.getScreenDimensions(mContext);
		// RelativeLayout ll = (RelativeLayout)dialog.findViewById(R.id.inviteList_layout);
		//  ll.setLayoutParams(new FrameLayout.LayoutParams((int)(sm.getHeightPixels()*0.8f),(int)(sm.getWidthPixels()*0.8f)));


		// WindowManager.LayoutParams lp = new WindowManager.LayoutParams((int)(sm.getHeightPixels()*0.8f),(int)(sm.getWidthPixels()*0.8f));
		//dialog.getWindow().setAttributes(lp);


		LayoutParams params = dialog.getWindow().getAttributes();
		// ll.setLayoutParams(new FrameLayout.LayoutParams(params.height,params.width));



		dialog.show();
		showProgress(true);
		//UtilsWhatAmIdoing.hideKeyBoard(context);

		//Add event listeners
		Button cancel = (Button) dialog.findViewById(R.id.cancel_invite);
		cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d("InviteListTask.InviteListTask","Cancel Button was clicked");
				dialog.cancel();
			}
		});

		Button inviteToView = (Button)dialog.findViewById(R.id.invite_to_view);
		inviteToView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				EditText emailEditText = (EditText)dialog.findViewById(R.id.invite_email);
				String email =emailEditText.getText().toString().trim();
				Log.d("InviteListTask.constructor","email=["+email+"]");
				Pattern pattern = Patterns.EMAIL_ADDRESS;

				List<String> previousSelected = new ArrayList<String>();
				if (expListAdapter != null) {
					previousSelected = expListAdapter.getAllPreviousSelectedInvites();
				}

				if ((email.length() > 0) && !pattern.matcher(email).matches()){
					emailEditText.setError(context.getString(R.string.error_invalid_email));
					emailEditText.requestFocus();
				} else if ((previousSelected.size() <= 0) && (email.length() <= 0)){
					emailEditText.setError(context.getString(R.string.enter_email));
					emailEditText.requestFocus();

				} else {
					String validEmail = (email.length() > 0) ? email : null;
					String inviteUrl = context.getString(R.string.send_invite_url);
					Authentication auth =  DatabaseHandler.getInstance(context).getDefaultAuthentication();
					String selected = null;
					if (previousSelected.size() > 0) {
						selected = previousSelected.toString().replace("[", "").replace("]", "").replaceAll("\\s", "")+(validEmail != null ? ","+validEmail: "");
					} else {
						selected = validEmail;
					}

					String url = inviteUrl+"?token="+auth.getToken()+"&email="+selected;
					Log.d("InviteListTask.constructory","url="+url+"");
					SendInviteEmailTask sendInviteEmailTask = new SendInviteEmailTask(url,context);
					sendInviteEmailTask.execute((Void) null);
					dialog.cancel();

				}

			}
		});

		CheckBox selectAllPrevious = (CheckBox)dialog.findViewById(R.id.select_all_previous_invites);
		selectAllPrevious.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				Log.d("InviteListTask.InviteListTask","Select all previous");
				if (expListAdapter != null) {
					if (isChecked) {
						expListAdapter.sellectAllPrevious();
					} else {
						expListAdapter.unSelectAllPrevious();
					}
				}

			}
		});
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

			ExpandableListView invitelist = (ExpandableListView) dialog.findViewById(R.id.invite_list);
			Map<String, String> groupList = new HashMap<String, String>();
			String[] values = inviteList.split(",");

			LinkedHashMap<String, List<String>> invitelistCollection = new LinkedHashMap<String, List<String>>();


			for(int i= 0; i < values.length; i++) {
				String val = values[i];
				String[] items = val.split(":");
				
				String email = null;
				if (items.length > 0)
					email = items[0];
				
				String firstName = null;
				if (items.length > 1)
					firstName = items[1];
				
				String lastName = null;
				if (items.length > 2)
					lastName = items[2];
				
				String groupName = "not defined";
				if ((firstName != null) && (lastName != null))
					groupName = firstName+" "+lastName;
				
				String tok = UUID.randomUUID().toString();
				groupList.put(tok,groupName);
				ArrayList<String> childList = new ArrayList<String>();
				childList.add(email);
				invitelistCollection.put(tok, childList);

			}
			expListAdapter = new InviteListExpandableAdapter(
					mContext, groupList, invitelistCollection);
			invitelist.setIndicatorBounds(0, 20);
			invitelist.setAdapter(expListAdapter);
			showProgress(false);
			Log.d("invitelist.onpostexecute","succes:"+inviteList);
		} else {
			showProgress(false);
			Log.d("invitelist.onpostexecute","failute:");
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
