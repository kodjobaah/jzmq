package com.watamidoing.nativecamera;

import com.watamidoing.view.WhatAmIdoing;

public class NativeCommunicator implements NativeCallback {

	private WhatAmIdoing activity;

	public NativeCommunicator(WhatAmIdoing activity) {
		this.activity = activity;
	}

}
