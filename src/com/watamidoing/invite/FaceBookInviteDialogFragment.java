package com.watamidoing.invite;

import java.util.Arrays;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.waid.R;
import com.watamidoing.view.WhatAmIdoing;

public class FaceBookInviteDialogFragment extends DialogFragment {

	
	
	private static FaceBookInviteDialogFragment fragment;
	public static final String TAG = "FaceBookInviteDialogFragment";
    private UiLifecycleHelper uiHelper;
    public static WhatAmIdoing waid;
    private Session.StatusCallback callback = new Session.StatusCallback() {

		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			onSessionStateChange(session,state,exception);
		}
    	
    };


    public static FaceBookInviteDialogFragment newInstance(boolean showAsDialog, WhatAmIdoing whatAmIdoing) {
    	waid = whatAmIdoing;
    	fragment = new FaceBookInviteDialogFragment();
        
    	 //We want this Dialog to be a Fragment in fact,
         //otherwise there are problems with showing another fragment, the DeviceListFragment
         fragment.setShowsDialog(false);
         //wDialog.setStyle(SherlockDialogFragment.STYLE_NORMAL,android.R.style.Theme_Holo_Light_Dialog);
         //We don't want to recreate the instance every time user rotates the phone
         fragment.setRetainInstance(true);
         //Don't close the dialog when touched outside
         fragment.setCancelable(true);
         
       return fragment;
    }

    /*
    public static FaceBookInviteDialogFragment newInstance() {
        return newInstance(true);
    }
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(getActivity(),callback);
        uiHelper.onCreate(savedInstanceState);
        
    
    }
    
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
    	if (state.isOpened()) {
    		Log.i(TAG,"Logged in...");
    		waid.setFacebookSession(session);
    		waid.shareOnFacebook();
    		
    	} else if (state.isClosed()){
    		Log.i(TAG,"Logged out..");
    	}
    }

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saveInstanceState) {
		View view = inflater.inflate(R.layout.facebook_login,container,false);
		LoginButton authButton = (LoginButton) view.findViewById(R.id.authButton);
		
		authButton.setPublishPermissions(Arrays.asList(new String[]{ "publish_actions"}));
		authButton.setFragment(this);
		return view;
			
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// For scenarios where the main activity is launched and user
		/// session is nut null, th esession state change notification
		// may not be triggered. Trigger it if is open/closed.
		Session session = Session.getActiveSession();
		if (session != null &&
		 	(session.isOpened() || session.isClosed())) {
		 		onSessionStateChange(session,session.getState(),null);
		 }
		uiHelper.onResume();
		
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}
}
