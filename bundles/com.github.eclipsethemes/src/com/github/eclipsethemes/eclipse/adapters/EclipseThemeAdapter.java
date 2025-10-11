package com.github.eclipsethemes.eclipse.adapters;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.TokenOptions;

import java.util.Optional;

public abstract class EclipseThemeAdapter {

	/**
	 * Returns the Eclipse preferences node ID for this adapter
	 */
	public abstract String getPreferencesId();

	/**
	 * Apply theme to Eclipse preferences.
	 */
	public abstract void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException;

	/**
	 * Clears all theme-related preferences for this adapter.
	 */
	public abstract void clear(IEclipsePreferences preferences) throws BackingStoreException;

	// === SEMANTIC STYLE PREFERENCES (using dots) ===

	protected static void putSemanticStyle(IEclipsePreferences preferences, String baseName, Token token) {
		putColor(preferences, baseName + ".color", token);
		putStyleFlags(preferences, baseName, token.getOptions(), ".");
	}

	protected static void mapSemanticStyle(IEclipsePreferences preferences, Theme theme, TokenKey key, String target) {
		preferences.putBoolean(target + ".enabled", true);
		putSemanticStyle(preferences, target, theme.get(key));
	}

	protected static void mapSemanticStyleOptional(IEclipsePreferences preferences, Theme theme, TokenKey key,
			String target) {
		boolean hasToken = theme.has(key);
		preferences.putBoolean(target + ".enabled", hasToken);
		if (hasToken) {
			putSemanticStyle(preferences, target, theme.get(key));
		} else {
			clearSemanticStyle(preferences, target);
		}
	}

	// === LEGACY STYLE PREFERENCES (using underscores) ===

	protected static void putLegacyStyle(IEclipsePreferences preferences, String baseName, Token token) {
		putColor(preferences, baseName, token);
		putStyleFlags(preferences, baseName, token.getOptions(), "_");
	}

	protected static void mapLegacyStyle(IEclipsePreferences preferences, Theme theme, TokenKey key, String target) {
		putLegacyStyle(preferences, target, theme.get(key));
	}

	// === HELPER METHODS ===

	public static void putColor(IEclipsePreferences preferences, String key, Token token) {
		preferences.put(key, token.getColor().toEclipseFormat());
	}

	private static void putStyleFlags(IEclipsePreferences preferences, String baseName,
			Optional<TokenOptions> tokenOptions, String separator) {

		TokenOptions options = TokenOptions.empty();
		if (tokenOptions.isPresent()) {
			options = tokenOptions.get();
		}

		preferences.putBoolean(baseName + separator + "bold", options.isBold());
		preferences.putBoolean(baseName + separator + "italic", options.isItalic());
		preferences.putBoolean(baseName + separator + "underline", options.isUnderline());
		preferences.putBoolean(baseName + separator + "strikethrough", options.isStrikethrough());
	}

	/**
	 * Clears all semantic style preferences for a given base name This removes
	 * color and all style flags from the preferences
	 */
	protected static void clearSemanticStyle(IEclipsePreferences preferences, String baseName) {
		// Clear color preference
		preferences.remove(baseName + ".color");

		// Clear all style flags
		preferences.remove(baseName + ".bold");
		preferences.remove(baseName + ".italic");
		preferences.remove(baseName + ".underline");
		preferences.remove(baseName + ".strikethrough");

		// Clear enabled flag (commonly used with semantic styles)
		preferences.remove(baseName + ".enabled");
	}

	/**
	 * Clears all legacy style preferences for a given base name This removes color
	 * and all style flags from the preferences
	 */
	protected static void clearLegacyStyle(IEclipsePreferences preferences, String baseName) {
		// Clear color preference (base name itself for legacy format)
		preferences.remove(baseName);

		// Clear all style flags with underscore separator
		preferences.remove(baseName + "_bold");
		preferences.remove(baseName + "_italic");
		preferences.remove(baseName + "_underline");
		preferences.remove(baseName + "_strikethrough");
	}

	/**
	 * Persist preferences to storage
	 */
	protected static void flushPreferences(IEclipsePreferences preferences) throws BackingStoreException {
		preferences.flush();
	}
}