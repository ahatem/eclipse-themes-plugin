package com.github.eclipsethemes.eclipse;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;

public class ThemeRepositoryImportTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static final String STORED =
            "<colorTheme name='Stored' ect-id='stored-source'><background color='#112233'/></colorTheme>";
    private static final String INCOMING =
            "<colorTheme name='Stored' ect-id='stored-source'><background color='#445566'/></colorTheme>";

    @Test public void sameBasenameAndBytesCreateAnIndependentNewRecord() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path installed = Files.createDirectory(directory.resolve("installed_themes"));
        Path existing = Files.writeString(installed.resolve("theme.xml"), STORED);
        var bindings = new ThemeIdentityBindings(RepositoryTestSupport.bindingsFile(directory));
        String establishedId = bindings.newIdentityFor("theme.xml");
        Path source = Files.writeString(directory.resolve("theme.xml"), STORED);

        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());
        ThemeRecord imported = repository.importTheme(source.toFile()).record();

        assertNotEquals("Equal basename and bytes never reuse identity", establishedId, imported.logicalId());
        Path managed = RepositoryTestSupport.managed(directory, imported);
        assertEquals(installed.toAbsolutePath().normalize(), managed.getParent().toAbsolutePath().normalize());
        assertTrue("Locator is repository-allocated",
                managed.getFileName().toString().matches("[0-9a-f-]{36}\\.xml"));
        assertArrayEquals("Committed bytes are the staged bytes", bytes(STORED), Files.readAllBytes(managed));
        assertEquals("Existing bytes are untouched", STORED, Files.readString(existing));
        assertEquals(establishedId, bindings.establishedIdentityFor("theme.xml").orElseThrow());
        assertEquals(imported.logicalId(),
                bindings.establishedIdentityFor(managed.getFileName().toString()).orElseThrow());
        assertEquals(Set.of(imported), Set.copyOf(repository.getAllThemes()));
        assertEquals("No staging residue after a successful import", List.of(),
                RepositoryTestSupport.stagedPieces(installed));
    }

    @Test public void importedIdentityAndBytesSurviveReloadAndStagingIsNotAdmitted() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path installed = Files.createDirectory(directory.resolve("installed_themes"));
        Files.writeString(installed.resolve("theme.xml"), STORED);
        var bindings = new ThemeIdentityBindings(RepositoryTestSupport.bindingsFile(directory));
        String establishedId = bindings.newIdentityFor("theme.xml");
        Path source = Files.writeString(directory.resolve("theme.xml"), INCOMING);

        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());
            ThemeRecord imported = repository.importTheme(source.toFile()).record();
            Files.writeString(Files.createDirectories(installed.resolve(".staging")).resolve("leftover.part"),
                    INCOMING);

            repository.reload();

            List<ThemeRecord> catalog = repository.getAllThemes();
            assertEquals(2, catalog.size());
            ThemeRecord established = catalog.stream()
                    .filter(record -> establishedId.equals(record.logicalId())).findFirst().orElseThrow();
            assertEquals("theme.xml", established.locator().orElseThrow());
            ThemeRecord reloaded = catalog.stream().filter(imported::equals).findFirst().orElseThrow();
            assertEquals(imported.logicalId(), reloaded.logicalId());
            assertArrayEquals(bytes(INCOMING),
                    Files.readAllBytes(RepositoryTestSupport.managed(directory, reloaded)));
        }
    }

    @Test public void importingTheSameSourceTwiceCreatesTwoIndependentRecords() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path source = Files.writeString(directory.resolve("theme.xml"), INCOMING);
        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());

        ThemeRecord first = repository.importTheme(source.toFile()).record();
        ThemeRecord second = repository.importTheme(source.toFile()).record();

        assertNotEquals(first.logicalId(), second.logicalId());
        assertNotEquals(first.locator().orElseThrow(), second.locator().orElseThrow());
        assertEquals(2, repository.getAllThemes().size());
        assertArrayEquals(bytes(INCOMING), Files.readAllBytes(source));
    }

    @Test public void uppercaseSourceExtensionIsImportedAsAManagedXmlLocator() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path source = Files.writeString(directory.resolve("THEME.XML"), INCOMING);
        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());

        ThemeRecord imported = repository.importTheme(source.toFile()).record();

        assertTrue(imported.locator().orElseThrow().endsWith(".xml"));
        assertEquals(INCOMING, Files.readString(source));
    }

    @Test public void blockedManagedStorageLeavesNoBindingAndAllowsRetry() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path blocked = Files.writeString(directory.resolve("installed_themes"), "not a directory");
        Path source = Files.writeString(directory.resolve("incoming.xml"), INCOMING);

        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());
        assertEquals(List.of(), repository.getAllThemes());
        assertFalse("The storage failure is reported", repository.importTheme(source.toFile()).successful());
        assertFalse("No binding may outlive a failed copy",
                Files.exists(RepositoryTestSupport.bindingsFile(directory)));
        assertTrue(repository.getAllThemes().isEmpty());

        // The failed attempt left no reservation: once the obstruction is removed, a retry admits normally.
        Files.delete(blocked);
        Files.createDirectory(blocked);
        ThemeRecord admitted = repository.importTheme(source.toFile()).record();
        assertTrue(admitted.logicalId().startsWith("local:"));
        assertEquals(List.of(admitted.locator().orElseThrow()),
                RepositoryTestSupport.fileNames(blocked));
        assertEquals(admitted.logicalId(), RepositoryTestSupport.establishedIdentity(directory,
                admitted.locator().orElseThrow()));
        assertEquals(INCOMING, Files.readString(source));
    }

    @Test public void bindingFailureAfterMoveRemovesOnlyTheAttemptLocator() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path installed = Files.createDirectory(directory.resolve("installed_themes"));
        Path existing = Files.writeString(installed.resolve("existing.xml"), STORED);
        Path bindingsFile = RepositoryTestSupport.bindingsFile(directory);
        String priorId = new ThemeIdentityBindings(bindingsFile).newIdentityFor("existing.xml");
        byte[] bindingsBefore = Files.readAllBytes(bindingsFile);
        byte[] existingBefore = Files.readAllBytes(existing);
        Path source = Files.writeString(directory.resolve("incoming.xml"), INCOMING);

        Path intentFile = directory.resolve("selected-intent.properties");
        var intent = new PreferenceStore(intentFile.toString());
        intent.setValue(PreferenceKeys.SELECTED_THEME_LOGICAL_ID, priorId);
        intent.setValue(PreferenceKeys.ACTIVE_THEME_ID, "stored-source");
        intent.save();
        byte[] intentBefore = Files.readAllBytes(intentFile);

        var failing = new ThemeIdentityBindings(bindingsFile) {
            @Override void replace(Path staged) throws IOException {
                assertTrue("A complete staged write precedes replacement", Files.size(staged) > 0);
                throw new IOException("injected metadata replacement failure");
            }
        };
        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog(), failing);

        assertFalse(repository.importTheme(source.toFile()).successful());

        assertEquals("Only this attempt's managed file is absent",
                List.of("existing.xml"), RepositoryTestSupport.fileNames(installed));
        assertEquals(List.of(), RepositoryTestSupport.stagedPieces(installed));
        assertArrayEquals("Pre-existing managed XML is unchanged", existingBefore, Files.readAllBytes(existing));
        assertArrayEquals("Prior bindings are unchanged", bindingsBefore, Files.readAllBytes(bindingsFile));
        assertArrayEquals("Selected intent is unchanged", intentBefore, Files.readAllBytes(intentFile));
        assertTrue("No catalog entry is admitted", repository.getAllThemes().isEmpty());
        assertEquals(priorId, RepositoryTestSupport.establishedIdentity(directory, "existing.xml"));
        assertEquals(INCOMING, Files.readString(source));
    }

    @Test public void unreadableBindingsRefuseImportBeforeAnyManagedWrite() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path installed = Files.createDirectory(directory.resolve("installed_themes"));
        Path existing = Files.writeString(installed.resolve("stored.xml"), STORED);
        Path source = Files.writeString(directory.resolve("incoming.xml"), INCOMING);
        Files.createDirectory(RepositoryTestSupport.bindingsFile(directory));

        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());
        assertFalse(repository.importTheme(source.toFile()).successful());

        assertEquals(List.of("stored.xml"), RepositoryTestSupport.fileNames(installed));
        assertEquals(List.of(), RepositoryTestSupport.stagedPieces(installed));
        assertEquals(STORED, Files.readString(existing));
        assertEquals(INCOMING, Files.readString(source));
        assertTrue(repository.getAllThemes().isEmpty());
    }

    @Test public void importingAManagedFileLeavesTheSourceRecordUnchanged() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path installed = Files.createDirectory(directory.resolve("installed_themes"));
        Path source = Files.writeString(installed.resolve("source.xml"), STORED);
        String sourceId = new ThemeIdentityBindings(RepositoryTestSupport.bindingsFile(directory))
                .newIdentityFor("source.xml");

        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());
            repository.reload();
            assertEquals(sourceId, repository.getAllThemes().get(0).logicalId());

            ThemeRecord imported = repository.importTheme(source.toFile()).record();

            assertNotEquals(sourceId, imported.logicalId());
            assertNotEquals(source.toAbsolutePath().normalize(),
                    RepositoryTestSupport.managed(directory, imported).toAbsolutePath().normalize());
            assertEquals(STORED, Files.readString(source));
            assertEquals(sourceId, RepositoryTestSupport.establishedIdentity(directory, "source.xml"));
            assertEquals(2, repository.getAllThemes().size());
        }
    }

    @Test public void invalidThemeCreatesNoBindingNoManagedFileAndKeepsSource() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path installed = Files.createDirectory(directory.resolve("installed_themes"));
        Path source = Files.writeString(directory.resolve("theme.xml"), "<colorTheme name='Broken'>");
        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());

        assertFalse(repository.importTheme(source.toFile()).successful());

        assertFalse("No binding may outlive a failed parse",
                Files.exists(RepositoryTestSupport.bindingsFile(directory)));
        assertEquals(List.of(), RepositoryTestSupport.fileNames(installed));
        assertEquals(List.of(), RepositoryTestSupport.stagedPieces(installed));
        assertTrue(repository.getAllThemes().isEmpty());
        assertEquals("<colorTheme name='Broken'>", Files.readString(source));
    }

    @Test public void unsupportedExtensionIsRefusedBeforeStaging() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path installed = Files.createDirectory(directory.resolve("installed_themes"));
        Path source = Files.writeString(directory.resolve("theme.txt"), INCOMING);
        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());

        assertFalse(repository.importTheme(source.toFile()).successful());

        assertFalse("Unsupported input never reaches staging", Files.exists(installed.resolve(".staging")));
        assertFalse(Files.exists(RepositoryTestSupport.bindingsFile(directory)));
        assertEquals(List.of(), RepositoryTestSupport.fileNames(installed));
        assertEquals(INCOMING, Files.readString(source));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
