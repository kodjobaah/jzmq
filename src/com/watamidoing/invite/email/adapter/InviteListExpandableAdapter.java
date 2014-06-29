package com.watamidoing.invite.email.adapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.watamidoing.invite.email.model.Invite;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InviteListExpandableAdapter extends BaseExpandableListAdapter {

	private Activity context;
	
	private ArrayList <LinearLayout> childLayouts = new ArrayList<LinearLayout>();
	private List<CheckBox> previousInvites;
	private List<Invite> invites;

	private List<Invite> originalList;

	public InviteListExpandableAdapter(Activity context, List<Invite> originalList, List<Invite>invites) {
		this.context = context;
		this.originalList = originalList;
		this.invites = invites;
		buildChildren(invites);
	}

	public void buildChildren(List<Invite> childInvites) {
		previousInvites = new ArrayList<CheckBox>();
		for(Invite invite: childInvites) {
			LinearLayout childLayout = new LinearLayout(context);
			CheckBox checkBox = new CheckBox(context);
			checkBox.setText(invite.getEmail());
			previousInvites.add(checkBox);
			childLayout.addView(checkBox);
			childLayouts.add(childLayout);

		}
	
	}
	public Object getChild(int groupPosition, int childPosition) {
		return invites.get(childPosition).getEmail();
	}

	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	public View getChildView(final int groupPosition, final int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		return childLayouts.get(groupPosition);
	}

	public int getChildrenCount(int groupPosition) {
		return 1;
	}


	public int getGroupCount() {
		return invites.size();
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
	public Object getGroup(int groupPosition) {
		Invite invite = invites.get(groupPosition);
		String firstName = invite.getFirstName();
		String lastName = invite.getLastName();
		
		String groupName = "";
		
		if (firstName != null) {
			groupName=" "+firstName;
		}
		
		if (lastName != null) {
			groupName = groupName + " "+ lastName;
		}
		
		if ((firstName == null) && (lastName == null)) {
			groupName = "Not Provided";
		}
		
		return groupName;
	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}


	public TextView getGenericView() {

		// Layout parameters for the ExpandableListView
		AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 64);
		TextView textView = new TextView(context);
		textView.setLayoutParams(lp);
		// Center the text vertically
		textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
		// Set the text starting position
		textView.setTypeface(null, Typeface.BOLD); 
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
	
	
	public List<Invite> getOriginal() {
		
		return originalList;
	}


}