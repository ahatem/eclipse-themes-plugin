package com.github.eclipsethemes.eclipse.adapters.editor;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public final class PluginEditorThemeAdapter extends EclipseThemeAdapter {

	@Override
	public String getPreferencesId() {
		return "org.eclipse.pde.ui";
	}

	@Override
	public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
		mapLegacyStyle(preferences, theme, TokenKey.FOREGROUND, "editor.color.default");
		mapLegacyStyle(preferences, theme, TokenKey.OPERATOR, "editor.color.header_assignment");
		mapLegacyStyle(preferences, theme, TokenKey.COMMENT, "editor.color.xml_comment");
		mapLegacyStyle(preferences, theme, TokenKey.STRING, "editor.color.string");
		mapLegacyStyle(preferences, theme, TokenKey.STRING, "editor.color.externalized_string");
		mapLegacyStyle(preferences, theme, TokenKey.STRING, "editor.color.header_value");
		mapLegacyStyle(preferences, theme, TokenKey.XML_DIRECTIVE, "editor.color.instr");
		mapLegacyStyle(preferences, theme, TokenKey.XML_TAG, "editor.color.tag");
		mapLegacyStyle(preferences, theme, TokenKey.KEY, "editor.color.header_attributes");
		mapLegacyStyle(preferences, theme, TokenKey.KEY, "editor.color.header_osgi");

		flushPreferences(preferences);
	}

	@Override
	public void clear(IEclipsePreferences preferences) throws BackingStoreException {
		clearLegacyStyle(preferences, "editor.color.default");
		clearLegacyStyle(preferences, "editor.color.header_assignment");
		clearLegacyStyle(preferences, "editor.color.xml_comment");
		clearLegacyStyle(preferences, "editor.color.string");
		clearLegacyStyle(preferences, "editor.color.externalized_string");
		clearLegacyStyle(preferences, "editor.color.header_value");
		clearLegacyStyle(preferences, "editor.color.instr");
		clearLegacyStyle(preferences, "editor.color.tag");
		clearLegacyStyle(preferences, "editor.color.header_attributes");
		clearLegacyStyle(preferences, "editor.color.header_osgi");

		flushPreferences(preferences);
	}

}
