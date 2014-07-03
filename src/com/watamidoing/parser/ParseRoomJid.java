package com.watamidoing.parser;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.watamidoing.value.User;
public class ParseRoomJid {


	private JSONObject json;

	public ParseRoomJid(String roomJid) {

		try {
			this.json = new JSONObject(roomJid);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public String getRoomJid() {

			try {
				return json.getString("jid");
		
			} catch (JSONException e) {
				return "";
			}
		
	}

	public String getNickName() {
		
		try {
			return json.getString("nickname");
		} catch (JSONException e) {
			return "";
		}
	}

	public String getOriginalRoomJid() {
		return json.toString();
	}


}
