package com.whatamidoing.tasks.callbacks;

import java.io.Serializable;

import android.os.Messenger;

public interface WebsocketController extends Serializable {
	
	String CONNECTION_OPEN = "websocket-connection-open";
	String CONNECTION_CLOSE = "websocket-connection-close";
	String FRAME = "websocket-frame";
	
	/**
	 * Called by the websocket task when socket connection has been establised or not
	 * @param results True if connection was established otherwise false;
	 */
	public void websocketConnectionCompleted(boolean results);
	public void websocketProblems(boolean result);
	public void networkStatusChange(boolean availabe);
	public void setMessengerService(Messenger mService);
	public void sendFrame(String res);
	public void websocketServiceStop(boolean serviceStopped);
	public void websocketServiceConnectionClose(boolean b);

}
