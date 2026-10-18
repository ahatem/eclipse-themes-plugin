package com.github.eclipsethemes.core.parser;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.eclipsethemes.core.validation.ThemeDiagnostic;

public final class ParserFactory {
	private static final ThemeParser XML_PARSER = new XmlThemeParser();

	private ParserFactory() {
	}

	/** Internal repository boundary for bounded validation findings. */
	public static Optional<ThemeParser> getParserFor(String fileName, Consumer<ThemeDiagnostic> diagnosticSink) {
		return getParserFor(fileName).map(parser -> new XmlThemeParser(diagnosticSink));
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
