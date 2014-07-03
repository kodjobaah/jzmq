package com.watamidoing.contentproviders;

import android.content.ContentValues;
import android.database.Cursor;

public class Authentication {
	
	public static final String TABLE_NAME = "Authentication";
	
	public static final String COL_ID = "_id";
	
	public static final String COL_TOKEN = "token";
	
	public static final String COL_PLAYSESSION= "playsession";
	
	
	//For database projects so order is consistent
	public static final String[] FIELDS = {COL_ID, COL_TOKEN,COL_PLAYSESSION};
	
	
	/*
	 * The sql code that creates  Table for storing Authentications.
	 * 
	 */
		
	public static final String CREATE_TABLE = 
			"CREATE TABLE " + TABLE_NAME + "("
			+ COL_ID + " TEXT PRIMARY KEY,"
			+ COL_PLAYSESSION + " TEXT NOT NULL,"
			+ COL_TOKEN + " TEXT NOT NULL"
			+ ")";

	private String id;

	private String token;

	private String playSession;


	
	public Authentication(String id, String token, String playSession) {
		this.id = id;
		this.token = token;
		this.playSession = playSession;
	}
	
	public Authentication(final Cursor cursor) {
		this.id = cursor.getString(0);
		this.token = cursor.getString(1);
		this.playSession = cursor.getString(2);
	}


	public ContentValues getContent() {
		final ContentValues values = new ContentValues();
		values.put(COL_ID,id);
		values.put(COL_TOKEN, token);
		values.put(COL_PLAYSESSION, playSession);
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



	public String getPlaySession() {
		return playSession;
	}



	public void setPlaySession(String playSession) {
		this.playSession = playSession;
	}
}
