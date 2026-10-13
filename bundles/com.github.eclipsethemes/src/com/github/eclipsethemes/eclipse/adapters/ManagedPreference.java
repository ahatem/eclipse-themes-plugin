package com.github.eclipsethemes.eclipse.adapters;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;

/** Exact preference ownership derived from an adapter's application mapping. */
public record ManagedPreference(List<String> keys, BiConsumer<Theme, IEclipsePreferences> application) {

	public ManagedPreference {
		keys = List.copyOf(keys);
	}

	public static ManagedPreference color(TokenKey token, String key) {
		return new ManagedPreference(List.of(key), (theme, prefs) -> EclipseThemeAdapter.putColor(prefs, key, theme.get(token)));
	}

	public static ManagedPreference booleanValue(String key, boolean value) {
		return new ManagedPreference(List.of(key), (theme, prefs) -> prefs.putBoolean(key, value));
	}

	public static ManagedPreference legacyStyle(TokenKey token, String base) {
		return new ManagedPreference(List.of(base, base + "_bold", base + "_italic", base + "_underline",
				base + "_strikethrough"), (theme, prefs) -> EclipseThemeAdapter.mapLegacyStyle(prefs, theme, token, base));
	}

	public static ManagedPreference semanticStyle(TokenKey token, String base) {
		return semanticStyle(token, base, false);
	}

	public static ManagedPreference optionalSemanticStyle(TokenKey token, String base) {
		return semanticStyle(token, base, true);
	}

	private static ManagedPreference semanticStyle(TokenKey token, String base, boolean optional) {
		return new ManagedPreference(List.of(base + ".enabled", base + ".color", base + ".bold", base + ".italic",
				base + ".underline", base + ".strikethrough"), (theme, prefs) -> {
			if (optional) EclipseThemeAdapter.mapSemanticStyleOptional(prefs, theme, token, base);
			else EclipseThemeAdapter.mapSemanticStyle(prefs, theme, token, base);
		});
	}

	public static ManagedPreference optionalColor(TokenKey token, String key, Function<Theme, Color> fallback) {
		return new ManagedPreference(List.of(key), (theme, prefs) -> {
			Color color = theme.has(token) ? theme.get(token).getColor() : fallback.apply(theme);
			if (color == null) prefs.remove(key);
			else EclipseThemeAdapter.putColor(prefs, key, color);
		});
	}
}
