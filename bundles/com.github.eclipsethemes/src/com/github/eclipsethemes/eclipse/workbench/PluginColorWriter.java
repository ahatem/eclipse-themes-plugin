package com.github.eclipsethemes.eclipse.workbench;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;

import com.github.eclipsethemes.core.workbench.PluginColorKey;
import com.github.eclipsethemes.core.workbench.PluginColorRecord;
import com.github.eclipsethemes.core.workbench.PluginColorTable;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;

/**
 * Writes the keys of {@link PluginColorTable} into other plug-ins' instance preferences under a conflict-aware
 * ownership model. Call on the UI thread, after the workbench theme switch.
 *
 * <ul>
 * <li>A key with a value before the plugin first writes it belongs to the user and is never written.</li>
 * <li>An absent key may be taken: the exact written value is recorded.</li>
 * <li>An owned key is updated or removed only while its stored value still equals the recorded one; any other
 * value (changed, or removed by someone else) releases ownership and is left as found.</li>
 * <li>Only {@code put}, {@code remove} and {@code flush} on named table keys are used: no node is cleared and no
 * default is guessed.</li>
 * </ul>
 *
 * <p>
 * Order: foreign nodes are flushed first, then the record is saved. When a flush fails, that node's record entries
 * are not changed. When the record cannot be saved, written values stay without an owner and are treated as user
 * values from then on: nothing a user owns is ever removed, only an orphan can stay.
 */
public final class PluginColorWriter {

	private final ForeignPreferences foreign;
	private final IPreferenceStore store;
	private PluginColorSummary last = PluginColorSummary.EMPTY;

	public PluginColorWriter(ForeignPreferences foreign, IPreferenceStore store) {
		this.foreign = foreign;
		this.store = store;
	}

	public static PluginColorWriter create(IPreferenceStore store) {
		return new PluginColorWriter(new InstancePreferences(), store);
	}

	/** True when the record says at least one key is owned. */
	public boolean hasOwned() {
		return !PluginColorRecord.parse(store.getString(PreferenceKeys.PLUGIN_COLORS_OWNED)).isEmpty();
	}

	/** The keys the record says are owned, as {@code node|key} ids in table order. */
	public List<String> ownedKeys() {
		Map<String, String> record = PluginColorRecord.parse(store.getString(PreferenceKeys.PLUGIN_COLORS_OWNED));
		List<String> owned = new ArrayList<>();
		for (PluginColorKey key : PluginColorTable.keys()) {
			if (record.containsKey(key.id())) {
				owned.add(key.id());
			}
		}
		return owned;
	}

	/** The result of the most recent {@link #sync}; empty before the first one in this session. */
	public PluginColorSummary lastSummary() {
		return last;
	}

	/** Releases every key whose stored value is still the plugin's own. */
	public PluginColorSummary release() {
		return sync(Map.of());
	}

	/**
	 * Brings the foreign keys in line with {@code desired} (key id to value); a key missing from the map, and an empty
	 * map as a whole, means "give this key back".
	 */
	public PluginColorSummary sync(Map<String, String> desired) {
		Map<String, String> record = PluginColorRecord.parse(store.getString(PreferenceKeys.PLUGIN_COLORS_OWNED));
		Map<String, String> next = new LinkedHashMap<>(record);
		Map<String, Map<String, String>> staged = new LinkedHashMap<>();
		List<String> userKeys = new ArrayList<>();
		List<String> releasedKeys = new ArrayList<>();
		List<String> failures = new ArrayList<>();

		for (PluginColorKey key : PluginColorTable.keys()) {
			String id = key.id();
			Optional<String> current = foreign.get(key.node(), key.key());
			String recorded = record.get(id);
			boolean installed = key.requiredBundle() == null || foreign.installed(key.requiredBundle());
			String wanted = installed ? desired.get(id) : null;

			if (recorded != null && !current.equals(Optional.of(recorded))) {
				next.remove(id);
				releasedKeys.add(id);
				recorded = null;
			}
			if (recorded != null) {
				if (wanted == null) {
					foreign.remove(key.node(), key.key());
					stage(staged, key, null);
				} else if (!wanted.equals(recorded)) {
					foreign.put(key.node(), key.key(), wanted);
					stage(staged, key, foreign.get(key.node(), key.key()).filter(wanted::equals).orElse(null));
				}
			} else if (wanted != null) {
				if (current.isPresent()) {
					userKeys.add(id);
				} else {
					foreign.put(key.node(), key.key(), wanted);
					// The color registry mirrors a put back; a value equal to the default is mirrored away again, so
					// ownership is claimed only for a value that really stays.
					Optional<String> stored = foreign.get(key.node(), key.key());
					if (stored.filter(wanted::equals).isPresent()) {
						stage(staged, key, wanted);
					}
				}
			}
		}

		for (Map.Entry<String, Map<String, String>> node : staged.entrySet()) {
			try {
				foreign.flush(node.getKey());
				node.getValue().forEach((id, value) -> {
					if (value == null) {
						next.remove(id);
					} else {
						next.put(id, value);
					}
				});
			} catch (IOException e) {
				failures.add(e.getMessage());
			}
		}

		saveRecord(next, failures);
		List<String> owned = new ArrayList<>();
		for (PluginColorKey key : PluginColorTable.keys()) {
			if (next.containsKey(key.id())) {
				owned.add(key.id());
			}
		}
		last = new PluginColorSummary(owned, userKeys, releasedKeys, failures);
		return last;
	}

	private static void stage(Map<String, Map<String, String>> staged, PluginColorKey key, String recordValue) {
		staged.computeIfAbsent(key.node(), n -> new LinkedHashMap<>()).put(key.id(), recordValue);
	}

	private void saveRecord(Map<String, String> next, List<String> failures) {
		if (next.isEmpty()) {
			store.setToDefault(PreferenceKeys.PLUGIN_COLORS_OWNED);
		} else {
			store.setValue(PreferenceKeys.PLUGIN_COLORS_OWNED, PluginColorRecord.format(next));
		}
		if (store instanceof IPersistentPreferenceStore persistent && persistent.needsSaving()) {
			try {
				persistent.save();
			} catch (IOException e) {
				failures.add("The ownership record could not be saved: " + e.getMessage());
			}
		}
	}
}
