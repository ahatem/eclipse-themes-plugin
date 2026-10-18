package com.github.eclipsethemes.eclipse.workbench;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;

/**
 * Republishes the workbench palette once per start. Does nothing, and loads no themes, unless the user has
 * enabled workbench coordination.
 */
public final class WorkbenchStartup implements IStartup {

	@Override
	public void earlyStartup() {
		EclipseThemes plugin = EclipseThemes.instance();
		if (plugin == null || !plugin.getPreferenceStore().getBoolean(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED)) {
			return;
		}
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			try {
				WorkbenchOutcome outcome = plugin.getManager().reconcileWorkbench();
				if (!outcome.succeeded()) {
					plugin.getLogger().warn("Workbench theming at startup: " + outcome.status() + " " + outcome.message());
				}
			} catch (RuntimeException | LinkageError e) {
				plugin.getLogger().error("Workbench theming at startup failed", e);
			}
		});
	}
}
