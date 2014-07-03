package com.watamidoing.invite.email.callback;

import android.view.View;
import android.widget.TextView;

public interface InviteDialogInteraction {
	

	public void showInviteProgress(boolean state);
	
	public void setInviteForm(View view);

	public void setStatusView(View view);
	
	public void setInviteStatusMessage(TextView view);
}
