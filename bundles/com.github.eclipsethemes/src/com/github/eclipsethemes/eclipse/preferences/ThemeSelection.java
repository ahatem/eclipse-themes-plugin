package com.github.eclipsethemes.eclipse.preferences;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jface.preference.IPreferenceStore;

import com.github.eclipsethemes.core.Constants;
import com.github.eclipsethemes.core.models.Theme;

/** Selected intent only, not a record of successful theme application. */
final class ThemeSelection {
    private ThemeSelection() {}

    static Optional<Theme> resolve(IPreferenceStore store, List<Theme> themes) {
        boolean modern = store.contains(PreferenceKeys.SELECTED_THEME_LOGICAL_ID)
                && !store.isDefault(PreferenceKeys.SELECTED_THEME_LOGICAL_ID);
        String key = modern ? PreferenceKeys.SELECTED_THEME_LOGICAL_ID : PreferenceKeys.ACTIVE_THEME_ID;
        if (!store.contains(key) || store.isDefault(key)) return Optional.empty();
        String reference = store.getString(key);
        if (reference.isBlank()) return Optional.empty();
        var matches = themes.stream().filter(theme -> theme.getLogicalId() != null)
                .filter(theme -> reference.equals(modern ? theme.getLogicalId() : theme.getId())).toList();
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    static void remember(IPreferenceStore store, Theme theme) {
        String id = theme.getLogicalId();
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Theme has no established logical identity");
        store.setValue(PreferenceKeys.SELECTED_THEME_LOGICAL_ID, id);
    }

    static void clear(IPreferenceStore store) {
        store.setToDefault(PreferenceKeys.SELECTED_THEME_LOGICAL_ID);
        store.setToDefault(PreferenceKeys.ACTIVE_THEME_ID);
    }

    static Theme defaultTheme(List<Theme> themes, boolean dark) {
        String id = dark ? Constants.DEFAULT_DARK_THEME_ID : Constants.DEFAULT_LIGHT_THEME_ID;
        return themes.stream().filter(theme -> Objects.equals(id, theme.getLogicalId())).findFirst()
                .orElseGet(() -> themes.stream().filter(theme -> theme.getType().name().equals(dark ? "DARK" : "LIGHT"))
                        .findFirst().orElse(themes.isEmpty() ? null : themes.get(0)));
    }
}
