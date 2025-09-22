package com.github.eclipsethemes.theme.adapters;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.theme.models.Theme;
import com.github.eclipsethemes.theme.models.TokenKey;

public final class GenericTextEditorAdapter extends ThemeAdapter {

    @Override
    public String getPreferencesId() {
        return "org.eclipse.ui.editors"; // Generic text editor preferences node
    }

    @Override
    public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
        // Simple color mapping for matching brackets
        putColor(preferences, "matchingBracketsColor", theme.get(TokenKey.MATCHING_BRACKET));
        
        flushPreferences(preferences);
    }
}
