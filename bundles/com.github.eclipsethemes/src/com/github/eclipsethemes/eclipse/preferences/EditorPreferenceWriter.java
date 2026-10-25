package com.github.eclipsethemes.eclipse.preferences;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome.EditorConflict;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome.EditorOutcome;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.preferences.EditorPreferenceOwnership.Key;
import com.github.eclipsethemes.eclipse.preferences.EditorPreferenceOwnership.Owned;

/**
 * Applies the built-in editor adapters through the ownership journal.
 *
 * <p>
 * The journal is read before the adapter runs. On a first write the key's current state is recorded as the
 * original. On a later write the current state must still equal what this plugin last wrote; otherwise the newer
 * value is kept, ownership is released and a conflict is reported. The journal is saved only after the adapter's
 * preference node has persisted successfully. A failed adapter or record save restores the keys that adapter
 * touched, so ownership is never claimed for a write that did not reach a trustworthy state.
 */
public final class EditorPreferenceWriter {

	private final Map<EclipseThemeAdapter, String> adapters;
	private final Function<String, IEclipsePreferences> nodes;
	private final EditorPreferenceOwnership ownership;
	private final ILog log;

	public EditorPreferenceWriter(Map<EclipseThemeAdapter, String> adapters,
			Function<String, IEclipsePreferences> nodes, EditorPreferenceOwnership ownership, ILog log) {
		this.adapters = new LinkedHashMap<>(adapters);
		this.nodes = nodes;
		this.ownership = ownership;
		this.log = log;
	}

	public EditorOutcome write(Theme theme) {
		Map<Key, Owned> journal = ownership.read();
		List<String> failed = new ArrayList<>();
		List<EditorConflict> conflicts = new ArrayList<>();

		for (Map.Entry<EclipseThemeAdapter, String> entry : adapters.entrySet()) {
			EclipseThemeAdapter adapter = entry.getKey();
			String plugin = entry.getValue();
			String integration = adapter.getClass().getSimpleName();

			if (plugin != null && Platform.getBundle(plugin) == null) {
				continue;
			}

			String nodeId = adapter.getPreferencesId();
			Set<String> keys = adapter.getManagedKeys();
			if (nodeId == null || keys.isEmpty()) {
				continue;
			}

			IEclipsePreferences prefs;
			try {
				prefs = nodes.apply(nodeId);
			} catch (RuntimeException e) {
				failed.add(integration);
				log.error("Could not access editor preferences for " + integration + " [" + nodeId + "]", e);
				continue;
			}
			if (prefs == null) {
				failed.add(integration);
				log.error("Editor preference node unavailable for " + integration + " [" + nodeId + "]");
				continue;
			}

			Map<Key, Owned> journalBeforeNode = new LinkedHashMap<>(journal);
			Map<String, PreferenceValue> before = new HashMap<>();
			Set<String> skipped = new LinkedHashSet<>();

			for (String key : keys) {
				Key id = new Key(nodeId, key);
				PreferenceValue current = PreferenceValue.read(prefs, key);
				Owned owned = journal.get(id);
				if (owned != null && !current.equals(owned.written())) {
					conflicts.add(new EditorConflict(integration, nodeId, key,
							"changed outside Eclipse Themes; the newer value was kept"));
					journal.remove(id);
					skipped.add(key);
					continue;
				}
				before.put(key, current);
			}

			Guarded guarded = new Guarded(prefs, skipped);
			try {
				adapter.apply(theme, guarded.proxy());
			} catch (BackingStoreException | RuntimeException | LinkageError e) {
				rollback(prefs, guarded.mutated(), before, integration, nodeId);
				failed.add(integration);
				log.error("Could not apply theme via adapter " + integration, e);
				continue;
			}

			for (String key : keys) {
				if (skipped.contains(key)) {
					continue;
				}
				Key id = new Key(nodeId, key);
				PreferenceValue after = PreferenceValue.read(prefs, key);
				Owned existing = journal.get(id);
				if (existing == null) {
					PreferenceValue original = before.get(key);
					if (!after.equals(original)) {
						journal.put(id, new Owned(original, after));
					}
				} else if (!after.equals(existing.written())) {
					journal.put(id, new Owned(existing.original(), after));
				}
			}

			if (!journal.equals(journalBeforeNode)) {
				try {
					ownership.save(journal);
				} catch (IOException e) {
					rollback(prefs, guarded.mutated(), before, integration, nodeId);
					journal = journalBeforeNode;
					try {
						ownership.save(journal);
					} catch (IOException retry) {
						log.error("Could not restore editor preference ownership for " + integration, retry);
					}
					failed.add(integration);
					log.error("Could not save editor preference ownership for " + integration, e);
				}
			}
		}

		return new EditorOutcome(failed, conflicts);
	}

	private void rollback(IEclipsePreferences prefs, Set<String> mutated, Map<String, PreferenceValue> before,
			String integration, String nodeId) {
		if (mutated.isEmpty()) {
			return;
		}
		boolean changed = false;
		for (String key : mutated) {
			PreferenceValue value = before.get(key);
			if (value == null) {
				continue;
			}
			try {
				value.restore(prefs, key);
				changed = true;
			} catch (RuntimeException e) {
				log.error("Could not restore editor preference " + key + " [" + nodeId + "]", e);
			}
		}
		if (!changed) {
			return;
		}
		try {
			prefs.flush();
		} catch (BackingStoreException | RuntimeException e) {
			log.error("Could not persist editor preference rollback for " + integration + " [" + nodeId + "]", e);
		}
	}

	/** Delegates everything except writes to the conflicting keys. */
	private static final class Guarded {

		private final Set<String> mutated = new LinkedHashSet<>();
		private final IEclipsePreferences proxy;

		private Guarded(IEclipsePreferences delegate, Set<String> skipped) {
			this.proxy = (IEclipsePreferences) Proxy.newProxyInstance(IEclipsePreferences.class.getClassLoader(),
					new Class<?>[] { IEclipsePreferences.class }, (ignored, method, args) -> {
						String name = method.getName();
						if (args != null && args.length > 0 && args[0] instanceof String key
								&& (name.startsWith("put") || name.equals("remove"))) {
							if (skipped.contains(key)) {
								return null;
							}
							mutated.add(key);
						}
						try {
							return method.invoke(delegate, args);
						} catch (InvocationTargetException e) {
							throw e.getCause();
						}
					});
		}

		private IEclipsePreferences proxy() {
			return proxy;
		}

		private Set<String> mutated() {
			return new HashSet<>(mutated);
		}
	}
}
