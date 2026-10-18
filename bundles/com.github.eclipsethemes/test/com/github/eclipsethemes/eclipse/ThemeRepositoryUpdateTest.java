package com.github.eclipsethemes.eclipse;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;

public class ThemeRepositoryUpdateTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static final String ORIGINAL =
            "<colorTheme name='Original' ect-id='source-original'><background color='#111111'/></colorTheme>";
    private static final String REPLACEMENT =
            "<colorTheme name='Replacement' ect-id='source-replacement'><background color='#222222'/></colorTheme>";

    @Test public void successfulUpdatePreservesIdentityLocatorAndBinding() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", ORIGINAL);
            Path source = Files.writeString(directory.resolve("replacement.xml"), REPLACEMENT);

            Theme updated = seeded.repository.updateTheme(seeded.record, source.toFile()).theme();

            assertEquals("Identity is preserved, not regenerated", seeded.identity, updated.getLogicalId());
            assertEquals(seeded.file.toAbsolutePath().normalize(),
                    updated.getFile().orElseThrow().toPath().toAbsolutePath().normalize());
            assertEquals("Committed bytes are exactly the replacement bytes", REPLACEMENT,
                    Files.readString(seeded.file));
            assertEquals(seeded.identity, RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertEquals(List.of("theme.xml"), RepositoryTestSupport.fileNames(seeded.file.getParent()));
            assertEquals(List.of(), RepositoryTestSupport.stagedPieces(seeded.file.getParent()));
            assertEquals(List.of(updated), seeded.repository.getAllThemes());

            seeded.repository.reload();
            Theme reloaded = seeded.repository.getAllThemes().get(0);
            assertEquals(seeded.identity, reloaded.getLogicalId());
            assertEquals("source-replacement", reloaded.getId());
            assertArrayEquals(bytes(REPLACEMENT), Files.readAllBytes(seeded.file));
            assertEquals(REPLACEMENT, Files.readString(source));
        }
    }

    @Test public void invalidReplacementLeavesThePriorRecordUnchanged() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", ORIGINAL);
            Path bindings = RepositoryTestSupport.bindingsFile(directory);
            byte[] bindingsBefore = Files.readAllBytes(bindings);
            Path source = Files.writeString(directory.resolve("replacement.xml"), "<colorTheme name='Broken'>");

            assertFalse(seeded.repository.updateTheme(seeded.record, source.toFile()).successful());

            assertEquals(ORIGINAL, Files.readString(seeded.file));
            assertArrayEquals(bindingsBefore, Files.readAllBytes(bindings));
            assertEquals(List.of(), RepositoryTestSupport.stagedPieces(seeded.file.getParent()));
            assertEquals(List.of(seeded.record), seeded.repository.getAllThemes());
            assertEquals(seeded.identity, RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertEquals("<colorTheme name='Broken'>", Files.readString(source));
        }
    }

    @Test public void refusedAtomicMoveFailsWithoutANonAtomicFallback() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path installed = Files.createDirectory(directory.resolve("installed_themes"));
        Path file = Files.writeString(installed.resolve("theme.xml"), ORIGINAL);
        String identity = new ThemeIdentityBindings(RepositoryTestSupport.bindingsFile(directory))
                .newIdentityFor("theme.xml");
        Path source = Files.writeString(directory.resolve("replacement.xml"), REPLACEMENT);

        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog()) {
                @Override void replaceManagedFile(Path staged, Path target) throws IOException {
                    throw new AtomicMoveNotSupportedException(staged.toString(), target.toString(),
                            "injected atomic move refusal");
                }
            };
            repository.reload();
            Theme record = find(repository, identity);

            assertFalse(repository.updateTheme(record, source.toFile()).successful());

            assertEquals("No fallback replacement occurred", ORIGINAL, Files.readString(file));
            assertEquals(identity, RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertEquals(List.of(), RepositoryTestSupport.stagedPieces(installed));
            assertEquals(List.of(record), repository.getAllThemes());
            assertEquals(REPLACEMENT, Files.readString(source));
        }
    }

    @Test public void missingTargetFailsWithoutRecreation() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", ORIGINAL);
            Files.delete(seeded.file);
            Path source = Files.writeString(directory.resolve("replacement.xml"), REPLACEMENT);

            assertFalse(seeded.repository.updateTheme(seeded.record, source.toFile()).successful());

            assertFalse("A missing target is never recreated", Files.exists(seeded.file));
            assertEquals("The record remains established while its file is absent", seeded.identity,
                    RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertEquals(List.of(), RepositoryTestSupport.stagedPieces(seeded.file.getParent()));
            assertEquals(List.of(seeded.record), seeded.repository.getAllThemes());
            assertEquals(REPLACEMENT, Files.readString(source));
        }
    }

    @Test public void unsupportedReplacementTypeIsRefusedBeforeStaging() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", ORIGINAL);
            Path source = Files.writeString(directory.resolve("replacement.txt"), REPLACEMENT);

            assertFalse(seeded.repository.updateTheme(seeded.record, source.toFile()).successful());

            assertEquals(ORIGINAL, Files.readString(seeded.file));
            assertEquals(seeded.identity, RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertFalse("Unsupported input never reaches staging",
                    Files.exists(seeded.file.getParent().resolve(".staging")));
            assertEquals(REPLACEMENT, Files.readString(source));
        }
    }

    @Test public void bundledTargetIsRefusedWithoutTouchingManagedState() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", ORIGINAL);
            Path source = Files.writeString(directory.resolve("replacement.xml"), REPLACEMENT);
            Theme bundled = new Theme("curated", "Bundled", null, null, null, null, ThemeType.DARK)
                    .withLogicalId("bundled:curated");

            assertFalse(seeded.repository.updateTheme(bundled, source.toFile()).successful());

            assertEquals(ORIGINAL, Files.readString(seeded.file));
            assertEquals(seeded.identity, RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertEquals(List.of(seeded.record), seeded.repository.getAllThemes());
            assertEquals(REPLACEMENT, Files.readString(source));
        }
    }

    @Test public void selectedFileOutsideManagedStorageIsRefused() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", ORIGINAL);
            Path outside = Files.writeString(directory.resolve("outside.xml"), ORIGINAL);
            Path source = Files.writeString(directory.resolve("replacement.xml"), REPLACEMENT);
            Theme foreign = new Theme("outside", "Outside", null, null, null, outside.toFile(), ThemeType.DARK)
                    .withLogicalId("local:outside");

            assertFalse(seeded.repository.updateTheme(foreign, source.toFile()).successful());

            assertEquals(ORIGINAL, Files.readString(outside));
            assertEquals(ORIGINAL, Files.readString(seeded.file));
            assertEquals(seeded.identity, RepositoryTestSupport.establishedIdentity(directory, "theme.xml"));
            assertEquals(List.of(seeded.record), seeded.repository.getAllThemes());
        }
    }

    @Test public void unboundManagedFileIsRefusedWithoutGeneratingBinding() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", ORIGINAL);
            Path bindings = RepositoryTestSupport.bindingsFile(directory);
            Files.delete(bindings);
            Path source = Files.writeString(directory.resolve("replacement.xml"), REPLACEMENT);

            assertFalse(seeded.repository.updateTheme(seeded.record, source.toFile()).successful());

            assertEquals(ORIGINAL, Files.readString(seeded.file));
            assertFalse("Refusal never generates a binding", Files.exists(bindings));
            assertEquals(List.of(), RepositoryTestSupport.stagedPieces(seeded.file.getParent()));
            assertEquals(List.of(seeded.record), seeded.repository.getAllThemes());
        }
    }

    @Test public void inconsistentBindingIsRefusedBeforeAnyManagedWrite() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            Seeded seeded = seed(directory, "theme.xml", ORIGINAL);
            Path bindings = RepositoryTestSupport.bindingsFile(directory);
            String inconsistent = "theme.xml=" + seeded.identity + "\nother.xml=" + seeded.identity + "\n";
            Files.writeString(bindings, inconsistent);
            Path source = Files.writeString(directory.resolve("replacement.xml"), REPLACEMENT);

            assertFalse(seeded.repository.updateTheme(seeded.record, source.toFile()).successful());

            assertEquals(ORIGINAL, Files.readString(seeded.file));
            assertEquals(inconsistent, Files.readString(bindings));
            assertEquals(List.of(), RepositoryTestSupport.stagedPieces(seeded.file.getParent()));
            assertEquals(List.of(seeded.record), seeded.repository.getAllThemes());
        }
    }

    @Test public void replacementMetadataDoesNotStealAnotherRecord() throws Exception {
        Path directory = temporary.newFolder().toPath();
        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            String firstId = seed(directory, "first.xml", ORIGINAL).identity;
            String secondId = seed(directory, "second.xml",
                    "<colorTheme name='Second' ect-id='source-second'><background color='#333333'/></colorTheme>")
                    .identity;
            var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());
            repository.reload();
            Theme first = find(repository, firstId);
            Theme second = find(repository, secondId);
            Path secondFile = second.getFile().orElseThrow().toPath();
            byte[] secondBefore = Files.readAllBytes(secondFile);
            Path source = Files.writeString(directory.resolve("replacement.xml"),
                    "<colorTheme name='Second' ect-id='source-second'><background color='#444444'/></colorTheme>");

            Theme updated = repository.updateTheme(first, source.toFile()).theme();

            assertEquals(firstId, updated.getLogicalId());
            assertEquals("Second", updated.getName());
            assertEquals("source-second", updated.getId());
            assertEquals(secondId, find(repository, secondId).getLogicalId());
            assertEquals("Second", find(repository, secondId).getName());
            assertArrayEquals("The other record's bytes are unchanged", secondBefore, Files.readAllBytes(secondFile));
            assertEquals(secondId, RepositoryTestSupport.establishedIdentity(directory, "second.xml"));
            assertEquals(2, repository.getAllThemes().size());
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

    private static Theme find(ThemeRepository repository, String identity) {
        return repository.getAllThemes().stream()
                .filter(theme -> identity.equals(theme.getLogicalId())).findFirst().orElseThrow();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private record Seeded(ThemeRepository repository, Theme record, Path file, String identity) {}
}
