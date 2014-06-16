package com.watamidoing.transport.service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

import com.watamidoing.transport.zeromq.tasks.PollingResponse;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.StrictMode;
import android.util.Log;

public class ZeroMQHandler extends Thread{
    public Handler mHandler;
	private String pUrl;
	private String token;
	private Context context;
	private Socket socket;
	private boolean connected = true;
	private String streamId;
	private Integer sequenceNumber =0;
	private PollingResponse pollingResponse;
	private Boolean socketClosed = false;
	private static final String END_STREAM = "END_STREAM";
	private static final String CONNECT_STRING = "CONNECT";
	private static final String TAG = "ZeroMQHandler";
	private final static int REQUEST_TIMEOUT = 4000; // msecs, (> 1000!)
	private final static int REQUEST_RETRIES = 3; // Before we abandon

    
	static {
		Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

	}

	
    public ZeroMQHandler(String pUrl, String token, ZMQ.Context context) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		this.pUrl = pUrl;
		this.token = token;
		this.context = context;
		socket = this.context.socket(ZMQ.REQ);
		socket.setIdentity(token.getBytes());
		socket.connect(pUrl);
		connected = true;
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
	}

	public boolean sendMessge(String frame, String time) {
	
		Message m = mHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("FRAME",frame);
        b.putString("TIME",time);
        m.setData(b);
        return mHandler.sendMessage(m);
   }

    public void run() {
        Looper.prepare();

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
            	
            	Bundle bundle = msg.getData();
                String frame = (String)bundle.get("FRAME");
                String time = (String)bundle.get("TIME");
                
                List<String> data = new ArrayList<String>();
				data.add(streamId);
				data.add(token);
				data.add(String.valueOf(sequenceNumber));
				data.add(time);
				data.add(frame);
				connected  = transmitData(data);
				
				if (!connected) {
					Looper looper = Looper.myLooper();
					looper.quit();
				}
            	// process incoming messages here
            }
        };

        Looper.loop();
        
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
    }

    public void quitLooper(){
    	Looper looper = mHandler.getLooper();
		looper.quit();

    }
	public void waitForSocketToBeClose() {
		quitLooper();
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
}
