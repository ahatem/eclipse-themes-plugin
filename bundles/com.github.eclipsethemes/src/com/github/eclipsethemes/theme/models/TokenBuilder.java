package com.github.eclipsethemes.theme.models;

/**
 * A builder for creating immutable Token instances. This is the ideal way to
 * construct Token objects during the XML parsing phase.
 */
public final class TokenBuilder {
	private TokenKey key;
	private Color color;
	private boolean isBold = false;
	private boolean isItalic = false;
	private boolean isUnderline = false;
	private boolean isStrikethrough = false;

	// A flag to track if any style options were explicitly set.
	private boolean hasOptions = false;

	public TokenBuilder setKey(TokenKey key) {
		this.key = key;
		return this;
	}

	public TokenBuilder setColor(Color color) {
		this.color = color;
		return this;
	}

	public TokenBuilder setBold(boolean isBold) {
		this.isBold = isBold;
		this.hasOptions = true;
		return this;
	}

	public TokenBuilder setItalic(boolean isItalic) {
		this.isItalic = isItalic;
		this.hasOptions = true;
		return this;
	}

	public TokenBuilder setUnderline(boolean isUnderline) {
		this.isUnderline = isUnderline;
		this.hasOptions = true;
		return this;
	}

	public TokenBuilder setStrikethrough(boolean isStrikethrough) {
		this.isStrikethrough = isStrikethrough;
		this.hasOptions = true;
		return this;
	}

	/**
	 * Constructs the final, immutable Token.
	 * 
	 * @return A new Token instance based on the builder's state.
	 * @throws IllegalStateException if the mandatory 'key' or 'hexColor' fields
	 *                               were not set.
	 */
	public Token build() {
		if (key == null) {
			throw new IllegalStateException("Token key cannot be null.");
		}
		if (color == null) {
            throw new IllegalStateException("Token color cannot be null.");
        }

		TokenOptions options = null;
		if (hasOptions) {
			options = new TokenOptions(isBold, isItalic, isUnderline, isStrikethrough);
		}

		return new Token(key, color, options);
	}
}