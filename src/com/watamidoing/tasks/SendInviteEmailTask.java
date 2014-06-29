package com.watamidoing.tasks;

import java.net.HttpURLConnection;

import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.utils.UtilsWhatAmIdoing;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

public class SendInviteEmailTask extends AsyncTask<Void, Void, Boolean> {

	
	private String url;
	private Activity context;
	private ConnectionResult  results;

	public SendInviteEmailTask() {
		
	}
	public SendInviteEmailTask(String url, Activity context) {
		this.url = url;
		this.context = context;
	}
	@Override
	protected Boolean doInBackground(Void... arg0) {
		HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper();
		results = httpConnectionHelper.connect(url);
		if (results == null) {
			return false;
		} else if(results.getStatusCode() != HttpURLConnection.HTTP_OK) {
			return false;
		}
		
		return true;
	}
	
	@Override
	protected void onPostExecute(final Boolean success) {
  
        
		if (success) {
			UtilsWhatAmIdoing.displaySuccessInvitesDialog(context);
		    Log.i("sendinviteemailtask.onpostexecute","succes:");
		} else {
			UtilsWhatAmIdoing.displayNetworkProblemsForInvitesDialog(context);
		    Log.i("sendinviteemailtask.onpostexecute","failure:");
			
		}
	}

}
