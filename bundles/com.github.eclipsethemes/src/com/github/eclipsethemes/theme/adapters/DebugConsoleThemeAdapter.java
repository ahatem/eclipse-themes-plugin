package com.github.eclipsethemes.theme.adapters;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.theme.models.Theme;
import com.github.eclipsethemes.theme.models.TokenKey;

public final class DebugConsoleThemeAdapter extends EclipseThemeAdapter {

    @Override
    public String getPreferencesId() {
        return "org.eclipse.debug.ui"; // Debug UI preferences node
    }

    @Override
    public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
        // Debug console colors - these are simple color mappings without style flags
        putColor(preferences, "org.eclipse.debug.ui.consoleBackground", theme.get(TokenKey.BACKGROUND));
        putColor(preferences, "org.eclipse.debug.ui.outColor", theme.get(TokenKey.FOREGROUND));
        putColor(preferences, "org.eclipse.debug.ui.errorColor", theme.get(TokenKey.ERROR));
        
        flushPreferences(preferences);
    }
}