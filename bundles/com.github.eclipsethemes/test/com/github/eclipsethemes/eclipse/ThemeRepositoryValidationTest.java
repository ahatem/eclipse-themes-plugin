package com.github.eclipsethemes.eclipse;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;

/** Admission semantics: warnings admit, errors refuse, nothing else changes. */
public class ThemeRepositoryValidationTest {
	@Rule public TemporaryFolder temporary = new TemporaryFolder();

	private static final String VALID =
			"<colorTheme name='Valid' ect-id='valid'><background color='#112233'/></colorTheme>";
	private static final String MISSING_NAME =
			"<colorTheme ect-id='broken'><background color='#112233'/></colorTheme>";
	private static final String WARNING_ONLY = "<colorTheme name='Warned' ect-id='warned' type='midnight'>"
			+ "<background color='#112233'/></colorTheme>";

	private Path directory() throws Exception {
		return temporary.newFolder().toPath();
	}

	@Test public void importWithValidationErrorIsRefusedAndChangesNothing() throws Exception {
		Path directory = directory();
		Path source = Files.writeString(directory.resolve("broken.xml"), MISSING_NAME);
		List<String> log = new ArrayList<>();
		var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.recordingLog(log));

		ThemeOperationOutcome outcome = repository.importTheme(source.toFile());

		assertFalse(outcome.successful());
		assertNull(outcome.theme());
		ThemeDiagnostic finding = outcome.diagnostics().get(0);
		assertTrue(finding.error());
		assertEquals(ThemeDiagnostic.NAME, finding.kind());
		assertEquals("The dialog reason is the validation finding", finding.message(), outcome.describeFailure());

		assertEquals("No catalog record is admitted", List.of(), repository.getAllThemes());
		assertEquals("No managed file is written", List.of(),
				RepositoryTestSupport.fileNames(directory.resolve("installed_themes")));
		assertNull("No binding is established",
				RepositoryTestSupport.establishedIdentity(directory, "broken.xml"));
		assertEquals("The source is left untouched", MISSING_NAME, Files.readString(source));
		assertTrue("The refusal is logged with its reason",
				log.stream().anyMatch(line -> line.contains("non-blank 'name'")));
	}

	@Test public void updateWithValidationErrorLeavesTheRecordIntact() throws Exception {
		Path directory = directory();
		var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());
		Path original = Files.writeString(directory.resolve("valid.xml"), VALID);
		Theme stored = repository.importTheme(original.toFile()).theme();
		Path managed = stored.getFile().orElseThrow().toPath();
		String locator = managed.getFileName().toString();
		Path replacement = Files.writeString(directory.resolve("broken.xml"), MISSING_NAME);

		ThemeOperationOutcome outcome = repository.updateTheme(stored, replacement.toFile());

		assertFalse(outcome.successful());
		assertEquals(ThemeDiagnostic.NAME, outcome.diagnostics().get(0).kind());
		assertEquals("Committed bytes are unchanged", VALID, Files.readString(managed));
		assertEquals(List.of(stored), repository.getAllThemes());
		assertEquals(stored.getLogicalId(), RepositoryTestSupport.establishedIdentity(directory, locator));
		assertEquals("No staging residue", List.of(), RepositoryTestSupport.stagedPieces(managed.getParent()));
		assertEquals(MISSING_NAME, Files.readString(replacement));
	}

	@Test public void warningOnlyCandidateCommitsAndCarriesItsFindings() throws Exception {
		Path directory = directory();
		Path source = Files.writeString(directory.resolve("warned.xml"), WARNING_ONLY);
		var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.quietLog());

		ThemeOperationOutcome outcome = repository.importTheme(source.toFile());

		assertTrue("Warnings never block admission", outcome.successful());
		assertEquals("Warned", outcome.theme().getName());
		assertEquals(1, outcome.diagnostics().size());
		assertFalse(outcome.diagnostics().get(0).error());
		assertEquals(ThemeDiagnostic.TYPE, outcome.diagnostics().get(0).kind());
		assertEquals(List.of(outcome.theme()), repository.getAllThemes());
		assertTrue(Files.isRegularFile(outcome.theme().getFile().orElseThrow().toPath()));
	}

	@Test public void reloadSkipsInvalidInstalledFileWithoutDeletingOrDisturbingOthers() throws Exception {
		Path directory = directory();
		Path installed = Files.createDirectory(directory.resolve("installed_themes"));
		Path valid = Files.writeString(installed.resolve("valid.xml"), VALID);
		Path broken = Files.writeString(installed.resolve("broken.xml"), MISSING_NAME);
		List<String> log = new ArrayList<>();

		try (var activator = RepositoryTestSupport.installActivator()) {
			RepositoryTestSupport.installBundled(List.of());
			var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.recordingLog(log));
			repository.reload();

			assertEquals(List.of("Valid"),
					repository.getAllThemes().stream().map(Theme::getName).toList());
			assertTrue("The invalid file is not deleted", Files.isRegularFile(broken));
			assertTrue("The valid file is untouched", Files.isRegularFile(valid));
			assertEquals(List.of("broken.xml", "valid.xml"), RepositoryTestSupport.fileNames(installed));
			assertNull("An invalid file gains no identity binding",
					RepositoryTestSupport.establishedIdentity(directory, "broken.xml"));
		}

		assertTrue("The reload reports the validation failure: " + log,
				log.stream().anyMatch(line -> line.contains("broken.xml") && line.contains("non-blank 'name'")));
	}

	@Test public void reloadAdmitsWarningOnlyInstalledThemeAndLogsTheFinding() throws Exception {
		Path directory = directory();
		Path installed = Files.createDirectory(directory.resolve("installed_themes"));
		Files.writeString(installed.resolve("warned.xml"), WARNING_ONLY);
		List<String> log = new ArrayList<>();

		try (var activator = RepositoryTestSupport.installActivator()) {
			RepositoryTestSupport.installBundled(List.of());
			var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.recordingLog(log));
			repository.reload();

			assertEquals(List.of("Warned"),
					repository.getAllThemes().stream().map(Theme::getName).toList());
		}

		assertTrue("The admitted warning is logged: " + log,
				log.stream().anyMatch(line -> line.contains("not 'dark' or 'light'")));
	}

	@Test public void bundledAndInstalledRoutesRefuseTheSameDocumentForTheSameReason() throws Exception {
		Path directory = directory();
		Path installed = Files.createDirectory(directory.resolve("installed_themes"));
		Files.writeString(installed.resolve("broken.xml"), MISSING_NAME);
		Path bundledDirectory = temporary.newFolder().toPath();
		var bundled = Files.writeString(bundledDirectory.resolve("broken.xml"), MISSING_NAME).toUri().toURL();
		List<String> log = new ArrayList<>();

		try (var activator = RepositoryTestSupport.installActivator()) {
			RepositoryTestSupport.installBundled(List.of(bundled));
			var repository = new ThemeRepository(directory.toFile(), RepositoryTestSupport.recordingLog(log));
			repository.reload();

			assertEquals("Neither route admits an invalid document", List.of(), repository.getAllThemes());
		}

		assertTrue("Bundled route reports the validation reason: " + log,
				log.stream().anyMatch(line -> line.contains("Bundled theme resource is not valid")
						&& line.contains("non-blank 'name'")));
		assertTrue("Installed route reports the validation reason: " + log,
				log.stream().anyMatch(line -> line.contains("Installed theme file is not valid")
						&& line.contains("non-blank 'name'")));
	}
}

