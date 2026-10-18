package com.github.eclipsethemes.eclipse.preferences;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jface.preference.IPreferenceStore;

import com.github.eclipsethemes.core.Constants;
import com.github.eclipsethemes.eclipse.ThemeRecord;

/** Selected intent only, not a record of successful theme application. */
public final class ThemeSelection {
    private ThemeSelection() {}

    public static Optional<ThemeRecord> resolve(IPreferenceStore store, List<ThemeRecord> themes) {
        boolean modern = store.contains(PreferenceKeys.SELECTED_THEME_LOGICAL_ID)
                && !store.isDefault(PreferenceKeys.SELECTED_THEME_LOGICAL_ID);
        String key = modern ? PreferenceKeys.SELECTED_THEME_LOGICAL_ID : PreferenceKeys.ACTIVE_THEME_ID;
        if (!store.contains(key) || store.isDefault(key)) return Optional.empty();
        String reference = store.getString(key);
        if (reference.isBlank()) return Optional.empty();
        var matches = themes.stream()
                .filter(theme -> reference.equals(modern ? theme.logicalId() : theme.theme().getId())).toList();
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    static void remember(IPreferenceStore store, ThemeRecord theme) {
        String id = theme.logicalId();
        if (id.isBlank()) throw new IllegalArgumentException("Theme has no established logical identity");
        store.setValue(PreferenceKeys.SELECTED_THEME_LOGICAL_ID, id);
    }

    static void clear(IPreferenceStore store) {
        store.setToDefault(PreferenceKeys.SELECTED_THEME_LOGICAL_ID);
        store.setToDefault(PreferenceKeys.ACTIVE_THEME_ID);
    }

    static ThemeRecord defaultTheme(List<ThemeRecord> themes, boolean dark) {
        String id = dark ? Constants.DEFAULT_DARK_THEME_ID : Constants.DEFAULT_LIGHT_THEME_ID;
        return themes.stream().filter(theme -> Objects.equals(id, theme.logicalId())).findFirst()
                .orElseGet(() -> themes.stream()
                        .filter(theme -> theme.theme().getType().name().equals(dark ? "DARK" : "LIGHT"))
                        .findFirst().orElse(themes.isEmpty() ? null : themes.get(0)));
    }
}
