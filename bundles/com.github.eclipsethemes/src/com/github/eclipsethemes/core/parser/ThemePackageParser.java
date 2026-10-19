package com.github.eclipsethemes.core.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.github.eclipsethemes.core.exceptions.ThemeParseException;
import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.PackageMetadata;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;

/**
 * Reads a version 1 theme package ({@code .ectheme}): one {@code <themePackage>} XML document under the same secure
 * XML policy as legacy themes. The {@code <editor>} section uses the legacy token rules; nothing in a package is ever
 * executed or resolved externally.
 */
public final class ThemePackageParser implements ThemeParser {

	/** Packages larger than this are refused before XML parsing. */
	public static final int MAX_BYTES = 256 * 1024;
	static final int MAX_ID_LENGTH = 128;
	static final int MAX_VERSION_LENGTH = 64;

	private static final String ROOT = "themePackage";
	private static final String PROVENANCE = "provenance";
	private static final String EDITOR = "editor";
	private static final String WORKBENCH = "workbench";
	private static final Pattern ID = Pattern.compile("[a-z0-9]+(\\.[a-z0-9][a-z0-9-]*)+");
	private static final Pattern DECIMAL = Pattern.compile("[0-9]+");
	private static final Pattern ONE = Pattern.compile("0*1");

	private final Supplier<DocumentBuilderFactory> factorySupplier;
	private final Consumer<ThemeDiagnostic> diagnosticSink;

	public ThemePackageParser() {
		this(finding -> {});
	}

	/** Internal loading-boundary sink; no per-parse state is retained on the parser. */
	public ThemePackageParser(Consumer<ThemeDiagnostic> diagnosticSink) {
		this(DocumentBuilderFactory::newInstance, diagnosticSink);
	}

	ThemePackageParser(Supplier<DocumentBuilderFactory> factorySupplier, Consumer<ThemeDiagnostic> diagnosticSink) {
		this.factorySupplier = factorySupplier;
		this.diagnosticSink = Objects.requireNonNull(diagnosticSink);
	}

	@Override
	public Theme parse(InputStream inputStream, File sourceFile) throws ThemeParseException {
		String source = sourceFile == null ? null : sourceFile.getPath();
		try {
			byte[] bytes = inputStream.readNBytes(MAX_BYTES + 1);
			if (bytes.length > MAX_BYTES) {
				throw refusal(error(ThemeDiagnostic.FILE_TOO_LARGE, "Theme package is larger than "
						+ (MAX_BYTES / 1024) + " KiB", source, null, null));
			}
			Element root = SecureXmlBuilder.create(factorySupplier).parse(new ByteArrayInputStream(bytes))
					.getDocumentElement();
			return read(root, source);
		} catch (ThemeParseException e) {
			throw e;
		} catch (SAXException e) {
			throw new ThemeParseException("Failed to parse theme package XML: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new ThemeParseException("Failed to parse theme package from stream", e);
		}
	}

	private Theme read(Element root, String source) throws ThemeParseException {
		if (!ROOT.equals(root.getTagName())) {
			throw refusal(error(ThemeDiagnostic.ROOT_ELEMENT, "Theme package root is '<" + root.getTagName()
					+ ">'; expected <" + ROOT + ">", source, root.getTagName(), null));
		}
		// The schema version is checked first: a newer version may redefine everything else.
		String schemaVersion = root.getAttribute("schemaVersion").trim();
		if (!DECIMAL.matcher(schemaVersion).matches()) {
			throw refusal(error(ThemeDiagnostic.SCHEMA_VERSION, "Theme package requires an integer 'schemaVersion'",
					source, ROOT, "schemaVersion"));
		}
		if (!ONE.matcher(schemaVersion).matches()) {
			throw refusal(error(ThemeDiagnostic.UNSUPPORTED_SCHEMA_VERSION, "Theme package schema version "
					+ schemaVersion + " is not supported; it requires a newer Eclipse Themes", source, ROOT,
					"schemaVersion"));
		}
		String packageId = root.getAttribute("id");
		if (packageId.length() > MAX_ID_LENGTH || !ID.matcher(packageId).matches()) {
			throw refusal(error(ThemeDiagnostic.PACKAGE_ID, "Theme package 'id' '" + packageId
					+ "' must be a lower-case dotted identifier of at most " + MAX_ID_LENGTH + " characters",
					source, ROOT, "id"));
		}
		String name = root.getAttribute("name");
		if (name.isBlank()) {
			throw refusal(error(ThemeDiagnostic.NAME, "Theme package requires a non-blank 'name' attribute on <"
					+ ROOT + ">", source, ROOT, "name"));
		}
		String appearance = root.getAttribute("appearance").trim().toLowerCase(Locale.ROOT);
		if (!appearance.equals("dark") && !appearance.equals("light")) {
			throw refusal(error(ThemeDiagnostic.APPEARANCE, "Theme package 'appearance' '"
					+ root.getAttribute("appearance") + "' must be 'dark' or 'light'", source, ROOT, "appearance"));
		}
		String website = root.getAttribute("website");
		if (root.hasAttribute("website")) {
			link(website, source, ROOT, "website", ThemeDiagnostic.WEBSITE);
		}
		String version = optional(root, "version");
		if (version != null && version.length() > MAX_VERSION_LENGTH) {
			warning(ThemeDiagnostic.VERSION, "Theme package 'version' is longer than " + MAX_VERSION_LENGTH
					+ " characters; it was ignored", source, ROOT, "version");
			version = null;
		}

		Map<String, List<Element>> sections = sections(root, source);
		Element provenance = single(sections, PROVENANCE, source);
		Element editor = single(sections, EDITOR, source);
		Element workbench = single(sections, WORKBENCH, source);
		if (editor == null) {
			throw refusal(error(ThemeDiagnostic.MISSING_SECTION, "Theme package requires an <" + EDITOR
					+ "> section", source, EDITOR, null));
		}

		LegacyTokenInterpreter interpreter = new LegacyTokenInterpreter(source, diagnosticSink);
		Map<TokenKey, Token> tokens = new LinkedHashMap<>();
		interpreter.interpret(editor, tokens);
		List<ThemeDiagnostic> missing = new ArrayList<>();
		for (TokenKey required : new TokenKey[] { TokenKey.BACKGROUND, TokenKey.FOREGROUND }) {
			if (!tokens.containsKey(required)) {
				missing.add(error(ThemeDiagnostic.REQUIRED_EDITOR_TOKEN, "Theme package <" + EDITOR
						+ "> requires a valid <" + required.getName() + "> token", source, required.getName(),
						"color"));
			}
		}
		if (!missing.isEmpty()) {
			throw new ThemeParseException(missing.get(0).message(), null, missing);
		}

		Map<String, Color> authored = workbench == null ? Map.of() : workbench(workbench, interpreter, source);
		PackageMetadata metadata = provenance == null
				? new PackageMetadata(packageId, version, null, null, null)
				: provenance(provenance, packageId, version, source);
		return new Theme(null, name, optional(root, "author"), website, optional(root, "description"),
				ThemeType.from(appearance), tokens, metadata, authored);
	}

	/** Groups known section elements; every other direct child is reported and ignored. */
	private Map<String, List<Element>> sections(Element root, String source) {
		Map<String, List<Element>> sections = new HashMap<>();
		for (Element child : children(root)) {
			String tag = child.getTagName();
			if (tag.equals(PROVENANCE) || tag.equals(EDITOR) || tag.equals(WORKBENCH)) {
				sections.computeIfAbsent(tag, unused -> new ArrayList<>()).add(child);
			} else {
				warning(ThemeDiagnostic.UNKNOWN_ELEMENT, "Unknown theme package element '<" + tag
						+ ">'; it was ignored", source, tag, null);
			}
		}
		return sections;
	}

	private static Element single(Map<String, List<Element>> sections, String tag, String source)
			throws ThemeParseException {
		List<Element> found = sections.getOrDefault(tag, List.of());
		if (found.size() > 1) {
			throw refusal(error(ThemeDiagnostic.DUPLICATE_SECTION, "Theme package contains more than one <" + tag
					+ "> section", source, tag, null));
		}
		return found.isEmpty() ? null : found.get(0);
	}

	private Map<String, Color> workbench(Element workbench, LegacyTokenInterpreter interpreter, String source)
			throws ThemeParseException {
		Map<String, Color> authored = new HashMap<>();
		Set<String> seen = new HashSet<>();
		for (Element role : children(workbench)) {
			String tag = role.getTagName();
			if (!Theme.WORKBENCH_ROLES.contains(tag)) {
				warning(ThemeDiagnostic.UNKNOWN_WORKBENCH_ROLE, "Unknown workbench role '<" + tag
						+ ">'; it was ignored", source, tag, null);
				continue;
			}
			if (!seen.add(tag)) {
				throw refusal(error(ThemeDiagnostic.DUPLICATE_WORKBENCH_ROLE, "Workbench role <" + tag
						+ "> is declared more than once", source, tag, null));
			}
			Color color = interpreter.color(role);
			if (color != null) {
				authored.put(tag, color);
			}
		}
		return authored;
	}

	private PackageMetadata provenance(Element provenance, String packageId, String version, String source) {
		for (Element child : children(provenance)) {
			warning(ThemeDiagnostic.UNKNOWN_ELEMENT, "Unknown provenance element '<" + child.getTagName()
					+ ">'; it was ignored", source, child.getTagName(), null);
		}
		String license = optional(provenance, "license");
		if (provenance.hasAttribute("license") && license == null) {
			warning(ThemeDiagnostic.LICENSE, "Provenance 'license' is blank; the license is not stated", source,
					PROVENANCE, "license");
		}
		String upstream = optional(provenance, "source");
		if (upstream != null) {
			link(upstream, source, PROVENANCE, "source", ThemeDiagnostic.SOURCE);
		}
		return new PackageMetadata(packageId, version, license, optional(provenance, "copyright"), upstream);
	}

	private void link(String value, String source, String element, String attribute, String kind) {
		ThemeDiagnostic finding = XmlThemeParser.linkFinding(value, source, element, attribute, kind);
		if (finding != null) {
			diagnosticSink.accept(finding);
		}
	}

	/** The attribute's value, or {@code null} when it is absent or blank. */
	private static String optional(Element element, String attribute) {
		String value = element.getAttribute(attribute);
		return value.isBlank() ? null : value;
	}

	private static List<Element> children(Element parent) {
		List<Element> children = new ArrayList<>();
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				children.add((Element) node);
			}
		}
		return children;
	}

	private void warning(String kind, String message, String source, String element, String attribute) {
		diagnosticSink.accept(new ThemeDiagnostic(ThemeDiagnostic.Severity.WARNING, kind, message, source, element,
				attribute));
	}

	private static ThemeDiagnostic error(String kind, String message, String source, String element,
			String attribute) {
		return new ThemeDiagnostic(ThemeDiagnostic.Severity.ERROR, kind, message, source, element, attribute);
	}

	private static ThemeParseException refusal(ThemeDiagnostic finding) {
		return new ThemeParseException(finding.message(), null, List.of(finding));
	}
}
