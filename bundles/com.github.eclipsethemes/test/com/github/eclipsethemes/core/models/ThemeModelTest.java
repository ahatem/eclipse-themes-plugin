package com.github.eclipsethemes.core.models;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

/** The validated theme is one immutable snapshot with a closed role vocabulary. */
public class ThemeModelTest {

	@Test public void mutatingTheSourceMapCannotAlterTheConstructedTheme() {
		Map<TokenKey, Token> source = new LinkedHashMap<>();
		source.put(TokenKey.BACKGROUND, token(TokenKey.BACKGROUND, "#112233"));
		Theme theme = theme(source);

		source.put(TokenKey.BACKGROUND, token(TokenKey.BACKGROUND, "#445566"));
		source.put(TokenKey.FOREGROUND, token(TokenKey.FOREGROUND, "#778899"));
		source.clear();

		assertEquals(1, theme.getTokens().size());
		assertEquals(Color.ofHex("#112233"), theme.get(TokenKey.BACKGROUND).getColor());
		assertFalse(theme.has(TokenKey.FOREGROUND));
	}

	@Test public void theTokenSnapshotExposesNoMutableView() {
		Theme theme = theme(Map.of(TokenKey.BACKGROUND, token(TokenKey.BACKGROUND, "#112233")));

		assertThrows(UnsupportedOperationException.class,
				() -> theme.getTokens().put(TokenKey.FOREGROUND, token(TokenKey.FOREGROUND, "#445566")));
		assertThrows(UnsupportedOperationException.class,
				() -> theme.getTokens().remove(TokenKey.BACKGROUND));
		assertThrows(UnsupportedOperationException.class, () -> theme.getTokens().clear());
		assertTrue("The rejected mutations changed nothing", theme.has(TokenKey.BACKGROUND));
		assertFalse(theme.has(TokenKey.FOREGROUND));
	}

	@Test public void requiredFieldsAreEnforcedAtConstruction() {
		Map<TokenKey, Token> tokens = Map.of(TokenKey.BACKGROUND, token(TokenKey.BACKGROUND, "#112233"));

		assertThrows(NullPointerException.class,
				() -> new Theme("source", "Name", "", "", "", null, tokens));
		assertThrows(NullPointerException.class,
				() -> new Theme("source", "Name", "", "", "", ThemeType.LIGHT, null));
		assertThrows("A token must be present for its declared role", NullPointerException.class,
				() -> theme(java.util.Collections.singletonMap(TokenKey.BACKGROUND, null)));
		assertThrows("Map key and token role must agree", IllegalArgumentException.class,
				() -> theme(Map.of(TokenKey.BACKGROUND, token(TokenKey.FOREGROUND, "#112233"))));
	}

	@Test public void roleLookupReturnsTheCanonicalDeclaredInstance() {
		assertSame(TokenKey.KEYWORD, TokenKey.byId("keyword"));
		assertSame(TokenKey.BACKGROUND, TokenKey.byId("background"));
		assertNull(TokenKey.byId("notAThemeRole"));
	}

	@Test public void declaringARoleTwiceFailsImmediately() throws Exception {
		Method register = TokenKey.class.getDeclaredMethod("register", String.class, TokenKey.class);
		register.setAccessible(true);

		InvocationTargetException failure = assertThrows(InvocationTargetException.class,
				() -> register.invoke(null, "background", (TokenKey) null));
		assertTrue("Duplicate role must fail immediately: " + failure.getCause(),
				failure.getCause() instanceof IllegalStateException);
		assertSame("The failed declaration did not replace the canonical role",
				TokenKey.BACKGROUND, TokenKey.byId("background"));
	}

	@Test public void themesUseIdentityEqualityRatherThanVisualContent() {
		Theme first = theme("Name", "#112233");
		Theme second = theme("Name", "#112233");

		assertNotSame(first, second);
		assertNotEquals("Equal visual content is not catalog identity", first, second);
		assertEquals(first, first);
	}

	private static Theme theme(Map<TokenKey, Token> tokens) {
		return new Theme("source", "Name", "", "", "", ThemeType.LIGHT, tokens);
	}

	private static Theme theme(String name, String background) {
		return new Theme("source", name, "", "", "", ThemeType.LIGHT,
				Map.of(TokenKey.BACKGROUND, token(TokenKey.BACKGROUND, background)));
	}

	private static Token token(TokenKey key, String color) {
		return new Token(key, Color.ofHex(color), null);
	}
}
