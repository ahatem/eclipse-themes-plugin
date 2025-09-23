package com.github.eclipsethemes.theme.models;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Theme {

	private final String id;
	private final String name;
	private final String author;
	private final String website;
	private final String description;

	private final Optional<File> file;
	private final ThemeType type;

	private final Map<TokenKey, Token> tokens;

	public Theme(String id, String name, String author, String website, String description, File file, ThemeType type) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.website = website;
		this.description = description;

		this.file = Optional.ofNullable(file);
		this.type = type;

		this.tokens = new HashMap<>();
	}

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

	public Optional<File> getFile() {
		return this.file;
	}

	public ThemeType getType() {
		return this.type;
	}

	public Map<TokenKey, Token> getTokens() {
		return tokens;
	}

	public void addToken(Token token) {
		tokens.put(token.getKey(), token);
	}

	public boolean has(TokenKey key) {
		return tokens.containsKey(key);
	}

	public Token get(TokenKey key) {
		if (key == null) {
			throw new NullPointerException();
		}

		Token result = null;
		while (result == null) {
			if (key == null) {
				return Token.BLACK;
			}

			result = tokens.get(key);
			key = key.getInheritsFrom();
		}

		return result;
	}
	
	@Override
	public String toString() {
	    return String.format("%-20s  ->  %s", name, file.map(File::getPath).orElse("PACKED THEME"));
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true;
	    if (!(obj instanceof Theme other)) return false;
	    return id != null && id.equals(other.id);
	}

	@Override
	public int hashCode() {
	    return id != null ? id.hashCode() : 0;
	}
}
