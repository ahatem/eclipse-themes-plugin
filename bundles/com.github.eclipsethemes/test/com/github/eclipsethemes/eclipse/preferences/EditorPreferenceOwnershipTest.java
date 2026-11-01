package com.github.eclipsethemes.eclipse.preferences;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Test;

import com.github.eclipsethemes.eclipse.preferences.EditorPreferenceOwnership.Key;
import com.github.eclipsethemes.eclipse.preferences.EditorPreferenceOwnership.Owned;

public class EditorPreferenceOwnershipTest {

	@Test
	public void presentAndAbsentStatesRoundTripIncludingNewlines() throws Exception {
		PreferenceStore store = memoryStore();
		EditorPreferenceOwnership ownership = new EditorPreferenceOwnership(store);

		Map<Key, Owned> entries = new LinkedHashMap<>();
		entries.put(new Key("org.eclipse.ui.editors", "AbstractTextEditor.Color.Background"),
				new Owned(PreferenceValue.present("1,2,3"), PreferenceValue.absent()));
		entries.put(new Key("org.eclipse.jdt.ui", "java_string"),
				new Owned(PreferenceValue.absent(), PreferenceValue.present("line one\nline two")));
		ownership.save(entries);

		Map<Key, Owned> read = ownership.read();
		assertEquals(entries, read);
		assertEquals(PreferenceValue.absent(),
				read.get(new Key("org.eclipse.ui.editors", "AbstractTextEditor.Color.Background")).written());
		assertEquals("line one\nline two",
				read.get(new Key("org.eclipse.jdt.ui", "java_string")).written().value().orElseThrow());
	}

	@Test
	public void emptyRecordRemovesThePreferenceAndCorruptLinesAreIgnored() throws Exception {
		PreferenceStore store = memoryStore();
		EditorPreferenceOwnership ownership = new EditorPreferenceOwnership(store);
		ownership.save(Map.of());
		assertFalse(ownership.ownsAnything());

		store.setValue(PreferenceKeys.EDITOR_PREFERENCES_OWNED,
				"not-a-record\nnode|key|broken|record\ngood|key|A|VYmx1ZQ");
		Map<Key, Owned> read = ownership.read();
		assertEquals(1, read.size());
		assertEquals("blue", read.get(new Key("good", "key")).written().value().orElseThrow());
		assertEquals(PreferenceValue.absent(), read.get(new Key("good", "key")).original());
	}

	private static PreferenceStore memoryStore() {
		return new PreferenceStore() {
			@Override
			public void save() {
				// in memory only
			}
		};
	}
}
