package com.github.eclipsethemes.core.parser;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public final class ParserFactory {
	private static final ThemeParser XML_PARSER = new XmlThemeParser();

	private ParserFactory() {
	}

	/** Internal repository boundary for bounded mapping warnings. */
	public static Optional<ThemeParser> getParserFor(String fileName, Consumer<String> mappingWarning) {
		return getParserFor(fileName).map(parser -> new XmlThemeParser(mappingWarning));
	}

	public static Optional<ThemeParser> getParserFor(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return Optional.empty();
		}

		if (fileName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
			return Optional.of(XML_PARSER);
		}

		return Optional.empty();
	}
}
