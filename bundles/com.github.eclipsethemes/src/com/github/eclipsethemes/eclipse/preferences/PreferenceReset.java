package com.github.eclipsethemes.eclipse.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.AntEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.CppEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.JavaEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.PluginEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.TextEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.ui.DebugConsoleThemeAdapter;
import com.github.eclipsethemes.eclipse.preferences.EditorPreferenceOwnership.Key;
import com.github.eclipsethemes.eclipse.preferences.EditorPreferenceOwnership.Owned;

/**
 * Hands back the editor preference keys this plugin owns, using the ownership journal.
 *
 * <p>
 * A key is restored only while it still holds the value this plugin last wrote. A key changed by the user,
 * Eclipse or another plug-in is left untouched and reported as a conflict. Missing optional plug-ins do not stop
 * recovery: the journal names the preference node directly and does not require the contributing bundle to be
 * installed.
 */
public final class PreferenceReset {

	// Used only to name the integration in diagnostics; recovery is driven by the journal.
	private static final List<EclipseThemeAdapter> ADAPTERS = List.of(new TextEditorThemeAdapter(),
			new JavaEditorThemeAdapter(), new CppEditorThemeAdapter(), new PluginEditorThemeAdapter(),
			new AntEditorThemeAdapter(), new DebugConsoleThemeAdapter());

	private final Function<String, IEclipsePreferences> nodes;
	private final EditorPreferenceOwnership ownership;

	public PreferenceReset(Function<String, IEclipsePreferences> nodes, IPreferenceStore store) {
		this.nodes = nodes;
		this.ownership = new EditorPreferenceOwnership(store);
	}

	public record Failure(String integration, String nodeId, List<String> keys, String operation, Exception cause) {
		public Failure {
			keys = List.copyOf(keys);
		}

		public String description() {
			return integration + " [" + nodeId + "] " + operation + " (keys: " + String.join(", ", keys)
					+ "): " + cause.getMessage();
		}
	}

	/** A newer external value was preserved; this is a warning, not a failed restoration. */
	public record Conflict(String nodeId, String key, String message) {
		public String description() {
			return nodeId + " [" + key + "] " + message;
		}
	}

	public record Result(List<Failure> failures, List<Conflict> conflicts) {
		public Result {
			failures = List.copyOf(failures);
			conflicts = List.copyOf(conflicts);
		}

		public boolean successful() {
			return failures.isEmpty();
		}

		public String describeFailures() {
			return failures.stream().map(Failure::description).collect(Collectors.joining("\n\n"));
		}

		public String describeConflicts() {
			return conflicts.stream().map(Conflict::description).collect(Collectors.joining("\n"));
		}
	}

	/** Run on the caller's thread; preference listeners may require the UI thread. */
	public Result reset() {
		Map<Key, Owned> owned = ownership.read();
		List<Failure> failures = new ArrayList<>();
		List<Conflict> conflicts = new ArrayList<>();
		if (owned.isEmpty()) {
			return new Result(failures, conflicts);
		}

		Map<String, List<Key>> byNode = new LinkedHashMap<>();
		for (Key key : owned.keySet()) {
			byNode.computeIfAbsent(key.nodeId(), unused -> new ArrayList<>()).add(key);
		}

		for (Map.Entry<String, List<Key>> node : byNode.entrySet()) {
			String nodeId = node.getKey();
			String integration = integration(nodeId);
			IEclipsePreferences prefs;
			try {
				prefs = Objects.requireNonNull(nodes.apply(nodeId), "Preference node unavailable");
			} catch (RuntimeException e) {
				failures.add(new Failure(integration, nodeId, keys(node.getValue()), "access node", e));
				continue;
			}

			Map<Key, Owned> journalBeforeNode = new LinkedHashMap<>(owned);
			List<Key> handled = new ArrayList<>();
			boolean mutated = false;

			for (Key key : node.getValue()) {
				Owned entry = owned.get(key);
				if (entry == null) {
					continue;
				}
				PreferenceValue current = PreferenceValue.read(prefs, key.key());
				if (!current.equals(entry.written())) {
					conflicts.add(new Conflict(nodeId, key.key(),
							"changed outside Eclipse Themes; the newer value was kept"));
					handled.add(key);
					continue;
				}
				try {
					entry.original().restore(prefs, key.key());
					handled.add(key);
					mutated = true;
				} catch (RuntimeException e) {
					failures.add(new Failure(integration, nodeId, List.of(key.key()), "restore original", e));
				}
			}

			if (mutated) {
				try {
					prefs.flush();
				} catch (BackingStoreException | RuntimeException e) {
					failures.add(new Failure(integration, nodeId, keys(handled), "persist restoration", e));
					continue;
				}
			}

			for (Key key : handled) {
				owned.remove(key);
			}
			try {
				ownership.save(owned);
			} catch (IOException e) {
				owned = journalBeforeNode;
				try {
					ownership.save(owned);
				} catch (IOException retry) {
					// The original record could not be restored either; the next reset retries safely.
				}
				failures.add(new Failure(integration, nodeId, keys(handled), "save ownership record", e));
			}
		}

		return new Result(failures, conflicts);
	}

	/** Active state and success UI may change only after every node persisted. Conflicts do not block release. */
	public Result resetAndCommit(Runnable onSuccess) {
		Result result = reset();
		if (result.successful()) onSuccess.run();
		return result;
	}

	private static List<String> keys(List<Key> entries) {
		return entries.stream().map(Key::key).toList();
	}

	private static String integration(String nodeId) {
		for (EclipseThemeAdapter adapter : ADAPTERS) {
			if (nodeId.equals(adapter.getPreferencesId())) {
				return adapter.getClass().getSimpleName();
			}
		}
		return "Editor preferences";
	}
}
