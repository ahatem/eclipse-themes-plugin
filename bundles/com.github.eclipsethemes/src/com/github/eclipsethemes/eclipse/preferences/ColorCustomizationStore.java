package com.github.eclipsethemes.eclipse.preferences;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.TokenOptions;

public final class ColorCustomizationStore {

	private ColorCustomizationStore() {
	}

	public static Theme customize(Theme base) {
		if (base == null) {
			return null;
		}
		Theme copy = base.copy();
		load(base.getId()).values().forEach(copy::addToken);
		return copy;
	}

	public static Map<TokenKey, Token> load(String themeId) {
		Map<TokenKey, Token> overrides = new LinkedHashMap<>();
		if (themeId == null || themeId.isBlank()) {
			return overrides;
		}
		String packed = preferences().get(key(themeId), "");
		if (packed == null || packed.isBlank()) {
			return overrides;
		}
		for (String line : packed.split("\n")) {
			Token token = decode(line.strip());
			if (token != null) {
				overrides.put(token.getKey(), token);
			}
		}
		return overrides;
	}

	public static void save(String themeId, Map<TokenKey, Token> overrides) {
		IEclipsePreferences prefs = preferences();
		if (themeId == null || themeId.isBlank() || overrides == null || overrides.isEmpty()) {
			prefs.remove(key(themeId));
			flush(prefs);
			return;
		}
		StringBuilder packed = new StringBuilder();
		for (Entry<TokenKey, Token> entry : overrides.entrySet()) {
			if (packed.length() > 0) {
				packed.append('\n');
			}
			packed.append(encode(entry.getValue()));
		}
		prefs.put(key(themeId), packed.toString());
		flush(prefs);
	}

	public static void clear(String themeId) {
		save(themeId, Map.of());
	}

	public static String encode(Token token) {
		TokenOptions options = token.getOptions().orElse(TokenOptions.empty());
		return token.getKey().getName()
				+ "=" + token.getColor()
				+ "," + flag(options.bold())
				+ "," + flag(options.italic())
				+ "," + flag(options.underline())
				+ "," + flag(options.strikethrough());
	}

	public static Token decode(String line) {
		if (line == null || line.isBlank() || !line.contains("=")) {
			return null;
		}
		int split = line.indexOf('=');
		TokenKey key = TokenKey.byId(line.substring(0, split));
		if (key == null) {
			return null;
		}
		String[] parts = line.substring(split + 1).split(",");
		if (parts.length < 1) {
			return null;
		}
		try {
			Color color = Color.ofHex(parts[0]);
			boolean bold = parts.length > 1 && "1".equals(parts[1]);
			boolean italic = parts.length > 2 && "1".equals(parts[2]);
			boolean underline = parts.length > 3 && "1".equals(parts[3]);
			boolean strikethrough = parts.length > 4 && "1".equals(parts[4]);
			TokenOptions options = new TokenOptions(bold, italic, underline, strikethrough);
			return new Token(key, color, options);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static String key(String themeId) {
		return PreferenceKeys.COLOR_CUSTOMIZATIONS_PREFIX + themeId;
	}

	private static IEclipsePreferences preferences() {
		return InstanceScope.INSTANCE.getNode(EclipseThemes.PLUGIN_ID);
	}

	private static void flush(IEclipsePreferences prefs) {
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			EclipseThemes.instance().getLogger().error("Could not store color customizations", e);
		}
	}

	private static String flag(boolean value) {
		return value ? "1" : "0";
	}
}
