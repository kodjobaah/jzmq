package com.watamidoing.chat;

import com.waid.R;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ChatMessageAdapter extends ArrayAdapter<String> {

	private Context context;
	private String[] values;

	public ChatMessageAdapter(Context context, String[] values) {
		super(context, R.layout.chat_layout_row, values);
		this.context = context;
		this.values = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


		Spanned text = Html.fromHtml(values[position]);

		if (position == 0) {
			View rowView = inflater.inflate(R.layout.chat_layout_header,
					parent, false);
			TextView textView = (TextView) rowView
					.findViewById(R.id.chatHeader);
			String messageHeader = context.getString(R.string.chat_message_header);
			textView.setText(messageHeader);
			TextView tv = (TextView) rowView.findViewById(R.id.chatRow);

			tv.setText(text);

			return rowView;
		} else {
			View rowView = inflater.inflate(R.layout.chat_layout_row, parent,
					false);
			TextView textView = (TextView) rowView.findViewById(R.id.chatRow);

			textView.setText(text);
			// change the icon for Windows and iPhone
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
