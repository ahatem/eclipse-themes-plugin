package com.github.eclipsethemes.theme.adapters;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.theme.models.Theme;
import com.github.eclipsethemes.theme.models.TokenKey;

public final class PluginEditorThemeAdapter extends EclipseThemeAdapter {

    @Override
    public String getPreferencesId() {
        return "org.eclipse.pde.ui"; // PDE editor preferences node
    }

    @Override
    public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
        // PDE editor uses legacy underscore format for style preferences
        mapLegacyStyle(preferences, theme, TokenKey.FOREGROUND, "editor.color.default");
        mapLegacyStyle(preferences, theme, TokenKey.OPERATOR, "editor.color.header_assignment");
        mapLegacyStyle(preferences, theme, TokenKey.COMMENT, "editor.color.xml_comment");
        mapLegacyStyle(preferences, theme, TokenKey.STRING, "editor.color.string");
        mapLegacyStyle(preferences, theme, TokenKey.STRING, "editor.color.externalized_string");
        mapLegacyStyle(preferences, theme, TokenKey.STRING, "editor.color.header_value");
        mapLegacyStyle(preferences, theme, TokenKey.XML_DIRECTIVE, "editor.color.instr");
        mapLegacyStyle(preferences, theme, TokenKey.XML_TAG, "editor.color.tag");
        mapLegacyStyle(preferences, theme, TokenKey.KEY, "editor.color.header_attributes");
        mapLegacyStyle(preferences, theme, TokenKey.KEY, "editor.color.header_osgi");
        
        flushPreferences(preferences);
    }
}
