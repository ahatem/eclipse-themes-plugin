package com.github.eclipsethemes.eclipse.adapters.editor;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public final class AntEditorThemeAdapter extends EclipseThemeAdapter {

    @Override
    public String getPreferencesId() {
        return "org.eclipse.ant.ui"; 
    }

    @Override
    public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
        // All Ant editor preferences use legacy underscore format
        mapLegacyStyle(preferences, theme, TokenKey.FOREGROUND, "org.eclipse.ant.ui.textColor");
        mapLegacyStyle(preferences, theme, TokenKey.COMMENT, "org.eclipse.ant.ui.commentsColor");
        mapLegacyStyle(preferences, theme, TokenKey.STRING, "org.eclipse.ant.ui.constantStringsColor");
        mapLegacyStyle(preferences, theme, TokenKey.XML_DIRECTIVE, "org.eclipse.ant.ui.processingInstructionsColor");
        mapLegacyStyle(preferences, theme, TokenKey.XML_DIRECTIVE, "org.eclipse.ant.ui.dtdColor");
        mapLegacyStyle(preferences, theme, TokenKey.XML_TAG, "org.eclipse.ant.ui.tagsColor");
        
        flushPreferences(preferences);
    }
}
