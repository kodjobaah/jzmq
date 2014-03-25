package com.watamidoing.invite.twitter;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.waid.R;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.contentproviders.TwitterAuthenticationToken;
import com.watamidoing.view.WhatAmIdoing;

public class TwitterAuthorization {
	private static final String TWITTER_CONSUMER_KEY ="zafDiHrQppvYEnzrMlzXQ";
	private static final String TWITTER_CONSUMER_SECRET="hX97yXn9znhVQTBniZDOaS2ECmJA0KL9wvL3tRiMfs";
	public static final String  TWITTER_CALLBACK_URL = "oob";
	
	protected static final String TAG = "TwitterAuthorization";
	public static final String TWITTER_AUTH_ID = "twitter_auth_id";
	private Dialog dialog;
	private RequestToken requestToken;
	private AccessToken accessToken = null;
	private WhatAmIdoing mContext;

	public TwitterAuthorization(WhatAmIdoing context){
			this.mContext = context;
	}
	
	public void authorizeTwitter() {
		
		
		TwitterFactory factory = buildTwitterFactory();
		
		final Twitter twitter = factory.getInstance();
	    try {
	    	requestToken = twitter.getOAuthRequestToken();
	    	Log.i(TAG,"----TWITTER AUTHENTICATION URL["+requestToken.getAuthenticationURL()+"]");
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));

		
		dialog = new Dialog(mContext, R.style.ThemeWithCorners);
		dialog.setContentView(R.layout.twitter_auth_layout);
        dialog.setTitle(mContext.getString(R.string.twitter_auth_dialog));
        dialog.setCancelable(true);
        
        final EditText twitterPinText = (EditText)dialog.findViewById(R.id.twitterPin);
        
	    dialog.show();
        //Add event listeners
        Button cancel = (Button) dialog.findViewById(R.id.twitterPinButton);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG,"----- click hit");
                String pin = twitterPinText.getText().toString();
                try {
					accessToken = twitter.getOAuthAccessToken(requestToken, pin);
					User user = twitter.verifyCredentials();
					
					TwitterAuthenticationToken tat = new TwitterAuthenticationToken(TWITTER_AUTH_ID, accessToken.getToken(), accessToken.getTokenSecret());
					DatabaseHandler.getInstance(mContext).putAuthentication(tat);
                } catch (TwitterException e) {
					Toast.makeText(mContext, mContext.getString(R.string.twitter_problems), Toast.LENGTH_LONG).show();
					dialog.cancel();
				}
                
               // try {
                	mContext.tweetWhatIAmDoing();
                	dialog.cancel();
					//Status status = twitter.updateStatus("checking to see if I can update your status");
				//} catch (TwitterException e) {
					// TODO Auto-generated catch block
				//	e.printStackTrace();
			//	}
                //TODO: POST TWEET TO TWIITER
              }
            
        });
	}

	public TwitterFactory buildTwitterFactory() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
		builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
		
		Configuration configuration = builder.build();
		TwitterFactory factory = new TwitterFactory(configuration);
		return factory;
	}
	
	public AccessToken getAccessToken() {		
		 TwitterAuthenticationToken tat = DatabaseHandler.getInstance(mContext).getDefaultTwitterAuthentication();
		 if (tat == null) {
			 return null;
		 }
		 
		 return new AccessToken(tat.getToken(), tat.getSecret());
	}
}
