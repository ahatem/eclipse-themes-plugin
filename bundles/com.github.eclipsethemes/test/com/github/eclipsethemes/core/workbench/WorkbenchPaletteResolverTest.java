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

/** The deterministic semantic conversion of a legacy theme into the workbench palette. */
public class WorkbenchPaletteResolverTest {

	private static final double TEXT = WorkbenchPaletteResolver.TEXT_CONTRAST;
	private static final double SELECTION = WorkbenchPaletteResolver.SELECTION_CONTRAST;

	// ---- structure and direction -------------------------------------------------------------------------------

	@Test
	public void darkThemeLightensChromeTowardTheForeground() {
		WorkbenchPalette palette = resolve(ThemeType.DARK, "#282c34", "#abb2bf");
		assertEquals(WorkbenchFamily.DARK, palette.family());
		assertEquals(Color.ofHex("#282c34"), palette.get(WorkbenchColor.BACKGROUND));
		assertTrue(lightness(palette, WorkbenchColor.SURFACE) > lightness(palette, WorkbenchColor.BACKGROUND));
		assertTrue(lightness(palette, WorkbenchColor.SURFACE_RAISED) > lightness(palette, WorkbenchColor.SURFACE));
		assertTrue(lightness(palette, WorkbenchColor.BORDER) > lightness(palette, WorkbenchColor.SURFACE_RAISED));
	}

	@Test
	public void lightThemeDarkensChromeTowardTheForeground() {
		WorkbenchPalette palette = resolve(ThemeType.LIGHT, "#fafafa", "#383a42");
		assertEquals(WorkbenchFamily.LIGHT, palette.family());
		assertTrue(lightness(palette, WorkbenchColor.SURFACE) < lightness(palette, WorkbenchColor.BACKGROUND));
		assertTrue(lightness(palette, WorkbenchColor.SURFACE_RAISED) < lightness(palette, WorkbenchColor.SURFACE));
		assertTrue(lightness(palette, WorkbenchColor.BORDER) < lightness(palette, WorkbenchColor.SURFACE_RAISED));
	}

	@Test
	public void conversionIsDeterministic() {
		Theme theme = theme(ThemeType.DARK, "#1e1e2e", "#cdd6f4", Map.of(TokenKey.CURRENT_LINE, "#313244",
				TokenKey.SELECTION_BACKGROUND, "#45475a", TokenKey.LINE_NUMBER, "#8a8fa8"));
		assertEquals(WorkbenchPaletteResolver.resolve(theme), WorkbenchPaletteResolver.resolve(theme));
	}

	// ---- authored candidates -----------------------------------------------------------------------------------

	@Test
	public void suitableAuthoredCurrentLineBecomesTheSurface() {
		WorkbenchPalette palette = resolve(ThemeType.DARK, "#282c34", "#abb2bf", TokenKey.CURRENT_LINE, "#2c313c");
		assertEquals(Color.ofHex("#2c313c"), palette.get(WorkbenchColor.SURFACE));
	}

	@Test
	public void authoredSourceHoverBackgroundIsPreferredOverCurrentLineForTheSurface() {
		Theme theme = theme(ThemeType.DARK, "#282c34", "#abb2bf",
				Map.of(TokenKey.CURRENT_LINE, "#2c313c", TokenKey.SOURCE_HOVER_BACKGROUND, "#2e3440"));
		assertEquals(Color.ofHex("#2e3440"), WorkbenchPaletteResolver.resolve(theme).get(WorkbenchColor.SURFACE));
	}

	@Test
	public void neonOrDistantCurrentLineIsRejectedAndTheChromeStaysCalm() {
		for (String unsuitable : new String[] { "#00ffcc", "#ff0066", "#6c7a96", "#282c34" }) {
			WorkbenchPalette palette = resolve(ThemeType.DARK, "#282c34", "#abb2bf", TokenKey.CURRENT_LINE, unsuitable);
			assertNotEquals(Color.ofHex(unsuitable), palette.get(WorkbenchColor.SURFACE));
			assertNotEquals(Color.ofHex(unsuitable), palette.get(WorkbenchColor.SURFACE_RAISED));
			assertTrue(PerceptualColor.of(palette.get(WorkbenchColor.SURFACE)).c < PerceptualColor
					.of(Color.ofHex("#282c34")).c + 0.05);
		}
	}

	@Test
	public void readableAuthoredLineNumberColorBecomesTheMutedForeground() {
		WorkbenchPalette palette = resolve(ThemeType.DARK, "#282c34", "#abb2bf", TokenKey.LINE_NUMBER, "#9ea5b2");
		assertEquals(Color.ofHex("#9ea5b2"), palette.get(WorkbenchColor.MUTED_FOREGROUND));
	}

	@Test
	public void lowContrastLineNumberColorIsNeverUsedAsSecondaryText() {
		WorkbenchPalette palette = resolve(ThemeType.DARK, "#282c34", "#abb2bf", TokenKey.LINE_NUMBER, "#4b5263");
		assertNotEquals(Color.ofHex("#4b5263"), palette.get(WorkbenchColor.MUTED_FOREGROUND));
		assertMutedReadable(palette);
	}

	@Test
	public void authoredSelectionIsKeptOnlyWhenVisibleAndReadable() {
		WorkbenchPalette kept = resolve(ThemeType.DARK, "#282c34", "#abb2bf", TokenKey.SELECTION_BACKGROUND, "#3e4451");
		assertEquals(Color.ofHex("#3e4451"), kept.get(WorkbenchColor.SELECTION));

		WorkbenchPalette tooBright = resolve(ThemeType.DARK, "#282c34", "#abb2bf", TokenKey.SELECTION_BACKGROUND,
				"#a0a8b4");
		assertNotEquals(Color.ofHex("#a0a8b4"), tooBright.get(WorkbenchColor.SELECTION));

		WorkbenchPalette invisible = resolve(ThemeType.DARK, "#282c34", "#abb2bf", TokenKey.SELECTION_BACKGROUND,
				"#282c34");
		assertNotEquals(invisible.get(WorkbenchColor.BACKGROUND), invisible.get(WorkbenchColor.SELECTION));
	}

	// ---- perceptual derivation ---------------------------------------------------------------------------------

	@Test
	public void coolDarkThemeKeepsCoolChrome() {
		assertChromeKeepsHue(resolve(ThemeType.DARK, "#1b2535", "#c5d0e0"), "#1b2535");
	}

	@Test
	public void warmDarkThemeKeepsWarmChrome() {
		assertChromeKeepsHue(resolve(ThemeType.DARK, "#2d2118", "#e6d5c0"), "#2d2118");
	}

	@Test
	public void tintedLightThemeKeepsItsTint() {
		WorkbenchPalette palette = resolve(ThemeType.LIGHT, "#fdf6e3", "#586e75");
		assertChromeKeepsHue(palette, "#fdf6e3");
		// The cream stays cream: the surface is still warmer than a neutral gray of the same lightness.
		assertTrue(PerceptualColor.of(palette.get(WorkbenchColor.SURFACE)).c > 0.01);
	}

	@Test
	public void neutralThemeStaysNeutral() {
		WorkbenchPalette palette = resolve(ThemeType.DARK, "#1e1e1e", "#d4d4d4");
		for (WorkbenchColor role : new WorkbenchColor[] { WorkbenchColor.SURFACE, WorkbenchColor.SURFACE_RAISED,
				WorkbenchColor.BORDER }) {
			assertTrue(role.name(), PerceptualColor.of(palette.get(role)).c < 0.02);
		}
	}

	// ---- readability correction --------------------------------------------------------------------------------

	@Test
	public void blackForegroundOnDarkBackgroundIsCorrectedToReadable() {
		WorkbenchPalette palette = resolve(ThemeType.DARK, "#202020", "#000000");
		assertReadable(palette);
		assertTrue(lightness(palette, WorkbenchColor.FOREGROUND) > lightness(palette, WorkbenchColor.BACKGROUND));
	}

	@Test
	public void whiteForegroundOnLightBackgroundIsCorrectedToReadable() {
		WorkbenchPalette palette = resolve(ThemeType.LIGHT, "#f5f5f5", "#ffffff");
		assertReadable(palette);
		assertTrue(lightness(palette, WorkbenchColor.FOREGROUND) < lightness(palette, WorkbenchColor.BACKGROUND));
	}

	@Test
	public void lowContrastForegroundKeepsItsHueWhileBeingCorrected() {
		WorkbenchPalette palette = resolve(ThemeType.DARK, "#202020", "#5c3a2e");
		assertReadable(palette);
		assertTrue(PerceptualColor.hueDistanceDegrees(PerceptualColor.of(Color.ofHex("#5c3a2e")),
				PerceptualColor.of(palette.get(WorkbenchColor.FOREGROUND))) < 25.0);
	}

	@Test
	public void everyConvertedPaletteMeetsEveryContrastRule() {
		Theme[] themes = {
				theme(ThemeType.DARK, "#282c34", "#abb2bf", Map.of(TokenKey.CURRENT_LINE, "#2c313c",
						TokenKey.SELECTION_BACKGROUND, "#3e4451", TokenKey.LINE_NUMBER, "#636d83")),
				theme(ThemeType.DARK, "#282a36", "#f8f8f2", Map.of(TokenKey.SELECTION_BACKGROUND, "#44475a")),
				theme(ThemeType.DARK, "#1e1e2e", "#cdd6f4", Map.of(TokenKey.CURRENT_LINE, "#313244")),
				theme(ThemeType.DARK, "#282828", "#ebdbb2", Map.of(TokenKey.LINE_NUMBER, "#7c6f64")),
				theme(ThemeType.LIGHT, "#fdf6e3", "#657b83", Map.of(TokenKey.CURRENT_LINE, "#eee8d5")),
				theme(ThemeType.LIGHT, "#ffffff", "#24292f", Map.of(TokenKey.CURRENT_LINE, "#f6f8fa")),
				theme(ThemeType.DARK, "#0b3a40", "#e8c9a0",
						Map.of(TokenKey.CURRENT_LINE, "#d4a017", TokenKey.SELECTION_BACKGROUND, "#7a3b1d")),
				theme(ThemeType.LIGHT, "#101010", "#202020", Map.of()) };
		for (Theme theme : themes) {
			WorkbenchPalette palette = WorkbenchPaletteResolver.resolve(theme);
			assertReadable(palette);
			assertMutedReadable(palette);
			assertTrue(WorkbenchPaletteResolver.contrast(palette.get(WorkbenchColor.FOREGROUND),
					palette.get(WorkbenchColor.SELECTION)) >= SELECTION);
			assertTrue(worst(palette.get(WorkbenchColor.ACCENT), palette, WorkbenchColor.BACKGROUND,
					WorkbenchColor.SURFACE) >= TEXT);
		}
	}

	// ---- no syntax mapping -------------------------------------------------------------------------------------

	@Test
	public void syntaxTokensNeverInfluenceTheWorkbench() {
		Map<TokenKey, Token> plain = tokens("#282c34", "#abb2bf", Map.of());
		Map<TokenKey, Token> colorful = new LinkedHashMap<>(plain);
		for (TokenKey key : new TokenKey[] { TokenKey.KEYWORD, TokenKey.STRING, TokenKey.COMMENT, TokenKey.NUMBER,
				TokenKey.ERROR, TokenKey.CLASS, TokenKey.METHOD }) {
			colorful.put(key, token(key, "#ff00aa"));
		}
		assertEquals(WorkbenchPaletteResolver.resolve(new Theme("a", "A", "", "", "", ThemeType.DARK, plain)),
				WorkbenchPaletteResolver.resolve(new Theme("b", "B", "", "", "", ThemeType.DARK, colorful)));
	}

	// ---- fallback ----------------------------------------------------------------------------------------------

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
	public void minimalThemeWithOnlyBackgroundAndForegroundStillConverts() {
		WorkbenchPalette palette = resolve(ThemeType.DARK, "#14202b", "#d0dae4");
		assertNotEquals(WorkbenchFamily.DARK.fallbackPalette(), palette);
		assertEquals(Color.ofHex("#14202b"), palette.get(WorkbenchColor.BACKGROUND));
		assertReadable(palette);
	}

	@Test
	public void familyFallbackPalettesAreThemselvesReadable() {
		for (WorkbenchFamily family : WorkbenchFamily.values()) {
			WorkbenchPalette palette = family.fallbackPalette();
			assertReadable(palette);
			assertMutedReadable(palette);
		}
	}

	// ---- helpers -----------------------------------------------------------------------------------------------

	private static void assertChromeKeepsHue(WorkbenchPalette palette, String background) {
		PerceptualColor bg = PerceptualColor.of(Color.ofHex(background));
		assertTrue("background should carry a hue", bg.c > 0.01);
		for (WorkbenchColor role : new WorkbenchColor[] { WorkbenchColor.SURFACE, WorkbenchColor.SURFACE_RAISED,
				WorkbenchColor.BORDER }) {
			PerceptualColor chrome = PerceptualColor.of(palette.get(role));
			assertTrue(role.name() + " hue", PerceptualColor.hueDistanceDegrees(bg, chrome) < 30.0);
		}
	}

	private static void assertReadable(WorkbenchPalette palette) {
		assertTrue("foreground", worst(palette.get(WorkbenchColor.FOREGROUND), palette, WorkbenchColor.BACKGROUND,
				WorkbenchColor.SURFACE, WorkbenchColor.SURFACE_RAISED) >= TEXT);
	}

	private static void assertMutedReadable(WorkbenchPalette palette) {
		assertTrue("muted", worst(palette.get(WorkbenchColor.MUTED_FOREGROUND), palette, WorkbenchColor.BACKGROUND,
				WorkbenchColor.SURFACE) >= TEXT);
	}

	private static double worst(Color color, WorkbenchPalette palette, WorkbenchColor... surfaces) {
		double worst = Double.MAX_VALUE;
		for (WorkbenchColor surface : surfaces) {
			worst = Math.min(worst, WorkbenchPaletteResolver.contrast(color, palette.get(surface)));
		}
		return worst;
	}

	private static double lightness(WorkbenchPalette palette, WorkbenchColor role) {
		return PerceptualColor.of(palette.get(role)).l;
	}

	private static WorkbenchPalette resolve(ThemeType type, String background, String foreground) {
		return WorkbenchPaletteResolver.resolve(theme(type, background, foreground, Map.of()));
	}

	private static WorkbenchPalette resolve(ThemeType type, String background, String foreground, TokenKey key,
			String hex) {
		return WorkbenchPaletteResolver.resolve(theme(type, background, foreground, Map.of(key, hex)));
	}

	private static Theme theme(ThemeType type, String background, String foreground, Map<TokenKey, String> extra) {
		return new Theme("id", "name", "", "", "", type, tokens(background, foreground, extra));
	}

	private static Map<TokenKey, Token> tokens(String background, String foreground, Map<TokenKey, String> extra) {
		Map<TokenKey, Token> tokens = new LinkedHashMap<>();
		tokens.put(TokenKey.BACKGROUND, token(TokenKey.BACKGROUND, background));
		tokens.put(TokenKey.FOREGROUND, token(TokenKey.FOREGROUND, foreground));
		extra.forEach((key, hex) -> tokens.put(key, token(key, hex)));
		return tokens;
	}

	private static Token token(TokenKey key, String hex) {
		return new TokenBuilder().setKey(key).setColor(Color.ofHex(hex)).build();
	}
}
