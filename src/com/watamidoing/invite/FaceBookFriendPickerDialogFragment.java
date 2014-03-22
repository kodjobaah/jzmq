package com.watamidoing.invite;
import com.waid.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;

public class FaceBookFriendPickerDialogFragment extends DialogFragment {
	
	
	private static final String TAG = "FaceBookFriendPickerDialogFragment";
	private static FaceBookFriendPickerDialogFragment fragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setStyle(STYLE_NO_TITLE, getTheme());
	}

	
	@Override 
	public void onStart() {
		super.onStart();
		
	}
	

	public static FaceBookFriendPickerDialogFragment newInstance(boolean b) {
    	fragment = new FaceBookFriendPickerDialogFragment();
        
    	 //We want this Dialog to be a Fragment in fact,
         //otherwise there are problems with showing another fragment, the DeviceListFragment
         fragment.setShowsDialog(false);
         //wDialog.setStyle(SherlockDialogFragment.STYLE_NORMAL,android.R.style.Theme_Holo_Light_Dialog);
         //We don't want to recreate the instance every time user rotates the phone
         fragment.setRetainInstance(true);
         //Don't close the dialog when touched outside
         fragment.setCancelable(false);
         
       return fragment;
	}
	
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        
		getFragmentManager().beginTransaction().addToBackStack(null).commit();
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        View view = getActivity().getLayoutInflater().inflate(R.layout.friends_activity, null);
        alertDialogBuilder.setView(view);
        return alertDialogBuilder.create();
    }


}
