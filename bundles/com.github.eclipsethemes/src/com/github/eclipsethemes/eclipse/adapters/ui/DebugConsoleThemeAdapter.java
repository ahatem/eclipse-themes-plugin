package com.github.eclipsethemes.eclipse.adapters.ui;

import java.util.List;
import java.util.Set;

import static com.github.eclipsethemes.eclipse.adapters.ManagedPreference.*;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.ManagedPreference;

public final class DebugConsoleThemeAdapter extends EclipseThemeAdapter {

    @Override
    public String getPreferencesId() {
        return "org.eclipse.debug.ui"; // Debug UI preferences node
    }

    private static final List<ManagedPreference> MAPPINGS = List.of(
        // Debug console colors - these are simple color mappings without style flags
        color(TokenKey.BACKGROUND, "org.eclipse.debug.ui.consoleBackground"),
        color(TokenKey.FOREGROUND, "org.eclipse.debug.ui.outColor"),
        color(TokenKey.ERROR, "org.eclipse.debug.ui.errorColor")
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
