package com.github.eclipsethemes.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.Constants;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
	@Override
	public void initializeDefaultPreferences() {
		ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, EclipseThemes.PLUGIN_ID);
		store.setDefault(PreferenceKeys.ACTIVE_THEME_ID, Constants.DEFAULT_LIGHT_THEME_NAME);
	}
}
