package com.github.eclipsethemes.theme.models;

import java.util.Optional;

public final class Token {
	private final TokenKey key;
	private final Color color;
	private final Optional<TokenOptions> options;

	public Token(TokenKey key, Color color, TokenOptions options) {
		this.key = key;
		this.color = color;
		this.options = Optional.ofNullable(options);
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
