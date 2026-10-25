package com.github.eclipsethemes.eclipse.preferences;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Test;

import com.github.eclipsethemes.eclipse.preferences.EditorPreferenceOwnership.Key;
import com.github.eclipsethemes.eclipse.preferences.EditorPreferenceOwnership.Owned;

public class PreferenceResetTest {

	private static final String EDITORS = "org.eclipse.ui.editors";
	private static final String JDT = "org.eclipse.jdt.ui";
	private static final String CDT = "org.eclipse.cdt.ui";

	@Test
	public void restoresExplicitOriginalAndAbsenceWithoutTouchingUnrelatedValues() throws Exception {
		Map<String, MemoryPreferences> nodes = nodes();
		MemoryPreferences editors = nodes.get(EDITORS);
		editors.values.put("AbstractTextEditor.Color.Background", "10,20,30");
		editors.values.put("currentLineColor", "40,50,60");
		editors.values.put("editor.tab.width", "7");
		editors.values.put("unrelated", "keep");
		PreferenceStore store = journal(EDITORS, "AbstractTextEditor.Color.Background", "1,2,3", "10,20,30");
		own(store, EDITORS, "currentLineColor", null, "40,50,60");

		PreferenceReset.Result result = new PreferenceReset(id -> nodes.get(id).node, store).reset();

		assertTrue(result.describeFailures(), result.successful());
		assertTrue(result.conflicts().isEmpty());
		assertEquals("1,2,3", editors.values.get("AbstractTextEditor.Color.Background"));
		assertFalse(editors.values.containsKey("currentLineColor"));
		assertEquals("7", editors.values.get("editor.tab.width"));
		assertEquals("keep", editors.values.get("unrelated"));
		assertEquals(0, editors.clears);
		assertTrue(new EditorPreferenceOwnership(store).read().isEmpty());
	}

	@Test
	public void conflictPreservesTheNewerValueAndStillReleasesTheRecord() throws Exception {
		Map<String, MemoryPreferences> nodes = nodes();
		MemoryPreferences editors = nodes.get(EDITORS);
		editors.values.put("AbstractTextEditor.Color.Background", "user,changed");
		PreferenceStore store = journal(EDITORS, "AbstractTextEditor.Color.Background", "original", "theme");
		AtomicInteger commits = new AtomicInteger();

		PreferenceReset.Result result = new PreferenceReset(id -> nodes.get(id).node, store)
				.resetAndCommit(commits::incrementAndGet);

		assertTrue(result.describeFailures(), result.successful());
		assertEquals(1, result.conflicts().size());
		assertTrue(result.describeConflicts().contains("AbstractTextEditor.Color.Background"));
		assertEquals("user,changed", editors.values.get("AbstractTextEditor.Color.Background"));
		assertEquals(1, commits.get());
		assertTrue(new EditorPreferenceOwnership(store).read().isEmpty());
	}

	@Test
	public void flushFailureKeepsTheRecoveryRecordAndDoesNotCommit() throws Exception {
		Map<String, MemoryPreferences> nodes = nodes();
		MemoryPreferences editors = nodes.get(EDITORS);
		editors.values.put("AbstractTextEditor.Color.Background", "theme");
		editors.failFlush = true;
		PreferenceStore store = journal(EDITORS, "AbstractTextEditor.Color.Background", "original", "theme");
		AtomicInteger commits = new AtomicInteger();

		PreferenceReset.Result result = new PreferenceReset(id -> nodes.get(id).node, store)
				.resetAndCommit(commits::incrementAndGet);

		assertFalse(result.successful());
		assertEquals(0, commits.get());
		assertTrue(result.describeFailures().contains("persist restoration"));
		assertTrue(new EditorPreferenceOwnership(store).read()
				.containsKey(new Key(EDITORS, "AbstractTextEditor.Color.Background")));
	}

	@Test
	public void nodeFailureIsContextualAndOtherNodesStillRecover() throws Exception {
		Map<String, MemoryPreferences> nodes = nodes();
		nodes.get(JDT).values.put("java_string", "theme");
		nodes.get(CDT).values.put("c_string", "theme");
		PreferenceStore store = journal(CDT, "c_string", "cdt-original", "theme");
		own(store, JDT, "java_string", "jdt-original", "theme");

		PreferenceReset.Result result = new PreferenceReset(id -> {
			if (id.equals(CDT)) throw new IllegalStateException("Node unavailable");
			return nodes.get(id).node;
		}, store).reset();

		assertFalse(result.successful());
		assertEquals(1, result.failures().size());
		assertTrue(result.describeFailures().contains("CppEditorThemeAdapter"));
		assertTrue(result.describeFailures().contains(CDT));
		assertEquals("jdt-original", nodes.get(JDT).values.get("java_string"));
		assertEquals("theme", nodes.get(CDT).values.get("c_string"));
		assertFalse(new EditorPreferenceOwnership(store).read().containsKey(new Key(JDT, "java_string")));
		assertTrue(new EditorPreferenceOwnership(store).read().containsKey(new Key(CDT, "c_string")));
	}

	@Test
	public void anOptionalPluginDoesNotNeedToBeInstalledToRecoverItsNode() throws Exception {
		Map<String, MemoryPreferences> nodes = nodes();
		nodes.get(CDT).values.put("c_string", "theme");
		PreferenceStore store = journal(CDT, "c_string", "cdt-original", "theme");

		PreferenceReset.Result result = new PreferenceReset(id -> nodes.get(id).node, store).reset();

		assertTrue(result.describeFailures(), result.successful());
		assertEquals("cdt-original", nodes.get(CDT).values.get("c_string"));
		assertTrue(new EditorPreferenceOwnership(store).read().isEmpty());
	}

	@Test
	public void absentOriginalRemovesThePluginValueAndIsRepeatable() throws Exception {
		Map<String, MemoryPreferences> nodes = nodes();
		MemoryPreferences editors = nodes.get(EDITORS);
		editors.values.put("AbstractTextEditor.Color.Background", "theme");
		PreferenceStore store = journal(EDITORS, "AbstractTextEditor.Color.Background", null, "theme");
		PreferenceReset reset = new PreferenceReset(id -> nodes.get(id).node, store);

		assertTrue(reset.reset().successful());
		assertTrue(reset.reset().successful());
		assertFalse(editors.values.containsKey("AbstractTextEditor.Color.Background"));
		assertEquals(0, editors.clears);
	}

	private static Map<String, MemoryPreferences> nodes() {
		Map<String, MemoryPreferences> nodes = new LinkedHashMap<>();
		for (String id : new String[] { EDITORS, JDT, CDT, "org.eclipse.pde.ui", "org.eclipse.ant.ui",
				"org.eclipse.debug.ui" }) {
			nodes.put(id, new MemoryPreferences());
		}
		return nodes;
	}

	private static PreferenceStore journal(String node, String key, String original, String written) throws Exception {
		PreferenceStore store = memoryStore();
		own(store, node, key, original, written);
		return store;
	}

	private static PreferenceStore memoryStore() {
		return new PreferenceStore() {
			@Override
			public void save() {
				// in memory only
			}
		};
	}

	private static void own(PreferenceStore store, String node, String key, String original, String written)
			throws Exception {
		EditorPreferenceOwnership ownership = new EditorPreferenceOwnership(store);
		Map<Key, Owned> entries = new LinkedHashMap<>(ownership.read());
		entries.put(new Key(node, key), new Owned(
				original == null ? PreferenceValue.absent() : PreferenceValue.present(original),
				written == null ? PreferenceValue.absent() : PreferenceValue.present(written)));
		ownership.save(entries);
	}
}
