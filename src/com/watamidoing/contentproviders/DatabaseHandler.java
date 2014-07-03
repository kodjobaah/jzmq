package com.watamidoing.contentproviders;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static DatabaseHandler singleton;

	public static DatabaseHandler getInstance(final Context context) {
		if (singleton == null) {
			singleton = new DatabaseHandler(context);
		}
		return singleton;
	}

	private static final String TAG ="DatabaseHandler";
	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "whatamidoing";

	private final Context context;

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		// Good idea to use process context here
		this.context = context.getApplicationContext();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	    Log.i(TAG,"---------------------should be creating table");
		db.execSQL(Authentication.CREATE_TABLE);
		db.execSQL(TwitterAuthenticationToken.CREATE_TABLE);
		db.execSQL(LinkedInAuthenticationToken.CREATE_TABLE);

	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	public synchronized TwitterAuthenticationToken getDefaultTwitterAuthentication() {
		
		final SQLiteDatabase db = this.getReadableDatabase();
		TwitterAuthenticationToken item = null;

		Cursor cursor = db.query(TwitterAuthenticationToken.TABLE_NAME,
				TwitterAuthenticationToken.FIELDS, null, null, null, null, null);
		if (cursor == null || cursor.isAfterLast()) {
			return null;
		}

		if (cursor.moveToFirst()) {
			item = new TwitterAuthenticationToken(cursor);
		}
		cursor.close();

		return item;
	}
	
public synchronized LinkedInAuthenticationToken getDefaultLinkedinAuthentication() {
		
		final SQLiteDatabase db = this.getReadableDatabase();
		LinkedInAuthenticationToken item = null;

		Cursor cursor = db.query(LinkedInAuthenticationToken.TABLE_NAME,
				LinkedInAuthenticationToken.FIELDS, null, null, null, null, null);
		if (cursor == null || cursor.isAfterLast()) {
			return null;
		}

		if (cursor.moveToFirst()) {
			item = new LinkedInAuthenticationToken(cursor);
		}
		cursor.close();

		return item;
	}
	
	public synchronized Authentication getDefaultAuthentication() {

		final SQLiteDatabase db = this.getReadableDatabase();
		Authentication item = null;

		Cursor cursor = db.query(Authentication.TABLE_NAME,
				Authentication.FIELDS, null, null, null, null, null);
		if (cursor == null || cursor.isAfterLast()) {
			return null;
		}

		if (cursor.moveToFirst()) {
			item = new Authentication(cursor);
		}
		cursor.close();

		return item;
	}


	public synchronized LinkedInAuthenticationToken getAuthenticationLinkedin(final String id) {

		final SQLiteDatabase db = this.getReadableDatabase();
		final Cursor cursor = db.query(LinkedInAuthenticationToken.TABLE_NAME,
				LinkedInAuthenticationToken.FIELDS, TwitterAuthenticationToken.COL_ID + " IS ?",
				new String[] { String.valueOf(id) }, null, null, null, null);
	
		LinkedInAuthenticationToken item = null;
		if ((cursor != null) && !cursor.isBeforeFirst() && !cursor.isAfterLast()){
			item = new LinkedInAuthenticationToken(cursor);
			cursor.close();
		}
		

		return item;

	}

	public synchronized TwitterAuthenticationToken getAuthenticationTwitter(final String id) {

		final SQLiteDatabase db = this.getReadableDatabase();
		final Cursor cursor = db.query(TwitterAuthenticationToken.TABLE_NAME,
				TwitterAuthenticationToken.FIELDS, TwitterAuthenticationToken.COL_ID + " IS ?",
				new String[] { String.valueOf(id) }, null, null, null, null);
	
		TwitterAuthenticationToken item = null;
		if ((cursor != null) && !cursor.isBeforeFirst() && !cursor.isAfterLast()){
			item = new TwitterAuthenticationToken(cursor);
			cursor.close();
		}
		

		return item;

	}
	
	public synchronized Authentication getAuthentication(final String id) {

		final SQLiteDatabase db = this.getReadableDatabase();
		final Cursor cursor = db.query(Authentication.TABLE_NAME,
				Authentication.FIELDS, Authentication.COL_ID + " IS ?",
				new String[] { String.valueOf(id) }, null, null, null, null);
	
		Authentication item = null;
		if ((cursor != null) && !cursor.isBeforeFirst() && !cursor.isAfterLast()){
			item = new Authentication(cursor);
			cursor.close();
		}
		

		return item;

	}

	public synchronized boolean putAuthentication(final Authentication auth) {
		boolean success = false;
		int result = 0;
		final SQLiteDatabase db = this.getWritableDatabase();

		
		if (auth.getId() != null) {
			result += db.update(Authentication.TABLE_NAME, auth.getContent(),
					Authentication.COL_ID + " IS ?",
					new String[] { auth.getId() });

		}

		if (result > 0) {
			
			success = true;
		} else {
		
			// Update failed or wasn't possible, insert instead
		
			final long id = db.insert(Authentication.TABLE_NAME, null,
					auth.getContent());

			if (id > -1) {
		
				db.close();
				success = true;
			}
		}

		return success;
	}
	
	public synchronized boolean putAuthentication(final TwitterAuthenticationToken auth) {
		boolean success = false;
		int result = 0;
		final SQLiteDatabase db = this.getWritableDatabase();

		
		if (auth.getId() != null) {
			result += db.update(TwitterAuthenticationToken.TABLE_NAME, auth.getContent(),
					TwitterAuthenticationToken.COL_ID + " IS ?",
					new String[] { auth.getId() });

		}

		if (result > 0) {
			
			success = true;
		} else {
		
			// Update failed or wasn't possible, insert instead
		
			final long id = db.insert(TwitterAuthenticationToken.TABLE_NAME, null,
					auth.getContent());

			if (id > -1) {
		
				db.close();
				success = true;
			}
		}

		return success;
	}

	public synchronized boolean putAuthentication(final LinkedInAuthenticationToken auth) {
		boolean success = false;
		int result = 0;
		final SQLiteDatabase db = this.getWritableDatabase();

		
		if (auth.getId() != null) {
			result += db.update(LinkedInAuthenticationToken.TABLE_NAME, auth.getContent(),
					LinkedInAuthenticationToken.COL_ID + " IS ?",
					new String[] { auth.getId() });

		}

		if (result > 0) {
			
			success = true;
		} else {
		
			// Update failed or wasn't possible, insert instead
		
			final long id = db.insert(LinkedInAuthenticationToken.TABLE_NAME, null,
					auth.getContent());

			if (id > -1) {
		
				db.close();
				success = true;
			}
		}

		return success;
	}


	public synchronized int removeAuthentication(final Authentication auth) {
		
		final SQLiteDatabase db = this.getWritableDatabase();
		final int result = db.delete(Authentication.TABLE_NAME,
				Authentication.COL_ID + " IS ?", new String[] { auth.getId() });

		return result;
	}
	

	public synchronized int removeAuthentication(final TwitterAuthenticationToken auth) {
		
		final SQLiteDatabase db = this.getWritableDatabase();
		final int result = db.delete(TwitterAuthenticationToken.TABLE_NAME,
				TwitterAuthenticationToken.COL_ID + " IS ?", new String[] { auth.getId() });

		return result;
	}
	
	

	public synchronized int removeAuthentication(final LinkedInAuthenticationToken auth) {
		
		final SQLiteDatabase db = this.getWritableDatabase();
		final int result = db.delete(LinkedInAuthenticationToken.TABLE_NAME,
				LinkedInAuthenticationToken.COL_ID + " IS ?", new String[] { auth.getId() });

		return result;
	}

}
