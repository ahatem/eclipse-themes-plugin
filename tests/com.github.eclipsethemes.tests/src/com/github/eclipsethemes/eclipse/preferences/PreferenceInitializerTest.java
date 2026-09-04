package com.github.eclipsethemes.eclipse.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.junit.jupiter.api.Test;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.Constants;

class PreferenceInitializerTest {

	@Test
	void initializesWorkbenchModernizationDefaultsAsEnabled() {
		new PreferenceInitializer().initializeDefaultPreferences();

		ScopedPreferenceStore store =
				new ScopedPreferenceStore(InstanceScope.INSTANCE, EclipseThemes.PLUGIN_ID);

		assertEquals(Constants.DEFAULT_LIGHT_THEME_NAME, store.getDefaultString(PreferenceKeys.ACTIVE_THEME_ID));
		assertTrue(store.getDefaultBoolean(PreferenceKeys.APPLY_WORKBENCH_THEME));
		assertTrue(store.getDefaultBoolean(PreferenceKeys.ENABLE_ECLIPSE_CSS_MODERNIZATION));
		assertTrue(store.getDefaultBoolean(PreferenceKeys.ENABLE_GTK_MODERNIZATION));
	}
}
