package com.github.eclipsethemes.core.parser;

import com.github.eclipsethemes.core.exceptions.ThemeParseException;
import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.TokenBuilder;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;
import com.github.eclipsethemes.core.validation.WebsiteUri;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.SAXException;
import java.io.File;
import java.io.InputStream;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XmlThemeParser implements ThemeParser {

	private static final String COLOR_THEME = "colorTheme";

	private final Supplier<DocumentBuilderFactory> factorySupplier;
	private final Consumer<ThemeDiagnostic> diagnosticSink;

	public XmlThemeParser() {
		this(finding -> {});
	}

	/** Internal loading-boundary sink; no per-parse state is retained on the parser. */
	public XmlThemeParser(Consumer<ThemeDiagnostic> diagnosticSink) {
		this(DocumentBuilderFactory::newInstance, diagnosticSink);
	}

	XmlThemeParser(Supplier<DocumentBuilderFactory> factorySupplier) {
		this(factorySupplier, finding -> {});
	}

	private XmlThemeParser(Supplier<DocumentBuilderFactory> factorySupplier, Consumer<ThemeDiagnostic> diagnosticSink) {
		this.factorySupplier = factorySupplier;
		this.diagnosticSink = java.util.Objects.requireNonNull(diagnosticSink);
	}

	/**
	 * @param inputStream The stream to parse.
	 * @param sourceFile  The original file, which can be null.
	 */
	public Theme parse(InputStream inputStream, File sourceFile) throws ThemeParseException {
		try {
			DocumentBuilder builder = SecureXmlBuilder.create(factorySupplier);
			Document document = builder.parse(inputStream);

			Element root = document.getDocumentElement();
			String source = sourceFile == null ? null : sourceFile.getPath();
			if (!COLOR_THEME.equals(root.getTagName())) {
				throw refusal(new ThemeDiagnostic(ThemeDiagnostic.Severity.ERROR, ThemeDiagnostic.ROOT_ELEMENT,
						"Theme document root is '<" + root.getTagName() + ">'; expected <" + COLOR_THEME + ">",
						source, root.getTagName(), null));
			}
			String name = root.getAttribute("name");
			if (name.isBlank()) {
				throw refusal(new ThemeDiagnostic(ThemeDiagnostic.Severity.ERROR, ThemeDiagnostic.NAME,
						"Theme document requires a non-blank 'name' attribute on <" + COLOR_THEME + ">",
						source, COLOR_THEME, "name"));
			}
			String id = root.getAttribute("ect-id");
			String author = root.getAttribute("author");
			String website = root.getAttribute("website");
			String description = root.getAttribute("description");
			String type = root.getAttribute("type");
			if (root.hasAttribute("type")) {
				validateType(type, source);
			}
			if (root.hasAttribute("website")) {
				validateWebsite(website, source);
			}

			// PM-V7 accepted compatibility behavior: pre-populate root tokens so
			// Theme.get() always has a fallback via inheritance. These are overwritten
			// if the XML defines them explicitly. Do not change these values here.
			Map<TokenKey, Token> tokens = new LinkedHashMap<>();
			tokens.put(TokenKey.BACKGROUND,
					new TokenBuilder().setKey(TokenKey.BACKGROUND).setColor(Color.ofRgb(30, 30, 30)).build());
			tokens.put(TokenKey.FOREGROUND,
					new TokenBuilder().setKey(TokenKey.FOREGROUND).setColor(Color.ofRgb(212, 212, 212)).build());

			new LegacyTokenInterpreter(source, diagnosticSink).interpret(root, tokens);
			return new Theme(id, name, author, website, description, ThemeType.from(type), tokens);
		} catch (ThemeParseException e) {
			throw e;
		} catch (SAXException e) {
			throw new ThemeParseException("Failed to parse theme XML: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new ThemeParseException("Failed to parse theme from stream", e);
		}
	}

	private void validateType(String type, String source) {
		if (type.equalsIgnoreCase("dark") || type.equalsIgnoreCase("light")) {
			return;
		}
		warning(ThemeDiagnostic.TYPE, "Attribute 'type' value '" + type
				+ "' is not 'dark' or 'light'; the theme is classified as light",
				source, COLOR_THEME, "type");
	}

	private void validateWebsite(String website, String source) {
		ThemeDiagnostic finding = linkFinding(website, source, COLOR_THEME, "website", ThemeDiagnostic.WEBSITE);
		if (finding != null) {
			diagnosticSink.accept(finding);
		}
	}

	/** The warning for a link attribute that is not launchable, or {@code null} when it is. */
	static ThemeDiagnostic linkFinding(String value, String source, String element, String attribute, String kind) {
		WebsiteUri.Check check = WebsiteUri.check(value);
		if (check.launchable()) {
			return null;
		}
		String detail;
		switch (check.status()) {
			case BLANK:
				detail = "is blank";
				break;
			case RELATIVE:
				detail = "value '" + check.value() + "' is not an absolute URI";
				break;
			case MALFORMED:
				detail = "value '" + check.value() + "' is not a valid URI";
				break;
			case CONTROL_CHARACTERS:
				detail = "value '" + check.value() + "' contains control characters";
				break;
			case DISALLOWED_SCHEME:
				detail = "scheme '" + check.scheme() + "' is not allowed";
				break;
			default:
				detail = "value '" + check.value() + "' is not usable";
				break;
		}
		return new ThemeDiagnostic(ThemeDiagnostic.Severity.WARNING, kind, "Attribute '" + attribute + "' " + detail
				+ "; no " + attribute + " link is available", source, element, attribute);
	}

	private void warning(String kind, String message, String source, String element, String attribute) {
		diagnosticSink.accept(new ThemeDiagnostic(ThemeDiagnostic.Severity.WARNING, kind, message, source,
				element, attribute));
	}

	private static ThemeParseException refusal(ThemeDiagnostic finding) {
		return new ThemeParseException(finding.message(), null, List.of(finding));
	}
}
