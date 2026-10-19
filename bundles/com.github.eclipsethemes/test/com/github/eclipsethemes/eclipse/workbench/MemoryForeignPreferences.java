package com.github.eclipsethemes.eclipse.workbench;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory stand-in for other plug-ins' instance preferences. Records every mutation so tests can assert that only
 * named keys were touched, can simulate the color registry mirror, a missing bundle and a failing flush.
 */
public final class MemoryForeignPreferences implements ForeignPreferences {

	/** node to (key to value); the persisted-or-not distinction is not modeled. */
	public final Map<String, Map<String, String>> nodes = new LinkedHashMap<>();
	/** Every put and remove, as {@code put node|key|value} or {@code remove node|key}. */
	public final List<String> mutations = new ArrayList<>();
	public final Set<String> missingBundles = new HashSet<>();
	public final Set<String> failingFlushes = new HashSet<>();
	/** Values for which a put is mirrored away again, like the color registry does for a default value. */
	public final Map<String, String> mirroredDefaults = new HashMap<>();

	@Override
	public boolean installed(String bundle) {
		return !missingBundles.contains(bundle);
	}

	@Override
	public Optional<String> get(String node, String key) {
		return Optional.ofNullable(nodes.getOrDefault(node, Map.of()).get(key));
	}

	@Override
	public void put(String node, String key, String value) {
		mutations.add("put " + node + "|" + key + "|" + value);
		nodes.computeIfAbsent(node, n -> new LinkedHashMap<>()).put(key, value);
		if (value.equals(mirroredDefaults.get(node + "|" + key))) {
			nodes.get(node).remove(key);
		}
	}

	@Override
	public void remove(String node, String key) {
		mutations.add("remove " + node + "|" + key);
		nodes.getOrDefault(node, new HashMap<>()).remove(key);
	}

	@Override
	public void flush(String node) throws IOException {
		if (failingFlushes.contains(node)) {
			throw new IOException("flush failed for " + node);
		}
	}

	/** A user (or another plug-in) sets a value, outside this plugin. */
	public void set(String node, String key, String value) {
		nodes.computeIfAbsent(node, n -> new LinkedHashMap<>()).put(key, value);
	}

	public void unset(String node, String key) {
		nodes.getOrDefault(node, new HashMap<>()).remove(key);
	}
}
