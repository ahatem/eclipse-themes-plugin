package com.github.eclipsethemes.eclipse.adapters.editor;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public final class GenericEditorThemeAdapter extends EclipseThemeAdapter {

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
