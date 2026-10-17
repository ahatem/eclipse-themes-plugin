package com.github.eclipsethemes.core.models;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Theme {

	private final String id;
	private final String logicalId;
	private final String name;
	private final String author;
	private final String website;
	private final String description;

	private final Optional<File> file;
	private final ThemeType type;

	private final Map<TokenKey, Token> tokens;

	public Theme(String id, String name, String author, String website, String description, File file, ThemeType type) {
		this(null, id, name, author, website, description, file, type);
	}

	private Theme(String logicalId, String id, String name, String author, String website, String description, File file, ThemeType type) {
		this.logicalId = logicalId;
		this.id = id;
		this.name = name;
		this.author = author;
		this.website = website;
		this.description = description;

		this.file = Optional.ofNullable(file);
		this.type = type;

		this.tokens = new HashMap<>();
	}

	/** Raw XML ect-id, retained for legacy lookup and preview, not logical identity. */
	public String getId() {
		return this.id;
	}

	/** Null for parser-only candidates; assigned by the repository on admission. */
	public String getLogicalId() {
		return logicalId;
	}

	public Theme withLogicalId(String logicalId) {
		if (logicalId == null || logicalId.isBlank()) throw new IllegalArgumentException("Logical identity is required");
		Theme established = new Theme(logicalId, id, name, author, website, description, file.orElse(null), type);
		established.tokens.putAll(tokens);
		return established;
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
		return Collections.unmodifiableMap(tokens);
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
	    return String.format("%-20s  ->  %s", name, file.map(File::getPath).orElse("PACKED THEME"));
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true;
	    if (!(obj instanceof Theme other)) return false;
	    return logicalId != null && logicalId.equals(other.logicalId);
	}

	@Override
	public int hashCode() {
	    return logicalId != null ? logicalId.hashCode() : System.identityHashCode(this);
	}
}
