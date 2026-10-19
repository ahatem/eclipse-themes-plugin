package com.github.eclipsethemes.eclipse;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.eclipsethemes.core.parser.ThemePackageParser;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;

/** Theme packages ({@code .ectheme}) through import, reload and update; identity stays repository-owned. */
public class ThemeRepositoryPackageTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static final String EDITOR = "<editor><background color='#1b1f2a'/><foreground color='#c8d0e0'/></editor>";
    private static final String PACKAGE = "<themePackage schemaVersion='1' id='io.example.nightfall' name='Nightfall'"
            + " appearance='dark'><fonts family='Mono'/>" + EDITOR + "</themePackage>";
    private static final String REVISED = "<themePackage schemaVersion='1' id='io.example.nightfall' name='Nightfall 2'"
            + " appearance='dark'>" + EDITOR + "</themePackage>";

    @Test public void packageImportKeepsItsFormatAndExactBytesAcrossReload() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path source = Files.writeString(directory.resolve("Nightfall.ECTHEME"), PACKAGE);

        try (var activator = RepositoryTestSupport.installActivator()) {
            RepositoryTestSupport.installBundled(List.of());
            var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());
            var outcome = repository.importTheme(source.toFile());
            ThemeRecord imported = outcome.record();

            Path managed = RepositoryTestSupport.managed(directory, imported);
            assertTrue(managed.getFileName().toString().matches("[0-9a-f-]{36}\\.ectheme"));
            assertTrue("Identity is repository-owned, not the package id", imported.logicalId().startsWith("local:"));
            assertArrayEquals("Unknown content is kept byte for byte", PACKAGE.getBytes(StandardCharsets.UTF_8),
                    Files.readAllBytes(managed));
            assertEquals(List.of(ThemeDiagnostic.UNKNOWN_ELEMENT),
                    outcome.diagnostics().stream().map(ThemeDiagnostic::kind).toList());

            repository.reload();
            ThemeRecord reloaded = repository.getAllThemes().stream().filter(imported::equals).findFirst().orElseThrow();
            assertEquals("io.example.nightfall", reloaded.theme().getPackageMetadata().orElseThrow().packageId());
        }
    }

    @Test public void twoPackagesWithTheSamePackageIdStayIndependent() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path source = Files.writeString(directory.resolve("a.ectheme"), PACKAGE);
        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());

        ThemeRecord first = repository.importTheme(source.toFile()).record();
        ThemeRecord second = repository.importTheme(source.toFile()).record();

        assertNotEquals(first.logicalId(), second.logicalId());
        assertNotEquals(first.locator(), second.locator());
        assertEquals(2, repository.getAllThemes().size());
    }

    @Test public void oversizedPackageIsRefusedWithoutChangingState() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path source = Files.writeString(directory.resolve("huge.ectheme"),
                PACKAGE + " ".repeat(ThemePackageParser.MAX_BYTES));
        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());

        var outcome = repository.importTheme(source.toFile());

        assertFalse(outcome.successful());
        assertEquals(ThemeDiagnostic.FILE_TOO_LARGE, outcome.diagnostics().get(0).kind());
        Path installed = directory.resolve("installed_themes");
        assertEquals(List.of(), RepositoryTestSupport.fileNames(installed));
        assertEquals(List.of(), RepositoryTestSupport.stagedPieces(installed));
        assertFalse(Files.exists(RepositoryTestSupport.bindingsFile(directory)));
        assertEquals(List.of(), repository.getAllThemes());
    }

    @Test public void packageToPackageUpdateKeepsIdentityAndLocator() throws Exception {
        Path directory = temporary.newFolder().toPath();
        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());
        ThemeRecord imported = repository
                .importTheme(Files.writeString(directory.resolve("n.ectheme"), PACKAGE).toFile()).record();

        ThemeRecord updated = repository
                .updateTheme(imported, Files.writeString(directory.resolve("n2.ectheme"), REVISED).toFile()).record();

        assertEquals(imported.logicalId(), updated.logicalId());
        assertEquals(imported.locator(), updated.locator());
        assertEquals("Nightfall 2", updated.theme().getName());
        assertEquals(REVISED, Files.readString(RepositoryTestSupport.managed(directory, updated)));
    }

    @Test public void crossFormatUpdateIsRefusedBeforeStaging() throws Exception {
        Path directory = temporary.newFolder().toPath();
        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());
        ThemeRecord packaged = repository
                .importTheme(Files.writeString(directory.resolve("n.ectheme"), PACKAGE).toFile()).record();
        ThemeRecord legacy = repository.importTheme(Files.writeString(directory.resolve("l.xml"),
                "<colorTheme name='Legacy'><background color='#000000'/></colorTheme>").toFile()).record();
        Path legacySource = Files.writeString(directory.resolve("r.xml"), "<colorTheme name='R'/>");
        Path packageSource = Files.writeString(directory.resolve("r.ectheme"), REVISED);
        Path installed = directory.resolve("installed_themes");
        Files.deleteIfExists(installed.resolve(".staging"));

        for (var attempt : List.of(new Object[] { packaged, legacySource }, new Object[] { legacy, packageSource })) {
            ThemeRecord target = (ThemeRecord) attempt[0];
            byte[] before = Files.readAllBytes(RepositoryTestSupport.managed(directory, target));
            var outcome = repository.updateTheme(target, ((Path) attempt[1]).toFile());

            assertFalse(outcome.successful());
            assertEquals(ThemeDiagnostic.FORMAT_MISMATCH, outcome.diagnostics().get(0).kind());
            assertArrayEquals(before, Files.readAllBytes(RepositoryTestSupport.managed(directory, target)));
            assertFalse("Refused before staging", Files.exists(installed.resolve(".staging")));
        }
    }

    @Test public void unreadableAuthoredWorkbenchRoleIsReportedAtAdmission() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path source = Files.writeString(directory.resolve("dim.ectheme"), PACKAGE.replace("</editor>",
                "</editor><workbench><surface color='#262c3b'/><accent color='#3a4a7a'/></workbench>"));
        var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());

        var outcome = repository.importTheme(source.toFile());

        assertTrue("A readability warning never refuses the package", outcome.successful());
        ThemeDiagnostic warning = outcome.diagnostics().stream()
                .filter(finding -> finding.kind().equals(ThemeDiagnostic.WORKBENCH_READABILITY))
                .findFirst().orElseThrow();
        assertFalse(warning.error());
        assertEquals("accent", warning.element());
    }
}
