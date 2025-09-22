package com.github.eclipsethemes.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.FrameworkUtil;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
	@Override
	public void initializeDefaultPreferences() {
		ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, String.valueOf(FrameworkUtil.getBundle(getClass()).getBundleId()));
		
		store.setDefault(Preferences.ACTIVE_THEME_ID, "default");
	}
}
