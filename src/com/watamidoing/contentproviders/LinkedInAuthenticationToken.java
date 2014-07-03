package com.watamidoing.contentproviders;

import android.content.ContentValues;
import android.database.Cursor;

public class LinkedInAuthenticationToken {
	
	public static final String TABLE_NAME = "linkedin";
	
	public static final String COL_ID = "_id";
	
	public static final String COL_TOKEN = "token";
	
	public static final String COL_SECRET = "secret";
	
	
	//For database projects so order is consistent
	public static final String[] FIELDS = {COL_ID, COL_TOKEN, COL_SECRET};
	
	
	/*
	 * The sql code that creates  Table for storing Authentications.
	 * 
	 */
		
	public static final String CREATE_TABLE = 
			"CREATE TABLE " + TABLE_NAME + "("
			+ COL_ID + " TEXT PRIMARY KEY,"
			+ COL_TOKEN + " TEXT NOT NULL,"
			+ COL_SECRET + " TEXT NOT NULL"
			+ ")";

	private String id;
	private String token;
	private String secret;

	
	public LinkedInAuthenticationToken(String id, String token, String secret) {
		this.id = id;
		this.token = token;
		this.secret = secret;
	}
	
	public LinkedInAuthenticationToken(final Cursor cursor) {
		this.id = cursor.getString(0);
		this.token = cursor.getString(1);
		this.secret = cursor.getString(2);
	}


	public ContentValues getContent() {
		final ContentValues values = new ContentValues();
		values.put(COL_ID,id);
		values.put(COL_TOKEN, token);		
		values.put(COL_SECRET, secret);
		return values;
	}

	public String getId() {
		return id;
	}



	public void setId(String id) {
		this.id = id;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
	public void setSecret(String secret) {
		this.secret = secret;
	}
	
	public String getSecret() {
		return this.secret;
	}

}
