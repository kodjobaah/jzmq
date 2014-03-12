package com.watamidoing.parser;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.watamidoing.value.User;
public class ParseInviteList {


	private JSONObject json;

	public ParseInviteList(String inviteListJson) {

		try {
			this.json = new JSONObject(inviteListJson);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public List<User> getAccepted() {

		List<User> users = new ArrayList<User>();
		try {

			System.out.println(json);

			JSONArray acceptedUsersList = json.getJSONArray("accepted");

			for(int i=0; i < acceptedUsersList.length(); i++) {
				JSONObject userData = acceptedUsersList.getJSONObject(i);
				User user = new User();
				user.setEmail(userData.getString("email"));
				user.setFirstName(userData.getString("firstName"));
				user.setLastName(userData.getString("lastName"));
				users.add(user);
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		return users;
	}

	public List<User> getNotAccepted() {
		List<User> users = new ArrayList<User>();
		try {

			System.out.println(json);

			JSONArray acceptedUsersList = json.getJSONArray("notAccepted");

			for(int i=0; i < acceptedUsersList.length(); i++) {
				JSONObject userData = acceptedUsersList.getJSONObject(i);
				User user = new User();
				user.setEmail(userData.getString("email"));
				user.setFirstName(userData.getString("firstName"));
				user.setLastName(userData.getString("lastName"));
				users.add(user);
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		return users;
	}


}
