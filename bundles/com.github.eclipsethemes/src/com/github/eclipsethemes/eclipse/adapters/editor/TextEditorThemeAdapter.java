package com.github.eclipsethemes.eclipse.adapters.editor;

import java.util.List;
import java.util.Set;

import static com.github.eclipsethemes.eclipse.adapters.ManagedPreference.*;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.ManagedPreference;


public class TextEditorThemeAdapter extends EclipseThemeAdapter {

	@Override
	public String getPreferencesId() {
		// Typically the text editor preferences node
		return "org.eclipse.ui.editors";
	}

	private static final List<ManagedPreference> MAPPINGS = List.of(
		// System color defaults
		booleanValue("AbstractTextEditor.Color.Background.SystemDefault", false),
		booleanValue("AbstractTextEditor.Color.Foreground.SystemDefault", false),
		booleanValue("AbstractTextEditor.Color.SelectionBackground.SystemDefault", false),
		booleanValue("AbstractTextEditor.Color.SelectionForeground.SystemDefault", false),

		// Basic editor colors
		color(TokenKey.BACKGROUND, "AbstractTextEditor.Color.Background"),
		color(TokenKey.FOREGROUND, "AbstractTextEditor.Color.Foreground"),
		color(TokenKey.FIND_SCOPE, "AbstractTextEditor.Color.FindScope"),
		color(TokenKey.SELECTION_BACKGROUND, "AbstractTextEditor.Color.SelectionBackground"),
		color(TokenKey.SELECTION_FOREGROUND, "AbstractTextEditor.Color.SelectionForeground"),

		// Editor UI elements
		color(TokenKey.CURRENT_LINE, "currentLineColor"),
		color(TokenKey.LINE_NUMBER, "lineNumberColor"),
		color(TokenKey.LINE_NUMBER, "printMarginColor"), // Same as line numbers

		// Occurrence highlighting
		color(TokenKey.OCCURRENCE, "occurrenceIndicationColor"),
		color(TokenKey.OCCURRENCE, "LSP4EReadOccurrenceIndicationColor"),
		color(TokenKey.OCCURRENCE, "org.eclipse.cdt.ui.occurrenceIndicationColor"),
		color(TokenKey.WRITE_OCCURRENCE, "writeOccurrenceIndicationColor"),
		color(TokenKey.WRITE_OCCURRENCE, "LSP4EWriteOccurrenceIndicationColor"),
		color(TokenKey.WRITE_OCCURRENCE, "org.eclipse.cdt.ui.writeOccurrenceIndicationColor"),
		color(TokenKey.TEXT_OCCURRENCE, "TextOccurrenceIndicationColor"),
		color(TokenKey.WRITE_OCCURRENCE, "LSP4ETextOccurrenceIndicationColor"),

		// Search and debug
		color(TokenKey.SEARCH_RESULT, "searchResultIndicationColor"),
		color(TokenKey.FILTERED_SEARCH_RESULT, "filteredSearchResultIndicationColor"),
		color(TokenKey.CURRENT_INSTRUCTION_POINTER, "currentIPIndication"),
		color(TokenKey.DEBUG_CALL_STACK, "secondaryIPIndication"),

		// Version control annotations (conditional)
		optionalColor(TokenKey.ADDED_LINE, "additionIndicationColor", theme -> null),
		optionalColor(TokenKey.MODIFIED_LINE, "changeIndicationColor", theme -> null),
		optionalColor(TokenKey.REMOVED_LINE, "deletionIndicationColor",
				theme -> theme.getType() == ThemeType.LIGHT ? null : Color.ofRgb(224, 226, 228)),

		color(TokenKey.MATCHING_BRACKET, "matchingBracketsColor")
	);

	@Override
	public Set<String> getManagedKeys() {
		return managedKeys(MAPPINGS);
	}

	@Override
	public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
		applyMappings(MAPPINGS, theme, preferences);
	}
}
