package com.github.eclipsethemes.eclipse;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;

/**
 * Re-registers the generated workbench stylesheet after Eclipse has created its
 * theme engine. Editor preference colors persist without this hook.
 */
public final class ThemeStartup implements IStartup {

	@Override
	public void earlyStartup() {
		var preferences = InstanceScope.INSTANCE.getNode(EclipseThemes.PLUGIN_ID);
		String themeId = preferences.get(PreferenceKeys.ACTIVE_THEME_ID, null);
		if (themeId == null || themeId.isBlank()) {
			return;
		}

		var workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().asyncExec(() ->
				EclipseThemes.instance().getManager().restoreActiveTheme(workbench, themeId));
	}
}
