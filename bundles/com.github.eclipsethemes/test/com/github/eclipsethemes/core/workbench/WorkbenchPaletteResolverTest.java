package com.github.eclipsethemes.core.workbench;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenBuilder;
import com.github.eclipsethemes.core.models.TokenKey;

public class WorkbenchPaletteResolverTest {

	@Test
	public void darkThemeLightensSurfacesTowardTheForeground() {
		WorkbenchPalette palette = WorkbenchPaletteResolver.resolve(theme(ThemeType.DARK, "#282c34", "#abb2bf", null));
		assertEquals(WorkbenchFamily.DARK, palette.family());
		assertEquals(Color.ofHex("#282c34"), palette.get(WorkbenchColor.BACKGROUND));
		assertEquals(Color.ofHex("#abb2bf"), palette.get(WorkbenchColor.FOREGROUND));
		Color background = palette.get(WorkbenchColor.BACKGROUND);
		assertTrue(luma(palette.get(WorkbenchColor.SURFACE)) > luma(background));
		assertTrue(luma(palette.get(WorkbenchColor.SURFACE_RAISED)) > luma(palette.get(WorkbenchColor.SURFACE)));
		assertTrue(luma(palette.get(WorkbenchColor.BORDER)) > luma(palette.get(WorkbenchColor.SURFACE_RAISED)));
	}

	@Test
	public void lightThemeDarkensSurfacesTowardTheForeground() {
		WorkbenchPalette palette = WorkbenchPaletteResolver.resolve(theme(ThemeType.LIGHT, "#fafafa", "#383a42", null));
		assertEquals(WorkbenchFamily.LIGHT, palette.family());
		Color background = palette.get(WorkbenchColor.BACKGROUND);
		assertTrue(luma(palette.get(WorkbenchColor.SURFACE)) < luma(background));
		assertTrue(luma(palette.get(WorkbenchColor.SURFACE_RAISED)) < luma(palette.get(WorkbenchColor.SURFACE)));
		assertTrue(luma(palette.get(WorkbenchColor.BORDER)) < luma(palette.get(WorkbenchColor.SURFACE_RAISED)));
	}

	@Test
	public void derivationIsDeterministic() {
		Theme theme = theme(ThemeType.DARK, "#1e1e2e", "#cdd6f4", "#45475a");
		assertEquals(WorkbenchPaletteResolver.resolve(theme), WorkbenchPaletteResolver.resolve(theme));
	}

	@Test
	public void syntaxTokensNeverInfluenceTheWorkbench() {
		Map<TokenKey, Token> plain = tokens("#282c34", "#abb2bf", null);
		Map<TokenKey, Token> colorful = new LinkedHashMap<>(plain);
		for (TokenKey key : new TokenKey[] { TokenKey.KEYWORD, TokenKey.STRING, TokenKey.COMMENT, TokenKey.NUMBER,
				TokenKey.ERROR, TokenKey.LINE_NUMBER, TokenKey.CURRENT_LINE }) {
			colorful.put(key, token(key, "#ff00aa"));
		}
		assertEquals(WorkbenchPaletteResolver.resolve(new Theme("a", "A", "", "", "", ThemeType.DARK, plain)),
				WorkbenchPaletteResolver.resolve(new Theme("b", "B", "", "", "", ThemeType.DARK, colorful)));
	}

	@Test
	public void missingBackgroundOrForegroundYieldsTheFamilyFallback() {
		Theme noForeground = new Theme("a", "A", "", "", "", ThemeType.DARK,
				Map.of(TokenKey.BACKGROUND, token(TokenKey.BACKGROUND, "#101010")));
		Theme noBackground = new Theme("b", "B", "", "", "", ThemeType.LIGHT,
				Map.of(TokenKey.FOREGROUND, token(TokenKey.FOREGROUND, "#101010")));
		Theme empty = new Theme("c", "C", "", "", "", ThemeType.LIGHT, Map.of());
		assertEquals(WorkbenchFamily.DARK.fallbackPalette(), WorkbenchPaletteResolver.resolve(noForeground));
		assertEquals(WorkbenchFamily.LIGHT.fallbackPalette(), WorkbenchPaletteResolver.resolve(noBackground));
		assertEquals(WorkbenchFamily.LIGHT.fallbackPalette(), WorkbenchPaletteResolver.resolve(empty));
	}

	@Test
	public void unreadableForegroundOnBackgroundYieldsTheFamilyFallback() {
		Theme nearlyInvisible = theme(ThemeType.DARK, "#202020", "#262626", null);
		assertEquals(WorkbenchFamily.DARK.fallbackPalette(), WorkbenchPaletteResolver.resolve(nearlyInvisible));
	}

	@Test
	public void explicitSelectionIsKeptOnlyWhenReadableAndDistinct() {
		WorkbenchPalette kept = WorkbenchPaletteResolver.resolve(theme(ThemeType.DARK, "#282c34", "#abb2bf", "#3e4451"));
		assertEquals(Color.ofHex("#3e4451"), kept.get(WorkbenchColor.SELECTION));

		WorkbenchPalette unreadable = WorkbenchPaletteResolver
				.resolve(theme(ThemeType.DARK, "#282c34", "#abb2bf", "#a0a8b4"));
		assertNotEquals(Color.ofHex("#a0a8b4"), unreadable.get(WorkbenchColor.SELECTION));

		WorkbenchPalette sameAsBackground = WorkbenchPaletteResolver
				.resolve(theme(ThemeType.DARK, "#282c34", "#abb2bf", "#282c34"));
		assertNotEquals(sameAsBackground.get(WorkbenchColor.BACKGROUND), sameAsBackground.get(WorkbenchColor.SELECTION));

		WorkbenchPalette none = WorkbenchPaletteResolver.resolve(theme(ThemeType.DARK, "#282c34", "#abb2bf", null));
		assertNotEquals(none.get(WorkbenchColor.BACKGROUND), none.get(WorkbenchColor.SELECTION));
		assertTrue(WorkbenchPaletteResolver.contrast(none.get(WorkbenchColor.FOREGROUND),
				none.get(WorkbenchColor.SELECTION)) >= WorkbenchPaletteResolver.MIN_CONTRAST);
	}

	@Test
	public void mutedForegroundFallsBackToTheForegroundWhenItWouldBeUnreadable() {
		WorkbenchPalette palette = WorkbenchPaletteResolver.resolve(theme(ThemeType.DARK, "#000000", "#808080", null));
		assertEquals(palette.get(WorkbenchColor.FOREGROUND), palette.get(WorkbenchColor.MUTED_FOREGROUND));
	}

	@Test
	public void everyDerivedPaletteKeepsTheForegroundReadableOnTheBackground() {
		for (WorkbenchPalette palette : new WorkbenchPalette[] {
				WorkbenchPaletteResolver.resolve(theme(ThemeType.DARK, "#282c34", "#abb2bf", null)),
				WorkbenchPaletteResolver.resolve(theme(ThemeType.LIGHT, "#ffffff", "#000000", null)),
				WorkbenchFamily.DARK.fallbackPalette(), WorkbenchFamily.LIGHT.fallbackPalette() }) {
			assertTrue(WorkbenchPaletteResolver.contrast(palette.get(WorkbenchColor.FOREGROUND),
					palette.get(WorkbenchColor.BACKGROUND)) >= WorkbenchPaletteResolver.MIN_CONTRAST);
		}
	}

	private static double luma(Color c) {
		return 0.2126 * c.red() + 0.7152 * c.green() + 0.0722 * c.blue();
	}

	private static Theme theme(ThemeType type, String background, String foreground, String selection) {
		return new Theme("id", "name", "", "", "", type, tokens(background, foreground, selection));
	}

	private static Map<TokenKey, Token> tokens(String background, String foreground, String selection) {
		Map<TokenKey, Token> tokens = new LinkedHashMap<>();
		tokens.put(TokenKey.BACKGROUND, token(TokenKey.BACKGROUND, background));
		tokens.put(TokenKey.FOREGROUND, token(TokenKey.FOREGROUND, foreground));
		if (selection != null) {
			tokens.put(TokenKey.SELECTION_BACKGROUND, token(TokenKey.SELECTION_BACKGROUND, selection));
		}
		return tokens;
	}

	private static Token token(TokenKey key, String hex) {
		return new TokenBuilder().setKey(key).setColor(Color.ofHex(hex)).build();
	}
}
