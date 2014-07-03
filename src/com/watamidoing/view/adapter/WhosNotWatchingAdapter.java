package com.watamidoing.view.adapter;

import java.util.List;

import com.waid.R;
import com.watamidoing.value.User;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class WhosNotWatchingAdapter extends BaseAdapter {

	
	private Activity activity;
	private List<User> users;
	private LayoutInflater inflater;

	public WhosNotWatchingAdapter(Activity activity, List<User> users) {
		this.activity = activity;
		this.users = users;
		inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
	}
	@Override
	public int getCount() {
		return users.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.whoes_not_watching_row, null);
		// TODO Auto-generated method stub
        
        TextView firstName = (TextView)vi.findViewById(R.id.firstName);
        TextView lastName = (TextView)vi.findViewById(R.id.lastName); 
        TextView email = (TextView)vi.findViewById(R.id.email); 
        
        User user = users.get(position);
      
        firstName.setText(user.getFirstName());
        lastName.setText(user.getLastName());
        email.setText(user.getEmail());
        
		return vi;
	}

}
