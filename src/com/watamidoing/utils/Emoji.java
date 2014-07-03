package com.watamidoing.utils;

/**
 * Copyright 2014 www.delight.im <info@delight.im>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;

/** Lets you replace all emoticons in a text with their corresponding Unicode Emoji by calling replaceInText(...) */
public class Emoji {

	private static class ReplacementsMap extends HashMap<String,Integer> {

		private static final long serialVersionUID = 4948071414363715958L;
		private static ReplacementsMap mInstance;

		private ReplacementsMap() {
			super();
			put(":-)", 0x1F60A);
			put(":)", 0x1F60A);
			put(":-(", 0x1F61E);
			put(":(", 0x1F61E);
			put(":-D", 0x1F603);
			put(":D", 0x1F603);
			put(";-)", 0x1F609);
			put(";)", 0x1F609);
			put(":-P", 0x1F61C);
			put(":P", 0x1F61C);
			put(":-*", 0x1F618);
			put(":*", 0x1F618);
			put("<3", 0x2764);
			put(":3", 0x2764);
			put(">:[", 0x1F621);
			put(":'|", 0x1F625);
			put(":-[", 0x1F629);
			put(":'(", 0x1F62D);
			put("=O", 0x1F631);
			put("xD", 0x1F601);
			put(":')", 0x1F602);
			put(":-/", 0x1F612);
			put(":/", 0x1F612);
			put(":-|", 0x1F614);
			put(":|", 0x1F614);
			put("*_*", 0x1F60D);
		}

		public static ReplacementsMap getInstance() {
			if (mInstance == null) {
				mInstance = new ReplacementsMap();
			}
			return mInstance;
		}

	}

	private static String getUnicodeChar(int codepoint) {
		return new String(Character.toChars(codepoint));
	}

	private static String replaceEmoticon(String text, String emoticon) {
		ReplacementsMap replacements = ReplacementsMap.getInstance();
		Integer codepoint = replacements.get(emoticon);
		if (codepoint == null) {
			return text;
		}
		else {
			String unicodeChar = getUnicodeChar(codepoint.intValue());
			return text.replace(emoticon, unicodeChar);
		}
	}

	/**
	 * Replaces all emoticons in the given text with their corresponding Unicode Emoji
	 * 
	 * @param text the String to search and replace in
	 * @return the new String containing the Unicode Emoji
	 */
	public static String replaceInText(String text) {
		ReplacementsMap replacements = ReplacementsMap.getInstance();
		for (String emoticon : replacements.keySet()) {
			text = replaceEmoticon(text, emoticon);
		}
		return text;
	}

}
