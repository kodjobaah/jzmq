package com.watamidoing.transport.zeromq.tasks;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;

import com.watamidoing.value.FrameData;

import android.os.AsyncTask;
import android.os.Process;
import android.os.StrictMode;
import android.util.Log;

public class ZeroMQTransportTask extends AsyncTask<Void, Void, Boolean> {

	private LinkedBlockingQueue<FrameData> dataToProcess = new LinkedBlockingQueue<FrameData>(100);
	private static final String END_STREAM = "END_STREAM";
	private static final String CONNECT_STRING = "CONNECT";
	private static final String TAG = "ZeroMQTask";
	private final static int REQUEST_TIMEOUT = 4000; // msecs, (> 1000!)
	private final static int REQUEST_RETRIES = 3; // Before we abandon
	ZMQ.Context context;
	ZMQ.Socket socket;

	String streamId = "";
	volatile boolean connected = true;
	long sequenceNumber = 0;
	long responses = 0;

	static {
		Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

	}

	private String token;
	private String pUrl;
	private PollingResponse pollingResponse;
	private Boolean socketClosed =false;

	public ZeroMQTransportTask(String pUrl, String token, ZMQ.Context context) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		this.pUrl = pUrl;
		this.token = token;

		this.token = token;
		this.context = context;
		socket = this.context.socket(ZMQ.REQ);
		socket.setIdentity(token.getBytes());
		socket.connect(pUrl);
		connected = true;
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
	}
	
	public boolean initialize() {
		List<String> data = new ArrayList<String>();
		data.add(CONNECT_STRING);
		boolean result =  transmitData(data);
		if (result) {
			reset();
		}
		return result;
		
	}
	
	public void reset() {
		sequenceNumber = 0;
		streamId = "";
	}
	public boolean sendMessge(String frame, String time) {
		
		FrameData data = new FrameData(frame,time);
		if (connected) {
			try {
				synchronized(dataToProcess) {
					dataToProcess.put(data);
					dataToProcess.notify();
				}
			} catch (InterruptedException e) {
				Log.d(TAG,e.getMessage());
				connected = false;
			}
		}
		return connected;
		
	}
	
	@Override
	protected Boolean doInBackground(Void... arg0) {

		while (connected && !isCancelled()) {
			
			 FrameData frameData ;
			try {
				Log.d(TAG,"waiting for data");
				frameData = dataToProcess.take();
				
				if (frameData != null) {
					List<String> data = new ArrayList<String>();
					data.add(streamId);
					data.add(token);
					data.add(String.valueOf(sequenceNumber));
					data.add(frameData.getTime());
					data.add(frameData.getFrame());
					connected  = transmitData(data);
				 } else {
					 connected = false;
				 }
			} catch (InterruptedException e) {
				Log.d(TAG,e.getMessage());
				connected = false;
			}
			
			Log.d(TAG,"CONNECTED STATUS:"+connected);
			
		}
		
		Log.d(TAG,"doInbackground:Stopped:"+pollingResponse.getStatus());
		if (pollingResponse.getStatus() == PollingResponse.SENT) {
			Log.d(TAG,"doInbackground:before sending close stream message");
			sendCloseStreamMessage();
			Log.d(TAG,"doInbackground:sent close stream message");
		} else {
			socket.setLinger(0);
		}
		
		socket.disconnect(pUrl);
		socket.close();
		synchronized(socketClosed) {
			socketClosed = true;
			Log.d(TAG,"SocketClosed set");
		}
		return true;
	}

	private void sendCloseStreamMessage() {
		List<String> data = new ArrayList<String>();
		data.add(END_STREAM);
		data.add(streamId);
		transmitData(data);
	}
	private synchronized boolean transmitData(List<String> data) {
		pollingResponse = null;
		int retries = 0;
		
		do {

			if (data.size() > 1) {
				for (int i = 0; i < (data.size() - 1); i++) {
					
					socket.send(data.get(i), ZMQ.SNDMORE);
				}
			}
			socket.send(data.get(data.size() - 1), 0);
			Log.d(TAG,"DATA SENT:"+data.size());
			pollingResponse = pollForResponse();
			if (pollingResponse.getStatus() == PollingResponse.RETRY) {
				socket = context.socket(ZMQ.REQ);
				Log.d(TAG,"UNABLE TO CONNECT -- RETRYING:"+retries+":"+pollingResponse.getStatus());
				socket.connect(pUrl);
			}
			retries = retries + 1;
		} while ((pollingResponse.getStatus() == PollingResponse.RETRY)
				&& (retries != REQUEST_RETRIES));

		if (pollingResponse.getStatus() == PollingResponse.SENT) {
			return true;
		}
		return false;

	}

	private PollingResponse pollForResponse() {
		PollingResponse pollingResponse = new PollingResponse();

		// Poll socket for a reply, with timeout
		PollItem items[] = { new PollItem(socket, Poller.POLLIN) };
		int rc = ZMQ.poll(items, REQUEST_TIMEOUT);
		if (rc == -1) {
			pollingResponse.setStatus(PollingResponse.NOT_ABLE_TO_CONNECT);
			return pollingResponse;
		}

		if (items[0].isReadable()) {
			// We got a reply from the server, must match sequence
			streamId = socket.recvStr(Charset.forName("UTF-8"));
			Log.d(TAG,"response from server:"+streamId);
			if (streamId == null) {
				pollingResponse.setStatus(PollingResponse.NOT_ABLE_TO_CONNECT);
				return pollingResponse;
			}

			pollingResponse.setStatus(PollingResponse.SENT);
			sequenceNumber = sequenceNumber + 1;
			return pollingResponse;

		} 
		pollingResponse.setStatus(PollingResponse.RETRY);
		return pollingResponse;
		
	}

	public void disconnect() {
			connected = false;		
		
	}
	@Override
	protected void onPostExecute(final Boolean success) {
	}

	@Override
	protected void onCancelled() {
		connected = false;
		if (socketClosed) {
			socket.setLinger(0);
		} else {
			socket.close();
			context.term();
		}
		
	}

	public void waitForSocketToBeClose() {
		connected = false;
		while(true) {
			synchronized(socketClosed) {
				if (socketClosed){
					break;
				}
			}
			Log.d(TAG,"WAITING FOR SOCKET TO BE CLOASE");
		}
		Log.d(TAG,"FINNISHED WAITING FOR SOCKET TO BE CLOASE");
	}


	
}
