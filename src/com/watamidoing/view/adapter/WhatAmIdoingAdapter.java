package com.watamidoing.view.adapter;

import com.watamidoing.R;
import com.watamidoing.view.WhatAmIdoing;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;

public class WhatAmIdoingAdapter extends BaseAdapter{

	private WhatAmIdoing mContext;

	public WhatAmIdoingAdapter(WhatAmIdoing whatAmIdoing) {
		this.mContext = whatAmIdoing;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
	//	  GridView gridview = (GridView)mContext.findViewById(R.id.option_layout);
		// TODO Auto-generated method stub
		return null;
	}

}
