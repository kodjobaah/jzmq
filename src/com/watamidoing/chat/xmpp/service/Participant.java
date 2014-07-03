package com.watamidoing.chat.xmpp.service;

import android.os.Parcel;
import android.os.Parcelable;

public class Participant implements Parcelable {

	private String type;
	private String fullnick;
	
	public Participant(String type, String fullnick) {
		this.type = type;
		this.fullnick= fullnick;
	}
	
	public Participant(Parcel in) {
		String[] data = new String[2];
		in.readStringArray(data);
		this.type = data[0];
		this.fullnick = data[1];
				
	}
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		  dest.writeStringArray(new String[] {this.type,
                  this.fullnick});

	}
	
     public static final Parcelable.Creator<Participant> CREATOR = new Parcelable.Creator<Participant>() {
         public Participant createFromParcel(Parcel in) {
             return new Participant(in); 
         }

         public Participant[] newArray(int size) {
             return new Participant[size];
         }
     };

	public String getType() {
		return type;
	}

	public String getFullnick() {
		return fullnick;
	}

}
