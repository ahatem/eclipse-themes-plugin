package com.github.eclipsethemes.eclipse.adapters.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/** Minimal CSS reader used to compare the generated overlay with Eclipse's own stylesheets. */
final class CssRules {

	private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
	private static final Pattern AT_RULE = Pattern.compile("@(import|define-color|charset)[^;]*;");
	private static final Pattern RULE = Pattern.compile("([^{}]+)\\{([^{}]*)\\}");
	private static final Pattern LITERAL_COLOR = Pattern.compile("#[0-9A-Fa-f]{3}(?:[0-9A-Fa-f]{3})?\\b");

	private CssRules() {
	}

	/** Maps every individual selector of a stylesheet to the properties it declares. */
	static Map<String, Set<String>> declarations(String css) {
		Map<String, Set<String>> declarations = new LinkedHashMap<>();
		Matcher rules = RULE.matcher(strip(css));
		while (rules.find()) {
			Set<String> properties = properties(rules.group(2));
			for (String selector : rules.group(1).split(",")) {
				String normalized = normalize(selector);
				if (!normalized.isEmpty()) {
					declarations.computeIfAbsent(normalized, key -> new LinkedHashSet<>()).addAll(properties);
				}
			}
		}
		return declarations;
	}

	/** Maps every individual selector to the properties it declares with a literal color. */
	static Map<String, Set<String>> colorDeclarations(String css) {
		Map<String, Set<String>> declarations = new LinkedHashMap<>();
		Matcher rules = RULE.matcher(strip(css));
		while (rules.find()) {
			Set<String> properties = new LinkedHashSet<>();
			for (String declaration : rules.group(2).split(";")) {
				int separator = declaration.indexOf(':');
				if (separator > 0 && LITERAL_COLOR.matcher(declaration.substring(separator)).find()) {
					properties.add(declaration.substring(0, separator).trim());
				}
			}
			if (properties.isEmpty()) {
				continue;
			}
			for (String selector : rules.group(1).split(",")) {
				String normalized = normalize(selector);
				if (!normalized.isEmpty()) {
					declarations.computeIfAbsent(normalized, key -> new LinkedHashSet<>()).addAll(properties);
				}
			}
		}
		return declarations;
	}

	static Set<String> literalColors(String css) {
		Set<String> colors = new LinkedHashSet<>();
		Matcher rules = RULE.matcher(strip(css));
		while (rules.find()) {
			Matcher matcher = LITERAL_COLOR.matcher(rules.group(2));
			while (matcher.find()) {
				colors.add(matcher.group().toLowerCase());
			}
		}
		// @define-color declarations live outside any rule block.
		Matcher defines = Pattern.compile("@define-color\\s+\\S+\\s+(\\S+);").matcher(css);
		while (defines.find()) {
			Matcher matcher = LITERAL_COLOR.matcher(defines.group(1));
			while (matcher.find()) {
				colors.add(matcher.group().toLowerCase());
			}
		}
		return colors;
	}

	static Set<String> paletteColors(WorkbenchPalette palette) {
		return new LinkedHashSet<>(List.of(palette.background(), palette.foreground(), palette.chrome(),
				palette.elevated(), palette.hover(), palette.input(), palette.border(), palette.muted(),
				palette.disabled(), palette.link(), palette.selectionBackground(), palette.selectionForeground()));
	}

	/** Reads a stylesheet shipped by {@code org.eclipse.ui.themes}. */
	static String platformStylesheet(String path) {
		Bundle themes = Platform.getBundle("org.eclipse.ui.themes");
		if (themes == null) {
			return "";
		}
		URL entry = themes.getEntry(path);
		if (entry == null) {
			return "";
		}
		try (InputStream stream = entry.openStream()) {
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Could not read " + path, e);
		}
	}

	private static String strip(String css) {
		return AT_RULE.matcher(COMMENT.matcher(css).replaceAll("")).replaceAll("");
	}

	private static Set<String> properties(String body) {
		Set<String> properties = new LinkedHashSet<>();
		for (String declaration : body.split(";")) {
			int separator = declaration.indexOf(':');
			if (separator > 0) {
				properties.add(declaration.substring(0, separator).trim());
			}
		}
		return properties;
	}

	private static String normalize(String selector) {
		return selector.trim().replaceAll("\\s+", " ");
	}
}
