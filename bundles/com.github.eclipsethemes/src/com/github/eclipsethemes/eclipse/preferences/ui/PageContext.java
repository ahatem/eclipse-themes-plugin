package com.github.eclipsethemes.eclipse.preferences.ui;

import com.github.eclipsethemes.eclipse.ThemeRecord;

/** What the panels of the Theme Manager need to know about, and tell, the page that hosts them. */
public interface PageContext {

	/** The theme chosen on the page, which Apply applies; null when none is chosen. */
	ThemeRecord selectedTheme();

	/** The theme that is applied, or null when none is. */
	ThemeRecord currentTheme();

	/** Shows one inline message in the page's message area; {@code null} restores the page description. */
	void showStatus(String message, int type);
}
