package com.github.eclipsethemes.eclipse.preferences;

import static com.github.eclipsethemes.eclipse.preferences.AdapterTestSupport.adapters;
import static com.github.eclipsethemes.eclipse.preferences.AdapterTestSupport.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.*;

public class AdapterBaselineTest {

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

	// Reviewed ordered baseline projections for all six adapters. Only approved debug,
	// hover, XML-comment/static-final routing and optional-enable removals differ.
	private static final String[] BASELINE = {
		"b8a7506a529e91e6bcee22fc91b36560db81512775f0326ff0416c9aecb91406",
		"3ae04b1807d16c66fabefedd2867d7c80bc2be8ad7f1623ee7b8b794400fea50",
		"5cd942dfbac704ed231cc94582570b203ab09a3f4d3b2899878cb0d1df440404",
		"c42b1e922feb5d315607b9f178bf32b1a9eb944b3483e784d3e405874d552d50",
		"b23844606f37e28853b278b77007561ed9a678856e46354143912bc936e76748",
		"b579e72bd9923204075b4304e42d8995d0ca7d3f40b4ac84a9fbb16e0e41623a",
		"1120cbb823dc3345d736f34795a1e977a71865fcef25453cbb0e95c22549ff46",
		"2ae5cc511ab7857b55f82d8cc0b8f13934cd95fcc801bc657c228b81971f8460",
		"af527ee2c839ba3a0ff4d4e64d418d35da129fdb50e42bc39fdb7ad91f7125ef",
		"48bba3c5ff9955d75220da2c0c7045cc465f3973547586b958a91707065da9d3",
		"95a98c1df9f6ab3521195f1e33756ee440377daf0261971568a021cd07c39fea",
		"df28957be209af298c510e0264418bcc214277668519e5f4a4e00ca563a589fb",
		"68ed51e3170ba6e25634495d946dd9ed18bb64dfb6c77d7bf4557327459835e1",
		"2ae5cc511ab7857b55f82d8cc0b8f13934cd95fcc801bc657c228b81971f8460",
		"af527ee2c839ba3a0ff4d4e64d418d35da129fdb50e42bc39fdb7ad91f7125ef",
		"48bba3c5ff9955d75220da2c0c7045cc465f3973547586b958a91707065da9d3",
		"95a98c1df9f6ab3521195f1e33756ee440377daf0261971568a021cd07c39fea",
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
				assertEquals(adapter.getPreferencesId() + " fixture=" + (index / 6) + " ordered mutations:\n"
						+ String.join("\n", prefs.mutations), BASELINE[index++], digest(prefs));
				assertEquals(1, prefs.flushes);
			}
		}
	}

	private static String digest(MemoryPreferences prefs) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(String.join("\n", prefs.mutations).getBytes(StandardCharsets.UTF_8)));
	}

}
