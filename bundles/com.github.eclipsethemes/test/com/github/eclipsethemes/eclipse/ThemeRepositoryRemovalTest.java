package com.github.eclipsethemes.eclipse;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;

public class ThemeRepositoryRemovalTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static final String STORED =
            "<colorTheme name='Stored' ect-id='stored-source'><background color='#112233'/></colorTheme>";
    private static final String OTHER =
            "<colorTheme name='Other' ect-id='other-source'><background color='#445566'/></colorTheme>";

    @Test public void removalDeletesOnlyTheSelectedRecord() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded first = seed(directory, "first.xml", STORED);
            Seeded second = seed(directory, "second.xml", OTHER);
            byte[] secondBytes = Files.readAllBytes(second.file);

            assertTrue(second.repository.removeTheme(first.record));

            assertFalse(Files.exists(first.file));
            assertTrue(second.repository.getAllThemes().stream()
                    .noneMatch(theme -> first.identity.equals(theme.getLogicalId())));
            assertEquals(second.file.toAbsolutePath().normalize(),
                    second.record.getFile().orElseThrow().toPath().toAbsolutePath().normalize());
            assertArrayEquals("The other record's bytes are unchanged", secondBytes,
                    Files.readAllBytes(second.file));
            assertNull(RepositoryTestSupport.establishedIdentity(directory, "first.xml"));
            assertEquals(second.identity, RepositoryTestSupport.establishedIdentity(directory, "second.xml"));
            assertEquals(List.of("second.xml"), RepositoryTestSupport.fileNames(first.file.getParent()));
            assertEquals(List.of(), RepositoryTestSupport.stagedPieces(first.file.getParent()));

            second.repository.reload();

            assertTrue("Removed logical identity remains absent after reload",
                    second.repository.getAllThemes().stream()
                            .noneMatch(theme -> first.identity.equals(theme.getLogicalId())));
            assertTrue("Unrelated logical identity remains present after reload",
                    second.repository.getAllThemes().stream()
                            .anyMatch(theme -> second.identity.equals(theme.getLogicalId())));
        }
    }

    @Test public void alreadyMissingFileIsSuccessfulRemoval() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", STORED);
            Files.delete(seeded.file);

            assertTrue(seeded.repository.removeTheme(seeded.record));

            assertFalse("Removal never recreates the missing file", Files.exists(seeded.file));
            assertNull(RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertTrue(seeded.repository.getAllThemes().isEmpty());
            assertEquals(List.of(), RepositoryTestSupport.fileNames(seeded.file.getParent()));
        }
    }

    @Test public void failedFileDeletionRestoresTheExactBinding() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path installed = Files.createDirectory(directory.resolve("installed_themes"));
        Path file = Files.writeString(installed.resolve("theme.xml"), STORED);
        String identity = new ThemeIdentityBindings(RepositoryTestSupport.bindingsFile(directory))
                .newIdentityFor("theme.xml");
        var failures = new AtomicInteger(1);

        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog()) {
                @Override void deleteManagedFile(Path target) throws IOException {
                    if (failures.getAndDecrement() > 0) {
                        throw new IOException("injected deletion failure");
                    }
                    super.deleteManagedFile(target);
                }
            };
            repository.reload();
            Theme record = repository.getAllThemes().get(0);

            assertFalse("The failed deletion is reported", repository.removeTheme(record));
            assertEquals("The file remains after a failed deletion", STORED, Files.readString(file));
            assertEquals("The prior binding is restored", identity,
                    RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertEquals(List.of(record), repository.getAllThemes());

            assertTrue("A clean retry removes the record", repository.removeTheme(record));
            assertFalse(Files.exists(file));
            assertNull(RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertTrue(repository.getAllThemes().isEmpty());
        }
    }

    @Test public void unexpectedManagedPathTypeFailsClosedAndRestoresBinding() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", STORED);
            Files.delete(seeded.file);
            Files.createDirectory(seeded.file);

            assertFalse(seeded.repository.removeTheme(seeded.record));

            assertTrue("The unexpected path is not deleted",
                    Files.isDirectory(seeded.file, LinkOption.NOFOLLOW_LINKS));
            assertEquals("The prior binding is restored", seeded.identity,
                    RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertEquals(List.of("theme.xml"), RepositoryTestSupport.fileNames(seeded.file.getParent()));
            assertEquals(List.of(seeded.record), seeded.repository.getAllThemes());
        }
    }

    @Test public void bindingRemovalFailureLeavesFileAndCatalogUntouched() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path installed = Files.createDirectory(directory.resolve("installed_themes"));
        Path file = Files.writeString(installed.resolve("theme.xml"), STORED);
        Path bindings = RepositoryTestSupport.bindingsFile(directory);
        String identity = new ThemeIdentityBindings(bindings).newIdentityFor("theme.xml");
        var failing = new ThemeIdentityBindings(bindings) {
            @Override void remove(String locator, String expectedIdentity) throws IOException {
                throw new IOException("injected binding removal failure");
            }
        };

        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog(), failing);
            repository.reload();
            Theme record = repository.getAllThemes().get(0);

            assertFalse(repository.removeTheme(record));

            assertEquals(STORED, Files.readString(file));
            assertEquals(identity, RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertEquals(List.of(record), repository.getAllThemes());
        }
    }

    @Test public void bundledRecordIsRefusedWithoutMutation() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", STORED);
            byte[] bindingsBefore = Files.readAllBytes(RepositoryTestSupport.bindingsFile(directory));
            Theme bundled = new Theme("curated", "Bundled", null, null, null, null, ThemeType.DARK)
                    .withLogicalId("bundled:curated");

            assertFalse(seeded.repository.removeTheme(bundled));

            assertArrayEquals(bindingsBefore, Files.readAllBytes(RepositoryTestSupport.bindingsFile(directory)));
            assertEquals(List.of(seeded.record), seeded.repository.getAllThemes());
        }
    }

    @Test public void selectedFileOutsideManagedStorageIsRefused() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", STORED);
            Path outside = Files.writeString(directory.resolve("outside.xml"), OTHER);
            Theme foreign = new Theme("outside", "Outside", null, null, null, outside.toFile(), ThemeType.DARK)
                    .withLogicalId("local:outside");

            assertFalse(seeded.repository.removeTheme(foreign));

            assertEquals(OTHER, Files.readString(outside));
            assertEquals(seeded.identity, RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertEquals(List.of(seeded.record), seeded.repository.getAllThemes());
        }
    }

    @Test public void unboundManagedFileIsRefused() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", STORED);
            Files.delete(RepositoryTestSupport.bindingsFile(directory));

            assertFalse(seeded.repository.removeTheme(seeded.record));

            assertEquals(STORED, Files.readString(seeded.file));
            assertFalse("A refused removal creates no binding",
                    Files.exists(RepositoryTestSupport.bindingsFile(directory)));
            assertEquals(List.of(seeded.record), seeded.repository.getAllThemes());
        }
    }

    @Test public void mismatchedIdentityIsRefusedBeforeBindingRemoval() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", STORED);
            byte[] bindingsBefore = Files.readAllBytes(RepositoryTestSupport.bindingsFile(directory));
            Theme forged = seeded.record.withLogicalId("local:forged");

            assertFalse(seeded.repository.removeTheme(forged));

            assertEquals(STORED, Files.readString(seeded.file));
            assertEquals(seeded.identity, RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertArrayEquals(bindingsBefore, Files.readAllBytes(RepositoryTestSupport.bindingsFile(directory)));
            assertEquals(List.of(seeded.record), seeded.repository.getAllThemes());
        }
    }

    private static Seeded seed(Path directory, String locator, String xml) throws IOException {
        Path installed = Files.createDirectories(directory.resolve("installed_themes"));
        Path file = Files.writeString(installed.resolve(locator), xml);
        String identity = new ThemeIdentityBindings(RepositoryTestSupport.bindingsFile(directory))
                .newIdentityFor(locator);
        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());
        repository.reload();
        Theme record = repository.getAllThemes().stream()
                .filter(theme -> identity.equals(theme.getLogicalId())).findFirst().orElseThrow();
        return new Seeded(repository, record, file, identity);
    }

    private record Seeded(ThemeRepository repository, Theme record, Path file, String identity) {}
}
