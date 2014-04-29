package com.watamidoing.invite.email.model;

public class Invite {
	
	private String firstName;
	private String lastName;
	private String email;
	
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
		return email.hashCode();
	}

    public boolean equals(Invite invite) {
    		return this.email.equalsIgnoreCase(invite.email);
    }
	
}
