package com.watamidoing.tasks;
import org.zeromq.ZMQ;

import android.os.AsyncTask;
import android.os.Process;
import android.os.StrictMode;
import android.util.Log;

import com.watamidoing.transport.service.ZeroMQService;
import com.watamidoing.view.WhatAmIdoing;

public class ZeroMQTask extends AsyncTask<Void, Void, Boolean> {


	private static final String TAG = "ZeroMQTask";
	ZMQ.Context context;
	ZMQ.Socket socket;

	String streamId = "";
	boolean connected = false;
	long sequenceNumber = 0;

	static {
		Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

	}

	private WhatAmIdoing mContext;
	private String token;

	public ZeroMQTask(String pUrl, String token) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		this.token = token;
		context = ZMQ.context(1);
		socket = context.socket(ZMQ.REQ);
		System.out.println("_-----"+pUrl);
		socket.connect (pUrl);
		connected = true;
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
	}

	public void sendMessge(String frame, String time) {
		Log.d(TAG,"SENDING:"+frame.length());
		socket.send(streamId, ZMQ.SNDMORE);
		socket.send(token,ZMQ.SNDMORE);
		socket.send(String.valueOf(sequenceNumber),ZMQ.SNDMORE);
		socket.send(time,ZMQ.SNDMORE);
		boolean result = socket.send(frame,0);
		if (result) {
			byte[] reply = socket.recv(0);
			streamId = new String (reply, ZMQ.CHARSET);
			sequenceNumber = sequenceNumber + 1;
			Log.d(TAG,"Received " + streamId) ;	
		} else {
			Log.d(TAG,"Unable to send message");
		}
	}

	public void disconnect() {
		connected = false;
	}
	@Override
	protected Boolean doInBackground(Void... arg0) {

		while(connected) {

		}
		socket.close();
		context.close();
		context.term();
		return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {
	}


	@Override
	protected void onCancelled() {
	}

}
