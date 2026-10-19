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

	/** Writes the editor-side preferences for a theme and returns the names of the adapters that failed. */
	interface EditorWriter {
		List<String> write(Theme theme);
	}

	private final EditorWriter editor;
	private final WorkbenchCoordinator workbench;
	private final ILog log;

	public ThemeService(AdapterRegistry registry, WorkbenchCoordinator workbench, ILog log) {
		this(theme -> writeEditorPreferences(registry, theme, log), workbench, log);
	}

	ThemeService(EditorWriter editor, WorkbenchCoordinator workbench, ILog log) {
		this.editor = editor;
		this.workbench = workbench;
		this.log = log;
	}

	/**
	 * Applies the workbench half, then the editor preferences; each part reports for itself.
	 *
	 * <p>
	 * The order is deliberate. Switching the Eclipse CSS theme makes the theme engine synchronously reset the
	 * preference defaults the previous theme overrode, and that reset also deletes instance values for the same
	 * keys (for example {@code AbstractTextEditor.Color.Background.SystemDefault} after leaving the stock dark
	 * theme). Writing the editor preferences first therefore lost them and left the editor light after one Apply.
	 * Writing them after the switch makes the plugin's own values the last word, and only the keys the plugin owns
	 * are written.
	 */
	public ThemeApplyOutcome applyTheme(IWorkbench workbenchHandle, Theme theme) {
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
		List<String> failedAdapters = editor.write(theme);
		return new ThemeApplyOutcome(new ThemeApplyOutcome.EditorOutcome(failedAdapters), workbenchOutcome);
	}

	private static List<String> writeEditorPreferences(AdapterRegistry registry, Theme theme, ILog log) {
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
		return failedAdapters;
	}
}
