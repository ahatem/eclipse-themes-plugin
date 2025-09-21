package com.github.eclipsethemes.theme.parser;

import java.util.Optional;

public final class ParserFactory {
	private static final ThemeParser XML_PARSER = new XmlThemeParser();

	private ParserFactory() {
	}

	public static Optional<ThemeParser> getParserFor(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return Optional.empty();
		}

		if (fileName.endsWith(".xml")) {
			return Optional.of(XML_PARSER);
		}

		return Optional.empty();
	}
}
