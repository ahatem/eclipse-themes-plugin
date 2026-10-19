package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.github.eclipsethemes.core.exceptions.ThemeParseException;
import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;

public class ThemePackageParserTest {

    private static final String EDITOR = "<editor><background color='#1b1f2a'/><foreground color='#c8d0e0'/></editor>";

    private static String pkg(String rootAttributes, String body) {
        return "<themePackage " + rootAttributes + ">" + body + "</themePackage>";
    }

    private static String minimal(String body) {
        return pkg("schemaVersion='1' id='io.github.example.nightfall' name='Nightfall' appearance='dark'", body);
    }

    private static Theme parse(String input, List<ThemeDiagnostic> findings) throws ThemeParseException {
        return new ThemePackageParser(findings::add).parse(ParserTestSupport.xml(input));
    }

    private static ThemeDiagnostic refusal(String input) {
        ThemeParseException failure = assertThrows(ThemeParseException.class, () -> parse(input, new ArrayList<>()));
        assertFalse("A refusal carries its finding", failure.diagnostics().isEmpty());
        assertTrue(failure.diagnostics().get(0).error());
        return failure.diagnostics().get(0);
    }

    private static List<String> kinds(List<ThemeDiagnostic> findings) {
        return findings.stream().map(ThemeDiagnostic::kind).toList();
    }

    @Test public void completePackageIsRead() throws Exception {
        List<ThemeDiagnostic> findings = new ArrayList<>();
        Theme theme = parse(pkg("schemaVersion='1' id='io.github.example.nightfall' name='Nightfall' appearance='Dark'"
                + " author='Jane' description='Cool' version='2.1' website='https://example.org'",
                "<provenance license='MIT' copyright='Original Designer' source='https://github.com/example/n'/>"
                + EDITOR + "<workbench><surface color='#222838'/><accent color='#82aaff'/></workbench>"), findings);

        assertEquals(List.of(), findings);
        assertEquals("Nightfall", theme.getName());
        assertEquals(ThemeType.DARK, theme.getType());
        assertEquals("Jane", theme.getAuthor());
        assertEquals("Cool", theme.getDescription());
        assertEquals("https://example.org", theme.getWebsite());
        assertNull("A package has no legacy source id", theme.getId());
        var metadata = theme.getPackageMetadata().orElseThrow();
        assertEquals("io.github.example.nightfall", metadata.packageId());
        assertEquals("2.1", metadata.version());
        assertEquals("MIT", metadata.license());
        assertEquals("Original Designer", metadata.copyright());
        assertEquals("https://github.com/example/n", metadata.source());
        assertEquals(Map.of("surface", Color.ofHex("#222838"), "accent", Color.ofHex("#82aaff")),
                theme.getAuthoredWorkbench());
        assertEquals(Color.ofHex("#1b1f2a"), theme.get(TokenKey.BACKGROUND).getColor());
    }

    @Test public void packageWithoutProvenanceStatesNothing() throws Exception {
        List<ThemeDiagnostic> findings = new ArrayList<>();
        var metadata = parse(minimal(EDITOR), findings).getPackageMetadata().orElseThrow();
        assertEquals(List.of(), findings);
        assertNull(metadata.license());
        assertNull(metadata.copyright());
        assertNull(metadata.source());
        assertNull(metadata.version());
    }

    @Test public void schemaVersionIsRequiredAndOnlyOneIsAccepted() {
        String rest = " id='a.b' name='N' appearance='dark'";
        assertEquals(ThemeDiagnostic.SCHEMA_VERSION, refusal(pkg(rest.trim(), EDITOR)).kind());
        assertEquals(ThemeDiagnostic.SCHEMA_VERSION, refusal(pkg("schemaVersion='abc'" + rest, EDITOR)).kind());
        assertEquals(ThemeDiagnostic.SCHEMA_VERSION, refusal(pkg("schemaVersion='1.0'" + rest, EDITOR)).kind());
        ThemeDiagnostic newer = refusal(pkg("schemaVersion='2'" + rest, EDITOR));
        assertEquals(ThemeDiagnostic.UNSUPPORTED_SCHEMA_VERSION, newer.kind());
        assertTrue(newer.message(), newer.message().contains("newer"));
        // A newer version is refused before anything else is judged, even content v1 would reject.
        assertEquals(ThemeDiagnostic.UNSUPPORTED_SCHEMA_VERSION,
                refusal(pkg("schemaVersion='99999999999999999999'", "")).kind());
    }

    @Test public void packageIdFollowsTheGrammar() throws Exception {
        for (String bad : new String[] { "", "My Theme", "nodots", "Io.github.x", "a..b", "a.-b", ".a.b",
                "a." + "b".repeat(127) }) {
            ThemeDiagnostic finding = refusal(pkg("schemaVersion='1' id='" + bad + "' name='N' appearance='dark'",
                    EDITOR));
            assertEquals(bad, ThemeDiagnostic.PACKAGE_ID, finding.kind());
            assertEquals("id", finding.attribute());
        }
        parse(pkg("schemaVersion='1' id='a.b-c.d9' name='N' appearance='light'", EDITOR), new ArrayList<>());
    }

    @Test public void nameAndAppearanceAreRequired() {
        assertEquals(ThemeDiagnostic.NAME,
                refusal(pkg("schemaVersion='1' id='a.b' name=' ' appearance='dark'", EDITOR)).kind());
        for (String appearance : new String[] { "", "auto", "high-contrast" }) {
            assertEquals(ThemeDiagnostic.APPEARANCE, refusal(pkg("schemaVersion='1' id='a.b' name='N' appearance='"
                    + appearance + "'", EDITOR)).kind());
        }
        assertEquals(ThemeDiagnostic.APPEARANCE, refusal(pkg("schemaVersion='1' id='a.b' name='N'", EDITOR)).kind());
    }

    @Test public void editorSectionIsRequiredOnceAndCompleteAtTheBase() {
        assertEquals(ThemeDiagnostic.MISSING_SECTION, refusal(minimal("")).kind());
        assertEquals(ThemeDiagnostic.DUPLICATE_SECTION, refusal(minimal(EDITOR + EDITOR)).kind());
        ThemeDiagnostic missing = refusal(pkg("schemaVersion='1' id='a.b' name='N' appearance='light'",
                "<editor><foreground color='#000000'/></editor>"));
        assertEquals(ThemeDiagnostic.REQUIRED_EDITOR_TOKEN, missing.kind());
        assertEquals("background", missing.element());
        // An invalid colour does not count as supplied, and no dark default is substituted.
        ThemeParseException both = assertThrows(ThemeParseException.class, () -> parse(minimal(
                "<editor><background color='nope'/></editor>"), new ArrayList<>()));
        assertEquals(List.of("background", "foreground"),
                both.diagnostics().stream().map(ThemeDiagnostic::element).toList());
    }

    @Test public void editorSectionUsesExactlyTheLegacyTokenRules() throws Exception {
        String children = "<background color='#101010'/><foreground color='#f0f0f0'/>"
                + "<singleLineComment color='#00ff00' italic='true'/><comment color='#ff0000'/>"
                + "<javadoc color='#123456'/><keyword color='#abc' bold='maybe'/><bogus color='#000000'/>";
        List<ThemeDiagnostic> legacyFindings = new ArrayList<>();
        Theme legacy = new XmlThemeParser(legacyFindings::add)
                .parse(ParserTestSupport.xml("<colorTheme name='N'>" + children + "</colorTheme>"));
        List<ThemeDiagnostic> packageFindings = new ArrayList<>();
        Theme packaged = parse(minimal("<editor>" + children + "</editor>"), packageFindings);

        assertEquals(legacy.getTokens().keySet(), packaged.getTokens().keySet());
        for (TokenKey key : legacy.getTokens().keySet()) {
            assertEquals(key.getName(), legacy.get(key).getColor(), packaged.get(key).getColor());
            assertEquals(key.getName(), legacy.get(key).getOptions(), packaged.get(key).getOptions());
        }
        assertEquals(kinds(legacyFindings), kinds(packageFindings));
        assertEquals(Color.ofHex("#ff0000"), packaged.get(TokenKey.COMMENT).getColor());
    }

    @Test public void workbenchSectionDeclaresOptionalRoles() throws Exception {
        List<ThemeDiagnostic> findings = new ArrayList<>();
        Theme theme = parse(minimal(EDITOR + "<workbench><surfaceRaised color='#333333' bold='true'/>"
                + "<mutedForeground color='bad'/><titleBar color='#000000'/></workbench>"), findings);
        assertEquals(Map.of("surfaceRaised", Color.ofHex("#333333")), theme.getAuthoredWorkbench());
        assertEquals(List.of(ThemeDiagnostic.INVALID_COLOR, ThemeDiagnostic.UNKNOWN_WORKBENCH_ROLE), kinds(findings));

        assertEquals(ThemeDiagnostic.DUPLICATE_WORKBENCH_ROLE, refusal(minimal(EDITOR
                + "<workbench><accent color='#111111'/><accent color='#222222'/></workbench>")).kind());
        assertEquals(ThemeDiagnostic.DUPLICATE_SECTION, refusal(minimal(EDITOR + "<workbench/><workbench/>")).kind());
    }

    @Test public void provenanceIsSingleAndBlankLicenseIsNotStated() throws Exception {
        assertEquals(ThemeDiagnostic.DUPLICATE_SECTION, refusal(minimal(EDITOR + "<provenance/><provenance/>")).kind());
        List<ThemeDiagnostic> findings = new ArrayList<>();
        var metadata = parse(minimal(EDITOR + "<provenance license=' ' source='file:///etc/passwd'/>"), findings)
                .getPackageMetadata().orElseThrow();
        assertNull(metadata.license());
        assertEquals("file:///etc/passwd", metadata.source());
        assertEquals(List.of(ThemeDiagnostic.LICENSE, ThemeDiagnostic.SOURCE), kinds(findings));
        assertEquals("source", findings.get(1).attribute());
    }

    @Test public void unknownContentIsHandledDeterministically() throws Exception {
        List<ThemeDiagnostic> findings = new ArrayList<>();
        parse(pkg("schemaVersion='1' id='a.b' name='N' appearance='dark' futureAttribute='x'",
                "<fonts><editor-font family='Mono'/></fonts>" + EDITOR), findings);
        assertEquals(List.of(ThemeDiagnostic.UNKNOWN_ELEMENT), kinds(findings));
        assertEquals("editor-font", findings.get(0).element());
    }

    @Test public void overlongVersionIsDropped() throws Exception {
        List<ThemeDiagnostic> findings = new ArrayList<>();
        Theme theme = parse(pkg("schemaVersion='1' id='a.b' name='N' appearance='dark' version='" + "9".repeat(65)
                + "'", EDITOR), findings);
        assertNull(theme.getPackageMetadata().orElseThrow().version());
        assertEquals(List.of(ThemeDiagnostic.VERSION), kinds(findings));
    }

    @Test public void wrongRootIsNotReinterpreted() {
        ThemeDiagnostic finding = refusal("<colorTheme name='Legacy'><background color='#000000'/></colorTheme>");
        assertEquals(ThemeDiagnostic.ROOT_ELEMENT, finding.kind());
        assertEquals("colorTheme", finding.element());
    }

    @Test public void oversizedPackageIsRefusedBeforeParsing() {
        XmlProviderSpy provider = new XmlProviderSpy();
        byte[] large = (minimal(EDITOR) + " ".repeat(ThemePackageParser.MAX_BYTES)).getBytes(StandardCharsets.UTF_8);
        ThemeParseException failure = assertThrows(ThemeParseException.class,
                () -> new ThemePackageParser(() -> provider, finding -> {}).parse(new ByteArrayInputStream(large)));
        assertEquals(ThemeDiagnostic.FILE_TOO_LARGE, failure.diagnostics().get(0).kind());
        assertEquals("No XML parse is attempted", 0, provider.parses);
    }

    @Test public void packagesUseTheSecureXmlPolicy() throws Exception {
        var canary = Files.createTempFile("xxe-package-", ".xml");
        try (LoopbackCanary network = new LoopbackCanary()) {
            Files.writeString(canary, "<background color='#13579b'/>");
            for (String declaration : List.of(
                    "<!DOCTYPE themePackage>",
                    "<!DOCTYPE themePackage [<!ENTITY x SYSTEM '" + canary.toUri() + "'>]>",
                    "<!DOCTYPE themePackage SYSTEM '" + network.url() + "'>",
                    "<!DOCTYPE themePackage [<!ENTITY % x SYSTEM '" + network.url() + "'>%x;]>")) {
                String input = declaration + minimal("<editor>" + (declaration.contains("ENTITY x") ? "&x;" : "")
                        + "<background color='#000000'/><foreground color='#ffffff'/></editor>");
                XmlProviderSpy provider = new XmlProviderSpy();
                assertThrows(declaration, ThemeParseException.class, () -> new ThemePackageParser(() -> provider,
                        finding -> {}).parse(ParserTestSupport.xml(input)));
                assertEquals(declaration, 0, provider.resolutions);
            }
            assertEquals(0, network.connections.get());
        } finally {
            Files.delete(canary);
        }
    }

    @Test public void factoryDispatchesByExtensionAlone() {
        assertTrue(ParserFactory.getParserFor("Nightfall.ECTHEME").orElseThrow() instanceof ThemePackageParser);
        assertTrue(ParserFactory.getParserFor("theme.XML").orElseThrow() instanceof XmlThemeParser);
        assertTrue(ParserFactory.getParserFor("x.ectheme", f -> {}).orElseThrow() instanceof ThemePackageParser);
        assertTrue(ParserFactory.getParserFor("x.zip").isEmpty());
        assertTrue(ParserFactory.getParserFor("ectheme").isEmpty());
        // A package document under .xml meets the legacy root refusal.
        assertThrows(ThemeParseException.class, () -> ParserFactory.getParserFor("p.xml").orElseThrow()
                .parse(ParserTestSupport.xml(minimal(EDITOR))));
    }
}
