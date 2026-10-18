package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.github.eclipsethemes.core.exceptions.ThemeParseException;
import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.TokenOptions;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;
import com.github.eclipsethemes.core.validation.WebsiteUri;

/**
 * Contract of the strict-but-admitting validation policy: structural problems
 * refuse the document, everything else is reported and the theme is admitted.
 */
public class XmlThemeValidationTest {

	private static final Color DEFAULT_FOREGROUND = Color.ofRgb(212, 212, 212);

	private record Parsed(Theme theme, List<ThemeDiagnostic> findings) {}

	private static Parsed parse(String xml) throws Exception {
		List<ThemeDiagnostic> findings = new ArrayList<>();
		Theme theme = new XmlThemeParser(findings::add).parse(ParserTestSupport.xml(xml));
		return new Parsed(theme, findings);
	}

	private static String document(String body) {
		return "<colorTheme name='Validation'>" + body + "</colorTheme>";
	}

	private static ThemeParseException refused(String xml) {
		List<ThemeDiagnostic> findings = new ArrayList<>();
		try {
			Theme admitted = new XmlThemeParser(findings::add).parse(ParserTestSupport.xml(xml));
			throw new AssertionError("Document was admitted with " + describe(findings) + ": " + admitted.getName());
		} catch (ThemeParseException expected) {
			return expected;
		}
	}

	private static ThemeDiagnostic only(List<ThemeDiagnostic> findings) {
		assertEquals("exactly one finding expected but got " + describe(findings), 1, findings.size());
		return findings.get(0);
	}

	private static ThemeDiagnostic single(List<ThemeDiagnostic> findings, String kind, String element,
			String attribute) {
		ThemeDiagnostic finding = only(findings);
		assertEquals(kind, finding.kind());
		assertEquals(element, finding.element());
		assertEquals(attribute, finding.attribute());
		return finding;
	}

	private static String describe(List<ThemeDiagnostic> findings) {
		if (findings.isEmpty()) {
			return "no findings";
		}
		return findings.stream()
				.map(finding -> finding.severity() + " " + finding.kind() + " at "
						+ finding.element() + "/" + finding.attribute() + ": " + finding.message())
				.collect(Collectors.joining("; "));
	}


	// --- Structure: the only two refusal rules ---

	@Test public void wrongRootElementIsRefused() {
		ThemeParseException failure = refused("<theme name='Wrong'/>");

		ThemeDiagnostic finding = only(failure.diagnostics());
		assertTrue(finding.error());
		assertEquals(ThemeDiagnostic.ROOT_ELEMENT, finding.kind());
		assertEquals("theme", finding.element());
		assertTrue(finding.message(), finding.message().contains("<theme>"));
		assertTrue(finding.message(), finding.message().contains("<colorTheme>"));
		assertEquals("The refusal message is the finding message", finding.message(), failure.getMessage());
	}

	@Test public void missingNameIsRefused() {
		ThemeParseException failure = refused("<colorTheme><background color='#112233'/></colorTheme>");

		ThemeDiagnostic finding = single(failure.diagnostics(), ThemeDiagnostic.NAME, "colorTheme", "name");
		assertTrue(finding.error());
		assertTrue(finding.message(), finding.message().contains("'name'"));
	}

	@Test public void blankNameIsRefused() {
		ThemeParseException failure = refused("<colorTheme name='   '><background color='#112233'/></colorTheme>");

		ThemeDiagnostic finding = only(failure.diagnostics());
		assertTrue(finding.error());
		assertEquals(ThemeDiagnostic.NAME, finding.kind());
	}

	// --- Metadata: admitted with a warning, or silent ---

	@Test public void absentOptionalMetadataIsSilent() throws Exception {
		Parsed parsed = parse(document("<background color='#112233'/>"));

		assertEquals(describe(parsed.findings()), List.of(), parsed.findings());
		assertEquals("Validation", parsed.theme().getName());
		assertEquals(ThemeType.LIGHT, parsed.theme().getType());
		assertEquals(Color.ofHex("#112233"), parsed.theme().get(TokenKey.BACKGROUND).getColor());
	}

	@Test public void presentBlankTypeWarnsAndKeepsTheLightClassification() throws Exception {
		Parsed parsed = parse("<colorTheme name='Validation' type=''/>");

		ThemeDiagnostic finding = single(parsed.findings(), ThemeDiagnostic.TYPE, "colorTheme", "type");
		assertFalse(finding.error());
		assertTrue(finding.message(), finding.message().contains("classified as light"));
		assertEquals(ThemeType.LIGHT, parsed.theme().getType());
	}

	@Test public void unrecognisedTypeWarnsAndKeepsTheLightClassification() throws Exception {
		Parsed parsed = parse("<colorTheme name='Validation' type='Midnight'/>");

		ThemeDiagnostic finding = single(parsed.findings(), ThemeDiagnostic.TYPE, "colorTheme", "type");
		assertTrue(finding.message(), finding.message().contains("'Midnight'"));
		assertEquals(ThemeType.LIGHT, parsed.theme().getType());
	}

	@Test public void recognisedTypesAreAcceptedInAnyCase() throws Exception {
		assertEquals(ThemeType.DARK, parse("<colorTheme name='Validation' type='DaRk'/>").theme().getType());
		assertEquals(ThemeType.LIGHT, parse("<colorTheme name='Validation' type='LIGHT'/>").theme().getType());
		assertEquals(List.of(), parse("<colorTheme name='Validation' type='dark'/>").findings());
	}

	@Test public void legacyIdentityAttributesAreNotInterpreted() throws Exception {
		Parsed parsed = parse("<colorTheme name='Legacy' ect-id='curated' id='obsolete' modified='2020-01-01'"
				+ " unknown='anything'><background color='#112233'/></colorTheme>");

		assertEquals(describe(parsed.findings()), List.of(), parsed.findings());
		assertEquals("curated", parsed.theme().getId());
		assertEquals("Legacy", parsed.theme().getName());
	}

	// --- Token definitions: reported and ignored, never refused ---

	@Test public void tokenDefectsWarnAndFallBackToTheInheritedDefault() throws Exception {
		String[][] cases = {
				{ "<mystery color='#123456'/>", ThemeDiagnostic.UNKNOWN_ELEMENT, "mystery", null, "'<mystery>'" },
				{ "<keyword/>", ThemeDiagnostic.MISSING_COLOR, "keyword", "color", "no usable 'color'" },
				{ "<keyword color='   '/>", ThemeDiagnostic.MISSING_COLOR, "keyword", "color", "no usable 'color'" },
				{ "<keyword color='blue'/>", ThemeDiagnostic.INVALID_COLOR, "keyword", "color", "'blue'" },
				{ "<keyword color='#12345'/>", ThemeDiagnostic.INVALID_COLOR, "keyword", "color", "'#12345'" },
				{ "<keyword color='#11223344'/>", ThemeDiagnostic.INVALID_COLOR, "keyword", "color", "'#11223344'" },
		};
		for (String[] testCase : cases) {
			Parsed parsed = parse(document(testCase[0]));

			ThemeDiagnostic finding = single(parsed.findings(), testCase[1], testCase[2], testCase[3]);
			assertFalse(finding.error());
			assertTrue(finding.message(), finding.message().contains(testCase[4]));
			assertEquals("Warning never blocks and the token inherits: " + testCase[0], DEFAULT_FOREGROUND,
					parsed.theme().get(TokenKey.KEYWORD).getColor());
		}
	}

	@Test public void documentedColorFormsAreAccepted() throws Exception {
		String[][] cases = {
				{ "#123456", "#123456" },
				{ "#1A2B3C", "#1a2b3c" },
				{ "#abc", "#aabbcc" },
				{ "  #123456  ", "#123456" },
		};
		for (String[] testCase : cases) {
			Parsed parsed = parse(document("<background color='" + testCase[0] + "'/>"));

			assertEquals("No findings for '" + testCase[0] + "': " + describe(parsed.findings()), List.of(),
					parsed.findings());
			assertEquals(testCase[0], testCase[1],
					parsed.theme().get(TokenKey.BACKGROUND).getColor().toString());
		}
	}

	@Test public void nonBooleanStyleFlagsWarnAndAreTreatedAsFalse() throws Exception {
		for (String flag : List.of("bold", "italic", "underline", "strikethrough")) {
			Parsed parsed = parse(document("<keyword color='#123456' " + flag + "='yes'/>"));

			ThemeDiagnostic finding = single(parsed.findings(), ThemeDiagnostic.STYLE_FLAG, "keyword", flag);
			assertTrue(finding.message(), finding.message().contains("'yes'"));
			assertEquals("Non-boolean flags are false", TokenOptions.EMPTY,
					parsed.theme().get(TokenKey.KEYWORD).getOptions().orElse(TokenOptions.EMPTY));
		}
	}

	@Test public void styleFlagNamesAreCaseInsensitive() throws Exception {
		Parsed parsed = parse(document("<keyword color='#123456' bold='True' italic='FALSE'/>"));

		assertEquals(describe(parsed.findings()), List.of(), parsed.findings());
		assertEquals(new TokenOptions(true, false, false, false),
				parsed.theme().get(TokenKey.KEYWORD).getOptions().orElse(TokenOptions.EMPTY));
	}

	@Test public void nestedElementsWarnButTheTokenAttributesStillApply() throws Exception {
		Parsed parsed = parse(document("<keyword color='#123456'><nested/></keyword>"));

		ThemeDiagnostic finding = single(parsed.findings(), ThemeDiagnostic.NESTED_ELEMENTS, "keyword", null);
		assertTrue(finding.message(), finding.message().contains("nested elements"));
		assertEquals(Color.ofHex("#123456"), parsed.theme().get(TokenKey.KEYWORD).getColor());
	}


	// --- Website metadata: the parser and the preference page share one policy ---

	@Test public void unusableWebsitesWarnAndTheThemeIsStillAdmitted() throws Exception {
		String[][] cases = {
				{ "", "is blank" },
				{ "   ", "is blank" },
				{ "example.com", "is not an absolute URI" },
				{ "http://", "is not a valid URI" },
				{ "ftp://example.com", "scheme 'ftp' is not allowed" },
				{ "javascript:alert(1)", "scheme 'javascript' is not allowed" },
				{ "file:///etc/passwd", "scheme 'file' is not allowed" },
		};
		for (String[] testCase : cases) {
			Parsed parsed = parse("<colorTheme name='Website' website='" + testCase[0] + "'/>");

			ThemeDiagnostic finding = single(parsed.findings(), ThemeDiagnostic.WEBSITE, "colorTheme", "website");
			assertFalse(finding.error());
			assertTrue(finding.message(), finding.message().contains(testCase[1]));
			assertTrue(finding.message(), finding.message().contains("no website link is available"));
			assertFalse("No value is launchable when it is reported: " + testCase[0],
					WebsiteUri.isLaunchable(parsed.theme().getWebsite()));
			assertEquals("Website findings never refuse the document", "Website", parsed.theme().getName());
		}
	}

	@Test public void launchableWebsitesProduceNoFindings() throws Exception {
		for (String website : List.of("https://example.com", "HTTP://Example.com",
				"  https://example.com/a?b=c#d  ")) {
			Parsed parsed = parse("<colorTheme name='Website' website='" + website + "'/>");

			assertEquals("No findings for '" + website + "': " + describe(parsed.findings()), List.of(),
					parsed.findings());
			assertTrue(website, WebsiteUri.isLaunchable(parsed.theme().getWebsite()));
		}
	}

	@Test public void controlCharactersAreNeverLaunchable() {
		assertEquals(WebsiteUri.Status.CONTROL_CHARACTERS, WebsiteUri.check("http://exam\tple.com").status());
		assertEquals(WebsiteUri.Status.CONTROL_CHARACTERS, WebsiteUri.check("https://example.com/\u0007").status());
		assertFalse(WebsiteUri.isLaunchable("http://exam\nple.com"));
	}

	// --- Reading from a file must not hide the refusal ---

	@Test public void fileBasedParsingPreservesTheRefusalReasonAndFindings() throws Exception {
		Path file = Files.createTempFile("invalid-theme", ".xml");
		try {
			Files.writeString(file, "<colorTheme/>");

			ThemeParseException failure = assertThrows(ThemeParseException.class,
					() -> new XmlThemeParser().parse(file.toFile()));

			assertTrue("The refusal reason survives the file overload: " + failure.getMessage(),
					failure.getMessage().contains("'name'"));
			assertEquals("The refusing finding is attached to the failure", 1, failure.diagnostics().size());
			assertTrue(failure.diagnostics().get(0).error());
		} finally {
			Files.deleteIfExists(file);
		}
	}

	// --- The bundled corpus must stay loadable ---

	@Test public void bundledThemesNeverProduceErrors() throws Exception {
		Path themes = ParserTestSupport.themes();
		List<String> warned = new ArrayList<>();
		int checked = 0;

		try (var files = Files.list(themes)) {
			for (Path file : files.sorted().toList()) {
				if (!file.getFileName().toString().endsWith(".xml")) {
					continue;
				}
				List<ThemeDiagnostic> findings = new ArrayList<>();
				Theme theme = new XmlThemeParser(findings::add).parse(file.toFile());
				checked++;
				for (ThemeDiagnostic finding : findings) {
					assertFalse("Bundled theme " + file.getFileName() + " is refused: " + finding.message(),
							finding.error());
				}
				if (!findings.isEmpty()) {
					warned.add(theme.getName() + " (" + file.getFileName() + "): " + describe(findings));
				}
			}
		}

		System.out.println("Bundled theme validation: " + checked + " files, " + warned.size()
				+ " with warnings");
		warned.forEach(line -> System.out.println("  " + line));
		assertTrue("The bundled theme corpus is present", checked > 0);
	}
}

