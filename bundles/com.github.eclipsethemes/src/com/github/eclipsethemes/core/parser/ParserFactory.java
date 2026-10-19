package com.github.eclipsethemes.core.parser;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.github.eclipsethemes.core.validation.ThemeDiagnostic;

/** Chooses the parser by final file extension alone; content is never sniffed. */
public final class ParserFactory {
	public static final String LEGACY_EXTENSION = ".xml";
	public static final String PACKAGE_EXTENSION = ".ectheme";
	/** Every extension that discovery, import and update accept. */
	public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(LEGACY_EXTENSION, PACKAGE_EXTENSION);

	private static final ThemeParser XML_PARSER = new XmlThemeParser();
	private static final ThemeParser PACKAGE_PARSER = new ThemePackageParser();

	private ParserFactory() {
	}

	/** Internal repository boundary for bounded validation findings. */
	public static Optional<ThemeParser> getParserFor(String fileName, Consumer<ThemeDiagnostic> diagnosticSink) {
		return extensionOf(fileName).map(extension -> extension.equals(PACKAGE_EXTENSION)
				? new ThemePackageParser(diagnosticSink)
				: new XmlThemeParser(diagnosticSink));
	}

	public static Optional<ThemeParser> getParserFor(String fileName) {
		return extensionOf(fileName).map(extension -> extension.equals(PACKAGE_EXTENSION) ? PACKAGE_PARSER : XML_PARSER);
	}

	/** The supported extension {@code fileName} ends with, compared case-insensitively in the root locale. */
	public static Optional<String> extensionOf(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return Optional.empty();
		}
		String lower = fileName.toLowerCase(Locale.ROOT);
		return SUPPORTED_EXTENSIONS.stream().filter(lower::endsWith).findFirst();
	}
}
