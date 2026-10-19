package com.github.eclipsethemes.eclipse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.ILog;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.exceptions.ThemeParseException;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.parser.ParserFactory;
import com.github.eclipsethemes.core.parser.ThemeParser;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;
import com.github.eclipsethemes.core.workbench.WorkbenchColor;
import com.github.eclipsethemes.core.workbench.WorkbenchPaletteResolver;

public class ThemeRepository {

	private final File installedThemesDir;
	private final Path stagingDir;
	private final ILog log;
	private final ThemeIdentityBindings identities;
	private final TreeMap<String, ThemeRecord> themes = new TreeMap<>();

	public ThemeRepository(File pluginDataDirectory, ILog log) {
		this(pluginDataDirectory, log,
				new ThemeIdentityBindings(new File(pluginDataDirectory, "theme-identities.properties").toPath()));
	}

	// Test seam for exercising identity persistence failure without production hooks.
	ThemeRepository(File pluginDataDirectory, ILog log, ThemeIdentityBindings identities) {
		this.log = log;
		this.installedThemesDir = new File(pluginDataDirectory, "installed_themes");
		this.stagingDir = new File(installedThemesDir, ".staging").toPath();
		this.identities = identities;
		// Best effort so the first reload can list a fresh workspace; every mutating
		// operation re-checks storage and reports failure instead of assuming success.
		installedThemesDir.mkdirs();
	}

	public void reload() {
		themes.clear();
		loadPackedThemes();
		loadInstalledThemes();
	}

	public List<ThemeRecord> getAllThemes() {
		var result = new ArrayList<>(themes.values());
		result.sort(Comparator.comparing((ThemeRecord record) -> record.theme().getName())
				.thenComparing(ThemeRecord::logicalId));
		return result;
	}

	/**
	 * Imports one independent new installed record. The source basename, display
	 * name, source ID, and content never select an existing record or locator.
	 */
	public ThemeOperationOutcome importTheme(File source) {
		Path staged = null;
		Path locator = null;
		boolean moved = false;
		boolean committed = false;
		List<ThemeDiagnostic> findings = new ArrayList<>();

		try {
			ensureManagedStorage();
			Optional<ThemeParser> parser = supportedParser(source, findings);
			if (parser.isEmpty()) {
				return ThemeOperationOutcome.refused(findings);
			}

			staged = stage(source);
			// The locator keeps the source format's extension so reload selects the same parser.
			locator = allocateLocator(ParserFactory.extensionOf(source.getName()).orElseThrow());
			// The staged bytes are parsed once and become the committed bytes; the
			// source is never reopened for the commit.
			Theme candidate;
			try (InputStream input = Files.newInputStream(staged)) {
				candidate = parser.get().parse(input, locator.toFile());
			}
			reportUnreadableAuthoredRoles(candidate, locator, findings);
			// The locator was unused, so this move never replaces an existing file.
			moveWithoutReplacement(staged, locator);
			staged = null;
			moved = true;

			ThemeRecord record = ThemeRecord.installed(
					identities.newIdentityFor(locator.getFileName().toString()),
					locator.getFileName().toString(), candidate);
			themes.put(record.logicalId(), record);
			committed = true;
			return ThemeOperationOutcome.committed(record, findings);
		} catch (ThemeParseException e) {
			log.error("Failed to parse imported theme: " + source.getName() + ": " + describe(e), e);
			findings.addAll(e.diagnostics());
			return ThemeOperationOutcome.refused(findings);
		} catch (IOException e) {
			log.error("Could not import theme: " + source.getName(), e);
			return ThemeOperationOutcome.refused(findings);
		} finally {
			deleteStaged(staged);
			if (moved && !committed) {
				// Only the locator created by this attempt is removed; its binding
				// was never persisted, and no catalog record was admitted.
				deleteQuietly(locator, "incomplete imported theme");
			}
		}
	}

	/**
	 * Replaces the bytes of one explicitly selected installed record while
	 * preserving its logical identity, locator, and binding.
	 */
	public ThemeOperationOutcome updateTheme(ThemeRecord selected, File source) {
		Path staged = null;
		List<ThemeDiagnostic> findings = new ArrayList<>();

		try {
			Path target = managedTarget(selected);
			if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
				log.error("Update refused: managed theme file is missing or not a regular file: " + target);
				return ThemeOperationOutcome.refused(findings);
			}
			Optional<ThemeParser> parser = supportedParser(source, findings);
			if (parser.isEmpty()) {
				return ThemeOperationOutcome.refused(findings);
			}
			// An update keeps the record's format: the locator's extension selects its parser on every reload.
			Optional<String> format = ParserFactory.extensionOf(target.getFileName().toString());
			if (!format.equals(ParserFactory.extensionOf(source.getName()))) {
				findings.add(new ThemeDiagnostic(ThemeDiagnostic.Severity.ERROR, ThemeDiagnostic.FORMAT_MISMATCH,
						"Replacement '" + source.getName() + "' is not a " + format.orElse("?")
								+ " theme; an installed theme can only be updated with a file of the same format",
						source.getPath(), null, null));
				log.error("Update refused: format of " + source.getName() + " differs from " + target.getFileName());
				return ThemeOperationOutcome.refused(findings);
			}

			staged = stage(source);
			// Parse exactly the staged bytes while attributing the record to the
			// locator it is about to replace.
			Theme candidate;
			try (InputStream input = Files.newInputStream(staged)) {
				candidate = parser.get().parse(input, target.toFile());
			}
			reportUnreadableAuthoredRoles(candidate, target, findings);
			// A target that disappeared while staging fails without recreation.
			if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
				throw new IOException("Update target disappeared before replacement: " + target.getFileName());
			}
			replaceManagedFile(staged, target);
			staged = null;

			ThemeRecord updated = ThemeRecord.installed(selected.logicalId(),
					selected.locator().orElseThrow(), candidate);
			themes.put(updated.logicalId(), updated);
			return ThemeOperationOutcome.committed(updated, findings);
		} catch (ThemeParseException e) {
			log.error("Failed to parse replacement theme: " + source.getName() + ": " + describe(e), e);
			findings.addAll(e.diagnostics());
			return ThemeOperationOutcome.refused(findings);
		} catch (IOException e) {
			log.error("Could not update theme: " + source.getName(), e);
			return ThemeOperationOutcome.refused(findings);
		} finally {
			deleteStaged(staged);
		}
	}


	/**
	 * Removes one selected installed record. The binding is removed first; a
	 * failed file deletion restores that exact binding and reports failure.
	 */
	public boolean removeTheme(ThemeRecord selected) {
		Path target;
		String locator;
		String identity;
		try {
			target = managedTarget(selected);
			locator = target.getFileName().toString();
			identity = selected.logicalId();
			identities.remove(locator, identity);
		} catch (IOException e) {
			log.error("Removal refused or failed before file deletion: " + describe(selected), e);
			return false;
		}

		try {
			deleteManagedFile(target);
		} catch (IOException e) {
			restoreBinding(locator, identity, e);
			return false;
		}

		themes.remove(identity);
		return true;
	}

	/**
	 * Deletes exactly the managed file or confirms it is already absent. An
	 * existing path of an unexpected type/state fails closed and is not deleted.
	 */
	void deleteManagedFile(Path target) throws IOException {
		if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
			if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
				throw new IOException("Refusing to remove an unexpected managed path type: " + target.getFileName());
			}
			Files.delete(target);
		} else if (!Files.notExists(target, LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException("Cannot determine whether the managed theme file is absent: " + target.getFileName());
		}
		if (!Files.notExists(target, LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException("Managed theme file is still present after deletion: " + target.getFileName());
		}
	}

	private void restoreBinding(String locator, String identity, IOException removalFailure) {
		try {
			identities.restore(locator, identity);
			log.error("Removal failed after binding removal; the prior binding was restored: " + locator,
					removalFailure);
		} catch (IOException restoreFailure) {
			removalFailure.addSuppressed(restoreFailure);
			log.error("Removal failed and the prior binding could not be restored: " + locator, removalFailure);
		}
	}

	/**
	 * Resolves a selected record to its managed locator after all identity,
	 * location, and binding preconditions. Bundled, foreign, and unbound records
	 * are refused before any managed write.
	 */
	private Path managedTarget(ThemeRecord selected) throws IOException {
		if (selected == null || selected.origin() != ThemeOrigin.INSTALLED) {
			throw new IOException("Only installed themes can be changed: " + describe(selected));
		}
		String locator = selected.locator()
				.orElseThrow(() -> new IOException("Selected installed theme has no managed locator"));
		Path target = installedThemesDir.toPath().resolve(locator).toAbsolutePath().normalize();
		Path managedDirectory = installedThemesDir.toPath().toAbsolutePath().normalize();
		if (!managedDirectory.equals(target.getParent())
				|| target.getFileName() == null
				|| !target.getFileName().toString().equals(locator)
				|| ParserFactory.extensionOf(target.getFileName().toString()).isEmpty()) {
			throw new IOException("Selected theme is outside managed theme storage: " + target);
		}

		String established = identities.establishedIdentityFor(locator)
				.orElseThrow(() -> new IOException("No established identity binding for " + locator));
		if (!established.equals(selected.logicalId())) {
			throw new IOException("Identity binding for " + locator + " does not match the selected record");
		}
		return target;
	}

	private void ensureManagedStorage() throws IOException {
		try {
			Files.createDirectories(installedThemesDir.toPath());
		} catch (FileAlreadyExistsException e) {
			throw new IOException("Managed theme storage is not a directory: " + installedThemesDir, e);
		}
		if (!Files.isDirectory(installedThemesDir.toPath(), LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException("Managed theme storage is not a directory: " + installedThemesDir);
		}
	}

	/** One copy of the selected bytes into an attempt-owned staging file. */
	private Path stage(File source) throws IOException {
		Files.createDirectories(stagingDir);
		if (!Files.isDirectory(stagingDir, LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException("Theme staging location is not a directory: " + stagingDir);
		}
		Path staged = Files.createTempFile(stagingDir, "theme-", ".part");
		try {
			Files.copy(source.toPath(), staged, StandardCopyOption.REPLACE_EXISTING);
			return staged;
		} catch (IOException e) {
			try {
				Files.deleteIfExists(staged);
			} catch (IOException cleanupFailure) {
				e.addSuppressed(cleanupFailure);
			}
			throw e;
		}
	}

	private Optional<ThemeParser> supportedParser(File source, List<ThemeDiagnostic> findings) {
		Optional<ThemeParser> parser = ParserFactory.getParserFor(source.getName(), admissionSink(findings));
		if (parser.isEmpty()) {
			log.error("Unsupported theme file type: " + source.getName());
		}
		return parser;
	}

	/**
	 * Admission-time report of authored workbench roles that coordination would not show verbatim. The palette is only
	 * computed here, never applied: importing a package does not change workbench coordination.
	 */
	private void reportUnreadableAuthoredRoles(Theme candidate, Path locator, List<ThemeDiagnostic> findings) {
		for (WorkbenchColor role : WorkbenchPaletteResolver.unreadableAuthoredRoles(candidate)) {
			admissionSink(findings).accept(new ThemeDiagnostic(ThemeDiagnostic.Severity.WARNING,
					ThemeDiagnostic.WORKBENCH_READABILITY, "Workbench role '" + role.schemaName()
							+ "' is not readable with the other workbench colors; it will be adjusted when applied",
					locator.toString(), role.schemaName(), "color"));
		}
	}

	/** Admission sink: findings are returned to the caller and logged. */
	private Consumer<ThemeDiagnostic> admissionSink(List<ThemeDiagnostic> findings) {
		return finding -> {
			findings.add(finding);
			log.warn(finding.message());
		};
	}

	/** Reload sink: findings describe admitted or rejected files and are only logged. */
	private Consumer<ThemeDiagnostic> loggingSink() {
		return finding -> log.warn(finding.message());
	}

	/** Chooses a repository-owned locator unused on disk and in identity metadata. */
	private Path allocateLocator(String extension) throws IOException {
		Path directory = installedThemesDir.toPath();
		for (int attempt = 0; attempt < 10; attempt++) {
			Path candidate = directory.resolve(UUID.randomUUID() + extension);
			if (Files.notExists(candidate, LinkOption.NOFOLLOW_LINKS)
					&& identities.establishedIdentityFor(candidate.getFileName().toString()).isEmpty()) {
				return candidate;
			}
		}
		throw new IOException("Could not allocate an unused managed theme locator");
	}

	private void moveWithoutReplacement(Path staged, Path locator) throws IOException {
		// Deliberately no REPLACE_EXISTING: an unexpectedly occupied locator fails.
		Files.move(staged, locator);
	}

	// Test seam for exercising atomic replacement failure without a fallback.
	void replaceManagedFile(Path staged, Path target) throws IOException {
		// No non-atomic fallback: an unsupported atomic move fails truthfully.
		Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
	}

	private void deleteStaged(Path staged) {
		if (staged != null) {
			deleteQuietly(staged, "staged theme data");
		}
	}

	private void deleteQuietly(Path path, String description) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			log.error("Could not remove " + description + ": " + path.getFileName(), e);
		}
	}

	private static String describe(ThemeRecord record) {
		return record == null ? "no selection" : record.theme().getName();
	}

	private static String describe(ThemeParseException failure) {
		String message = failure.getMessage();
		return message == null || message.isBlank() ? "theme document is not valid" : message;
	}


	private void loadPackedThemes() {
		var bundle = EclipseThemes.instance().getBundle();
		var entries = bundle.findEntries("themes", "*.xml", false);
		if (entries == null) return;

		while (entries.hasMoreElements()) {
			URL res = entries.nextElement();
			String path = res.getPath();

			try (var in = res.openStream()) {
				var parserOpt = ParserFactory.getParserFor(path, loggingSink());
				if (parserOpt.isEmpty()) {
					log.warn("No parser for: " + path);
					continue;
				}

				Theme theme = parserOpt.get().parse(in);
				if (theme.getId() == null || theme.getId().isBlank()) {
					log.warn("Bundled theme without curated identity: " + path);
					continue;
				}
				ThemeRecord record = ThemeRecord.bundled("bundled:" + theme.getId(), theme);
				if (themes.putIfAbsent(record.logicalId(), record) != null) {
					log.warn("Duplicate bundled theme identity: " + path);
				}
			} catch (ThemeParseException e) {
				log.error("Bundled theme resource is not valid: " + path + ": " + describe(e), e);
			} catch (Exception e) {
				log.error("Failed to load theme: " + path, e);
			}
		}
	}

	private void loadInstalledThemes() {
		try (var filesStream = Files.list(installedThemesDir.toPath())) {
			List<Path> themeFiles = filesStream
					.filter(p -> !Files.isDirectory(p))
					.filter(p -> ParserFactory.extensionOf(p.getFileName().toString()).isPresent())
					.collect(Collectors.toList());

			for (Path themePath : themeFiles) {
				File themeFile = themePath.toFile();
				Optional<ThemeParser> parserOpt = ParserFactory.getParserFor(themeFile.getName(), loggingSink());
				if (parserOpt.isEmpty()) {
					log.info("No parser found for file: " + themeFile.getName());
					continue;
				}

				try {
					ThemeRecord record = installedIdentity(parserOpt.get().parse(themeFile), themeFile);
					themes.put(record.logicalId(), record);
				} catch (ThemeParseException e) {
					log.error("Installed theme file is not valid: " + themeFile.getName() + ": " + describe(e), e);
				} catch (IOException e) {
					log.error("Could not establish theme identity: " + themeFile.getName(), e);
				}
			}
		} catch (IOException e) {
			log.error("Could not load themes", e);
		}
	}

	private ThemeRecord installedIdentity(Theme candidate, File file) throws IOException {
		String locator = file.getName();
		return ThemeRecord.installed(identities.identityFor(locator), locator, candidate);
	}
}
