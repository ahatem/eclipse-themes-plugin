package com.github.eclipsethemes.eclipse.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.TokenOptions;

class ColorCustomizationStoreTest {

	@Test
	void encodeAndDecodeRoundTripPreservesColorAndStyle() {
		Token original = new Token(TokenKey.KEYWORD, Color.ofHex("#ff00aa"),
				new TokenOptions(true, false, true, false));

		Token decoded = ColorCustomizationStore.decode(ColorCustomizationStore.encode(original));

		assertEquals(TokenKey.KEYWORD, decoded.getKey());
		assertEquals(Color.ofHex("#ff00aa"), decoded.getColor());
		assertTrue(decoded.getOptions().orElseThrow().bold());
		assertTrue(decoded.getOptions().orElseThrow().underline());
	}

	@Test
	void catalogListsEveryRegisteredToken() {
		java.util.Set<TokenKey> catalogued = ColorElementCatalog.categories().stream()
				.flatMap(category -> category.keys().stream())
				.collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
		java.util.Set<TokenKey> registered = java.util.Arrays.stream(TokenKey.class.getDeclaredFields())
				.filter(field -> java.lang.reflect.Modifier.isStatic(field.getModifiers())
						&& field.getType() == TokenKey.class)
				.map(field -> {
					try {
						return (TokenKey) field.get(null);
					} catch (IllegalAccessException e) {
						throw new IllegalStateException(e);
					}
				})
				.collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

		assertEquals(registered, catalogued);
	}

	@Test
	void displayNameIsReadable() {
		assertEquals("Selection Background", TokenKey.SELECTION_BACKGROUND.getDisplayName());
		assertEquals("XML Tag", TokenKey.XML_TAG.getDisplayName());
	}

	@Test
	void copyKeepsBaseTokensWhenOverrideIsApplied() {
		Theme theme = new Theme("id", "Name", "Author", null, null, null, ThemeType.DARK);
		theme.addToken(new Token(TokenKey.BACKGROUND, Color.ofHex("#22272e"), null));
		theme.addToken(new Token(TokenKey.FOREGROUND, Color.ofHex("#adbac7"), null));
		theme.addToken(new Token(TokenKey.KEYWORD, Color.ofHex("#f47067"), null));

		Theme copy = theme.copy();
		copy.addToken(new Token(TokenKey.KEYWORD, Color.ofHex("#ff0000"), new TokenOptions(true, false, false, false)));

		assertEquals(Color.ofHex("#22272e"), copy.get(TokenKey.BACKGROUND).getColor());
		assertEquals(Color.ofHex("#ff0000"), copy.get(TokenKey.KEYWORD).getColor());
		assertEquals(Color.ofHex("#f47067"), theme.get(TokenKey.KEYWORD).getColor());
	}
}
