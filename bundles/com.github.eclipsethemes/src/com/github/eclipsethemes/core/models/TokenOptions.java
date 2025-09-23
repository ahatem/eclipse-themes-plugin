package com.github.eclipsethemes.core.models;

public final class TokenOptions {

	private static final TokenOptions DEFAULT = new TokenOptions(false, false, false, false);

	private final boolean isBold;
	private final boolean isItalic;
	private final boolean isUnderline;
	private final boolean isStrikethrough;

	public TokenOptions(boolean isBold, boolean isItalic, boolean isUnderline, boolean isStrikethrough) {
		this.isBold = isBold;
		this.isItalic = isItalic;
		this.isUnderline = isUnderline;
		this.isStrikethrough = isStrikethrough;
	}

	public boolean isBold() {
		return this.isBold;
	}

	public boolean isItalic() {
		return this.isItalic;
	}

	public boolean isUnderline() {
		return this.isUnderline;
	}

	public boolean isStrikethrough() {
		return this.isStrikethrough;
	}

	public static TokenOptions empty() {
		return DEFAULT;
	}

}
