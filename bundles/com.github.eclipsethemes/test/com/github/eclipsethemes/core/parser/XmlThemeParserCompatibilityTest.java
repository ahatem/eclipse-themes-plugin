package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;

import org.eclipse.core.runtime.ILog;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.models.*;

public class XmlThemeParserCompatibilityTest {
    private static Object previousActivator;

    // Only the existing warning path needs an activator. Install a test-only logger,
    // preserving/restoring the singleton; no production lifecycle changes.
    @BeforeClass public static void loggerStandIn() throws Exception {
        Field singleton = EclipseThemes.class.getDeclaredField("instance");
        singleton.setAccessible(true);
        previousActivator = singleton.get(null);
        EclipseThemes standIn = new EclipseThemes();
        Field logger = EclipseThemes.class.getDeclaredField("pluginLog");
        logger.setAccessible(true);
        logger.set(standIn, Proxy.newProxyInstance(ILog.class.getClassLoader(), new Class<?>[] { ILog.class },
                (proxy, method, args) -> null));
        singleton.set(null, standIn);
    }

    @AfterClass public static void restoreActivator() throws Exception {
        Field singleton = EclipseThemes.class.getDeclaredField("instance");
        singleton.setAccessible(true);
        singleton.set(null, previousActivator);
    }

    static ByteArrayInputStream xml(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test public void metadataStylesAliasesAndReferences() throws Exception {
        String input = "<colorTheme ect-id='42' name='A &amp; B &lt;C&gt; &quot;D&quot; &apos;E&apos; &#65; &#x263A;'"
                + " author='Author' website='https://example.invalid/' description='Description' type='dark'>"
                + "<background color='#123'/><foreground color='#456789'/>"
                + "<singleLineComment color='#abcdef' bold='true' italic='true' underline='true' strikethrough='true'/>"
                + "<staticFinalField color='#112233'/><constant color='#334455'/></colorTheme>";
        XmlThemeParser parser = new XmlThemeParser();
        Path source = Path.of("source.xml");
        for (int i = 0; i < 3; i++) {
            Theme theme = parser.parse(xml(input), source.toFile());
            assertEquals("42", theme.getId());
            assertEquals("A & B <C> \"D\" 'E' A ☺", theme.getName());
            assertEquals("Author", theme.getAuthor());
            assertEquals("https://example.invalid/", theme.getWebsite());
            assertEquals("Description", theme.getDescription());
            assertEquals(ThemeType.DARK, theme.getType());
            assertEquals(source.toFile(), theme.getFile().orElseThrow());
            assertEquals(Color.ofHex("#112233"), theme.get(TokenKey.BACKGROUND).getColor());
            assertEquals(new TokenOptions(true, true, true, true), theme.get(TokenKey.COMMENT).getOptions().orElseThrow());
            assertEquals(Color.ofHex("#334455"), theme.get(TokenKey.CONSTANT).getColor());
            assertSame(theme.get(TokenKey.FOREGROUND), theme.get(TokenKey.KEYWORD));
        }
    }

    @Test public void sparseDefaultsAndSourceEntryPoints() throws Exception {
        XmlThemeParser parser = new XmlThemeParser();
        Theme sparse = parser.parse(xml("<colorTheme name='Sparse'/>"));
        assertEquals(ThemeType.LIGHT, sparse.getType());
        assertTrue(sparse.getFile().isEmpty());
        assertEquals(Color.ofRgb(30, 30, 30), sparse.get(TokenKey.BACKGROUND).getColor());
        assertEquals(Color.ofRgb(212, 212, 212), sparse.get(TokenKey.KEYWORD).getColor());
        Path file = Files.createTempFile("safe-theme-", ".xml");
        try {
            Files.writeString(file, "<colorTheme name='File'/>");
            assertEquals(file.toFile(), parser.parse(file.toFile()).getFile().orElseThrow());
        } finally { Files.delete(file); }
    }

    @Test public void bundledCorpusMatchesApprovedMappingInterpretation() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Path themes = Path.of("themes");
        if (!Files.isDirectory(themes)) themes = Path.of("bundles/com.github.eclipsethemes/themes");
        try (var paths = Files.list(themes)) {
            var files = paths.filter(p -> p.toString().endsWith(".xml")).sorted().toList();
            assertEquals(33, files.size());
            for (Path file : files) {
                Theme theme = new XmlThemeParser().parse(file.toFile());
                StringBuilder snapshot = new StringBuilder(file.getFileName().toString()).append('\n')
                        .append(theme.getId()).append('\n').append(theme.getName()).append('\n')
                        .append(theme.getAuthor()).append('\n').append(theme.getWebsite()).append('\n')
                        .append(theme.getDescription()).append('\n').append(theme.getType()).append('\n');
                java.util.List<TokenKey> keys = new java.util.ArrayList<>();
                for (Field field : TokenKey.class.getFields()) {
                    if (field.getType() == TokenKey.class) keys.add((TokenKey) field.get(null));
                }
                for (TokenKey key : keys.stream()
                        .sorted(Comparator.comparing(TokenKey::getName)).toList()) {
                    Token token = theme.get(key);
                    snapshot.append(key.getName()).append(':').append(theme.has(key)).append(':')
                            .append(token.getColor()).append(':').append(token.getOptions()).append('\n');
                }
                digest.update(snapshot.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        String actual = HexFormat.of().formatHex(digest.digest());
        System.out.println("Bundled corpus interpretation SHA-256: " + actual);
        // Reviewed against named/source assertions and baseline projections for all 33 themes.
        // Only separated roles and descendants using the unchanged fallback chains differ.
        assertEquals("251c86d30bd40784acd92b7e92a61ab96a4635536161d29a106678bb3beeea02", actual);
    }
}
