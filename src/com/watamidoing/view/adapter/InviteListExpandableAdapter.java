package com.watamidoing.view.adapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.watamidoing.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InviteListExpandableAdapter extends BaseExpandableListAdapter {
 
    private Activity context;
    private Map<String, List<String>> inviteListCollections;
    private Map<String, String> inviteList;
    private LayoutInflater inflater;
    
    
	private ArrayList <LinearLayout> childLayouts = new ArrayList<LinearLayout>();
	private Set<String> inviteListKeys;
	private List<CheckBox> previousInvites;
    
    public InviteListExpandableAdapter(Activity context, Map<String, String> inviteList,
            Map<String, List<String>> inviteListCollections) {
        this.context = context;
        this.inviteListCollections = inviteListCollections;
        this.inviteList = inviteList;
        this.inflater =  LayoutInflater.from(context);
        
    	inviteListKeys = inviteList.keySet();
    	Iterator<String> tokens = inviteListKeys.iterator();
    
    	previousInvites = new ArrayList<CheckBox>();
    	while(tokens.hasNext()) {
    		String token = tokens.next();
    		LinearLayout childLayout = new LinearLayout(context);
 		   	CheckBox checkBox = new CheckBox(context);
 		    String email = (String)inviteListCollections.get(token).get(0);
 		   	checkBox.setText(email);
 		   	previousInvites.add(checkBox);
 		   	childLayout.addView(checkBox);
 		   	childLayouts.add(childLayout);
    		
    	}
    }
 
    public Object getChild(int groupPosition, int childPosition) {
    	String tok = (String)inviteListKeys.toArray()[groupPosition];
    	return inviteListCollections.get(tok).get(childPosition);
    }
 
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }
 
    public View getChildView(final int groupPosition, final int childPosition,
        boolean isLastChild, View convertView, ViewGroup parent) {
        final String invite = (String) getChild(groupPosition, childPosition);

        
        
       return getChildView(invite,groupPosition);
   }
    
    
 
    public int getChildrenCount(int groupPosition) {
    	Set<String> keys = inviteList.keySet();
    	String tok = (String)keys.toArray()[groupPosition];
    	return inviteListCollections.get(tok).size();
    }
 
    public Object getGroup(int groupPosition) {
    	
    	Set<String> keys = inviteList.keySet();
    	String tok = (String)keys.toArray()[groupPosition];
        return inviteList.get(tok);
    }
 
    public int getGroupCount() {
        return inviteList.size();
    }
 
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }
 
    public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {
        	String invite = (String) getGroup(groupPosition);
        
         TextView textView = getGenericView();
         textView.setText(invite);
         return textView;
    }
 
    public boolean hasStableIds() {
        return true;
    }
 
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
    
    public View getChildView(String invite, int groupPosition) {
    	
    	LinearLayout childLayout = null;
    	childLayout = childLayouts.get(groupPosition);
    	return childLayout;
    }
    public TextView getGenericView() {
    	
    			    // Layout parameters for the ExpandableListView
    		    AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 64);
    		 
    		    TextView textView = new TextView(context);
    		    textView.setLayoutParams(lp);
    		    // Center the text vertically
    		    textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
    		    // Set the text starting position
    		    textView.setPadding(45, 0, 0, 0);
    		    
    		
    		    return textView;
  	  }
    
    public void sellectAllPrevious() {
    	
    	for(int i=0; i < previousInvites.size(); i++) {
    		CheckBox cb = previousInvites.get(i);
    		cb.setChecked(true);
    	}
    }
    
    public void unSelectAllPrevious() {
    	
    	context.runOnUiThread(new Runnable(){

			@Override
			public void run() {
				for(int i=0; i < previousInvites.size(); i++) {
		    		CheckBox cb = previousInvites.get(i);
		    		cb.setChecked(false);
		    	}		
			}});
    	
    	
    	
    }
    public List<String> getAllPreviousSelectedInvites() {
    	
    	List<String> pi = new ArrayList<String>();
    	for (int i=0; i < previousInvites.size();i++) {
    		CheckBox cb = previousInvites.get(i);
    		
    		if (cb.isChecked()) {
    			pi.add(cb.getText().toString());
    		}
    	}
    	return pi;
    }
    
  
    
   
}