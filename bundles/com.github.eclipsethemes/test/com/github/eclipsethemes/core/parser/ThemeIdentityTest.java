package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;

import org.eclipse.core.runtime.Plugin;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.eclipse.ThemeRepository;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.Color;
import org.eclipse.jface.preference.PreferenceStore;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;

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
            List<Theme> first = repository.getAllThemes();
            var reconstructed = new ThemeRepository(directory.toFile(), ParserTestSupport.quietLog());
            reconstructed.reload();
            assertEquals(first, reconstructed.getAllThemes());
            List<String> ids = first.stream().map(Theme::getLogicalId).toList();
            assertEquals(ids.stream().sorted().toList(), ids);
            for (Theme theme : first) {
                assertTrue(theme.getLogicalId().startsWith("local:"));
                assertEquals("XML remains unchanged", "<colorTheme name='Same'"
                        + (theme.getFile().orElseThrow().getName().charAt(0) < '2' ? "" : " ect-id='same-source'")
                        + "/>", Files.readString(theme.getFile().orElseThrow().toPath()));
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
            Theme old = repository.getAllThemes().get(0);
            Files.writeString(xml, "<colorTheme name='New' ect-id='new-source'><background color='#445566'/></colorTheme>");
            repository = new ThemeRepository(directory.toFile(), ParserTestSupport.quietLog());
            repository.reload();
            Theme changed = repository.getAllThemes().get(0);
            assertEquals(old, changed);
            assertEquals(old.hashCode(), changed.hashCode());
            assertEquals("New", changed.getName());
            assertEquals("new-source", changed.getId());
            assertEquals(Color.ofHex("#445566"), changed.get(TokenKey.BACKGROUND).getColor());
            assertEquals(Color.ofHex("#112233"), old.get(TokenKey.BACKGROUND).getColor());
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
            Theme packed = repository.getAllThemes().stream().filter(t -> t.getFile().isEmpty()).findFirst().orElseThrow();
            assertEquals("bundled:curated", packed.getLogicalId());
            assertEquals("curated", packed.getId());
            Files.writeString(resource, "<colorTheme name='Renamed' ect-id='curated'><background color='#abcdef'/></colorTheme>");
            repository.reload();
            assertEquals(packed, repository.getAllThemes().stream().filter(t -> t.getFile().isEmpty()).findFirst().orElseThrow());
        }
    }

    @Test public void bindingReadFailureWithholdsAdmissionAndCannotCreateIncomingXml() throws Exception {
        var directory = temporary.newFolder().toPath();
        var installed = Files.createDirectory(directory.resolve("installed_themes"));
        var target = installed.resolve("same.xml");
        Files.writeString(target, "<colorTheme name='Stored'/>");
        var source = directory.resolve("incoming.xml");
        Files.writeString(source, "<colorTheme name='Incoming'/>");
        Files.createDirectory(directory.resolve("theme-identities.properties"));
        List<String> errors = new ArrayList<>();
        var log = (org.eclipse.core.runtime.ILog) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {org.eclipse.core.runtime.ILog.class}, (proxy, method, args) -> {
                    if (method.getName().equals("error")) errors.add(String.valueOf(args[0]));
                    return null;
                });
        try (var logger = ParserTestSupport.installLogger(ParserTestSupport.quietLog())) {
            bundled(List.of());
            var repository = new ThemeRepository(directory.toFile(), log);
            repository.reload();
            assertTrue(repository.getAllThemes().isEmpty());
            assertTrue(repository.importTheme(source.toFile()).isEmpty());
            assertEquals("<colorTheme name='Stored'/>", Files.readString(target));
            assertEquals("<colorTheme name='Incoming'/>", Files.readString(source));
            assertFalse(Files.exists(installed.resolve("incoming.xml")));
            assertTrue(errors.stream().anyMatch(e -> e.contains("identity")));
            assertTrue(Files.isDirectory(directory.resolve("theme-identities.properties")));
        }
    }

    @Test public void occupiedOrReservedLocatorRefusesImportWithoutChangingEstablishedState() throws Exception {
        for (boolean missingFile : List.of(false, true)) {
            var directory = temporary.newFolder().toPath();
            var installed = Files.createDirectory(directory.resolve("installed_themes"));
            var target = installed.resolve("same.xml");
            Files.writeString(target, "<colorTheme name='Established' ect-id='old-source'/>");
            var source = directory.resolve("same.xml");
            Files.writeString(source, "<colorTheme name='Unrelated' ect-id='incoming-source'/>");
            List<String> diagnostics = new ArrayList<>();
            var log = (org.eclipse.core.runtime.ILog) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] {org.eclipse.core.runtime.ILog.class}, (proxy, method, args) -> {
                        if (method.getName().equals("error")) diagnostics.add(java.util.Arrays.toString(args));
                        return null;
                    });
            try (var logger = ParserTestSupport.installLogger(ParserTestSupport.quietLog())) {
                bundled(List.of());
                var repository = new ThemeRepository(directory.toFile(), log);
                repository.reload();
                var catalog = repository.getAllThemes();
                Theme established = catalog.get(0);
                byte[] xmlBefore = Files.readAllBytes(target);
                var binding = directory.resolve("theme-identities.properties");
                byte[] bindingsBefore = Files.readAllBytes(binding);
                var intentFile = directory.resolve("selected-intent.properties");
                var intent = new PreferenceStore(intentFile.toString());
                intent.setValue(PreferenceKeys.SELECTED_THEME_LOGICAL_ID, established.getLogicalId());
                intent.setValue(PreferenceKeys.ACTIVE_THEME_ID, "old-source");
                intent.save();
                byte[] intentBefore = Files.readAllBytes(intentFile);
                if (missingFile) Files.delete(target);

                assertTrue("Refusal returns no incoming theme/identity", repository.importTheme(source.toFile()).isEmpty());
                assertEquals(catalog, repository.getAllThemes());
                assertSame("Catalog entry is not replaced/reloaded", established, repository.getAllThemes().get(0));
                assertArrayEquals(bindingsBefore, Files.readAllBytes(binding));
                assertArrayEquals(intentBefore, Files.readAllBytes(intentFile));
                assertEquals(established.getLogicalId(), intent.getString(PreferenceKeys.SELECTED_THEME_LOGICAL_ID));
                if (missingFile) {
                    assertFalse("Reservation must not be recycled", Files.exists(target));
                    assertTrue(diagnostics.toString(), diagnostics.stream().anyMatch(d -> d.contains("reserved")));
                } else {
                    assertArrayEquals(xmlBefore, Files.readAllBytes(target));
                    assertTrue(diagnostics.toString(), diagnostics.stream().anyMatch(d -> d.contains("occupied")));
                    // Selecting the managed destination itself must not truncate it either.
                    assertTrue(repository.importTheme(target.toFile()).isEmpty());
                    assertArrayEquals(xmlBefore, Files.readAllBytes(target));
                    assertArrayEquals(bindingsBefore, Files.readAllBytes(binding));
                }
            }
        }
    }

    @Test public void occupiedUnboundLocatorIsRefusedWithoutAllocatingIdentity() throws Exception {
        var directory = temporary.newFolder().toPath();
        var installed = Files.createDirectory(directory.resolve("installed_themes"));
        var target = installed.resolve("same.xml");
        Files.writeString(target, "<colorTheme name='Unbound'/>");
        var source = directory.resolve("same.xml");
        Files.writeString(source, "<colorTheme name='Incoming'/>");
        byte[] before = Files.readAllBytes(target);
        var repository = new ThemeRepository(directory.toFile(), ParserTestSupport.quietLog());
        assertTrue(repository.importTheme(source.toFile()).isEmpty());
        assertArrayEquals(before, Files.readAllBytes(target));
        assertFalse(Files.exists(directory.resolve("theme-identities.properties")));
        assertTrue(repository.getAllThemes().isEmpty());
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
            for (Theme theme : repository.getAllThemes()) {
                assertEquals("bundled:" + theme.getId(), theme.getLogicalId());
            }
            var source = directory.resolve("external.xml");
            Files.writeString(source, "<colorTheme name='Darcula' ect-id='63d6a50b-371f-4211-9025-9bfea76cc850'/>");
            Theme imported = repository.importTheme(source.toFile()).orElseThrow();
            assertTrue(imported.getLogicalId().startsWith("local:"));
            assertTrue(repository.getAllThemes().stream().anyMatch(t -> t == imported));
            // Moving the external origin leaves the installed record and its identity untouched.
            Files.move(source, directory.resolve("moved-origin.xml"));
            repository.reload();
            assertEquals(34, repository.getAllThemes().size());
            assertEquals(1, repository.getAllThemes().stream().filter(imported::equals).count());
            assertEquals(2, repository.getAllThemes().stream().filter(t -> t.getName().equals("Darcula")).count());
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
