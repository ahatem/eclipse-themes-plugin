package com.github.eclipsethemes.eclipse;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.ui.IWorkbench;

import com.github.eclipsethemes.EclipseThemes;

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

	public List<ThemeRecord> getAllThemes() {
		return repository.getAllThemes();
	}

	public void applyTheme(IWorkbench workbench, ThemeRecord theme) {
		service.applyTheme(workbench, theme.theme());
	}

	public ThemeOperationOutcome importTheme(File file) {
		return repository.importTheme(file);
	}

	public ThemeOperationOutcome updateTheme(ThemeRecord selected, File file) {
		return repository.updateTheme(selected, file);
	}

	public boolean removeTheme(ThemeRecord selected) {
		return repository.removeTheme(selected);
	}
}
