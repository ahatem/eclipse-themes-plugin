package com.github.eclipsethemes.eclipse.preferences;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Value-exact ownership of the editor preference keys the built-in adapters declare.
 *
 * <p>
 * Each entry records two states: what the key held immediately before this plugin first took it, and what the
 * plugin wrote most recently. The original state survives theme switches. A key changed by anyone else is never
 * overwritten on the next apply or reset; it is released instead.
 *
 * <p>
 * Values are Base64 encoded so arbitrary preference text, including newlines, can be stored in one line.
 */
public final class EditorPreferenceOwnership {

	private static final String ABSENT = "A";
	private static final String VALUE = "V";

	/** One editor preference key. */
	public record Key(String nodeId, String key) {
		public Key {
			Objects.requireNonNull(nodeId, "nodeId");
			Objects.requireNonNull(key, "key");
			if (nodeId.indexOf('|') >= 0 || nodeId.indexOf('\n') >= 0
					|| key.indexOf('|') >= 0 || key.indexOf('\n') >= 0) {
				throw new IllegalArgumentException("Preference node and key must not contain '|' or a newline");
			}
		}

	}

	/** The state before ownership began and the state this plugin last left behind. */
	public record Owned(PreferenceValue original, PreferenceValue written) {
		public Owned {
			Objects.requireNonNull(original, "original");
			Objects.requireNonNull(written, "written");
		}
	}

	private final IPreferenceStore store;

	public EditorPreferenceOwnership(IPreferenceStore store) {
		this.store = Objects.requireNonNull(store, "store");
	}

	public Map<Key, Owned> read() {
		Map<Key, Owned> owned = new LinkedHashMap<>();
		String text = store.getString(PreferenceKeys.EDITOR_PREFERENCES_OWNED);
		for (String line : text.split("\n")) {
			if (line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\\|", -1);
			if (parts.length != 4) {
				continue;
			}
			try {
				PreferenceValue original = decode(parts[2]);
				PreferenceValue written = decode(parts[3]);
				if (original == null || written == null) {
					continue;
				}
				owned.put(new Key(parts[0], parts[1]), new Owned(original, written));
			} catch (IllegalArgumentException e) {
				// A corrupt record must not stop theme management. The affected key is treated as unowned.
			}
		}
		return owned;
	}

	public boolean ownsAnything() {
		return !read().isEmpty();
	}

	/** Saves the whole record; an empty map removes the preference. */
	public void save(Map<Key, Owned> owned) throws IOException {
		if (owned.isEmpty()) {
			store.setToDefault(PreferenceKeys.EDITOR_PREFERENCES_OWNED);
		} else {
			StringBuilder text = new StringBuilder();
			for (Map.Entry<Key, Owned> entry : owned.entrySet()) {
				if (text.length() > 0) {
					text.append('\n');
				}
				text.append(entry.getKey().nodeId()).append('|').append(entry.getKey().key()).append('|')
						.append(encode(entry.getValue().original())).append('|')
						.append(encode(entry.getValue().written()));
			}
			store.setValue(PreferenceKeys.EDITOR_PREFERENCES_OWNED, text.toString());
		}
		if (store instanceof IPersistentPreferenceStore persistent && persistent.needsSaving()) {
			persistent.save();
		}
	}

	private static String encode(PreferenceValue value) {
		if (value.value().isEmpty()) {
			return ABSENT;
		}
		return VALUE + Base64.getUrlEncoder().withoutPadding()
				.encodeToString(value.value().get().getBytes(StandardCharsets.UTF_8));
	}

	private static PreferenceValue decode(String encoded) {
		if (ABSENT.equals(encoded)) {
			return PreferenceValue.absent();
		}
		if (encoded.startsWith(VALUE)) {
			byte[] bytes = Base64.getUrlDecoder().decode(encoded.substring(1));
			return PreferenceValue.present(new String(bytes, StandardCharsets.UTF_8));
		}
		return null;
	}
}
