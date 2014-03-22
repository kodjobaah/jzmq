package com.watamidoing.tasks;
import android.os.AsyncTask;

import com.facebook.widget.WebDialog;
import com.watamidoing.view.WhatAmIdoing;

public class CloseFacebookFeedDialogTask extends AsyncTask<Void, Void, Boolean> {

	
	private FacebookPostTask facebookPostTask;


	public CloseFacebookFeedDialogTask(final FacebookPostTask facebookPostTask) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		this.facebookPostTask = facebookPostTask;
	}
	
	@Override
	protected Boolean doInBackground(Void... arg0) {
		
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {
  
        
		if (success) {
			facebookPostTask.cancelDialog();
		}
	}
	

	@Override
	protected void onCancelled() {
	}

}
