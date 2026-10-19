package com.github.eclipsethemes.core.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.github.eclipsethemes.core.models.PackageMetadata;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeFonts;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.TokenOptions;

/**
 * Writes a {@link Theme} as a version 1 theme package. The output is deterministic and contains only what the theme
 * states: no installed path, no catalog identity and nothing derived. Fonts and authored workbench colors are written
 * only when the theme has them.
 *
 * <p>
 * A legacy theme has no package id, so one is generated from its display name: {@code legacy.} followed by the name
 * in lower case with every run of characters other than letters and digits replaced by one hyphen ({@code theme} when
 * nothing is left), cut to the 128 characters a package id allows.
 */
public final class ThemePackageWriter {

	static final String LEGACY_ID_PREFIX = "legacy.";
	private static final List<String> WORKBENCH_ORDER = List.of("background", "foreground", "surface", "surfaceRaised",
			"border", "mutedForeground", "selection", "accent");

	private ThemePackageWriter() {
	}

	/** The package id to write: the package's own claim, or the generated legacy id. */
	public static String packageIdFor(Theme theme) {
		return theme.getPackageMetadata().map(PackageMetadata::packageId)
				.orElseGet(() -> legacyPackageId(theme.getName()));
	}

	static String legacyPackageId(String name) {
		String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
		if (slug.isEmpty()) {
			slug = "theme";
		}
		String id = LEGACY_ID_PREFIX + slug;
		if (id.length() > ThemePackageParser.MAX_ID_LENGTH) {
			id = id.substring(0, ThemePackageParser.MAX_ID_LENGTH).replaceAll("-+$", "");
		}
		return id;
	}

	public static String write(Theme theme) {
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<themePackage schemaVersion=\"1\"");
		attribute(xml, "id", packageIdFor(theme));
		attribute(xml, "name", theme.getName());
		attribute(xml, "appearance", theme.getType().name().toLowerCase(Locale.ROOT));
		optional(xml, "author", theme.getAuthor());
		optional(xml, "description", theme.getDescription());
		optional(xml, "website", theme.getWebsite());
		theme.getPackageMetadata().ifPresent(metadata -> optional(xml, "version", metadata.version()));
		xml.append(">\n");

		theme.getPackageMetadata().ifPresent(metadata -> provenance(xml, metadata));

		xml.append("  <editor>\n");
		for (Token token : orderedTokens(theme)) {
			xml.append("    <").append(token.getTokenName());
			attribute(xml, "color", token.getColor().toString());
			token.getOptions().ifPresent(options -> flags(xml, options));
			xml.append("/>\n");
		}
		xml.append("  </editor>\n");

		if (!theme.getAuthoredWorkbench().isEmpty()) {
			xml.append("  <workbench>\n");
			for (String role : WORKBENCH_ORDER) {
				Optional.ofNullable(theme.getAuthoredWorkbench().get(role)).ifPresent(color -> {
					xml.append("    <").append(role);
					attribute(xml, "color", color.toString());
					xml.append("/>\n");
				});
			}
			xml.append("  </workbench>\n");
		}

		ThemeFonts fonts = theme.getFonts();
		if (!fonts.isEmpty()) {
			xml.append("  <fonts>\n");
			fonts.editorFont().ifPresent(face -> face(xml, "editor", face));
			fonts.workbenchFont().ifPresent(face -> face(xml, "workbench", face));
			xml.append("  </fonts>\n");
		}
		xml.append("</themePackage>\n");
		return xml.toString();
	}

	private static List<Token> orderedTokens(Theme theme) {
		for (TokenKey required : new TokenKey[] { TokenKey.BACKGROUND, TokenKey.FOREGROUND }) {
			if (!theme.getTokens().containsKey(required)) {
				throw new IllegalArgumentException("The theme has no " + required.getName()
						+ " color, which a theme package requires");
			}
		}
		List<Token> tokens = new ArrayList<>(theme.getTokens().values());
		// Background and foreground first, then the rest by name, so the same theme always writes the same file.
		tokens.sort(Comparator.comparingInt((Token token) -> token.getKey().equals(TokenKey.BACKGROUND) ? 0
				: token.getKey().equals(TokenKey.FOREGROUND) ? 1 : 2).thenComparing(Token::getTokenName));
		return tokens;
	}

	private static void provenance(StringBuilder xml, PackageMetadata metadata) {
		if (metadata.license() == null && metadata.copyright() == null && metadata.source() == null) {
			return;
		}
		xml.append("  <provenance");
		optional(xml, "license", metadata.license());
		optional(xml, "copyright", metadata.copyright());
		optional(xml, "source", metadata.source());
		xml.append("/>\n");
	}

	/** True flags are written; a token whose style flags were all explicitly off keeps that with one false flag. */
	private static void flags(StringBuilder xml, TokenOptions options) {
		if (!options.bold() && !options.italic() && !options.underline() && !options.strikethrough()) {
			attribute(xml, "bold", "false");
			return;
		}
		if (options.bold()) {
			attribute(xml, "bold", "true");
		}
		if (options.italic()) {
			attribute(xml, "italic", "true");
		}
		if (options.underline()) {
			attribute(xml, "underline", "true");
		}
		if (options.strikethrough()) {
			attribute(xml, "strikethrough", "true");
		}
	}

	private static void face(StringBuilder xml, String entry, ThemeFonts.Face face) {
		xml.append("    <").append(entry);
		attribute(xml, "family", face.family());
		attribute(xml, "size", Integer.toString(face.size()));
		xml.append("/>\n");
	}

	private static void optional(StringBuilder xml, String name, String value) {
		if (value != null && !value.isBlank()) {
			attribute(xml, name, value);
		}
	}

	private static void attribute(StringBuilder xml, String name, String value) {
		xml.append(' ').append(name).append("=\"");
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '&' -> xml.append("&amp;");
			case '<' -> xml.append("&lt;");
			case '>' -> xml.append("&gt;");
			case '"' -> xml.append("&quot;");
			case '\n' -> xml.append("&#10;");
			case '\r' -> xml.append("&#13;");
			case '\t' -> xml.append("&#9;");
			default -> xml.append(c);
			}
		}
		xml.append('"');
	}
}
