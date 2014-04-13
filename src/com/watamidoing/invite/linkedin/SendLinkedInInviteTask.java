package com.watamidoing.invite.linkedin;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.xmlpull.v1.XmlSerializer;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.utils.UtilsWhatAmIdoing;
import com.watamidoing.view.WhatAmIdoing;
import com.waid.R;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

public class SendLinkedInInviteTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "SendLinkedInInviteTask";
	private String url;
	private WhatAmIdoing context;
	private ConnectionResult results;

	public SendLinkedInInviteTask() {

	}

	public SendLinkedInInviteTask(String url, WhatAmIdoing context) {
		this.url = url;
		this.context = context;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper();
		results = httpConnectionHelper.connect(url);
		if (results == null) {
			return false;
		} else if (results.getStatusCode() != HttpURLConnection.HTTP_OK) {
			return false;
		} else if (results.getResult().contains("Unable")){
			return false;
		}

		return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {

		if (success) {

			if (context.isVideoSharing() && context.isVideoStart()) {
				String url = context
							.getString(R.string.invite_location_url)
							+ "="
							+ results.getResult();
	
					// Now let's go and ask for a protected resource!
			        OAuthRequest request = new OAuthRequest(Verb.POST, ShareTag.RESOURCE_URL);
			        request.addHeader("Content-Type","text/xml");

			        XmlSerializer xmlSerializer = Xml.newSerializer();
			        StringWriter writer = new StringWriter();
			        try {   
			        	
			            xmlSerializer.setOutput(writer);
			        	xmlSerializer.startTag("",ShareTag.SHARE);
			            xmlSerializer.startTag("",ShareTag.COMMENT);
			            xmlSerializer.text(context.getString(R.string.linkedin_share_comment));
			            xmlSerializer.endTag("",ShareTag.COMMENT);
			            xmlSerializer.startTag("",ShareTag.CONTENT);
			            xmlSerializer.startTag("",ShareTag.TITLE);
			            xmlSerializer.text(context.getString(R.string.linkedin_share_title));
			            xmlSerializer.endTag("",ShareTag.TITLE);
			            xmlSerializer.startTag("",ShareTag.SUBMITTED_URL);
			            xmlSerializer.text(url);
			            xmlSerializer.endTag("",ShareTag.SUBMITTED_URL);
			            xmlSerializer.startTag("",ShareTag.SUBMITTED_IMAGE);
			            xmlSerializer.text("http://www.whatamidoing.info/assets/images/icon.png");
			            xmlSerializer.endTag("",ShareTag.SUBMITTED_IMAGE);
			            xmlSerializer.endTag("", ShareTag.CONTENT);
						xmlSerializer.startTag("", ShareTag.VISIBILITY);
			            xmlSerializer.startTag("",ShareTag.CODE);
			            xmlSerializer.text("anyone");
			            xmlSerializer.endTag("",ShareTag.CODE);
			            xmlSerializer.endTag("", ShareTag.VISIBILITY);
			            xmlSerializer.endTag("",ShareTag.SHARE);
			            xmlSerializer.endDocument();
			            String payload = writer.toString();
				        Log.i(TAG,"PAYLOAD[ "+payload+"]");
				        request.addPayload(payload);
				        
				        LinkedInAuthorization la = new LinkedInAuthorization(context);
				        Token accessToken = la.getAccessToken();
				        OAuthService service = la.buildLinkedInSerice();
				        service.signRequest(accessToken, request);
				        Response response = request.send();
				        Log.i(TAG,response.getBody());
				        Log.i(TAG,response.getHeaders().toString());
				        int responseCode = response.getCode();
				        
				        if  (responseCode >= 199 && responseCode < 300) {
				        	UtilsWhatAmIdoing.displaySuccessInvitesLinkedInDialog(context);
				        	Log.i(TAG,"HORRAY IT WORKED!");
				        } else  {
				        	UtilsWhatAmIdoing.displayNotAbleToUpdateLinkedInStatusDialog(context);
				        	Log.i(TAG,"Unable to update status["+responseCode+"]" );
				        }
           
			        } catch (IllegalArgumentException e) {
			        	UtilsWhatAmIdoing.displayNotAbleToUpdateLinkedInStatusDialog(context);
						e.printStackTrace();
					} catch (IllegalStateException e) {
			        	UtilsWhatAmIdoing.displayNotAbleToUpdateLinkedInStatusDialog(context);
						e.printStackTrace();
					} catch (IOException e) {
			        	UtilsWhatAmIdoing.displayNotAbleToUpdateLinkedInStatusDialog(context);
						e.printStackTrace();
					}	
			
			} else {
				String message = context.getString(R.string.camera_not_started);
				UtilsWhatAmIdoing.displayGenericMessageDialog(context, message);
			}
		} else {
			UtilsWhatAmIdoing.displayNetworkProblemsForInvitesDialog(context);

		}
	}
	

}
