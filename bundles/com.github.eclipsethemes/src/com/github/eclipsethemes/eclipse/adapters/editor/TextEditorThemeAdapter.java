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
		// Typically the text editor preferences node
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
				// Note: You'll need to adapt this based on your new Theme/Token model
				Color defaultDeletionColor = Color.ofRgb(224, 226, 228);
				// Assuming you have a way to create a Token from RGB - you may need to adjust
				// this
				Token defaultToken = createTokenFromRGB(defaultDeletionColor);
				putColor(preferences, "deletionIndicationColor", defaultToken);
			}
		}
	}

	private Token createTokenFromRGB(Color color) {
		return new Token(null, color, null);
	}

}