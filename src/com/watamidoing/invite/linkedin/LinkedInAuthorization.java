package com.watamidoing.invite.linkedin;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.LinkedInApi;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.waid.R;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.contentproviders.LinkedInAuthenticationToken;
import com.watamidoing.view.WhatAmIdoing;

public class LinkedInAuthorization {

	
	private static final String LINKEDIN_CONSUMER_KEY ="7778fa6wun4fok";
	private static final String LINKEDIN_CONSUMER_SECRET="4Dvd9wu8YEd4XTiq";
	public static final String  LINKEDIN_CALLBACK_URL = "oob";
	
	protected static final String TAG = "LinkedInAuthorization";
	public static final String LINKEDIN_AUTH_ID = "linkedin_auth_id";
	private Dialog dialog;
	private WhatAmIdoing mContext;
	private OAuthService service;

	protected Token accessToken;

	private Token requestToken;

	public LinkedInAuthorization(WhatAmIdoing context){
			this.mContext = context;
	}
	
	public void authorizeLinkedIn() {
		
		
		service = buildLinkedInSerice();
		
		requestToken = service.getRequestToken();
		
		String url = service.getAuthorizationUrl(requestToken);
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		mContext.startActivity(i);
	
		dialog = new Dialog(mContext, R.style.ThemeWithCorners);
		dialog.setContentView(R.layout.linkedin_auth_layout);
	    dialog.setTitle(mContext.getString(R.string.linkedin_auth_dialog));
	    
	    dialog.setCancelable(true);
	    
	    final EditText linkedInPinText = (EditText)dialog.findViewById(R.id.linkedInPin);
	    
	    dialog.show();
	    //Add event listeners
	    Button cancel = (Button) dialog.findViewById(R.id.linkedInPinButton);
	    cancel.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        		String pin = linkedInPinText.getText().toString();
	            	Verifier verifier = new Verifier(pin);
	            	accessToken = service.getAccessToken(requestToken, verifier);
	                Log.i(TAG,"(if your curious it looks like this: " + accessToken +" )");
	                
	                LinkedInAuthenticationToken tat = new LinkedInAuthenticationToken(LINKEDIN_AUTH_ID, accessToken.getToken(), accessToken.getSecret());
					DatabaseHandler.getInstance(mContext).putAuthentication(tat);
	            	mContext.shareOnLinkedIn();
	                
	        		dialog.cancel();
			
			}
	        
	    });
	}
	
	public OAuthService buildLinkedInSerice() {
		service = new ServiceBuilder()
	    .provider(LinkedInApi.class)
	    .apiKey(LINKEDIN_CONSUMER_KEY)
	    .signatureType(SignatureType.QueryString)
	    .apiSecret(LINKEDIN_CONSUMER_SECRET)
	    .build();
		return service;
	
	}
	
	public Token getAccessToken() {		
		 LinkedInAuthenticationToken tat = DatabaseHandler.getInstance(mContext).getDefaultLinkedinAuthentication();
		 if (tat == null) {
			 return null;
		 }
		 
		 return new Token(tat.getToken(), tat.getSecret());
	}
}
