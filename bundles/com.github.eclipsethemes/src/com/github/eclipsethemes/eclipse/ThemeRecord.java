package com.github.eclipsethemes.eclipse;

import java.util.Objects;
import java.util.Optional;

import com.github.eclipsethemes.core.models.Theme;

/**
 * One catalog entry: logical identity, explicit origin, an optional
 * repository-relative locator for installed themes, and the immutable theme.
 */
public record ThemeRecord(String logicalId, ThemeOrigin origin, Optional<String> locator, Theme theme) {

	public ThemeRecord {
		if (logicalId == null || logicalId.isBlank()) {
			throw new IllegalArgumentException("Logical identity is required");
		}
		origin = Objects.requireNonNull(origin, "origin");
		locator = Objects.requireNonNull(locator, "locator");
		theme = Objects.requireNonNull(theme, "theme");
		if (origin == ThemeOrigin.BUNDLED && locator.isPresent()) {
			throw new IllegalArgumentException("Bundled themes carry no managed locator: " + logicalId);
		}
		if (origin == ThemeOrigin.INSTALLED && (locator.isEmpty() || locator.get().isBlank())) {
			throw new IllegalArgumentException("Installed themes require a managed locator: " + logicalId);
		}
	}

	/** Logical identity prefix of bundled themes; the remainder is the curated source id. */
	public static final String BUNDLED_PREFIX = "bundled:";

	/**
	 * The legacy source id used for pre-logical-identity selections and the online preview: the curated id of a bundled
	 * record (packages carry none of their own), the theme's own source id otherwise. Null or blank when there is none.
	 */
	public String sourceId() {
		return origin == ThemeOrigin.BUNDLED && logicalId.startsWith(BUNDLED_PREFIX)
				? logicalId.substring(BUNDLED_PREFIX.length())
				: theme.getId();
	}

	public static ThemeRecord bundled(String logicalId, Theme theme) {
		return new ThemeRecord(logicalId, ThemeOrigin.BUNDLED, Optional.empty(), theme);
	}

	public static ThemeRecord installed(String logicalId, String locator, Theme theme) {
		return new ThemeRecord(logicalId, ThemeOrigin.INSTALLED, Optional.of(locator), theme);
	}

	/** Catalog identity equality: two records for the same logical theme are equal. */
	@Override
	public boolean equals(Object other) {
		return this == other || other instanceof ThemeRecord record && logicalId.equals(record.logicalId);
	}

	@Override
	public int hashCode() {
		return logicalId.hashCode();
	}
}
