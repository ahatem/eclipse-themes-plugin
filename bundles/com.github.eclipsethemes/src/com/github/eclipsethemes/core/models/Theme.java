package com.github.eclipsethemes.core.models;

import java.util.Map;
import java.util.Objects;

/**
 * One immutable, validated visual theme definition. Catalog identity, origin,
 * and managed storage belong to the repository record, never to this class.
 */
public final class Theme {

	private final String id;
	private final String name;
	private final String author;
	private final String website;
	private final String description;

	private final ThemeType type;

	private final Map<TokenKey, Token> tokens;

	public Theme(String id, String name, String author, String website, String description, ThemeType type,
			Map<TokenKey, Token> tokens) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.website = website;
		this.description = description;
		this.type = Objects.requireNonNull(type, "type");
		this.tokens = Map.copyOf(Objects.requireNonNull(tokens, "tokens"));
		for (Map.Entry<TokenKey, Token> entry : this.tokens.entrySet()) {
			if (!entry.getKey().equals(entry.getValue().getKey())) {
				throw new IllegalArgumentException("Token map key does not match token role: "
						+ entry.getKey().getName());
			}
		}
	}

	/** Raw XML ect-id, retained for legacy lookup and preview, not logical identity. */
	public String getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getAuthor() {
		return this.author;
	}

	public String getWebsite() {
		return this.website;
	}

	public String getDescription() {
		return this.description;
	}

	public ThemeType getType() {
		return this.type;
	}

	/** The immutable token snapshot; callers cannot add, replace, or remove tokens. */
	public Map<TokenKey, Token> getTokens() {
		return this.tokens;
	}

	public boolean has(TokenKey key) {
		return tokens.containsKey(key);
	}

	public Token get(TokenKey key) {
		if (key == null) {
			throw new NullPointerException();
		}

		TokenKey current = key;
		while (current != null) {
			Token result = tokens.get(current);
			if (result != null) return result;
			current = current.getInheritsFrom();
		}

		throw new IllegalStateException("Token chain broken — no value found for key: " + key.getName()
				+ ". Ensure BACKGROUND and FOREGROUND are always populated.");
	}

	@Override
	public String toString() {
		return name;
	}
}
