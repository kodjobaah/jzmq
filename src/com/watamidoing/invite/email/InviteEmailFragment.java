package com.watamidoing.invite.email;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.GridLayout;
import android.widget.TextView;

import com.waid.R;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.invite.email.adapter.InviteListExpandableAdapter;
import com.watamidoing.invite.email.callback.InviteDialogInteraction;
import com.watamidoing.invite.email.model.Invite;
import com.watamidoing.tasks.SendInviteEmailTask;
import com.watamidoing.utils.UtilsWhatAmIdoing;
import com.watamidoing.view.WhatAmIdoing;

public class InviteEmailFragment extends DialogFragment {

	private static final String TextView = null;
	private View view;
	private Activity mContext;
	private InviteEmailFragment fragment;
	private InviteListExpandableAdapter expListAdapter;
	private InviteDialogInteraction inviteDialogInteration;

	public static InviteEmailFragment newInstance(String title, Activity context, InviteDialogInteraction inviteDialogInteraction) {

		InviteEmailFragment frag = new InviteEmailFragment();
		Bundle args = new Bundle();
		args.putString("title", title);
		frag.setArguments(args);
		frag.mContext = context;
		frag.inviteDialogInteration = inviteDialogInteraction;
		return frag;
	}
	
	@Override
	public void onStart() {
	
		//This happens when the screen is rotated -- so removing the dialog
		if (inviteDialogInteration != null) {
			inviteDialogInteration.setInviteForm(view.findViewById(R.id.invite_form));
			inviteDialogInteration.setStatusView(view.findViewById(R.id.invite_status));
			inviteDialogInteration.setInviteStatusMessage((TextView) view.findViewById(R.id.invite_status_message));
			inviteDialogInteration.showInviteProgress(true);
		} else {
			getDialog().dismiss();
	
			//		WhatAmIdoing waid  = (WhatAmIdoing) getActivity();
			//		waid.sendEmail();
		}
		super.onStart();
		
		
	}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle(DialogFragment.STYLE_NO_TITLE, R.style.accepted);
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Translucent);
    }
	
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		
		view = inflater.inflate(R.layout.invite, container);

		
		LayoutParams viewLayoutParams = new ViewGroup.LayoutParams(
		        ViewGroup.LayoutParams.WRAP_CONTENT,
		        ViewGroup.LayoutParams.WRAP_CONTENT);
		Point size = UtilsWhatAmIdoing.getScreenSize(mContext);
	
		if (size !=  null) {
			viewLayoutParams.height = (int)(size.y * 0.8);
			view.setLayoutParams(viewLayoutParams);
		}
		fragment = this;

		Button cancel = (Button) view.findViewById(R.id.cancel_invite);
		cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.i("InviteListTask.InviteListTask","Cancel Button was clicked");
				fragment.getDialog().dismiss();

			}
		});

		EditText emailEditText = (EditText)view.findViewById(R.id.invite_email);
		emailEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				List<Invite> original = expListAdapter.getOriginal();
				
				String filter = s.toString();
				
				List<Invite> invites = original;
				if ((filter != null) && (filter.trim().length() > 1)) {
					List<Invite> filtered = new ArrayList<Invite>();
					for(Invite inv : original) {
							if (inv.getEmail().contains(filter)) {
								filtered.add(inv);
							}
					}
					invites = filtered;
				} 
				expListAdapter = new InviteListExpandableAdapter(mContext,original,invites);
				ExpandableListView invitelist = (ExpandableListView) view.findViewById(R.id.invite_list);
				invitelist.setAdapter(expListAdapter);
				expListAdapter.notifyDataSetChanged();
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		Button inviteToView = (Button)view.findViewById(R.id.invite_to_view);
		inviteToView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				EditText emailEditText = (EditText)view.findViewById(R.id.invite_email);
				String email =emailEditText.getText().toString().trim();
				Log.i("InviteListTask.constructor","email=["+email+"]");
				Pattern pattern = Patterns.EMAIL_ADDRESS;

				List<String> previousSelected = new ArrayList<String>();
				if (expListAdapter != null) {
					previousSelected = expListAdapter.getAllPreviousSelectedInvites();
				}

				if ((previousSelected.size() < 1) && (email.length() > 0) && !pattern.matcher(email).matches()){
					emailEditText.setError(mContext.getString(R.string.error_invalid_email));
					emailEditText.requestFocus();
				} else if ((previousSelected.size() <= 0) && (email.length() <= 0)){
					emailEditText.setError(mContext.getString(R.string.enter_email));
					emailEditText.requestFocus();

				} else {
					
					String validEmail = null;	
					if (pattern.matcher(email).matches()) {
						validEmail = email;
					}
				
					String inviteUrl = mContext.getString(R.string.send_invite_url);
					Authentication auth =  DatabaseHandler.getInstance(mContext).getDefaultAuthentication();
					String selected = null;
					if (previousSelected.size() > 0) { 
						selected = previousSelected.toString().replace("[", "").replace("]", "").replaceAll("\\s", "")+(validEmail != null ? ","+validEmail: "");
					} else {
						selected = validEmail;
					}

					String url = inviteUrl+"?token="+auth.getToken()+"&email="+selected;
					Log.i("InviteListTask.constructory","url="+url+"");
					SendInviteEmailTask sendInviteEmailTask = new SendInviteEmailTask(url,mContext);
					sendInviteEmailTask.execute((Void) null);
					fragment.getDialog().dismiss();
				}

			}
		});

		CheckBox selectAllPrevious = (CheckBox)view.findViewById(R.id.select_all_previous_invites);
		selectAllPrevious.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				Log.i("InviteListTask.InviteListTask","Select all previous");
				if (expListAdapter != null) {
					if (isChecked) {
						expListAdapter.sellectAllPrevious();
					} else {
						expListAdapter.unSelectAllPrevious();
					}
				}

			}
		});


		//getDialog().getWindow().setSoftInputMode(
		//		WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		//view.requestLayout();
		return view;
	}

	public void populate(String inviteList) {


		ExpandableListView invitelist = (ExpandableListView) view.findViewById(R.id.invite_list);
		Map<String, String> groupList = new HashMap<String, String>();
		String[] values = inviteList.split(",");

		LinkedHashMap<String, List<String>> invitelistCollection = new LinkedHashMap<String, List<String>>();


		List<Invite> invites = new ArrayList<Invite>();
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

			Invite in = new Invite(email,lastName,firstName);
			invites.add(in);
		
		}
		expListAdapter = new InviteListExpandableAdapter(mContext,invites,invites);
		invitelist.setIndicatorBounds(0, 20);
		invitelist.setAdapter(expListAdapter);
		
		LayoutParams viewLayoutParams = view.getLayoutParams();
		Point size = UtilsWhatAmIdoing.getScreenSize(mContext);
		viewLayoutParams.height = (int)(size.y * 0.9);
		view.setLayoutParams(viewLayoutParams);
		
		GridLayout gl = (GridLayout) view.findViewById(R.id.invite_form);
		LayoutParams glLayoutParams = gl.getLayoutParams();
		glLayoutParams.height = (int)(size.y * 0.9);
		gl.setLayoutParams(glLayoutParams);
		
		
		ExpandableListView elv = (ExpandableListView) view.findViewById(R.id.invite_list);
		 LayoutParams elvLayoutParams = elv.getLayoutParams();
		elvLayoutParams.height = (int)(size.y * 0.55);
		elv.setLayoutParams(elvLayoutParams);
		
		
		view.requestLayout();
		gl.requestLayout();
		
		elv.requestLayout();
		

	}

	public View getInviteForm() {
		return view.findViewById(R.id.invited_form);
	}

	public View getStatusView() {
		return view.findViewById(R.id.invited_status);
	}

	public TextView getInviteStatusMessage() {
		return (TextView) view.findViewById(R.id.invite_status_message);
	}
}
