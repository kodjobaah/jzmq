package com.watamidoing.reeiver.callbacks;

import java.io.Serializable;

import android.os.Messenger;

/**
 * This class is used to communicate between external ZeroMQService and WhatAmIdoing
 * via the receivers
 * @author kodjobaah
 *
 */
public interface ZeroMQController extends Serializable {
	
	String CONNECTION_OPEN = "zeromq-connection-open";
	String CONNECTION_CLOSE = "zermq-connection-close";
	String FRAME = "zeromq-frame";
	
	public void networkStatusChange(boolean availabe);
	
	/**
	 * Called by ZeroMQServiceConnection when connection has been established
	 * with the service
	 * @param mService
	 */
	public void setMessengerService(Messenger mService);
	
	/**
	 * Called by the ZeroMQServiceStoppedReceiver 
	 * @param results True if service was stopped;
	 */
	public void zeroMQServiceStop(boolean serviceStopped);

	public void zeroMQServiceDestroyed(boolean serviceDestroyed);

	public void zeroMQServiceStarted(boolean serviceStarted);

}
