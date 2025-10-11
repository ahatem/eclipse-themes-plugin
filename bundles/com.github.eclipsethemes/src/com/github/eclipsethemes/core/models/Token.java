package com.github.eclipsethemes.core.models;

import java.util.Optional;

public final class Token {

	public static final Token BLACK = new Token(null, Color.ofRgb(30, 30, 30), null);
	public static final Token WHITE = new Token(null, Color.ofRgb(255, 255, 255), null);

	private final TokenKey key;
	private final Color color;
	private final Optional<TokenOptions> options;

	public Token(TokenKey key, Color color, TokenOptions options) {
		this.key = key;
		this.color = color;
		this.options = Optional.ofNullable(options);
	}

	public TokenKey getKey() {
		return this.key;
	}

	public String getTokenName() {
		return this.key.getName();
	}

	public Color getColor() {
		return this.color;
	}

	public Optional<TokenOptions> getOptions() {
		return this.options;
	}
}
