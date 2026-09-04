package com.github.eclipsethemes.eclipse.adapters.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenKey;

/**
 * Guards the reason workbench theming used to leak Eclipse's own greys: the e4
 * CSS engine picks the most specific rule first and only falls back to
 * stylesheet order for ties, so the overlay has to repeat the selectors used by
 * {@code org.eclipse.ui.themes} instead of relying on plainer ones.
 */
class PlatformThemeOverrideTest {

	private static final List<String> PLATFORM_STYLESHEETS = List.of(
			"css/dark/e4-dark_globalstyle.css",
			"css/dark/e4-dark_partstyle.css",
			"css/dark/e4-dark_tabstyle.css",
			"css/e4-dark_linux.css",
			"css/e4_default_gtk.css");

	/**
	 * Eclipse paints the splash screen before any theme is active, so its colors
	 * are intentionally left alone.
	 */
	private static final Set<String> UNTHEMED_SELECTORS = Set.of(
			"Label#org-eclipse-ui-splash-progressText",
			"Label#org-eclipse-ui-buildid-text",
			"ProgressIndicator#org-eclipse-ui-splash-progressIndicator",
			".Mpart ScrolledComposite ProgressInfoItem",
			".Mpart OleFrame");

	/** {@code background-image} expects an image or a gradient, not a plain color. */
	private static final Set<String> UNTHEMED_PROPERTIES = Set.of("background-image");

	@Test
	void overlayRepeatsEverySelectorWhereEclipseHardCodesAColor() {
		Map<String, Set<String>> overlay = CssRules.declarations(WorkbenchCssGenerator.generate(theme()));
		List<String> missing = new ArrayList<>();

		for (String stylesheet : PLATFORM_STYLESHEETS) {
			String css = CssRules.platformStylesheet(stylesheet);
			assertFalse(css.isEmpty(), "Could not read " + stylesheet + " from org.eclipse.ui.themes");

			CssRules.colorDeclarations(css).forEach((selector, properties) -> {
				if (UNTHEMED_SELECTORS.contains(selector)) {
					return;
				}
				for (String property : properties) {
					if (UNTHEMED_PROPERTIES.contains(property)) {
						continue;
					}
					if (!overlay.getOrDefault(selector, Set.of()).contains(property)) {
						missing.add(stylesheet + ": " + selector + " { " + property + " }");
					}
				}
			});
		}

		assertTrue(missing.isEmpty(), "Eclipse colors that the overlay does not replace:\n" + String.join("\n", missing));
	}

	@Test
	void overlayOverridesEveryColorDefinitionTheBaseThemesDeclare() {
		Map<String, Set<String>> overlay = CssRules.declarations(WorkbenchCssGenerator.generate(theme()));
		List<String> missing = new ArrayList<>();

		for (String stylesheet : List.of("css/dark/e4-dark_ide_colorextensions.css",
				"css/light/e4-light_ide_colorextensions.css")) {
			String css = CssRules.platformStylesheet(stylesheet);
			assertFalse(css.isEmpty(), "Could not read " + stylesheet + " from org.eclipse.ui.themes");

			for (String selector : CssRules.declarations(css).keySet()) {
				if (selector.startsWith("ColorDefinition#") && !overlay.containsKey(selector)) {
					missing.add(stylesheet + ": " + selector);
				}
			}
		}

		assertTrue(missing.isEmpty(), "Color definitions the overlay leaves at Eclipse's value:\n"
				+ String.join("\n", missing));
	}

	/**
	 * Eclipse releases after the 2024-09 build target moved several colors behind
	 * selectors that outrank the ones this test can discover from the target
	 * platform, most visibly the editor area stack (addressed by id) and view
	 * bodies tagged {@code .View}. They are pinned here so the overlay keeps
	 * working on a newer IDE than the one the plugin is compiled against.
	 */
	private static final List<String> SELECTORS_ADDED_AFTER_THE_BUILD_TARGET = List.of(
			"ColorDefinition#org-eclipse-ui-workbench-SECONDARY_BACKGROUND",
			"#org-eclipse-ui-editorss CTabFolder",
			"#org-eclipse-ui-editorss CTabItem",
			"#org-eclipse-ui-editorss CTabItem:selected",
			"#org-eclipse-e4-ui-compatibility-editor Canvas",
			"#org-eclipse-e4-ui-compatibility-editor Composite",
			".MPartStack.active",
			".MPartStack CTabFolder[style~='SWT.DOWN'][style~='SWT.BOTTOM']",
			".MPartStack.active Table",
			".MPart CTabFolder",
			".MPart Form Label",
			".MPart Form Section",
			".MPartStack.active .MPart Form Label",
			"#org-eclipse-help-ui-HelpView Form",
			".View Composite",
			".View Composite Text",
			".View Group Combo",
			".View Button[style~='SWT.PUSH']",
			".View TitleRegion",
			"Composite.MArea",
			"Button.disabled",
			"Combo:selected",
			".MTrimBar#org-eclipse-ui-trim-status");

	@Test
	void overlayAlsoCoversSelectorsFromNewerEclipseReleases() {
		Map<String, Set<String>> overlay = CssRules.declarations(WorkbenchCssGenerator.generate(theme()));
		List<String> missing = new ArrayList<>();

		for (String selector : SELECTORS_ADDED_AFTER_THE_BUILD_TARGET) {
			if (!overlay.containsKey(selector)) {
				missing.add(selector);
			}
		}

		assertTrue(missing.isEmpty(), "Selectors from newer Eclipse releases that the overlay does not replace:\n"
				+ String.join("\n", missing));
	}

	@Test
	void overlayDoesNotReplaceTheColorDefinitionRegistration() {
		// A ThemesExtension rule of ours would win over Eclipse's and drop every
		// definition it registers, which would unstyle the tabs completely.
		Map<String, Set<String>> overlay = CssRules.declarations(WorkbenchCssGenerator.generate(theme()));

		assertFalse(overlay.containsKey("ThemesExtension"));
	}

	private static Theme theme() {
		Theme theme = new Theme("mocha", "Catppuccin Mocha", "Eclipse Themes", null, null, null, ThemeType.DARK);
		theme.addToken(new Token(TokenKey.BACKGROUND, Color.ofHex("#1e1e2e"), null));
		theme.addToken(new Token(TokenKey.FOREGROUND, Color.ofHex("#cdd6f4"), null));
		theme.addToken(new Token(TokenKey.SELECTION_BACKGROUND, Color.ofHex("#3e4056"), null));
		theme.addToken(new Token(TokenKey.SELECTION_FOREGROUND, Color.ofHex("#cdd6f4"), null));
		theme.addToken(new Token(TokenKey.CURRENT_LINE, Color.ofHex("#28283d"), null));
		theme.addToken(new Token(TokenKey.DOC_LINK, Color.ofHex("#89b4fa"), null));
		return theme;
	}
}
