package com.whatamidoing.utils;

import com.whatamidoing.contentproviders.Authentication;

public class NotDone {

		private boolean notDone = true;
		private Authentication auth = null;

		public void setAuthentication(Authentication auth) {
			this.auth = auth;
		}

		public Authentication getAuthentication() {
			return auth;
		}

		public void setNotDone(boolean notDone) {
			this.notDone = notDone;
		}

		public boolean getNotDone() {
			return notDone;
		}

}
