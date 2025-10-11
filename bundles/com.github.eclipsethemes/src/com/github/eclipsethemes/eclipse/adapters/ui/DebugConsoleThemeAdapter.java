package com.github.eclipsethemes.eclipse.adapters.ui;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public final class DebugConsoleThemeAdapter extends EclipseThemeAdapter {

	@Override
	public String getPreferencesId() {
		return "org.eclipse.debug.ui";
	}

	@Override
	public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
		putColor(preferences, "org.eclipse.debug.ui.consoleBackground", theme.get(TokenKey.BACKGROUND));
		putColor(preferences, "org.eclipse.debug.ui.outColor", theme.get(TokenKey.FOREGROUND));
		putColor(preferences, "org.eclipse.debug.ui.errorColor", theme.get(TokenKey.ERROR));

		flushPreferences(preferences);
	}

	@Override
	public void clear(IEclipsePreferences preferences) throws BackingStoreException {
		preferences.remove("org.eclipse.debug.ui.consoleBackground");
		preferences.remove("org.eclipse.debug.ui.outColor");
		preferences.remove("org.eclipse.debug.ui.errorColor");

		flushPreferences(preferences);
	}
}