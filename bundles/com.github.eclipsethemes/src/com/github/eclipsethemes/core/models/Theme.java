package com.github.eclipsethemes.core.models;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

	private final Optional<PackageMetadata> packageMetadata;
	private final Map<String, Color> authoredWorkbench;

	/** The palette role names a package's {@code <workbench>} section may author. */
	public static final Set<String> WORKBENCH_ROLES = Set.of("background", "foreground", "surface", "surfaceRaised",
			"border", "mutedForeground", "selection", "accent");

	/** A legacy theme: no package metadata and no authored workbench colors. */
	public Theme(String id, String name, String author, String website, String description, ThemeType type,
			Map<TokenKey, Token> tokens) {
		this(id, name, author, website, description, type, tokens, null, Map.of());
	}

	/** A package theme; {@code authoredWorkbench} keys must be {@link #WORKBENCH_ROLES}. */
	public Theme(String id, String name, String author, String website, String description, ThemeType type,
			Map<TokenKey, Token> tokens, PackageMetadata packageMetadata, Map<String, Color> authoredWorkbench) {
		this.packageMetadata = Optional.ofNullable(packageMetadata);
		this.authoredWorkbench = Map.copyOf(Objects.requireNonNull(authoredWorkbench, "authoredWorkbench"));
		for (String role : this.authoredWorkbench.keySet()) {
			if (!WORKBENCH_ROLES.contains(role)) {
				throw new IllegalArgumentException("Unknown workbench role: " + role);
			}
		}
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

	/** Raw XML ect-id, retained for legacy lookup and preview, not logical identity; absent for packages. */
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

	/** Present only for a theme read from a package. */
	public Optional<PackageMetadata> getPackageMetadata() {
		return this.packageMetadata;
	}

	/** Workbench palette colors a package authored, keyed by role name; empty for legacy themes. */
	public Map<String, Color> getAuthoredWorkbench() {
		return this.authoredWorkbench;
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
