package com.github.eclipsethemes.eclipse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.IWorkbench;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchCoordinator;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchOutcome;

public final class ThemeService {

	private final AdapterRegistry registry;
	private final WorkbenchCoordinator workbench;
	private final ILog log;

	public ThemeService(AdapterRegistry registry, WorkbenchCoordinator workbench, ILog log) {
		this.registry = registry;
		this.workbench = workbench;
		this.log = log;
	}

	/** Applies the editor preferences, then the workbench half; each part reports for itself. */
	public ThemeApplyOutcome applyTheme(IWorkbench workbenchHandle, Theme theme) {
		List<String> failedAdapters = new ArrayList<>();
		registry.getAdapters().forEach((adapter, plugin) -> {
			// If a required plugin is declared, skip when it is not installed
			if (plugin != null && Platform.getBundle(plugin) == null) return;

			try {
				String prefNodeId = adapter.getPreferencesId();
				IEclipsePreferences prefs = prefNodeId != null
						? InstanceScope.INSTANCE.getNode(prefNodeId)
						: null;
				adapter.apply(theme, prefs);
			} catch (BackingStoreException e) {
				failedAdapters.add(adapter.getClass().getSimpleName());
				log.error("Could not apply theme via adapter " + adapter.getClass().getSimpleName(), e);
			}
		});

		WorkbenchOutcome workbenchOutcome;
		try {
			workbenchOutcome = workbench.apply(theme);
		} catch (RuntimeException | LinkageError e) {
			log.error("Workbench theming failed unexpectedly", e);
			workbenchOutcome = WorkbenchOutcome.failed(null, "Workbench theming failed unexpectedly: " + e);
		}
		if (!workbenchOutcome.succeeded()) {
			log.warn("Workbench theming: " + workbenchOutcome.status() + " " + workbenchOutcome.message());
		}
		return new ThemeApplyOutcome(new ThemeApplyOutcome.EditorOutcome(failedAdapters), workbenchOutcome);
	}
}
