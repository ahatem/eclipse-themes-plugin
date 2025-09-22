package com.github.eclipsethemes.theme;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.IWorkbench;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.theme.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.theme.models.Theme;
import com.github.eclipsethemes.theme.parser.ParserFactory;
import com.github.eclipsethemes.theme.parser.ThemeParseException;
import com.github.eclipsethemes.theme.parser.ThemeParser;

public final class ThemeManager {

	private final File installedThemesDir;
	private final TreeMap<String, Theme> themes = new TreeMap<>();
	private final Map<EclipseThemeAdapter, String> adapters = new HashMap<>();

	public ThemeManager() {
		this.installedThemesDir = new File(EclipseThemes.getPluginDataDirectory(), "installed_themes");
		if (!installedThemesDir.exists()) {
			installedThemesDir.mkdirs();
		}

		loadThemes();
		registerAdapters();
	}

	private void loadThemes() {
		themes.clear();
		loadPackedThemes();
		loadInstalledThemes();
	}

	private void loadPackedThemes() {
		var bundle = EclipseThemes.instance().getBundle();
		var entries = bundle.findEntries("themes", "*.xml", false);
		if (entries == null)
			return;

		while (entries.hasMoreElements()) {
			URL res = entries.nextElement();
			String path = res.getPath();

			try (var in = res.openStream()) {
				var parserOpt = ParserFactory.getParserFor(path);
				if (parserOpt.isEmpty()) {
					EclipseThemes.instance().getLogger().warn("No parser for: " + path);
					continue;
				}

				Theme theme = parserOpt.get().parse(in);
				if (theme.getName() == null || theme.getName().isBlank()) {
					EclipseThemes.instance().getLogger().warn("Theme without name: " + path);
					continue;
				}

				this.themes.put(theme.getName(), theme);
			} catch (Exception e) {
				EclipseThemes.instance().getLogger().error("Failed to load theme: " + path, e);
			}
		}
	}

	private void loadInstalledThemes() {

		try (var filesStream = Files.list(this.installedThemesDir.toPath())) {
			List<Path> themeFiles = filesStream.filter(file -> !Files.isDirectory(file))
					.filter(file -> file.getFileName().toString().endsWith(".xml")).collect(Collectors.toList());

			for (Path themePath : themeFiles) {
				File themeFile = themePath.toFile();
				Optional<ThemeParser> parserOpt = ParserFactory.getParserFor(themeFile.getName());

				if (!parserOpt.isPresent()) {
					EclipseThemes.instance().getLogger().info("No Parser Found for file: " + themeFile.getName());
					continue;
				}

				ThemeParser parser = parserOpt.get();

				try {
					Theme theme = parser.parse(themeFile);
					this.themes.put(theme.getName(), theme);
				} catch (ThemeParseException e) {
					EclipseThemes.instance().getLogger().error("Failed to parse imported theme: " + themeFile.getName(),
							e);
				}
			}

		} catch (IOException e) {
			EclipseThemes.instance().getLogger().error("Could not load themes", e);
		}
	}

	private void registerAdapters() {
		var registerdAdapters = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(EclipseThemes.ADAPTER_EXT_ID);
		for (IConfigurationElement element : registerdAdapters) {
			if (!element.getName().equals("adapter")) {
				continue;
			}

			try {
				Object adapter = element.createExecutableExtension("class");
				if (!(adapter instanceof EclipseThemeAdapter)) {
					continue;
				}

				adapters.put((EclipseThemeAdapter) adapter, element.getAttribute("plugin"));
			} catch (CoreException error) {
				EclipseThemes.instance().getLogger()
						.error("Could not register adapter " + element.getAttribute("class"), error);
			}
		}
		
		System.out.println("Adapters: " + adapters.values());
	}

	public List<Theme> getAllThemes() {
		System.out.println(themes.values());
		return new ArrayList<>(themes.values());
	}

	public void applyTheme(IWorkbench workbench, Theme selectedTheme) {
		adapters.forEach((adapter, plugin) -> {
			if (plugin != null) {
				if (Platform.getBundle(plugin) == null) {
					return;
				}
			} else if (adapter.getPreferencesId() == null) {
				return;
			}

			try {
				String id = adapter.getPreferencesId() == null ? plugin : adapter.getPreferencesId();
				IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(id);
				adapter.apply(selectedTheme, preferences);
			} catch (BackingStoreException error) {
				EclipseThemes.instance().getLogger().error("Could not apply theme", error);
			}
		});
	}
}
