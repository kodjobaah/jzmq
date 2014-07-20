package com.watamidoing.nativecamera;

public interface NativeCallback {

	void ableToConnect();
	void unableToConnect();
	void connectionDropped();
	void updateMessagesSent(long messageCount);
}
