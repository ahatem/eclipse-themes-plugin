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

public final class AntEditorThemeAdapter extends EclipseThemeAdapter {

    @Override
    public String getPreferencesId() {
        return "org.eclipse.ant.ui"; 
    }

    private static final List<ManagedPreference> MAPPINGS = List.of(
        // All Ant editor preferences use legacy underscore format
        legacyStyle(TokenKey.FOREGROUND, "org.eclipse.ant.ui.textColor"),
        legacyStyle(TokenKey.COMMENT, "org.eclipse.ant.ui.commentsColor"),
        legacyStyle(TokenKey.STRING, "org.eclipse.ant.ui.constantStringsColor"),
        legacyStyle(TokenKey.XML_DIRECTIVE, "org.eclipse.ant.ui.processingInstructionsColor"),
        legacyStyle(TokenKey.XML_DIRECTIVE, "org.eclipse.ant.ui.dtdColor"),
        legacyStyle(TokenKey.XML_TAG, "org.eclipse.ant.ui.tagsColor"),
        legacyStyle(TokenKey.XML_ATTRIBUTE, "org.eclipse.ant.ui.attributeColor")
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
