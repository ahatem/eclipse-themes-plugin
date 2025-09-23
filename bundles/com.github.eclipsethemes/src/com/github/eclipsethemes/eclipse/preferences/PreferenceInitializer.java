package com.github.eclipsethemes.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.FrameworkUtil;

import com.github.eclipsethemes.core.Constants;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
	@Override
	public void initializeDefaultPreferences() {
		ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, String.valueOf(FrameworkUtil.getBundle(getClass()).getBundleId()));
		store.setDefault(PreferenceKeys.ACTIVE_THEME_ID, Constants.DEFAULT_LIGHT_THEME_NAME);
	}
}
