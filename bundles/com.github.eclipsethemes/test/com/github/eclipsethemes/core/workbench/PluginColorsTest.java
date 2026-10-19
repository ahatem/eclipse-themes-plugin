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
		assertEquals(PluginColorTable.keys().size(), values.size());
		for (PluginColorKey key : PluginColorTable.keys()) {
			String actual = values.get(key.id());
			switch (key.source()) {
				case BACKGROUND -> assertEquals(key.id(), role(palette, WorkbenchColor.BACKGROUND), actual);
				case RAISED -> assertEquals(key.id(), role(palette, WorkbenchColor.SURFACE_RAISED), actual);
				case BORDER -> assertEquals(key.id(), role(palette, WorkbenchColor.BORDER), actual);
				case FOREGROUND -> assertEquals(key.id(), role(palette, WorkbenchColor.FOREGROUND), actual);
				case MUTED -> assertEquals(key.id(), role(palette, WorkbenchColor.MUTED_FOREGROUND), actual);
				case ACCENT -> assertEquals(key.id(), role(palette, WorkbenchColor.ACCENT), actual);
				case DARK_FLAG -> assertEquals(key.id(), "true", actual);
				case OFF_FLAG -> assertEquals(key.id(), "false", actual);
				default -> assertTrue(key.id() + " " + actual, actual.matches("\\d{1,3},\\d{1,3},\\d{1,3}"));
			}
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
	public void textColorsAreReadableOnEverySurfaceForEveryDarkTheme() {
		Set<PluginColorSource> text = Set.of(PluginColorSource.ACCENT, PluginColorSource.ACTIVE_ACCENT,
				PluginColorSource.FOREGROUND, PluginColorSource.ERROR, PluginColorSource.WARNING,
				PluginColorSource.SUCCESS, PluginColorSource.INFO);
		for (String pair : THEMES) {
			String[] colors = pair.split(" ");
			WorkbenchPalette palette = palette(ThemeType.DARK, colors[0], colors[1]);
			Map<String, String> values = PluginColors.derive(palette);
			for (PluginColorKey key : PluginColorTable.keys()) {
				boolean muted = key.source() == PluginColorSource.MUTED;
				if (!text.contains(key.source()) && !muted) {
					continue;
				}
				Color color = parse(values.get(key.id()));
				// The palette guarantees muted text on the background and surface, not on the raised color.
				List<WorkbenchColor> surfaces = muted ? List.of(WorkbenchColor.BACKGROUND, WorkbenchColor.SURFACE)
						: List.of(WorkbenchColor.BACKGROUND, WorkbenchColor.SURFACE, WorkbenchColor.SURFACE_RAISED);
				for (WorkbenchColor surface : surfaces) {
					assertTrue(pair + " " + key.key() + " on " + surface,
							WorkbenchPaletteResolver.contrast(color, palette.get(surface)) >= 4.5);
				}
			}
		}
	}

	@Test
	public void tintsKeepTheForegroundReadableAndStayDistinctFromTheBackground() {
		Set<PluginColorSource> tints = Set.of(PluginColorSource.ERROR_TINT, PluginColorSource.WARNING_TINT,
				PluginColorSource.SUCCESS_TINT, PluginColorSource.INFO_TINT, PluginColorSource.WARNING_TINT_SOFT);
		for (String pair : THEMES) {
			String[] colors = pair.split(" ");
			WorkbenchPalette palette = palette(ThemeType.DARK, colors[0], colors[1]);
			Map<String, String> values = PluginColors.derive(palette);
			Color background = palette.get(WorkbenchColor.BACKGROUND);
			Color foreground = palette.get(WorkbenchColor.FOREGROUND);
			for (PluginColorKey key : PluginColorTable.keys()) {
				if (!tints.contains(key.source())) {
					continue;
				}
				Color tint = parse(values.get(key.id()));
				assertTrue(pair + " " + key.key(), WorkbenchPaletteResolver.contrast(foreground, tint) >= 4.5);
				assertNotEquals(pair + " " + key.key(), background, tint);
			}
			Color strong = parse(values.get("org.eclipse.ui.workbench|org.eclipse.jface.REVISION_NEWEST_COLOR"));
			Color soft = parse(values.get("org.eclipse.ui.workbench|org.eclipse.jface.REVISION_OLDEST_COLOR"));
			assertTrue(pair, distance(soft, background) <= distance(strong, background) + 1e-9);
		}
	}

	@Test
	public void statusHuesAreDistinctFromEachOther() {
		WorkbenchPalette palette = palette(ThemeType.DARK, "#282c34", "#abb2bf");
		Map<String, String> values = PluginColors.derive(palette);
		Set<String> status = new HashSet<>();
		status.add(values.get("org.eclipse.ui.workbench|ERROR_COLOR"));
		status.add(values.get("org.eclipse.ui.workbench|org.eclipse.ui.workbench.FORM_HEADING_WARNING_COLOR"));
		status.add(values.get("org.eclipse.ui.workbench|INCOMING_COLOR"));
		status.add(values.get("org.eclipse.ui.workbench|HYPERLINK_COLOR"));
		assertEquals(4, status.size());
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
	public void keysOutsideThePlatformNodesNameTheirOwnBundle() {
		for (PluginColorKey key : PluginColorTable.keys()) {
			if (!key.node().equals(PluginColorTable.WORKBENCH) && !key.node().equals(PluginColorTable.EDITORS)) {
				assertEquals(key.id(), key.node(), key.requiredBundle());
			}
		}
	}

	@Test
	public void theApprovedPopupAndHyperlinkKeysAreStillCoordinated() {
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

	/** Euclidean distance in Oklab, a rough measure of how different two colors look. */
	private static double distance(Color a, Color b) {
		PerceptualColor pa = PerceptualColor.of(a);
		PerceptualColor pb = PerceptualColor.of(b);
		double da = pa.c * Math.cos(pa.h) - pb.c * Math.cos(pb.h);
		double db = pa.c * Math.sin(pa.h) - pb.c * Math.sin(pb.h);
		return Math.sqrt((pa.l - pb.l) * (pa.l - pb.l) + da * da + db * db);
	}

	private static String role(WorkbenchPalette palette, WorkbenchColor color) {
		return palette.get(color).toEclipseFormat();
	}

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
