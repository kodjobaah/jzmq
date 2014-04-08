package com.watamidoing.chat;

import com.waid.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ParticipantMessageAdapter extends ArrayAdapter<String> {

	private Context context;
	private String[] values;

	public ParticipantMessageAdapter(Context context, String[] values) {
		super(context, R.layout.chat_layout_row,values);
		this.context = context;
		this.values = values;
	}
	
	@Override
	 public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View rowView = inflater.inflate(R.layout.chat_layout_row, parent, false);
	    TextView textView = (TextView) rowView.findViewById(R.id.chatRow);
	   
	    textView.setText(values[position]);
	    // change the icon for Windows and iPhone
	    String s = values[position];
	    return rowView;
	  }
	
	  public void setValues(String values[]) {
		  this.values = values;
	  }
	  
	  public String[] getValues() {
		  return this.values;
	  }
	  
	  

}
