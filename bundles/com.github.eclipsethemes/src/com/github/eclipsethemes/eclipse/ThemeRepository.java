package com.github.eclipsethemes.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.ILog;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.exceptions.ThemeParseException;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.parser.ParserFactory;
import com.github.eclipsethemes.core.parser.ThemeParser;

public final class ThemeRepository {

	private final File installedThemesDir;
	private final ILog log;
	private final ThemeIdentityBindings identities;
	private final TreeMap<String, Theme> themes = new TreeMap<>();

	public ThemeRepository(File pluginDataDirectory, ILog log) {
		this(pluginDataDirectory, log,
				new ThemeIdentityBindings(new File(pluginDataDirectory, "theme-identities.properties").toPath()));
	}

	// Test seam for exercising identity persistence failure without production hooks.
	ThemeRepository(File pluginDataDirectory, ILog log, ThemeIdentityBindings identities) {
		this.log = log;
		this.installedThemesDir = new File(pluginDataDirectory, "installed_themes");
		this.identities = identities;
		if (!installedThemesDir.exists()) {
			installedThemesDir.mkdirs();
		}
	}

	public void reload() {
		themes.clear();
		loadPackedThemes();
		loadInstalledThemes();
	}

	public List<Theme> getAllThemes() {
		var result = new ArrayList<>(themes.values());
		result.sort(Comparator.comparing(Theme::getName).thenComparing(Theme::getLogicalId));
		return result;
	}

	public Optional<Theme> importTheme(File file) {
		File copied = new File(installedThemesDir, file.getName());
		boolean created = false;

		try {
			// PM6-A: a locator must be unused both on disk and in identity metadata.
			// Unknown filesystem state also fails closed; no destination has been opened.
			if (!Files.notExists(copied.toPath(), LinkOption.NOFOLLOW_LINKS)) {
				log.error("Import refused: managed theme locator is occupied or unavailable: " + copied.getName());
				return Optional.empty();
			}
			identities.requireUnused(copied.getName());
			Optional<ThemeParser> parserOpt = ParserFactory.getParserFor(file.getName(), log::warn);
			if (parserOpt.isEmpty()) {
				log.error("No parser found for theme: " + file.getName());
				return Optional.empty();
			}

			Theme candidate;
			try (var in = new FileInputStream(file)) {
				candidate = parserOpt.get().parse(in, copied);
			}
			// Establish the new file before its identity. A failure at either step removes only
			// the destination created by this attempt; no binding or catalog entry exists yet.
			try (var in = new FileInputStream(file); var out = Files.newOutputStream(copied.toPath(),
					StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
				created = true;
				in.transferTo(out);
			}
			// Imports establish a new identity; only reload may reuse an existing binding.
			Theme theme = candidate.withLogicalId(identities.newIdentityFor(copied.getName()));
			themes.put(theme.getLogicalId(), theme);
			return Optional.of(theme);
		} catch (ThemeParseException e) {
			log.error("Failed to parse imported theme: " + file.getName(), e);
		} catch (IOException e) {
			if (created) {
				discardNewDestination(copied);
			}
			log.error("Could not admit imported theme or persist identity: " + copied.getName(), e);
		}

		return Optional.empty();
	}

	/**
	 * Best-effort removal of a destination created by the current import attempt. The
	 * caller only invokes this after exclusive creation succeeded, so a pre-existing file
	 * is never deleted.
	 */
	private void discardNewDestination(File destination) {
		try {
			Files.deleteIfExists(destination.toPath());
		} catch (IOException e) {
			log.error("Could not remove incomplete imported theme: " + destination.getName(), e);
		}
	}

	private void loadPackedThemes() {
		var bundle = EclipseThemes.instance().getBundle();
		var entries = bundle.findEntries("themes", "*.xml", false);
		if (entries == null) return;

		while (entries.hasMoreElements()) {
			URL res = entries.nextElement();
			String path = res.getPath();

			try (var in = res.openStream()) {
				var parserOpt = ParserFactory.getParserFor(path, log::warn);
				if (parserOpt.isEmpty()) {
					log.warn("No parser for: " + path);
					continue;
				}

				Theme theme = parserOpt.get().parse(in);
				if (theme.getName() == null || theme.getName().isBlank()) {
					log.warn("Theme without name: " + path);
					continue;
				}

				if (theme.getId() == null || theme.getId().isBlank()) {
					log.warn("Bundled theme without curated identity: " + path);
					continue;
				}
				theme = theme.withLogicalId("bundled:" + theme.getId());
				if (themes.putIfAbsent(theme.getLogicalId(), theme) != null) {
					log.warn("Duplicate bundled theme identity: " + path);
				}
			} catch (Exception e) {
				log.error("Failed to load theme: " + path, e);
			}
		}
	}

	private void loadInstalledThemes() {
		try (var filesStream = Files.list(installedThemesDir.toPath())) {
			List<Path> themeFiles = filesStream
					.filter(p -> !Files.isDirectory(p))
					.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml"))
					.collect(Collectors.toList());

			for (Path themePath : themeFiles) {
				File themeFile = themePath.toFile();
				Optional<ThemeParser> parserOpt = ParserFactory.getParserFor(themeFile.getName(), log::warn);
				if (parserOpt.isEmpty()) {
					log.info("No parser found for file: " + themeFile.getName());
					continue;
				}

				try {
					Theme theme = installedIdentity(parserOpt.get().parse(themeFile), themeFile);
					themes.put(theme.getLogicalId(), theme);
				} catch (ThemeParseException e) {
					log.error("Failed to parse imported theme: " + themeFile.getName(), e);
				} catch (IOException e) {
					log.error("Could not establish theme identity: " + themeFile.getName(), e);
				}
			}
		} catch (IOException e) {
			log.error("Could not load themes", e);
		}
	}

	private Theme installedIdentity(Theme candidate, File file) throws IOException {
		return candidate.withLogicalId(identities.identityFor(file.getName()));
	}
}
