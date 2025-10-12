package com.github.eclipsethemes.eclipse.adapters.editor;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public final class GenericEditorThemeAdapter extends EclipseThemeAdapter {

	@Override
	public String getPreferencesId() {
		return "org.eclipse.ui.editors";
	}

	@Override
	public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
		putColor(preferences, "matchingBracketsColor", theme.get(TokenKey.MATCHING_BRACKET));

		flushPreferences(preferences);
	}

	@Override
	public void clear(IEclipsePreferences preferences) throws BackingStoreException {
		preferences.remove("matchingBracketsColor");
		flushPreferences(preferences);
	}
}
