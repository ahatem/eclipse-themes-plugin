package com.github.eclipsethemes.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.ui.IWorkbench;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.eclipse.fonts.EclipseWorkbenchFonts;
import com.github.eclipsethemes.eclipse.fonts.FontOutcome;
import com.github.eclipsethemes.eclipse.fonts.ThemeFontCoordinator;
import com.github.eclipsethemes.eclipse.preferences.ThemeSelection;
import com.github.eclipsethemes.eclipse.workbench.EclipseWorkbenchThemeEngine;
import com.github.eclipsethemes.eclipse.workbench.PluginColorWriter;
import com.github.eclipsethemes.eclipse.workbench.RegistryPaletteSink;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchCoordinator;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchOutcome;

public final class ThemeManager {

	private final ThemeRepository repository;
	private final AdapterRegistry registry;
	private final ThemeService service;
	private final WorkbenchCoordinator workbench;
	private final ThemeFontCoordinator fonts;

	public ThemeManager(ILog log) {
		File dataDir = EclipseThemes.getPluginDataDirectory();
		this.repository = new ThemeRepository(dataDir, log);
		this.registry = new AdapterRegistry(log);
		this.workbench = new WorkbenchCoordinator(new EclipseWorkbenchThemeEngine(), new RegistryPaletteSink(),
				EclipseThemes.instance().getPreferenceStore(),
				PluginColorWriter.create(EclipseThemes.instance().getPreferenceStore()));
		this.fonts = new ThemeFontCoordinator(new EclipseWorkbenchFonts(), EclipseThemes.instance().getPreferenceStore());
		this.service = new ThemeService(registry, workbench, fonts, log);

		repository.reload();
		registry.discover();
	}

	public void loadThemes() {
		repository.reload();
	}

	public List<ThemeRecord> getAllThemes() {
		return repository.getAllThemes();
	}

	public ThemeApplyOutcome applyTheme(IWorkbench workbenchHandle, ThemeRecord theme) {
		return service.applyTheme(workbenchHandle, theme.theme());
	}

	/** Removes the listener registrations made by the workbench coordinator. */
	public void dispose() {
		workbench.dispose();
	}

	public WorkbenchCoordinator workbench() {
		return workbench;
	}

	public ThemeFontCoordinator fonts() {
		return fonts;
	}

	/** Startup: republish the workbench palette from the selected theme when coordination is active. */
	public WorkbenchOutcome reconcileWorkbench() {
		if (!workbench.isEnabled()) {
			// Coordination is off: only leftover plug-in colors from an earlier run are released.
			return workbench.reconcile(null);
		}
		return ThemeSelection.resolve(EclipseThemes.instance().getPreferenceStore(), repository.getAllThemes())
				.map(record -> workbench.reconcile(record.theme()))
				.orElseGet(() -> WorkbenchOutcome.notRequested("No theme is selected."));
	}

	public ThemeOperationOutcome importTheme(File file) {
		return repository.importTheme(file);
	}

	public ThemeOperationOutcome updateTheme(ThemeRecord selected, File file) {
		return repository.updateTheme(selected, file);
	}

	/**
	 * Writes the theme as a new theme package at {@code target}; a legacy theme is converted, an installed package is
	 * written from its model. The installed theme is never changed.
	 */
	public void exportTheme(ThemeRecord selected, File target) throws IOException {
		ThemeExporter.export(selected.theme(), target);
	}

	public boolean removeTheme(ThemeRecord selected) {
		return repository.removeTheme(selected);
	}
}
