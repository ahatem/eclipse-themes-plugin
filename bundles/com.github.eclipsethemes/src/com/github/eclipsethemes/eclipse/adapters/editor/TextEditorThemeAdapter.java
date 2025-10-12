package com.github.eclipsethemes.eclipse.adapters.editor;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public class TextEditorThemeAdapter extends EclipseThemeAdapter {

	@Override
	public String getPreferencesId() {
		return "org.eclipse.ui.editors";
	}

	@Override
	public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
		// System color defaults
		preferences.putBoolean("AbstractTextEditor.Color.Background.SystemDefault", false);
		preferences.putBoolean("AbstractTextEditor.Color.Foreground.SystemDefault", false);
		preferences.putBoolean("AbstractTextEditor.Color.SelectionBackground.SystemDefault", false);
		preferences.putBoolean("AbstractTextEditor.Color.SelectionForeground.SystemDefault", false);

		// Basic editor colors
		putColor(preferences, "AbstractTextEditor.Color.Background", theme.get(TokenKey.BACKGROUND));
		putColor(preferences, "AbstractTextEditor.Color.Foreground", theme.get(TokenKey.FOREGROUND));
		putColor(preferences, "AbstractTextEditor.Color.FindScope", theme.get(TokenKey.FIND_SCOPE));
		putColor(preferences, "AbstractTextEditor.Color.SelectionBackground", theme.get(TokenKey.SELECTION_BACKGROUND));
		putColor(preferences, "AbstractTextEditor.Color.SelectionForeground", theme.get(TokenKey.SELECTION_FOREGROUND));

		// Editor UI elements
		putColor(preferences, "currentLineColor", theme.get(TokenKey.CURRENT_LINE));
		putColor(preferences, "lineNumberColor", theme.get(TokenKey.LINE_NUMBER));
		putColor(preferences, "printMarginColor", theme.get(TokenKey.LINE_NUMBER)); // Same as line numbers

		// Occurrence highlighting
		putColor(preferences, "occurrenceIndicationColor", theme.get(TokenKey.OCCURRENCE));
		putColor(preferences, "LSP4EReadOccurrenceIndicationColor", theme.get(TokenKey.OCCURRENCE));
		putColor(preferences, "org.eclipse.cdt.ui.occurrenceIndicationColor", theme.get(TokenKey.OCCURRENCE));
		putColor(preferences, "writeOccurrenceIndicationColor", theme.get(TokenKey.WRITE_OCCURRENCE));
		putColor(preferences, "LSP4EWriteOccurrenceIndicationColor", theme.get(TokenKey.WRITE_OCCURRENCE));
		putColor(preferences, "org.eclipse.cdt.ui.writeOccurrenceIndicationColor",
				theme.get(TokenKey.WRITE_OCCURRENCE));
		putColor(preferences, "TextOccurrenceIndicationColor", theme.get(TokenKey.TEXT_OCCURRENCE));
		putColor(preferences, "LSP4ETextOccurrenceIndicationColor", theme.get(TokenKey.WRITE_OCCURRENCE)); // Note:
																											// Using
		// Search and debug
		putColor(preferences, "searchResultIndicationColor", theme.get(TokenKey.SEARCH_RESULT));
		putColor(preferences, "filteredSearchResultIndicationColor", theme.get(TokenKey.FILTERED_SEARCH_RESULT));
		putColor(preferences, "currentIPIndication", theme.get(TokenKey.CURRENT_INSTRUCTION_POINTER));
		putColor(preferences, "secondaryIPIndication", theme.get(TokenKey.DEBUG_CALL_STACK));

		// Version control annotations (conditional)
		handleVersionControlColors(preferences, theme);

		flushPreferences(preferences);
	}

	private void handleVersionControlColors(IEclipsePreferences preferences, Theme theme) {
		// Added lines
		if (theme.has(TokenKey.ADDED_LINE)) {
			putColor(preferences, "additionIndicationColor", theme.get(TokenKey.ADDED_LINE));
		} else {
			preferences.remove("additionIndicationColor");
		}

		// Modified lines
		if (theme.has(TokenKey.MODIFIED_LINE)) {
			putColor(preferences, "changeIndicationColor", theme.get(TokenKey.MODIFIED_LINE));
		} else {
			preferences.remove("changeIndicationColor");
		}

		// Removed lines - special handling for light themes
		if (theme.has(TokenKey.REMOVED_LINE)) {
			putColor(preferences, "deletionIndicationColor", theme.get(TokenKey.REMOVED_LINE));
		} else {
			if (theme.getType() == ThemeType.LIGHT) {
				preferences.remove("deletionIndicationColor");
			} else {
				// Create a default color for dark themes
				Color defaultDeletionColor = Color.ofRgb(224, 226, 228);
				Token defaultToken = createTokenFromRGB(defaultDeletionColor);
				putColor(preferences, "deletionIndicationColor", defaultToken);
			}
		}
	}

	private Token createTokenFromRGB(Color color) {
		return new Token(null, color, null);
	}

	@Override
	public void clear(IEclipsePreferences preferences) throws BackingStoreException {
		// System color defaults
		preferences.remove("AbstractTextEditor.Color.Background.SystemDefault");
		preferences.remove("AbstractTextEditor.Color.Foreground.SystemDefault");
		preferences.remove("AbstractTextEditor.Color.SelectionBackground.SystemDefault");
		preferences.remove("AbstractTextEditor.Color.SelectionForeground.SystemDefault");

		// Basic editor colors
		preferences.remove("AbstractTextEditor.Color.Background");
		preferences.remove("AbstractTextEditor.Color.Foreground");
		preferences.remove("AbstractTextEditor.Color.FindScope");
		preferences.remove("AbstractTextEditor.Color.SelectionBackground");
		preferences.remove("AbstractTextEditor.Color.SelectionForeground");

		// Editor UI elements
		preferences.remove("currentLineColor");
		preferences.remove("lineNumberColor");
		preferences.remove("printMarginColor");

		// Occurrence highlighting
		preferences.remove("occurrenceIndicationColor");
		preferences.remove("LSP4EReadOccurrenceIndicationColor");
		preferences.remove("org.eclipse.cdt.ui.occurrenceIndicationColor");
		preferences.remove("writeOccurrenceIndicationColor");
		preferences.remove("LSP4EWriteOccurrenceIndicationColor");
		preferences.remove("org.eclipse.cdt.ui.writeOccurrenceIndicationColor");
		preferences.remove("TextOccurrenceIndicationColor");
		preferences.remove("LSP4ETextOccurrenceIndicationColor");

		// Search and debug
		preferences.remove("searchResultIndicationColor");
		preferences.remove("filteredSearchResultIndicationColor");
		preferences.remove("currentIPIndication");
		preferences.remove("secondaryIPIndication");

		// Version control annotations
		preferences.remove("additionIndicationColor");
		preferences.remove("changeIndicationColor");
		preferences.remove("deletionIndicationColor");

		flushPreferences(preferences);
	}

}