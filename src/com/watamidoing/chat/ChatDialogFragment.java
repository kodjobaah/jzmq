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
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.waid.R;
import com.watamidoing.chat.xmpp.service.XMPPService;
import com.watamidoing.utils.UtilsWhatAmIdoing;
import com.watamidoing.view.WhatAmIdoing;

public class ChatDialogFragment extends DialogFragment {

	protected static final String TAG = "ChatDialogFragment";
	private static WhatAmIdoing context;
	private static Messenger messenger;
	private LinkedList<String> pendingMessages;
	private LinkedList<String>pendingParticipantsMessages;
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
	private View chatLayoutView;
	private Button sendMessage;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));
		chatLayoutView = inflater.inflate(R.layout.chat_layout, container, false);

	    sendMessage = (Button) chatLayoutView.findViewById(R.id.sendChat);
		chatMessage = (EditText) chatLayoutView.findViewById(R.id.chatMessage);

		String[] values = {};
		cma = new ChatMessageAdapter(context, values);
		pma = new ParticipantMessageAdapter(context, values);
		
		messageView = (ListView) chatLayoutView.findViewById(R.id.chatWindow);
		participantsView = (ListView) chatLayoutView.findViewById(R.id.chatParticipants);
		participantsView.setAdapter(pma);
		sendMessage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String message = chatMessage.getText().toString();
				if (message.length() > 0) {
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
				
				} else {
					UtilsWhatAmIdoing.displayGenericToast(context, "Enter Some Text");
				}
	
			}
		});
		messageView.setAdapter(cma);
		
		chatLayoutView.post(new Runnable() {
			@Override
			public void run() {
				int height = sendMessage.getHeight();

				chatMessage.getLayoutParams().height = height;
				RelativeLayout sendChatLayout = (RelativeLayout)chatLayoutView.findViewById(R.id.sendChatLayout);
				sendChatLayout.requestLayout();
			}
		});
		return chatLayoutView;
	}

	@Override 
	public void onStart(){
		super.onStart();
		if (pendingMessages != null) {
		String oldMessage = pendingMessages.poll();
		while(oldMessage != null) {
			addMessage(oldMessage);
			oldMessage = pendingMessages.poll();
		}
		}
		
		if (pendingParticipantsMessages != null) {
			String oldMessage = pendingParticipantsMessages.poll();
			while(oldMessage != null) {
				addParticipant(oldMessage);
				oldMessage = pendingParticipantsMessages.poll();
			}
		}
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		/*
		
		*/
	//	chatMessage.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,(int) (height)));

		if (cma != null) 
		cma.notifyDataSetChanged();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	//	int height = sendMessage.getHeight();
		//chatMessage.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,(int) (height)));
		
		

	}

	
	public void addMessage(String message) {
		
		if (cma == null) {
			if (pendingMessages == null) 
				pendingMessages = new LinkedList<String>();
			
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
			if (pendingParticipantsMessages == null)
				pendingParticipantsMessages = new LinkedList<String>();
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
		pma = null;
		cma = null;
		pendingMessages = null;
		pendingParticipantsMessages = null;
		context.stopChatService();
	}
	
	
}
