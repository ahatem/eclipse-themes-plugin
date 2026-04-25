package com.github.eclipsethemes.eclipse;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.IWorkbench;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public final class ThemeService {

	private final AdapterRegistry registry;
	private final ILog log;

	public ThemeService(AdapterRegistry registry, ILog log) {
		this.registry = registry;
		this.log = log;
	}

	public void applyTheme(IWorkbench workbench, Theme theme) {
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
				log.error("Could not apply theme via adapter " + adapter.getClass().getSimpleName(), e);
			}
		});
	}
}
