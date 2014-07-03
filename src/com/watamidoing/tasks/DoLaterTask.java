package com.watamidoing.tasks;
import android.os.AsyncTask;

import com.watamidoing.view.WhatAmIdoing;

public class DoLaterTask extends AsyncTask<Void, Void, Boolean> {

	
	private WhatAmIdoing mContext;

	public DoLaterTask(final WhatAmIdoing context) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		this.mContext = context;
		
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
			mContext.startVideo();
		}
	}
	

	@Override
	protected void onCancelled() {
	}

}
