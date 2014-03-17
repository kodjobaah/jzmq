package com.watamidoing.tasks;

import java.net.HttpURLConnection;

import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.utils.UtilsWhatAmIdoing;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

public class SendLocationInformationTask extends AsyncTask<Void, Void, Boolean> {

	
	private String url;
	private Activity context;
	private ConnectionResult  results;

	public SendLocationInformationTask() {
		
	}
	public SendLocationInformationTask(String url, Activity context) {
		this.url = url;
		this.context = context;
	}
	@Override
	protected Boolean doInBackground(Void... arg0) {
		HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper();
		results = httpConnectionHelper.connectOther(url);
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
			UtilsWhatAmIdoing.displaySuccessLocationInformationSent(context);
		    Log.d("sendinviteemailtask.onpostexecute","succes:");
		} else {
			UtilsWhatAmIdoing.displayNetworkProblemsForLocationDialog(context);
		    Log.d("sendinviteemailtask.onpostexecute","failure:");
			
		}
	}

}
