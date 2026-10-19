package com.github.eclipsethemes.eclipse.workbench;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenBuilder;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.workbench.PluginColorKey;
import com.github.eclipsethemes.core.workbench.PluginColorRecord;
import com.github.eclipsethemes.core.workbench.PluginColorTable;
import com.github.eclipsethemes.core.workbench.PluginColors;
import com.github.eclipsethemes.core.workbench.WorkbenchPaletteResolver;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;

/** The ownership model of coordinated plug-in keys, driven through in-memory foreign preferences. */
public class PluginColorWriterTest {

	private static final String WB = "org.eclipse.ui.workbench";
	private static final String JDT = "org.eclipse.jdt.ui";
	private static final String HOVER_BG = "org.eclipse.ui.workbench.HOVER_BACKGROUND";
	private static final String HOVER_BG_ID = WB + "|" + HOVER_BG;
	private static final String HYPERLINK_ID = WB + "|HYPERLINK_COLOR";
	private static final String JAVADOC_BG_ID = WB + "|org.eclipse.jdt.ui.Javadoc.backgroundColor";
	private static final String DARK_FLAG = "javadocElementsStyling.darkModeDefaultColors";

	private MemoryForeignPreferences foreign;
	private TestPreferenceStore store;
	private PluginColorWriter writer;
	private Map<String, String> atom;
	private Map<String, String> mocha;

	@Before
	public void setUp() {
		foreign = new MemoryForeignPreferences();
		store = new TestPreferenceStore();
		writer = new PluginColorWriter(foreign, store);
		atom = PluginColors.derive(WorkbenchPaletteResolver.resolve(theme("#282c34", "#abb2bf")));
		mocha = PluginColors.derive(WorkbenchPaletteResolver.resolve(theme("#1e1e2e", "#cdd6f4")));
	}

	// ---- the four ownership branches -------------------------------------------------------------------------

	@Test
	public void absentKeysAreAcquiredWithTheExactValueAndRecorded() {
		PluginColorSummary summary = writer.sync(atom);
		assertEquals(PluginColorTable.keys().size(), summary.ownedKeys().size());
		assertTrue(summary.userKeys().isEmpty() && summary.releasedKeys().isEmpty() && !summary.failed());
		for (PluginColorKey key : PluginColorTable.keys()) {
			assertEquals(Optional.of(atom.get(key.id())), foreign.get(key.node(), key.key()));
		}
		assertEquals(atom, recorded());
		assertTrue(writer.hasOwned());
	}

	@Test
	public void aValueThatExistedBeforeTheFirstWriteIsUserOwnedAndNeverWritten() {
		foreign.set(WB, HOVER_BG, "10,20,30");
		PluginColorSummary summary = writer.sync(atom);
		assertEquals(List.of(HOVER_BG_ID), summary.userKeys());
		assertEquals(Optional.of("10,20,30"), foreign.get(WB, HOVER_BG));
		assertFalse(foreign.mutations.stream().anyMatch(m -> m.startsWith("put " + WB + "|" + HOVER_BG + "|")));
		assertFalse(recorded().containsKey(HOVER_BG_ID));
		// it stays the user's on every later Apply and on release
		writer.sync(mocha);
		writer.release();
		assertEquals(Optional.of("10,20,30"), foreign.get(WB, HOVER_BG));
	}

	@Test
	public void ownedUnchangedKeysAreUpdatedToTheNewThemeAndRecordFollows() {
		writer.sync(atom);
		PluginColorSummary summary = writer.sync(mocha);
		assertTrue(summary.releasedKeys().isEmpty());
		for (PluginColorKey key : PluginColorTable.keys()) {
			assertEquals(Optional.of(mocha.get(key.id())), foreign.get(key.node(), key.key()));
		}
		assertEquals(mocha, recorded());
	}

	@Test
	public void anExternalChangeReleasesOwnershipAndIsNeverOverwrittenOrRemoved() {
		writer.sync(atom);
		foreign.set(WB, "HYPERLINK_COLOR", "1,2,3");
		PluginColorSummary summary = writer.sync(mocha);
		assertEquals(List.of(HYPERLINK_ID), summary.releasedKeys());
		assertEquals(Optional.of("1,2,3"), foreign.get(WB, "HYPERLINK_COLOR"));
		assertFalse(recorded().containsKey(HYPERLINK_ID));
		// later syncs treat it as the user's value
		assertEquals(List.of(HYPERLINK_ID), writer.sync(atom).userKeys());
		writer.release();
		assertEquals(Optional.of("1,2,3"), foreign.get(WB, "HYPERLINK_COLOR"));
	}

	@Test
	public void releaseRemovesOnlyValuesThatStillEqualTheOwnedOnes() {
		writer.sync(atom);
		foreign.set(WB, HOVER_BG, "9,9,9");
		PluginColorSummary summary = writer.release();
		assertEquals(Optional.of("9,9,9"), foreign.get(WB, HOVER_BG));
		assertEquals(List.of(HOVER_BG_ID), summary.releasedKeys());
		for (PluginColorKey key : PluginColorTable.keys()) {
			if (!key.id().equals(HOVER_BG_ID)) {
				assertEquals(key.id(), Optional.empty(), foreign.get(key.node(), key.key()));
			}
		}
		assertFalse(writer.hasOwned());
		assertEquals("", store.getString(PreferenceKeys.PLUGIN_COLORS_OWNED));
	}

	@Test
	public void releasingWhenNothingIsOwnedTouchesNothing() {
		foreign.set(WB, HOVER_BG, "9,9,9");
		writer.release();
		assertTrue(foreign.mutations.isEmpty());
		assertEquals(Optional.of("9,9,9"), foreign.get(WB, HOVER_BG));
	}

	@Test
	public void aLightTargetSyncsAnEmptyMapAndReleasesDarkValues() {
		writer.sync(atom);
		Map<String, String> light = PluginColors
				.derive(WorkbenchPaletteResolver.resolve(lightTheme("#fafafa", "#383a42")));
		assertTrue(light.isEmpty());
		writer.sync(light);
		for (PluginColorKey key : PluginColorTable.keys()) {
			assertEquals(Optional.empty(), foreign.get(key.node(), key.key()));
		}
	}

	@Test
	public void anOwnedKeyThatVanishedIsTakenAgainBecauseItIsAbsent() {
		writer.sync(atom);
		foreign.unset(WB, "HYPERLINK_COLOR");
		PluginColorSummary summary = writer.sync(atom);
		assertEquals(List.of(HYPERLINK_ID), summary.releasedKeys());
		assertEquals(Optional.of(atom.get(HYPERLINK_ID)), foreign.get(WB, "HYPERLINK_COLOR"));
		assertEquals(atom, recorded());
	}

	@Test
	public void aStableSyncWritesNothingTheSecondTime() {
		writer.sync(atom);
		foreign.mutations.clear();
		writer.sync(atom);
		assertTrue(foreign.mutations.isEmpty());
	}

	// ---- mirror and optional bundles -------------------------------------------------------------------------

	@Test
	public void aValueTheRegistryMirrorsAwayAsADefaultIsNotClaimed() {
		foreign.mirroredDefaults.put(HYPERLINK_ID, atom.get(HYPERLINK_ID));
		PluginColorSummary summary = writer.sync(atom);
		assertFalse(summary.ownedKeys().contains(HYPERLINK_ID));
		assertFalse(recorded().containsKey(HYPERLINK_ID));
		assertEquals(Optional.empty(), foreign.get(WB, "HYPERLINK_COLOR"));
	}

	@Test
	public void withoutJdtNeitherTheJdtNodeNorTheJavadocDefinitionsAreWritten() {
		foreign.missingBundles.add(JDT);
		PluginColorSummary summary = writer.sync(atom);
		assertFalse(foreign.nodes.containsKey(JDT));
		assertEquals(Optional.empty(), foreign.get(WB, "org.eclipse.jdt.ui.Javadoc.backgroundColor"));
		assertEquals(Optional.empty(), foreign.get(WB, "org.eclipse.jdt.ui.Javadoc.foregroundColor"));
		assertEquals(Optional.of(atom.get(HYPERLINK_ID)), foreign.get(WB, "HYPERLINK_COLOR"));
		assertFalse(summary.ownedKeys().contains(JAVADOC_BG_ID));
		assertEquals(10 - 2, summary.ownedKeys().size());
	}

	@Test
	public void withJdtPresentTheJdtKeysAndTheDarkModeFlagAreWritten() {
		writer.sync(atom);
		assertEquals(Optional.of("true"), foreign.get(JDT, DARK_FLAG));
		assertEquals(Optional.of(atom.get(JAVADOC_BG_ID)), foreign.get(WB, "org.eclipse.jdt.ui.Javadoc.backgroundColor"));
		assertEquals(Optional.of(atom.get(JDT + "|content_assist_proposals_background")),
				foreign.get(JDT, "content_assist_proposals_background"));
	}

	// ---- scope safety ----------------------------------------------------------------------------------------

	@Test
	public void unrelatedKeysInTheSameNodesAreNeverTouched() {
		foreign.set(WB, "SOME_OTHER_COLOR", "1,1,1");
		foreign.set(JDT, "unrelated", "x");
		writer.sync(atom);
		writer.sync(mocha);
		writer.release();
		assertEquals(Optional.of("1,1,1"), foreign.get(WB, "SOME_OTHER_COLOR"));
		assertEquals(Optional.of("x"), foreign.get(JDT, "unrelated"));
		Set<String> allowed = new HashSet<>();
		PluginColorTable.keys().forEach(key -> allowed.add(key.node() + "|" + key.key()));
		for (String mutation : foreign.mutations) {
			String target = mutation.substring(mutation.indexOf(' ') + 1);
			String nodeKey = target.contains("|") && mutation.startsWith("put")
					? target.substring(0, target.lastIndexOf('|'))
					: target;
			assertTrue(mutation, allowed.contains(nodeKey));
		}
	}

	// ---- failures --------------------------------------------------------------------------------------------

	@Test
	public void aFailedNodeFlushRecordsNothingForThatNodeAndIsReported() {
		foreign.failingFlushes.add(JDT);
		PluginColorSummary summary = writer.sync(atom);
		assertTrue(summary.failed());
		assertTrue(recorded().keySet().stream().noneMatch(id -> id.startsWith(JDT + "|")));
		assertTrue(recorded().containsKey(HYPERLINK_ID));
	}

	@Test
	public void aFailedRecordSaveIsReportedAndLeavesOrphansThatCountAsUserValues() {
		store.failSave = true;
		PluginColorSummary summary = writer.sync(atom);
		assertTrue(summary.failed());
		// The store still has the in-memory record for this session, but a restart (fresh store) sees none:
		PluginColorWriter restarted = new PluginColorWriter(foreign, new TestPreferenceStore());
		PluginColorSummary after = restarted.sync(mocha);
		assertEquals(PluginColorTable.keys().size(), after.userKeys().size());
		restarted.release();
		for (PluginColorKey key : PluginColorTable.keys()) {
			assertEquals(Optional.of(atom.get(key.id())), foreign.get(key.node(), key.key()));
		}
	}

	@Test
	public void aMalformedRecordOwnsNothingAndRemovesNothing() {
		foreign.set(WB, HOVER_BG, "10,20,30");
		store.setValue(PreferenceKeys.PLUGIN_COLORS_OWNED, "garbage without separators");
		assertFalse(writer.hasOwned());
		writer.release();
		assertEquals(Optional.of("10,20,30"), foreign.get(WB, HOVER_BG));
		assertTrue(foreign.mutations.isEmpty());
	}

	@Test
	public void restartReconciliationReadsTheRecordFromTheStoreAndReleasesOnlyOwnedValues() {
		writer.sync(atom);
		foreign.set(WB, HOVER_BG, "7,7,7");
		PluginColorWriter restarted = new PluginColorWriter(foreign, store);
		assertTrue(restarted.hasOwned());
		restarted.release();
		assertEquals(Optional.of("7,7,7"), foreign.get(WB, HOVER_BG));
		assertEquals(Optional.empty(), foreign.get(WB, "HYPERLINK_COLOR"));
	}

	// ---- helpers ---------------------------------------------------------------------------------------------

	private Map<String, String> recorded() {
		return PluginColorRecord.parse(store.getString(PreferenceKeys.PLUGIN_COLORS_OWNED));
	}

	private static Theme theme(String background, String foreground) {
		return build(ThemeType.DARK, background, foreground);
	}

	private static Theme lightTheme(String background, String foreground) {
		return build(ThemeType.LIGHT, background, foreground);
	}

	private static Theme build(ThemeType type, String background, String foreground) {
		Map<TokenKey, Token> tokens = new LinkedHashMap<>();
		tokens.put(TokenKey.BACKGROUND,
				new TokenBuilder().setKey(TokenKey.BACKGROUND).setColor(Color.ofHex(background)).build());
		tokens.put(TokenKey.FOREGROUND,
				new TokenBuilder().setKey(TokenKey.FOREGROUND).setColor(Color.ofHex(foreground)).build());
		return new Theme("id", "name", "", "", "", type, tokens);
	}
}
