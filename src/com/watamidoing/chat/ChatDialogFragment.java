package com.watamidoing.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.waid.R;
import com.watamidoing.view.WhatAmIdoing;
import com.watamidoing.xmpp.service.XMPPService;

public class ChatDialogFragment extends DialogFragment {

	protected static final String TAG = "ChatDialogFragment";
	private static WhatAmIdoing context;
	private static Messenger messenger;
	private LinkedList<String> pendingMessages = new LinkedList<String>();
	private LinkedList<String>pendingParticipantsMessages =  new LinkedList<String>();
	/**
	 * Create a new instance of MyDialogFragment, providing "num" as an
	 * argument.
	 * 
	 * @param messenger
	 */
	public static ChatDialogFragment newInstance(boolean b,
			WhatAmIdoing activity, Messenger msgr) {
		ChatDialogFragment f = new ChatDialogFragment();
		context = activity;
		messenger = msgr;
		return f;
	}

	private ChatMessageAdapter cma;
	private EditText chatMessage;
	private ListView messageView;
	private ParticipantMessageAdapter pma;
	private ListView participantsView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));
		View v = inflater.inflate(R.layout.chat_layout, container, false);

		Button sendMessage = (Button) v.findViewById(R.id.sendChat);
		chatMessage = (EditText) v.findViewById(R.id.chatMessage);

		// String[] values = new String[] { "Android", "iPhone",
		// "WindowsMobile",
		// "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
		// "Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
		// "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
		// "Android", "iPhone", "WindowsMobile" };

		String[] values = {};
		cma = new ChatMessageAdapter(context, values);
		pma = new ParticipantMessageAdapter(context, values);
		
		messageView = (ListView) v.findViewById(R.id.chatWindow);
		participantsView = (ListView) v.findViewById(R.id.chatParticipants);
		participantsView.setAdapter(pma);
		sendMessage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String message = chatMessage.getText().toString();
				Message msgObj = Message.obtain(null,
						XMPPService.PUSH_MESSAGE_TO_QUEUE);
				Bundle b = new Bundle();
				b.putString("chatMessage", message);
				msgObj.setData(b);
				try {
					if (messenger != null)
						messenger.send(msgObj);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				

				/*
				 * String getGroupJidUrl =
				 * context.getString(R.string.xmmp_roomjid_url); Authentication
				 * auth =
				 * DatabaseHandler.getInstance(context).getDefaultAuthentication
				 * ();
				 * 
				 * String url = getGroupJidUrl+"?token="+auth.getToken();
				 * Log.i(TAG,url);
				 * 
				 * GetGroupIdTask ggit = new GetGroupIdTask(url, context);
				 * ggit.execute((Void)null);
				 */
			}
		});

		messageView.setAdapter(cma);
		return v;
	}

	@Override 
	public void onStart(){
		super.onStart();
		String oldMessage = pendingMessages.poll();
		while(oldMessage != null) {
			addMessage(oldMessage);
			oldMessage = pendingMessages.poll();
		}
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		if (cma != null) 
		cma.notifyDataSetChanged();
	}

	
	public void addMessage(String message) {
		
		if (cma == null) {
			pendingMessages.add(message);
		} else {
		String[] oldValues = cma.getValues();
		// cma.clear();

		ArrayList<String> values = new ArrayList<String>(
				Arrays.asList(oldValues));
		if (values.size() == 5) {

			Collections.reverse(values);
			values.add(message);
			values.remove(0);
			Collections.reverse(values);

		} else {
			values.add(message);
		}

		String[] newValues = Arrays.copyOf(values.toArray(), values.size(),
				String[].class);

		cma = new ChatMessageAdapter(context, newValues);
		chatMessage.setText("");
		messageView.setAdapter(cma);
		cma.notifyDataSetChanged();

		}
	}
	

	public void addParticipant(String message) {
		
		if (pma == null) {
			pendingParticipantsMessages.add(message);
		} else {
		String[] oldValues = pma.getValues();
		// cma.clear();

		ArrayList<String> values = new ArrayList<String>(
				Arrays.asList(oldValues));
		if (values.size() == 5) {

			Collections.reverse(values);
			values.add(message);
			values.remove(0);
			Collections.reverse(values);

		} else {
			values.add(message);
		}

		String[] newValues = Arrays.copyOf(values.toArray(), values.size(),
				String[].class);

		pma = new ParticipantMessageAdapter(context, newValues);
		participantsView.setAdapter(pma);
		pma.notifyDataSetChanged();

		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		context.stopChatService();
	}
}
