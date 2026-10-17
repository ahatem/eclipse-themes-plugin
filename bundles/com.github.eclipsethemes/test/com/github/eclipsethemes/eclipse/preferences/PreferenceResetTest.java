package com.github.eclipsethemes.eclipse.preferences;

import static com.github.eclipsethemes.eclipse.preferences.AdapterTestSupport.adapters;
import static com.github.eclipsethemes.eclipse.preferences.AdapterTestSupport.theme;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public class PreferenceResetTest {
	@Test
	public void hoverAndOptionalEnableOverridesResetWithContextualFailures() throws Exception {
		for (String key : List.of("sourceHoverBackgroundColor", "sourceHoverBackgroundColor.SystemDefault",
				"semanticHighlighting.inheritedMethodInvocation.enabled", "semanticHighlighting.inheritedField.enabled",
				"semanticHighlighting.typeArgument.enabled", "semanticHighlighting.deprecatedMember.enabled")) {
			for (boolean failRemoval : List.of(false, true)) {
				for (boolean failFlush : List.of(false, true)) {
					Map<String, MemoryPreferences> nodes = seededNodes();
					MemoryPreferences jdt = nodes.get("org.eclipse.jdt.ui");
					assertTrue("Managed " + key, jdt.values.containsKey(key));
					jdt.failingKey = failRemoval ? key : null;
					jdt.failFlush = failFlush;
					AtomicInteger commits = new AtomicInteger();
					PreferenceReset reset = new PreferenceReset(id -> nodes.get(id).node);
					for (int run = 0; run < 2; run++) {
						PreferenceReset.Result result = reset.resetAndCommit(commits::incrementAndGet);
						assertEquals(!failRemoval && !failFlush, result.successful());
						if (!result.successful()) {
							assertTrue(result.describeFailures().contains("JavaEditorThemeAdapter"));
							assertTrue(result.describeFailures().contains("org.eclipse.jdt.ui"));
							assertTrue(result.describeFailures().contains(key));
						}
						assertEquals(failRemoval ? "legacy override" : null, jdt.values.get(key));
						assertEquals("7", jdt.values.get("editor.tab.width"));
						assertEquals(0, jdt.clears);
					}
					assertEquals(!failRemoval && !failFlush ? 2 : 0, commits.get());
				}
			}
		}
	}

	@Test
	public void correctedKeysResetWithoutGuessingHistoricalIndications() {
		for (String indication : java.util.Arrays.asList("true", "false", null, "1,2,3")) {
			for (String failingKey : java.util.Arrays.asList(null, "currentIPColor", "secondaryIPColor")) {
				for (boolean failFlush : List.of(false, true)) {
					Map<String, MemoryPreferences> nodes = seededNodes();
					MemoryPreferences editor = nodes.get("org.eclipse.ui.editors");
					if (indication != null) {
						editor.values.put("currentIPIndication", indication);
						editor.values.put("secondaryIPIndication", indication);
					}
					editor.failingKey = failingKey;
					editor.failFlush = failFlush;
					AtomicInteger commits = new AtomicInteger();
					PreferenceReset reset = new PreferenceReset(id -> nodes.get(id).node);
					for (int run = 0; run < 2; run++) {
						PreferenceReset.Result result = reset.resetAndCommit(commits::incrementAndGet);
						assertEquals(failingKey == null && !failFlush, result.successful());
						if (failingKey != null) assertTrue(result.describeFailures().contains(failingKey));
						if (failFlush) assertTrue(result.describeFailures().contains("persist removals"));
						assertEquals(indication, editor.values.get("currentIPIndication"));
						assertEquals(indication, editor.values.get("secondaryIPIndication"));
						for (String key : List.of("currentIPColor", "secondaryIPColor")) {
							assertEquals(key.equals(failingKey) ? "legacy override" : null, editor.values.get(key));
						}
						assertEquals("7", editor.values.get("editor.tab.width"));
						assertEquals(0, editor.clears);
					}
					assertEquals(failingKey == null && !failFlush ? 2 : 0, commits.get());
				}
			}
		}
	}

	private Map<String, MemoryPreferences> seededNodes() {
		Map<String, MemoryPreferences> nodes = new LinkedHashMap<>();
		for (EclipseThemeAdapter adapter : adapters()) {
			MemoryPreferences prefs = new MemoryPreferences();
			adapter.getManagedKeys().forEach(key -> prefs.values.put(key, "legacy override"));
			prefs.values.put("unrelated.preference", "keep\n=☃");
			prefs.values.put("editor.tab.width", "7");
			prefs.values.put("semanticHighlighting.unmapped.color", "1,2,3");
			nodes.put(adapter.getPreferencesId(), prefs);
		}
		return nodes;
	}

	@Test
	public void resetPreservesMultipleUnrelatedValuesInEveryNode() {
		Map<String, MemoryPreferences> nodes = seededNodes();
		AtomicInteger commits = new AtomicInteger();
		PreferenceReset.Result result = new PreferenceReset(id -> nodes.get(id).node).resetAndCommit(commits::incrementAndGet);
		assertTrue(result.describeFailures(), result.successful());
		assertEquals(1, commits.get());
		for (MemoryPreferences prefs : nodes.values()) {
			assertEquals(Map.of("unrelated.preference", "keep\n=☃", "editor.tab.width", "7",
					"semanticHighlighting.unmapped.color", "1,2,3"), prefs.values);
			assertEquals(0, prefs.clears);
			assertEquals(1, prefs.flushes);
			assertTrue(prefs.mutations.stream().allMatch(mutation -> mutation.startsWith("remove ")));
		}
	}

	@Test
	public void ownershipExactlyMatchesExistingFullAndSparseAdapterMutations() throws Exception {
		for (EclipseThemeAdapter adapter : adapters()) {
			Set<String> observed = new LinkedHashSet<>();
			for (Theme theme : List.of(theme(ThemeType.DARK, true),
					theme(ThemeType.DARK, false), theme(ThemeType.LIGHT, false))) {
				MemoryPreferences prefs = new MemoryPreferences();
				adapter.apply(theme, prefs.node);
				prefs.mutations.stream().map(mutation -> mutation.substring(mutation.indexOf(' ') + 1).split("=", 2)[0])
						.forEach(observed::add);
			}
			assertEquals(adapter.getPreferencesId(), observed, adapter.getManagedKeys());
		}
	}

	@Test
	public void absentManagedKeysStayAbsentAndResetIsRepeatable() {
		Map<String, MemoryPreferences> nodes = seededNodes();
		nodes.values().forEach(prefs -> prefs.values.clear());
		PreferenceReset reset = new PreferenceReset(id -> nodes.get(id).node);
		assertTrue(reset.reset().successful());
		assertTrue(reset.reset().successful());
		nodes.values().forEach(prefs -> {
			assertTrue(prefs.values.isEmpty());
			assertEquals(0, prefs.clears);
		});
	}

	@Test
	public void removalFailureIsContextualAndDoesNotCommitSuccessOrClearNodes() {
		Map<String, MemoryPreferences> nodes = seededNodes();
		MemoryPreferences failing = nodes.get("org.eclipse.jdt.ui");
		failing.failingKey = "java_default";
		AtomicInteger commits = new AtomicInteger();
		PreferenceReset.Result result = new PreferenceReset(id -> nodes.get(id).node).resetAndCommit(commits::incrementAndGet);
		assertFalse(result.successful());
		assertEquals(0, commits.get());
		assertEquals(1, result.failures().size());
		assertTrue(result.describeFailures().contains("JavaEditorThemeAdapter"));
		assertTrue(result.describeFailures().contains("org.eclipse.jdt.ui"));
		assertTrue(result.describeFailures().contains("java_default"));
		assertEquals("legacy override", failing.values.get("java_default"));
		assertNull(failing.values.get("java_string"));
		assertEquals("7", failing.values.get("editor.tab.width"));
		nodes.values().forEach(prefs -> assertEquals(0, prefs.clears));
	}

	@Test
	public void flushFailureReportsAllUncertainKeysAndOtherNodesContinue() {
		Map<String, MemoryPreferences> nodes = seededNodes();
		nodes.get("org.eclipse.ui.editors").failFlush = true;
		nodes.get("org.eclipse.ant.ui").failFlush = true;
		AtomicInteger commits = new AtomicInteger();
		PreferenceReset.Result result = new PreferenceReset(id -> nodes.get(id).node).resetAndCommit(commits::incrementAndGet);
		assertFalse(result.successful());
		assertEquals(0, commits.get());
		assertEquals(2, result.failures().size());
		assertTrue(result.describeFailures().contains("AbstractTextEditor.Color.Background.SystemDefault"));
		assertTrue(result.describeFailures().contains("org.eclipse.ant.ui.tagsColor"));
		nodes.values().forEach(prefs -> {
			assertEquals(1, prefs.flushes);
			assertEquals(0, prefs.clears);
			assertEquals(3, prefs.values.size());
		});
	}

	@Test
	public void nodeAccessFailureIsReportedAndOtherNodesContinue() {
		Map<String, MemoryPreferences> nodes = seededNodes();
		PreferenceReset.Result result = new PreferenceReset(id -> {
			if (id.equals("org.eclipse.cdt.ui")) throw new IllegalStateException("Node unavailable");
			return nodes.get(id).node;
		}).reset();
		assertFalse(result.successful());
		assertEquals(1, result.failures().size());
		assertTrue(result.describeFailures().contains("CppEditorThemeAdapter"));
		assertTrue(result.describeFailures().contains("org.eclipse.cdt.ui"));
		assertEquals(3, nodes.get("org.eclipse.debug.ui").values.size());
	}

	@Test
	public void recordedAbsenceAndExplicitValuesRemainDistinct() {
		MemoryPreferences prefs = new MemoryPreferences();
		prefs.values.put("key", "theme value");
		PreferenceValue.absent().restore(prefs.node, "key");
		assertFalse(prefs.values.containsKey("key"));
		PreferenceValue.present("").restore(prefs.node, "key");
		assertTrue(prefs.values.containsKey("key"));
		assertEquals("", prefs.values.get("key"));
		PreferenceValue.present("original value").restore(prefs.node, "key");
		assertEquals("original value", prefs.values.get("key"));
		assertEquals(0, prefs.clears);
	}
}
