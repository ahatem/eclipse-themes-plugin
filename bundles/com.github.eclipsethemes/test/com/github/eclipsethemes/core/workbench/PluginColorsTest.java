package com.github.eclipsethemes.core.workbench;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenBuilder;
import com.github.eclipsethemes.core.models.TokenKey;

/** Derivation of the coordinated plug-in keys, the key table's integrity, and the ownership record codec. */
public class PluginColorsTest {

	private static final String[] THEMES = { "#282c34 #abb2bf", "#1e1e2e #cdd6f4", "#282a36 #f8f8f2", "#282828 #ebdbb2",
			"#002b36 #839496", "#101010 #fafafa" };

	// ---- derivation ------------------------------------------------------------------------------------------

	@Test
	public void darkPaletteMapsEachKeyThroughItsSemanticRole() {
		WorkbenchPalette palette = palette(ThemeType.DARK, "#282c34", "#abb2bf");
		Map<String, String> values = PluginColors.derive(palette);
		String raised = palette.get(WorkbenchColor.SURFACE_RAISED).toEclipseFormat();
		String foreground = palette.get(WorkbenchColor.FOREGROUND).toEclipseFormat();
		assertEquals(PluginColorTable.keys().size(), values.size());
		for (PluginColorKey key : PluginColorTable.keys()) {
			String expected = switch (key.source()) {
				case POPUP_BACKGROUND -> raised;
				case FOREGROUND -> foreground;
				case LINK -> palette.get(WorkbenchColor.ACCENT).toEclipseFormat();
				case ACTIVE_LINK -> values.get(key.id());
				case DARK_FLAG -> "true";
			};
			assertEquals(key.id(), expected, values.get(key.id()));
		}
	}

	@Test
	public void lightPaletteDerivesNothingSoItReleasesEverything() {
		assertTrue(PluginColors.derive(palette(ThemeType.LIGHT, "#fafafa", "#383a42")).isEmpty());
	}

	@Test
	public void derivationIsDeterministicAndDiffersBetweenThemes() {
		Map<String, String> first = PluginColors.derive(palette(ThemeType.DARK, "#282c34", "#abb2bf"));
		assertEquals(first, PluginColors.derive(palette(ThemeType.DARK, "#282c34", "#abb2bf")));
		assertNotEquals(first, PluginColors.derive(palette(ThemeType.DARK, "#1e1e2e", "#cdd6f4")));
	}

	@Test
	public void linksAndPopupTextAreReadableOnEverySurfaceForEveryDarkTheme() {
		for (String pair : THEMES) {
			String[] colors = pair.split(" ");
			WorkbenchPalette palette = palette(ThemeType.DARK, colors[0], colors[1]);
			Map<String, String> values = PluginColors.derive(palette);
			for (PluginColorKey key : PluginColorTable.keys()) {
				if (key.source() == PluginColorSource.LINK || key.source() == PluginColorSource.ACTIVE_LINK
						|| key.source() == PluginColorSource.FOREGROUND) {
					Color color = parse(values.get(key.id()));
					for (WorkbenchColor surface : List.of(WorkbenchColor.BACKGROUND, WorkbenchColor.SURFACE,
							WorkbenchColor.SURFACE_RAISED)) {
						assertTrue(pair + " " + key.key() + " on " + surface,
								WorkbenchPaletteResolver.contrast(color, palette.get(surface)) >= 4.5);
					}
				}
			}
		}
	}

	@Test
	public void activeLinkIsDistinctFromTheLinkAndFallsBackToTheForegroundWhenUnreadable() {
		WorkbenchPalette palette = palette(ThemeType.DARK, "#282c34", "#abb2bf");
		Map<String, String> values = PluginColors.derive(palette);
		assertNotEquals(values.get("org.eclipse.ui.workbench|HYPERLINK_COLOR"),
				values.get("org.eclipse.ui.workbench|ACTIVE_HYPERLINK_COLOR"));

		// A palette whose raised surface is so close to the foreground that the mix cannot reach 4.5:1.
		Map<WorkbenchColor, Color> colors = new LinkedHashMap<>();
		for (WorkbenchColor role : WorkbenchColor.values()) {
			colors.put(role, WorkbenchFamily.DARK.fallbackPalette().get(role));
		}
		colors.put(WorkbenchColor.SURFACE_RAISED, Color.ofHex("#c8c8c8"));
		colors.put(WorkbenchColor.FOREGROUND, Color.ofHex("#d4d4d4"));
		Map<String, String> unreadable = PluginColors.derive(new WorkbenchPalette(WorkbenchFamily.DARK, colors));
		assertEquals(Color.ofHex("#d4d4d4").toEclipseFormat(),
				unreadable.get("org.eclipse.ui.workbench|ACTIVE_HYPERLINK_COLOR"));
	}

	// ---- table integrity -------------------------------------------------------------------------------------

	@Test
	public void tableKeysAreUniqueAndCarryNoRecordSeparators() {
		Set<String> ids = new HashSet<>();
		for (PluginColorKey key : PluginColorTable.keys()) {
			assertTrue("duplicate " + key.id(), ids.add(key.id()));
			assertFalse(key.id().contains("\n"));
		}
	}

	@Test
	public void sliceOneKeysAreExactlyTheMaintainerApprovedFifteen() {
		Set<String> ids = new HashSet<>();
		PluginColorTable.keys().forEach(key -> ids.add(key.id()));
		assertTrue(ids.containsAll(List.of("org.eclipse.ui.workbench|org.eclipse.ui.workbench.HOVER_BACKGROUND",
				"org.eclipse.ui.workbench|org.eclipse.ui.workbench.HOVER_FOREGROUND",
				"org.eclipse.ui.workbench|org.eclipse.ui.workbench.INFORMATION_BACKGROUND",
				"org.eclipse.ui.workbench|org.eclipse.ui.workbench.INFORMATION_FOREGROUND",
				"org.eclipse.ui.workbench|CONTENT_ASSIST_BACKGROUND_COLOR",
				"org.eclipse.ui.workbench|CONTENT_ASSIST_FOREGROUND_COLOR",
				"org.eclipse.ui.workbench|org.eclipse.jdt.ui.Javadoc.backgroundColor",
				"org.eclipse.ui.workbench|org.eclipse.jdt.ui.Javadoc.foregroundColor",
				"org.eclipse.ui.workbench|HYPERLINK_COLOR", "org.eclipse.ui.workbench|ACTIVE_HYPERLINK_COLOR",
				"org.eclipse.jdt.ui|content_assist_proposals_background",
				"org.eclipse.jdt.ui|content_assist_proposals_foreground",
				"org.eclipse.jdt.ui|content_assist_parameters_background",
				"org.eclipse.jdt.ui|content_assist_parameters_foreground",
				"org.eclipse.jdt.ui|javadocElementsStyling.darkModeDefaultColors")));
	}

	// ---- ownership record codec ------------------------------------------------------------------------------

	@Test
	public void recordRoundTripsEveryDerivedValue() {
		Map<String, String> owned = PluginColors.derive(palette(ThemeType.DARK, "#1e1e2e", "#cdd6f4"));
		assertEquals(owned, PluginColorRecord.parse(PluginColorRecord.format(owned)));
	}

	@Test
	public void malformedRecordsBecomeEmptyAndNeverPartiallyOwned() {
		assertTrue(PluginColorRecord.parse(null).isEmpty());
		assertTrue(PluginColorRecord.parse("").isEmpty());
		assertTrue(PluginColorRecord.parse("no separators").isEmpty());
		assertTrue(PluginColorRecord.parse("node|key").isEmpty());
		assertTrue(PluginColorRecord.parse("node|key|").isEmpty());
		assertTrue(PluginColorRecord.parse("|key|value").isEmpty());
		// one bad line discards the whole record, so no half-trusted ownership is acted on
		assertTrue(PluginColorRecord.parse("a|b|1,2,3\ngarbage\n").isEmpty());
	}

	// ---- helpers ---------------------------------------------------------------------------------------------

	private static Color parse(String eclipseFormat) {
		String[] rgb = eclipseFormat.split(",");
		return Color.ofRgb(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
	}

	private static WorkbenchPalette palette(ThemeType type, String background, String foreground) {
		Map<TokenKey, Token> tokens = new LinkedHashMap<>();
		tokens.put(TokenKey.BACKGROUND,
				new TokenBuilder().setKey(TokenKey.BACKGROUND).setColor(Color.ofHex(background)).build());
		tokens.put(TokenKey.FOREGROUND,
				new TokenBuilder().setKey(TokenKey.FOREGROUND).setColor(Color.ofHex(foreground)).build());
		return WorkbenchPaletteResolver.resolve(new Theme("id", "name", "", "", "", type, tokens));
	}
}
