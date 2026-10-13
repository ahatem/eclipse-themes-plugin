package com.github.eclipsethemes.eclipse.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenBuilder;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.*;
import com.github.eclipsethemes.eclipse.adapters.ui.DebugConsoleThemeAdapter;

public class AdapterBaselineTest {

	static List<EclipseThemeAdapter> adapters() {
		return List.of(new TextEditorThemeAdapter(), new JavaEditorThemeAdapter(), new CppEditorThemeAdapter(),
				new PluginEditorThemeAdapter(), new AntEditorThemeAdapter(), new DebugConsoleThemeAdapter());
	}

	static Theme theme(ThemeType type, boolean full) {
		Theme theme = new Theme("test", "test", null, null, null, null, type);
		int value = 0;
		for (TokenKey key : Arrays.stream(TokenKey.class.getFields())
				.filter(field -> field.getType() == TokenKey.class).map(field -> {
					try { return (TokenKey) field.get(null); }
					catch (IllegalAccessException e) { throw new AssertionError(e); }
				}).sorted(Comparator.comparing(TokenKey::getName)).toList()) {
			// Stable token-name order avoids relying on reflection order. Increment even
			// for omitted tokens so full and sparse fixtures use the same token values.
			int tokenValue = ++value;
			if (full || key == TokenKey.BACKGROUND || key == TokenKey.FOREGROUND) {
				// Encode a unique RGB value and vary all four style flags by token.
				theme.addToken(new TokenBuilder().setKey(key)
						.setColor(Color.ofRgb((tokenValue >> 16) & 255, (tokenValue >> 8) & 255, tokenValue & 255))
						.setBold((tokenValue & 1) != 0).setItalic((tokenValue & 2) != 0)
						.setUnderline((tokenValue & 4) != 0).setStrikethrough((tokenValue & 8) != 0).build());
			}
		}
		return theme;
	}

	@Test
	public void fixtureDistinguishesTokensAndDetectsMappingSwaps() throws Exception {
		Theme original = theme(ThemeType.DARK, true);
		Theme swapped = theme(ThemeType.DARK, true);
		Set<Color> colors = new HashSet<>();
		for (Token token : original.getTokens().values()) {
			assertTrue("Duplicate fixture color for " + token.getTokenName(), colors.add(token.getColor()));
			assertEquals(token.getColor(), swapped.get(token.getKey()).getColor());
			assertEquals(token.getOptions(), swapped.get(token.getKey()).getOptions());
		}
		Token keyword = original.get(TokenKey.KEYWORD);
		Token string = original.get(TokenKey.STRING);
		assertNotEquals(keyword.getOptions(), string.getOptions());
		// Swapping source values simulates a token mapping swap without touching production.
		swapped.addToken(new Token(TokenKey.KEYWORD, string.getColor(), string.getOptions().orElseThrow()));
		swapped.addToken(new Token(TokenKey.STRING, keyword.getColor(), keyword.getOptions().orElseThrow()));
		MemoryPreferences expected = new MemoryPreferences();
		MemoryPreferences changed = new MemoryPreferences();
		JavaEditorThemeAdapter adapter = new JavaEditorThemeAdapter();
		adapter.apply(original, expected.node);
		adapter.apply(swapped, changed.node);
		assertEquals(expected.mutations.stream().map(mutation -> mutation.split("=", 2)[0]).toList(),
				changed.mutations.stream().map(mutation -> mutation.split("=", 2)[0]).toList());
		assertEquals(BASELINE[1], digest(expected));
		assertNotEquals("A source-token swap must change the baseline even when keys/order are unchanged",
				BASELINE[1], digest(changed));
	}

	// Recorded from the unmodified develop adapters at ebec341 with the distinct-token fixture.
	// Includes ordered puts/removes,
	// so this safety fix cannot silently change mapping values or optional-token behavior.
	private static final String[] BASELINE = {
		"ade047864f04769cd404a8fd2c89fc3410b90d04a031390ac44a44265537c9de",
		"87c462d0f43978d75785154c0076ef451cffdf6ca0669953e78416c9479a9dac",
		"5cd942dfbac704ed231cc94582570b203ab09a3f4d3b2899878cb0d1df440404",
		"e104e2fbf96e1ccb638b9c179d1c42b08a6c6d1d44061d5348ff39edc118f342",
		"c6c1cac2f573676a47925c2393db2c33aa24d6839535003676b64a851fea6aed",
		"b579e72bd9923204075b4304e42d8995d0ca7d3f40b4ac84a9fbb16e0e41623a",
		"29d7561dea290ef1ece6cab830685b86221f2a98bfea0ae6ae965f52670fef9a",
		"f225d1a50cb2c7315810ca956525ff2c1f129c5979f7ca67f76fe78788f65d0d",
		"af527ee2c839ba3a0ff4d4e64d418d35da129fdb50e42bc39fdb7ad91f7125ef",
		"e330fa3f3ba6f0880cd826e6f94af44ed9311dfc187c13cca805e058e4104270",
		"1c533ce66fb0c568edb02f095f9aa99cbc09dfe2c041f2e95bbd0439fb678151",
		"df28957be209af298c510e0264418bcc214277668519e5f4a4e00ca563a589fb",
		"3a130c02444a440829829736b9c2fa81497475bf45bfed5ba9a4be966fc625ab",
		"f225d1a50cb2c7315810ca956525ff2c1f129c5979f7ca67f76fe78788f65d0d",
		"af527ee2c839ba3a0ff4d4e64d418d35da129fdb50e42bc39fdb7ad91f7125ef",
		"e330fa3f3ba6f0880cd826e6f94af44ed9311dfc187c13cca805e058e4104270",
		"1c533ce66fb0c568edb02f095f9aa99cbc09dfe2c041f2e95bbd0439fb678151",
		"df28957be209af298c510e0264418bcc214277668519e5f4a4e00ca563a589fb"
	};

	@Test
	public void applyingFullAndSparseThemesKeepsExistingMappings() throws Exception {
		int index = 0;
		for (Theme theme : List.of(theme(ThemeType.DARK, true), theme(ThemeType.DARK, false),
				theme(ThemeType.LIGHT, false))) {
			for (EclipseThemeAdapter adapter : adapters()) {
				MemoryPreferences prefs = new MemoryPreferences();
				adapter.apply(theme, prefs.node);
				assertEquals(adapter.getPreferencesId(), BASELINE[index++], digest(prefs));
				assertEquals(1, prefs.flushes);
			}
		}
	}

	private static String digest(MemoryPreferences prefs) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(String.join("\n", prefs.mutations).getBytes(StandardCharsets.UTF_8)));
	}

}
