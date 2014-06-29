package com.watamidoing.chat;

import com.waid.R;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ParticipantMessageAdapter extends ArrayAdapter<String> {

	private Context context;
	private String[] values;

	public ParticipantMessageAdapter(Context context, String[] values) {
		super(context, R.layout.chat_layout_row, values);
		this.context = context;
		this.values = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		String xmppParticipantDelimeter = context.getString(R.string.xmpp_participant_nick_delimeter);
		if (position == 0) {
			View rowView = inflater.inflate(R.layout.chat_layout_header,
					parent, false);
			TextView textView = (TextView) rowView
					.findViewById(R.id.chatHeader);
			String participantHeader = context.getString(R.string.chat_participant_header);
			textView.setText(participantHeader);
			TextView tv = (TextView) rowView.findViewById(R.id.chatRow);

			String value = values[position];
			
			Integer n = value.lastIndexOf(xmppParticipantDelimeter);
			if (n > 0) {
				value = value.substring(n + 13, value.length());
			}
			tv.setText(value);

			return rowView;
		} else {
			View rowView = inflater.inflate(R.layout.chat_layout_row, parent,
					false);
			TextView textView = (TextView) rowView.findViewById(R.id.chatRow);

			String value = values[position];
			Integer n = value.lastIndexOf(xmppParticipantDelimeter);
			if (n > 0) {
				value = value.substring(n + 13, value.length());
			}
			textView.setText(value);
			return rowView;
		}
	}

	public void setValues(String values[]) {
		this.values = values;
	}

	public String[] getValues() {
		return this.values;
	}

}
