package com.watamidoing.reeiver.callbacks;

import java.io.Serializable;

import android.os.Messenger;

public interface ZeroMQController extends Serializable {
	
	String CONNECTION_OPEN = "zeromq-connection-open";
	String CONNECTION_CLOSE = "zermq-connection-close";
	String FRAME = "zeromq-frame";
	
	/**
	 * Called by the ZeroMQServiceStartedReceiver 
	 * @param results True if connection was established otherwise false;
	 */
	public void zeroMQServiceStarted(boolean results);
	
	
	
	/**
	 * Called by the ZeroMQNotAbleToConnectReceiver 
	 * @param results True if unable to connect
	 */
	public void zeroMQProblems(boolean result);
	
	public void networkStatusChange(boolean availabe);
	public void setMessengerService(Messenger mService);
	public void sendFrame(String res);
	
	/**
	 * Called by the ZeroMQServiceStoppedReceiver 
	 * @param results True if service was stopped;
	 */
	public void zeroMQServiceStop(boolean serviceStopped);
	
	/**
	 * Called by the ZeroMQServiceConnectionCloseReceiver 
	 * @param results True if zeromq connection was closed
	 */
	public void zeroMQServiceConnectionClose(boolean b);

}
