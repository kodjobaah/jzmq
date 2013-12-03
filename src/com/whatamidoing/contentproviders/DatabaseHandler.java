package com.whatamidoing.contentproviders;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static DatabaseHandler singleton;

	public static DatabaseHandler getInstance(final Context context) {
		if (singleton == null) {
			singleton = new DatabaseHandler(context);
		}
		return singleton;
	}

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "whatamidoing";

	private final Context context;

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		// Good idea to use process context here
		this.context = context.getApplicationContext();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL(Authentication.CREATE_TABLE);

	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

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

	public synchronized Authentication getAuthentication(final String id) {

		final SQLiteDatabase db = this.getReadableDatabase();
		final Cursor cursor = db.query(Authentication.TABLE_NAME,
				Authentication.FIELDS, Authentication.COL_ID + " IS ?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if (cursor == null || cursor.isAfterLast()) {
			return null;
		}

		Authentication item = null;
		if (cursor.moveToFirst()) {
			item = new Authentication(cursor);
		}
		cursor.close();

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

}
