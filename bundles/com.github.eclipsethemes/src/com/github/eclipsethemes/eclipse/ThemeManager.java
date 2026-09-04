package com.github.eclipsethemes.eclipse;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.ILog;
import org.eclipse.ui.IWorkbench;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.eclipse.preferences.ColorCustomizationStore;

public final class ThemeManager {

	private final ThemeRepository repository;
	private final AdapterRegistry registry;
	private final ThemeService service;

	public ThemeManager(ILog log) {
		File dataDir = EclipseThemes.getPluginDataDirectory();
		this.repository = new ThemeRepository(dataDir, log);
		this.registry = new AdapterRegistry(log);
		this.service = new ThemeService(registry, log);

		repository.reload();
		registry.discover();
	}

	public void loadThemes() {
		repository.reload();
	}

	public List<Theme> getAllThemes() {
		return repository.getAllThemes();
	}

	public void applyTheme(IWorkbench workbench, Theme theme) {
		service.applyTheme(workbench, ColorCustomizationStore.customize(theme));
	}

	public Optional<Theme> findById(String id) {
		return repository.findById(id);
	}

	public void restoreActiveTheme(IWorkbench workbench, String themeId) {
		repository.findById(themeId).ifPresent(theme -> applyTheme(workbench, theme));
	}

	public Optional<Theme> importTheme(File file) {
		return repository.importTheme(file);
	}
}
