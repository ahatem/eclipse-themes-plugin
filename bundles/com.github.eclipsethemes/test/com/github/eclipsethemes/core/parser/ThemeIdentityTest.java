package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.net.URL;

import org.eclipse.core.runtime.Plugin;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.eclipse.ThemeRecord;
import com.github.eclipsethemes.eclipse.ThemeRepository;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.Color;

public class ThemeIdentityTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void parserCandidatesDoNotShareIdentityFromMissingOrClaimedSourceIds() throws Exception {
        for (String attribute : List.of("", " ect-id=''", " ect-id='same-source'")) {
            var parser = new XmlThemeParser();
            var first = parser.parse(ParserTestSupport.xml("<colorTheme name='First'" + attribute + "/>"));
            var second = parser.parse(ParserTestSupport.xml("<colorTheme name='Second'" + attribute + "/>"));
            assertNotEquals("Unestablished candidates must not share logical identity: " + attribute, first, second);
            assertEquals(first, first);
        }
    }

    @Test public void independentStoredRecordsSurviveDuplicateNamesAndSourceClaims() throws Exception {
        var directory = temporary.newFolder().toPath();
        var installed = Files.createDirectory(directory.resolve("installed_themes"));
        for (int i = 0; i < 4; i++) {
            Files.writeString(installed.resolve(i + ".xml"), "<colorTheme name='Same'"
                    + (i < 2 ? "" : " ect-id='same-source'") + "/>");
        }
        try (var logger = ParserTestSupport.installLogger(ParserTestSupport.quietLog())) {
            bundled(List.of());
            var repository = new ThemeRepository(directory.toFile(), ParserTestSupport.quietLog());
            repository.reload();
            assertEquals("All independent records remain selectable", 4, repository.getAllThemes().size());
            assertEquals("No source-ID based equality collapse", 4, new HashSet<>(repository.getAllThemes()).size());
            List<ThemeRecord> first = repository.getAllThemes();
            var reconstructed = new ThemeRepository(directory.toFile(), ParserTestSupport.quietLog());
            reconstructed.reload();
            assertEquals(first, reconstructed.getAllThemes());
            List<String> ids = first.stream().map(ThemeRecord::logicalId).toList();
            assertEquals(ids.stream().sorted().toList(), ids);
            for (ThemeRecord theme : first) {
                assertTrue(theme.logicalId().startsWith("local:"));
                Path file = installed.resolve(theme.locator().orElseThrow());
                assertEquals("XML remains unchanged", "<colorTheme name='Same'"
                        + (file.getFileName().toString().charAt(0) < '2' ? "" : " ect-id='same-source'")
                        + "/>", Files.readString(file));
            }
        }
    }

    @Test public void establishedRecordSurvivesNameSourceClaimAndContentChanges() throws Exception {
        var directory = temporary.newFolder().toPath();
        var installed = Files.createDirectory(directory.resolve("installed_themes"));
        var xml = installed.resolve("stored.xml");
        Files.writeString(xml, "<colorTheme name='Old' ect-id='old-source'><background color='#112233'/></colorTheme>");
        try (var logger = ParserTestSupport.installLogger(ParserTestSupport.quietLog())) {
            bundled(List.of());
            var repository = new ThemeRepository(directory.toFile(), ParserTestSupport.quietLog());
            repository.reload();
            ThemeRecord old = repository.getAllThemes().get(0);
            Files.writeString(xml, "<colorTheme name='New' ect-id='new-source'><background color='#445566'/></colorTheme>");
            repository = new ThemeRepository(directory.toFile(), ParserTestSupport.quietLog());
            repository.reload();
            ThemeRecord changed = repository.getAllThemes().get(0);
            assertEquals(old, changed);
            assertEquals(old.hashCode(), changed.hashCode());
            assertEquals("New", changed.theme().getName());
            assertEquals("new-source", changed.theme().getId());
            assertEquals(Color.ofHex("#445566"), changed.theme().get(TokenKey.BACKGROUND).getColor());
            assertEquals(Color.ofHex("#112233"), old.theme().get(TokenKey.BACKGROUND).getColor());
        }
    }

    @Test public void bundledAndInstalledClaimsUseSeparateAuthorityAndRetainCuratedIds() throws Exception {
        var directory = temporary.newFolder().toPath();
        var installed = Files.createDirectory(directory.resolve("installed_themes"));
        var resource = directory.resolve("bundled.xml");
        String xml = "<colorTheme name='Same' ect-id='curated'/>";
        Files.writeString(resource, xml);
        Files.writeString(installed.resolve("copy.xml"), xml);
        try (var logger = ParserTestSupport.installLogger(ParserTestSupport.quietLog())) {
            bundled(List.of(resource.toUri().toURL()));
            var repository = new ThemeRepository(directory.toFile(), ParserTestSupport.quietLog());
            repository.reload();
            assertEquals(2, repository.getAllThemes().size());
            ThemeRecord packed = repository.getAllThemes().stream().filter(t -> t.locator().isEmpty()).findFirst().orElseThrow();
            assertEquals("bundled:curated", packed.logicalId());
            assertEquals("curated", packed.theme().getId());
            Files.writeString(resource, "<colorTheme name='Renamed' ect-id='curated'><background color='#abcdef'/></colorTheme>");
            repository.reload();
            assertEquals(packed, repository.getAllThemes().stream().filter(t -> t.locator().isEmpty()).findFirst().orElseThrow());
        }
    }

    @Test public void uppercaseXmlImportIsAcceptedUnderTurkishDefaultLocale() throws Exception {
        var previous = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
            assertTrue(ParserFactory.getParserFor("THEME.XML").isPresent());
            assertTrue(ParserFactory.getParserFor("Theme.XmL").isPresent());
            var directory = temporary.newFolder().toPath();
            var installed = Files.createDirectory(directory.resolve("installed_themes"));
            var source = directory.resolve("THEME.XML");
            Files.writeString(source, "<colorTheme name='Uppercase'/>");
            var repository = new ThemeRepository(directory.toFile(), ParserTestSupport.quietLog());
            ThemeRecord imported = repository.importTheme(source.toFile()).record();
            assertTrue(imported.logicalId().startsWith("local:"));
            Path installedFile = installed.resolve(imported.locator().orElseThrow());
            assertEquals(installed, installedFile.getParent());
            assertTrue(installedFile.getFileName().toString().matches("[0-9a-f-]{36}\\.xml"));
            try (var files = Files.list(installed)) {
                assertEquals(1, files.filter(Files::isRegularFile).count());
            }
            assertEquals("<colorTheme name='Uppercase'/>", Files.readString(source));
        } finally {
            java.util.Locale.setDefault(previous);
        }
    }

    @Test public void bundledCorpusHasCuratedIdentitiesAndImportReturnsAnAdmittedRecord() throws Exception {
        var directory = temporary.newFolder().toPath();
        List<URL> resources;
        try (var files = Files.list(ParserTestSupport.themes())) {
            resources = files.filter(p -> p.toString().endsWith(".xml")).sorted()
                    .map(path -> {
                        try { return path.toUri().toURL(); }
                        catch (java.net.MalformedURLException e) { throw new AssertionError(e); }
                    }).toList();
        }
        try (var logger = ParserTestSupport.installLogger(ParserTestSupport.quietLog())) {
            bundled(resources);
            var repository = new ThemeRepository(directory.toFile(), ParserTestSupport.quietLog());
            repository.reload();
            assertEquals(33, repository.getAllThemes().size());
            assertEquals(33, new HashSet<>(repository.getAllThemes()).size());
            for (ThemeRecord theme : repository.getAllThemes()) {
                assertEquals("bundled:" + theme.theme().getId(), theme.logicalId());
            }
            var source = directory.resolve("external.xml");
            Files.writeString(source, "<colorTheme name='Darcula' ect-id='63d6a50b-371f-4211-9025-9bfea76cc850'/>");
            ThemeRecord imported = repository.importTheme(source.toFile()).record();
            assertTrue(imported.logicalId().startsWith("local:"));
            assertTrue(repository.getAllThemes().stream().anyMatch(t -> t == imported));
            // Moving the external origin leaves the installed record and its identity untouched.
            Files.move(source, directory.resolve("moved-origin.xml"));
            repository.reload();
            assertEquals(34, repository.getAllThemes().size());
            assertEquals(1, repository.getAllThemes().stream().filter(imported::equals).count());
            assertEquals(2, repository.getAllThemes().stream().filter(t -> t.theme().getName().equals("Darcula")).count());
        }
    }

    private void bundled(List<URL> resources) throws Exception {
        Field field = Plugin.class.getDeclaredField("bundle");
        field.setAccessible(true);
        var bundle = (Bundle) Proxy.newProxyInstance(Bundle.class.getClassLoader(), new Class<?>[] {Bundle.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findEntries")) return Collections.enumeration(resources);
                    throw new AssertionError(method);
                });
        field.set(EclipseThemes.instance(), bundle);
    }
}
