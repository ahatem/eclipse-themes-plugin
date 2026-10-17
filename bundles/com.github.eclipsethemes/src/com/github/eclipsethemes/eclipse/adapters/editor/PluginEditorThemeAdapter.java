package com.github.eclipsethemes.eclipse.adapters.editor;

import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.function.Supplier;

import static com.github.eclipsethemes.eclipse.adapters.ManagedPreference.*;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.TokenOptions;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.ManagedPreference;

public final class PluginEditorThemeAdapter extends EclipseThemeAdapter {

    private final Supplier<IPreferenceStore> liveStore;

    public PluginEditorThemeAdapter() {
        this(PdePreferenceAccess::liveStore);
    }

    PluginEditorThemeAdapter(Supplier<IPreferenceStore> liveStore) {
        this.liveStore = Objects.requireNonNull(liveStore);
    }

    private static ManagedPreference pdeStyle(TokenKey key, String base) {
        // R4_33 BasePDEScanner/XMLConfiguration support only bold/italic.
        // Other suffix notifications are interpreted as color keys, corrupting live colors.
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
        return "org.eclipse.pde.ui"; // PDE editor preferences node
    }

    private static final List<ManagedPreference> MAPPINGS = List.of(
        // PDE editor uses legacy underscore format for style preferences
        pdeStyle(TokenKey.FOREGROUND, "editor.color.default"),
        pdeStyle(TokenKey.OPERATOR, "editor.color.header_assignment"),
        // PDE R4_33 IPDEColorConstants: XML comments inherit generic comments only when omitted.
        pdeStyle(TokenKey.XML_COMMENT, "editor.color.xml_comment"),
        pdeStyle(TokenKey.STRING, "editor.color.string"),
        pdeStyle(TokenKey.STRING, "editor.color.externalized_string"),
        pdeStyle(TokenKey.STRING, "editor.color.header_value"),
        pdeStyle(TokenKey.XML_DIRECTIVE, "editor.color.instr"),
        pdeStyle(TokenKey.XML_TAG, "editor.color.tag"),
        pdeStyle(TokenKey.XML_ATTRIBUTE, "editor.color.attribute"),
        pdeStyle(TokenKey.KEY, "editor.color.header_attributes"),
        pdeStyle(TokenKey.KEY, "editor.color.header_osgi")
    );

    @Override
    public Set<String> getManagedKeys() {
        return managedKeys(MAPPINGS);
    }

    @Override
    public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
        IPreferenceStore store;
        try {
            store = Objects.requireNonNull(liveStore.get(), "PDE live preference store is null");
        } catch (RuntimeException | LinkageError e) {
            // Existing ThemeService handles this checked integration failure and continues.
            // Resolve before any write; never claim success via a separate public store.
            throw new BackingStoreException("PDE live preference store unavailable/incompatible; no PDE mappings applied", e);
        }
        for (ManagedPreference mapping : MAPPINGS) {
            mapping.application().accept(theme, preferences);
            for (String key : mapping.keys().subList(1, 3)) {
                // Raw-node events contain Strings, but R4_33 PDE requires Boolean payloads.
                // Keep the explicit override (also when equal to a default) and notify the
                // actual store. No old event payload is supplied; repeat application repairs
                // a previously ineffective String notification even when the value is equal.
                try {
                    store.firePropertyChangeEvent(key, null, Boolean.valueOf(preferences.getBoolean(key, false)));
                } catch (RuntimeException | LinkageError e) {
                    throw new BackingStoreException("PDE live style notification failed: " + key, e);
                }
            }
        }
        flushPreferences(preferences);
    }
}
