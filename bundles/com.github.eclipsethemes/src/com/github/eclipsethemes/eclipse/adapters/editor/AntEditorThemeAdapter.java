package com.github.eclipsethemes.eclipse.adapters.editor;

import java.util.List;
import java.util.Set;

import static com.github.eclipsethemes.eclipse.adapters.ManagedPreference.*;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.TokenOptions;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.ManagedPreference;

public final class AntEditorThemeAdapter extends EclipseThemeAdapter {

    private static ManagedPreference antStyle(TokenKey key, String base) {
        // Eclipse R4_33 AntSourceViewerConfiguration/AbstractAntEditorScanner:
        // only bold/italic are style suffixes; underline/strike are parsed as RGB.
        return new ManagedPreference(List.of(base, base + "_bold", base + "_italic"), (theme, prefs) -> {
            var token = theme.get(key);
            var options = token.getOptions().orElse(TokenOptions.EMPTY);
            putColor(prefs, base, token);
            prefs.putBoolean(base + "_bold", options.bold());
            prefs.putBoolean(base + "_italic", options.italic());
        });
    }

    @Override
    public String getPreferencesId() {
        return "org.eclipse.ant.ui"; 
    }

    private static final List<ManagedPreference> MAPPINGS = List.of(
        // All Ant editor preferences use legacy underscore format
        antStyle(TokenKey.FOREGROUND, "org.eclipse.ant.ui.textColor"),
        // Ant XML comments use the specific role, with the one-way generic-comment fallback.
        antStyle(TokenKey.XML_COMMENT, "org.eclipse.ant.ui.commentsColor"),
        antStyle(TokenKey.STRING, "org.eclipse.ant.ui.constantStringsColor"),
        antStyle(TokenKey.XML_DIRECTIVE, "org.eclipse.ant.ui.processingInstructionsColor"),
        antStyle(TokenKey.XML_DIRECTIVE, "org.eclipse.ant.ui.dtdColor"),
        antStyle(TokenKey.XML_TAG, "org.eclipse.ant.ui.tagsColor"),
        antStyle(TokenKey.XML_ATTRIBUTE, "org.eclipse.ant.ui.attributeColor")
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
