package com.watamidoing.invite.email.model;

import android.util.Log;

public class Invite implements Comparable {
	
	private String firstName;
	private String lastName;
	private String email;
	private String TAG ="Invite";
	
	public Invite(String email, String lastName, String firstName) {
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	public String getEmail() {
		return email;
	}
	
	public int hashCode() {
		return email.trim().hashCode();
	}

    public boolean equals(Invite invite) {
    		Log.i(TAG,"compairng ["+email+"] to ["+invite.email+"]");
    		return this.email.trim().equalsIgnoreCase(invite.email.trim());
    }

	@Override
	public int compareTo(Object another) {
		Invite other = (Invite)another;
		return email.trim().toLowerCase().compareTo(other.email.trim().toLowerCase());
	}
	
}
