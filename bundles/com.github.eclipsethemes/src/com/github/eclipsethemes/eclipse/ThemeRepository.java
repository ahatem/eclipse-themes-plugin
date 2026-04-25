package com.github.eclipsethemes.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
	private final TreeMap<String, Theme> themes = new TreeMap<>();

	public ThemeRepository(File pluginDataDirectory, ILog log) {
		this.log = log;
		this.installedThemesDir = new File(pluginDataDirectory, "installed_themes");
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
		return new ArrayList<>(themes.values());
	}

	public Optional<Theme> importTheme(File file) {
		File copied = new File(installedThemesDir, file.getName());

		try (var in = new FileInputStream(file); var out = new FileOutputStream(copied)) {
			in.transferTo(out);
		} catch (IOException e) {
			log.error("Could not copy theme file", e);
			return Optional.empty();
		}

		try {
			Optional<ThemeParser> parserOpt = ParserFactory.getParserFor(file.getName());
			if (parserOpt.isEmpty()) {
				log.error("No parser found for theme: " + file.getName());
				return Optional.empty();
			}

			try (var in = new FileInputStream(copied)) {
				Theme theme = parserOpt.get().parse(in, copied);
				if (themes.containsKey(theme.getName())) {
					log.info("Re-importing theme '" + theme.getName() + "' — overwriting previous version");
				}
				themes.put(theme.getName(), theme);
				return Optional.of(theme);
			}
		} catch (ThemeParseException e) {
			log.error("Failed to parse imported theme: " + file.getName(), e);
			copied.delete();
		} catch (IOException e) {
			log.error("Could not open copied theme file: " + copied.getName(), e);
		}

		return Optional.empty();
	}

	private void loadPackedThemes() {
		var bundle = EclipseThemes.instance().getBundle();
		var entries = bundle.findEntries("themes", "*.xml", false);
		if (entries == null) return;

		while (entries.hasMoreElements()) {
			URL res = entries.nextElement();
			String path = res.getPath();

			try (var in = res.openStream()) {
				var parserOpt = ParserFactory.getParserFor(path);
				if (parserOpt.isEmpty()) {
					log.warn("No parser for: " + path);
					continue;
				}

				Theme theme = parserOpt.get().parse(in);
				if (theme.getName() == null || theme.getName().isBlank()) {
					log.warn("Theme without name: " + path);
					continue;
				}

				if (themes.containsKey(theme.getName())) {
					log.warn("Duplicate theme name '" + theme.getName() + "' in " + path + " — skipping");
					continue;
				}
				themes.put(theme.getName(), theme);
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
				Optional<ThemeParser> parserOpt = ParserFactory.getParserFor(themeFile.getName());
				if (parserOpt.isEmpty()) {
					log.info("No parser found for file: " + themeFile.getName());
					continue;
				}

				try {
					Theme theme = parserOpt.get().parse(themeFile);
					if (themes.containsKey(theme.getName())) {
						log.warn("Duplicate theme name '" + theme.getName() + "' in " + themeFile.getName() + " — skipping");
					} else {
						themes.put(theme.getName(), theme);
					}
				} catch (ThemeParseException e) {
					log.error("Failed to parse imported theme: " + themeFile.getName(), e);
				}
			}
		} catch (IOException e) {
			log.error("Could not load themes", e);
		}
	}
}
