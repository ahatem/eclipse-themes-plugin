package com.github.eclipsethemes.core.models;

public record TokenOptions(boolean bold, boolean italic, boolean underline, boolean strikethrough) {

	public static final TokenOptions EMPTY = new TokenOptions(false, false, false, false);

	public static TokenOptions empty() {
		return EMPTY;
	}

}
