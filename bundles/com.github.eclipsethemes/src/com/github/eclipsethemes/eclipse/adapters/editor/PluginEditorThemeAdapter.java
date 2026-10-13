package com.github.eclipsethemes.eclipse.adapters.editor;

import java.util.List;
import java.util.Set;

import static com.github.eclipsethemes.eclipse.adapters.ManagedPreference.*;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.ManagedPreference;

public final class PluginEditorThemeAdapter extends EclipseThemeAdapter {

    @Override
    public String getPreferencesId() {
        return "org.eclipse.pde.ui"; // PDE editor preferences node
    }

    private static final List<ManagedPreference> MAPPINGS = List.of(
        // PDE editor uses legacy underscore format for style preferences
        legacyStyle(TokenKey.FOREGROUND, "editor.color.default"),
        legacyStyle(TokenKey.OPERATOR, "editor.color.header_assignment"),
        legacyStyle(TokenKey.COMMENT, "editor.color.xml_comment"),
        legacyStyle(TokenKey.STRING, "editor.color.string"),
        legacyStyle(TokenKey.STRING, "editor.color.externalized_string"),
        legacyStyle(TokenKey.STRING, "editor.color.header_value"),
        legacyStyle(TokenKey.XML_DIRECTIVE, "editor.color.instr"),
        legacyStyle(TokenKey.XML_TAG, "editor.color.tag"),
        legacyStyle(TokenKey.XML_ATTRIBUTE, "editor.color.attribute"),
        legacyStyle(TokenKey.KEY, "editor.color.header_attributes"),
        legacyStyle(TokenKey.KEY, "editor.color.header_osgi")
    );

    @Override
    public Set<String> getManagedKeys() {
        return managedKeys(MAPPINGS);
    }

    @Override
    public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
        applyMappings(MAPPINGS, theme, preferences);
    }
}
