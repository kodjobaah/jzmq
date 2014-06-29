package com.watamidoing.invite.email.task;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import com.watamidoing.invite.email.InviteEmailFragment;
import com.watamidoing.invite.email.model.Invite;
import com.watamidoing.view.WhatAmIdoing;

public class ContactsListTask extends AsyncTask<Void, Void, Boolean> {

	private WhatAmIdoing mContext;
	private InviteEmailFragment emailFragment;
	List<Invite> invites;

	public ContactsListTask(final WhatAmIdoing context,
			InviteEmailFragment emailFragment) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		this.mContext = context;
		this.emailFragment = emailFragment;
		invites = new ArrayList<Invite>();
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
		String _ID = ContactsContract.Contacts._ID;
		String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
		Uri EmailCONTENT_URI = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
		String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
		String DATA = ContactsContract.CommonDataKinds.Email.DATA;
		ContentResolver contentResolver = mContext.getContentResolver();
		Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null,
				null);
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				String contact_id = cursor
						.getString(cursor.getColumnIndex(_ID));
				String name = cursor.getString(cursor
						.getColumnIndex(DISPLAY_NAME));
				Cursor emailCursor = contentResolver.query(EmailCONTENT_URI,
						null, EmailCONTACT_ID + " = ?",
						new String[] { contact_id }, null);
				while (emailCursor.moveToNext()) {
					String email = emailCursor.getString(emailCursor.getColumnIndex(DATA));
					Invite invite = new Invite(email,name,null);
				    invites.add(invite);
				}
					emailCursor.close();
			}
		}
		cursor.close();
		return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {

		if (success) {
			emailFragment.populateContacts(invites);
		} 
	}

}
