package com.github.eclipsethemes.eclipse.adapters.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.parser.mapper.LegacyEclipsePropertyMapper;

class CssGeneratorTest {

	@Test
	void workbenchCssUsesThemeSurfaceColors() {
		String css = WorkbenchCssGenerator.generate(darkDimmed());

		assertTrue(css.contains("DARK_BACKGROUND { color: #22272e; }"));
		assertTrue(css.contains("DARK_FOREGROUND { color: #adbac7; }"));
		assertTrue(css.contains("background-color: #22272e;"));
	}

	@Test
	void workbenchCssOverridesPlatformRulesForListsAndInputs() {
		String css = WorkbenchCssGenerator.generate(darkDimmed());

		// Eclipse's dark stylesheet pins these with more specific selectors than a
		// bare "Tree" or "Text", so the overlay has to repeat them verbatim.
		assertTrue(css.contains("Shell Tree, Shell Table, Shell List"));
		assertTrue(css.contains("Text[style~='SWT.SEARCH']"));
		assertTrue(css.contains(".MPart Composite,"));
		assertTrue(css.contains(".MPart Label,"));
	}

	@Test
	void workbenchCssOnlyUsesColorsDerivedFromTheTheme() {
		WorkbenchPalette palette = WorkbenchPalette.of(darkDimmed());
		String css = WorkbenchCssGenerator.generate(darkDimmed());

		for (String color : CssRules.literalColors(css)) {
			assertTrue(CssRules.paletteColors(palette).contains(color),
					"Generated workbench CSS still contains a hard-coded color: " + color);
		}
	}

	@Test
	void gtkCssStylesTheNativeControlsEclipseCssCannotReach() {
		String css = GtkCssGenerator.generate(darkDimmed());

		assertTrue(css.contains("@define-color theme_bg_color #22272e;"));
		assertTrue(css.contains("@define-color theme_fg_color #adbac7;"));
		assertTrue(css.contains("@define-color theme_selected_bg_color #3d4149;"));
		assertTrue(css.contains("entry, spinbutton, textview, textview text, .entry"));
		assertTrue(css.contains("menubar, menu,"));
		assertTrue(css.contains("scrollbar slider"));
		assertTrue(css.contains("*:selected"));
	}

	@Test
	void gtkCssRaisesPushButtonsAndKeepsToolbarButtonsFlat() {
		WorkbenchPalette palette = WorkbenchPalette.of(darkDimmed());
		String css = GtkCssGenerator.generate(darkDimmed());

		// Chrome is darker than the surface, so a button filled with it reads as a
		// hole rather than a raised control.
		assertTrue(css.contains("button {\n    background-color: " + palette.elevated() + ";"));
		assertTrue(css.contains("toolbar button, toolbar togglebutton, headerbar button, "
				+ "notebook button, button.flat {\n    background-color: transparent;"));
	}

	@Test
	void workbenchCssThemesTheTeamDecorationColors() {
		String css = WorkbenchCssGenerator.generate(darkDimmed());

		assertTrue(css.contains("ColorDefinition#org-eclipse-egit-ui-UncommittedChangeBackgroundColor"));
		assertTrue(css.contains("ColorDefinition#org-eclipse-egit-ui-IgnoredResourceBackgroundColor"));
	}

	@Test
	void gtkCssOnlyUsesColorsDerivedFromTheTheme() {
		WorkbenchPalette palette = WorkbenchPalette.of(darkDimmed());
		String css = GtkCssGenerator.generate(darkDimmed());

		for (String color : CssRules.literalColors(css)) {
			assertTrue(CssRules.paletteColors(palette).contains(color),
					"Generated GTK CSS still contains a hard-coded color: " + color);
		}
	}

	@Test
	void paletteDerivesChromeAndInputSurfacesFromTheBackground() {
		WorkbenchPalette dark = WorkbenchPalette.of(darkDimmed());
		WorkbenchPalette light = WorkbenchPalette.of(latte());

		assertTrue(dark.isDark());
		assertNotEquals(dark.background(), dark.chrome());
		assertNotEquals(dark.background(), dark.input());
		assertNotEquals(dark.foreground(), dark.muted());

		assertTrue(!light.isDark());
		assertNotEquals(light.background(), light.chrome());
		assertNotEquals(light.background(), light.input());
	}

	@Test
	void paletteUsesTheDocLinkColorForHyperlinks() {
		Theme theme = darkDimmed();
		theme.addToken(new Token(TokenKey.DOC_LINK, Color.ofHex("#539bf5"), null));

		assertEquals("#539bf5", WorkbenchPalette.of(theme).link());
	}

	@Test
	void sourceHoverDoesNotOverwriteSelectionBackground() {
		String mapped = LegacyEclipsePropertyMapper.mapProperty("sourceHoverBackground");

		assertEquals(TokenKey.SOURCE_HOVER_BACKGROUND.getName(), mapped);
		assertNotEquals(TokenKey.SELECTION_BACKGROUND, TokenKey.byId(mapped));
	}

	private static Theme darkDimmed() {
		Theme theme = new Theme("test", "GitHub Dark Dimmed", "Eclipse Themes", null, null, null, ThemeType.DARK);
		theme.addToken(new Token(TokenKey.BACKGROUND, Color.ofHex("#22272e"), null));
		theme.addToken(new Token(TokenKey.FOREGROUND, Color.ofHex("#adbac7"), null));
		theme.addToken(new Token(TokenKey.SELECTION_BACKGROUND, Color.ofHex("#3d4149"), null));
		theme.addToken(new Token(TokenKey.SELECTION_FOREGROUND, Color.ofHex("#adbac7"), null));
		theme.addToken(new Token(TokenKey.CURRENT_LINE, Color.ofHex("#2d333b"), null));
		return theme;
	}

	private static Theme latte() {
		Theme theme = new Theme("latte", "Catppuccin Latte", "Eclipse Themes", null, null, null, ThemeType.LIGHT);
		theme.addToken(new Token(TokenKey.BACKGROUND, Color.ofHex("#eff1f5"), null));
		theme.addToken(new Token(TokenKey.FOREGROUND, Color.ofHex("#4c4f69"), null));
		theme.addToken(new Token(TokenKey.SELECTION_BACKGROUND, Color.ofHex("#bcc0cc"), null));
		theme.addToken(new Token(TokenKey.SELECTION_FOREGROUND, Color.ofHex("#4c4f69"), null));
		return theme;
	}
}
