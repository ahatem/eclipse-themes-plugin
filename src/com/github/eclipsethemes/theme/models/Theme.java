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

	private final Map<String, Token> tokens;

	public Theme(String id, String name, String author, String website, String description, File file, ThemeType type) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.website = website;
		this.description = description;

		this.file = Optional.of(file);
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

	public Map<String, Token> getTokens() {
		return tokens;
	}

	public void addToken(Token token) {
		tokens.put(token.getTokenName(), token);
	}
}
