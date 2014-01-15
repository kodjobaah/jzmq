package com.watamidoing;


public class SessionParser {

	private String setCookie;
	private String token;
	private String playSession;

	public SessionParser(String setCookie) {
		this.setCookie = setCookie;
		
		 String tokens[] = setCookie.split(";");
		 for(int i=0; i < tokens.length; i++) {
			 String val = tokens[i];
			 if (val.startsWith("PLAY_SESSION")){
				 playSession = val;
				 break;
			 }
			 
		 }
		 if (playSession != null)  {
			 String sub = playSession.substring(playSession.indexOf("=") + 1,playSession.length() - 1);
			 this.token = sub.substring(sub.indexOf("=")+1,sub.length());
			 
		 }
		
	}

	public String getSetCookie() {
		return setCookie;
	}

	public String getToken() {
		return token;
	}

	public String getPlaySession() {
		return playSession;
	}

	public void setSetCookie(String setCookie) {
		this.setCookie = setCookie;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setPlaySession(String playSession) {
		this.playSession = playSession;
	}
}
