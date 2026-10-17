package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

import com.github.eclipsethemes.core.models.*;

public class XmlThemeMappingTest {
    @org.junit.ClassRule public static final org.junit.rules.ExternalResource LOGGER = ParserTestSupport.loggerRule();

    private static final String[][] ALIASES = {
        {"occurrenceIndication", "occurrence"}, {"writeOccurrenceIndication", "writeOccurrence"},
        {"deletionIndication", "removedLine"}, {"searchResultIndication", "searchResult"},
        {"filteredSearchResultIndication", "filteredSearchResult"},
        {"debugSecondaryInstructionPointer", "debugCallStack"}, {"singleLineComment", "comment"},
        {"commentTaskTag", "taskTag"}, {"javadoc", "doc"}, {"javadocTag", "docXmlTag"},
        {"javadocKeyword", "docTag"}, {"javadocLink", "docLink"}, {"parameterVariable", "argument"},
        {"typeArgument", "templateArgument"}, {"typeParameter", "templateParameter"},
        {"deprecatedMember", "deprecated"}
    };

    @Test public void canonicalWholeTokensWinOverEveryLegacyAliasInBothOrders() throws Exception {
        for (String[] alias : ALIASES) {
            String legacy = property(alias[0], "#abcdef", true);
            String canonical = "<" + alias[1] + " color='#123456'/>";
            for (String input : List.of(canonical + legacy, legacy + canonical)) {
                Theme theme = parse(input);
                assertToken(theme, alias[1], "#123456", false);
                assertTrue(theme.get(TokenKey.byId(alias[1])).getOptions().isEmpty());
            }
            assertToken(parse(legacy), alias[1], "#abcdef", true);
            assertToken(parse(legacy + "<" + alias[1] + "/><" + alias[1] + " color='invalid'/>"),
                    alias[1], "#abcdef", true);
            assertToken(parse(canonical + property(alias[1], "#abcdef", true)), alias[1], "#abcdef", true);
            assertToken(parse(legacy + property(alias[0], "#123456", false)), alias[1], "#123456", false);
        }
    }
    static Theme parse(String properties) throws Exception {
        return new XmlThemeParser().parse(ParserTestSupport.xml(
                "<colorTheme name='Mapping'>" + properties + "</colorTheme>"));
    }

    static String property(String name, String color, boolean flags) {
        return "<" + name + " color='" + color + "' bold='" + flags + "' italic='" + flags
                + "' underline='" + flags + "' strikethrough='" + flags + "'/>";
    }

    static void assertToken(Theme theme, String role, String color, boolean flags) {
        TokenKey key = TokenKey.byId(role);
        assertNotNull("Missing canonical role " + role, key);
        assertEquals(role, Color.ofHex(color), theme.get(key).getColor());
        assertEquals(role, new TokenOptions(flags, flags, flags, flags),
                theme.get(key).getOptions().orElse(TokenOptions.EMPTY));
    }

    @Test public void distinctPairsSurviveBothOrdersAndOneSidedFallbacks() throws Exception {
        for (String[] pair : List.of(new String[] {"selectionBackground", "sourceHoverBackground", "background"},
                new String[] {"comment", "xmlComment", "foreground"},
                new String[] {"constant", "staticFinalField", "foreground"})) {
            String general = property(pair[0], "#123456", true);
            String specific = property(pair[1], "#abcdef", false);
            for (String input : List.of(general + specific, specific + general)) {
                Theme theme = parse(input);
                assertToken(theme, pair[0], "#123456", true);
                assertToken(theme, pair[1], "#abcdef", false);
            }
            Theme generalOnly = parse(general);
            TokenKey specificKey = TokenKey.byId(pair[1]);
            assertFalse(generalOnly.has(specificKey));
            if (!pair[1].equals("sourceHoverBackground")) {
                assertSame(generalOnly.get(TokenKey.byId(pair[0])), generalOnly.get(specificKey));
            } else assertSame(generalOnly.get(TokenKey.BACKGROUND), generalOnly.get(specificKey));
            Theme specificOnly = parse(specific);
            assertFalse(specificOnly.has(TokenKey.byId(pair[0])));
            assertSame(specificOnly.get(TokenKey.byId(pair[2])), specificOnly.get(TokenKey.byId(pair[0])));
            Theme neither = parse("");
            assertFalse(neither.has(specificKey));
            assertSame(neither.get(TokenKey.byId(pair[2])), neither.get(specificKey));
        }
    }

    @Test public void namedBundledRegressions() throws Exception {
        Theme github = ParserTestSupport.bundled("GitHub Dark Dimmed");
        assertToken(github, "selectionBackground", "#3d4149", false);
        assertToken(github, "sourceHoverBackground", "#2d333b", false);
        assertEquals(Color.ofHex("#e0823d"), github.get(TokenKey.byId("staticFinalField")).getColor());
        assertEquals(Color.ofHex("#6cb6ff"), github.get(TokenKey.CONSTANT).getColor());
        Theme frappe = ParserTestSupport.bundled("Catppuccin Frappé");
        assertTrue(frappe.get(TokenKey.COMMENT).getOptions().orElseThrow().italic());
        assertEquals(TokenOptions.EMPTY, frappe.get(TokenKey.byId("xmlComment")).getOptions().orElse(TokenOptions.EMPTY));
        TokenOptions field = frappe.get(TokenKey.byId("staticFinalField")).getOptions().orElseThrow();
        assertTrue(field.bold());
        assertTrue(field.italic());
        assertEquals(TokenOptions.EMPTY, frappe.get(TokenKey.CONSTANT).getOptions().orElse(TokenOptions.EMPTY));
        Theme jupyter = ParserTestSupport.bundled("Jupyter");
        assertTrue(jupyter.get(TokenKey.byId("staticFinalField")).getOptions().orElseThrow().bold());
        assertTrue(jupyter.get(TokenKey.byId("staticFinalField")).getOptions().orElseThrow().italic());
    }

    @Test public void diagnosticsNameSourcesOccurrencesAndFinalWholeTokenWinner() throws Exception {
        for (String[] alias : ALIASES) {
            for (boolean reverse : List.of(false, true)) {
                java.util.List<String> warnings = new java.util.ArrayList<>();
                String canonical = property(alias[1], "#123456", false);
                String legacy = property(alias[0], "#abcdef", true);
                new XmlThemeParser(warnings::add).parse(ParserTestSupport.xml(
                        "<colorTheme>" + (reverse ? legacy + canonical : canonical + legacy) + "</colorTheme>"));
                assertEquals(1, warnings.size());
                String warning = warnings.get(0);
                assertTrue(warning.contains("role " + alias[1]));
                assertTrue(warning.contains(alias[0] + "#" + (reverse ? 1 : 2)));
                assertTrue(warning.contains("selected " + alias[1] + "#" + (reverse ? 2 : 1)));
            }
            java.util.List<String> warnings = new java.util.ArrayList<>();
            new XmlThemeParser(warnings::add).parse(ParserTestSupport.xml("<colorTheme>"
                    + "<" + alias[1] + " color='#123456'/>" + property(alias[0], "#123456", false) + "</colorTheme>"));
            assertTrue(warnings.toString(), warnings.isEmpty());
        }
        java.util.List<String> warnings = new java.util.ArrayList<>();
        new XmlThemeParser(warnings::add).parse(ParserTestSupport.xml("<colorTheme>"
                + property("comment", "#abcdef", true) + "<comment color='#123456'/>"
                + property("xmlComment", "#fedcba", true) + "</colorTheme>"));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("comment#1"));
        assertTrue(warnings.get(0).contains("selected comment#2"));
    }

    @Test public void allBundledPairsRetainExactSourceColorsAndEffectiveStyles() throws Exception {
        try (var files = Files.list(ParserTestSupport.themes())) {
            for (Path file : files.toList()) {
                Theme theme = new XmlThemeParser().parse(file.toFile());
                var document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
                for (String[] mapping : List.of(new String[] {"selectionBackground", "selectionBackground"},
                        new String[] {"sourceHoverBackground", "sourceHoverBackground"},
                        new String[] {"singleLineComment", "comment"}, new String[] {"xmlComment", "xmlComment"},
                        new String[] {"staticFinalField", "staticFinalField"}, new String[] {"constant", "constant"})) {
                    var element = (org.w3c.dom.Element) document.getElementsByTagName(mapping[0]).item(0);
                    assertNotNull(file + " " + mapping[0], element);
                    Token token = theme.get(TokenKey.byId(mapping[1]));
                    assertEquals(file + " " + mapping[1], Color.ofHex(element.getAttribute("color")), token.getColor());
                    assertEquals(file + " " + mapping[1], new TokenOptions(
                            Boolean.parseBoolean(element.getAttribute("bold")), Boolean.parseBoolean(element.getAttribute("italic")),
                            Boolean.parseBoolean(element.getAttribute("underline")), Boolean.parseBoolean(element.getAttribute("strikethrough"))),
                            token.getOptions().orElse(TokenOptions.EMPTY));
                }
            }
        }
    }
}
