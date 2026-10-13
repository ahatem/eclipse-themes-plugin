package com.github.eclipsethemes.eclipse.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.AntEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.CppEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.JavaEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.PluginEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.TextEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.ui.DebugConsoleThemeAdapter;

/** Legacy cleanup: remove only declared built-in instance overrides, never guess originals. */
public final class PreferenceReset {

	// Include previously themed optional integrations even when they are now uninstalled.
	private static final List<EclipseThemeAdapter> ADAPTERS = List.of(new TextEditorThemeAdapter(),
			new JavaEditorThemeAdapter(), new CppEditorThemeAdapter(), new PluginEditorThemeAdapter(),
			new AntEditorThemeAdapter(), new DebugConsoleThemeAdapter());
	private final Function<String, IEclipsePreferences> nodes;

	public PreferenceReset(Function<String, IEclipsePreferences> nodes) {
		this.nodes = nodes;
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

	public record Result(List<Failure> failures) {
		public Result {
			failures = List.copyOf(failures);
		}

		public boolean successful() {
			return failures.isEmpty();
		}

		public String describeFailures() {
			return failures.stream().map(Failure::description).collect(Collectors.joining("\n\n"));
		}
	}

	/** Run on the caller's thread; preference listeners may require the UI thread. */
	public Result reset() {
		List<Failure> failures = new ArrayList<>();
		for (EclipseThemeAdapter adapter : ADAPTERS) {
			String integration = adapter.getClass().getSimpleName();
			String nodeId = adapter.getPreferencesId();
			List<String> keys = List.copyOf(adapter.getManagedKeys());
			IEclipsePreferences prefs;
			try {
				prefs = java.util.Objects.requireNonNull(nodes.apply(nodeId), "Preference node unavailable");
			} catch (RuntimeException e) {
				failures.add(new Failure(integration, nodeId, keys, "access node", e));
				continue;
			}
			for (String key : keys) {
				try {
					PreferenceValue.absent().restore(prefs, key);
				} catch (RuntimeException e) {
					failures.add(new Failure(integration, nodeId, List.of(key), "remove override", e));
				}
			}
			try {
				prefs.flush();
			} catch (BackingStoreException | RuntimeException e) {
				failures.add(new Failure(integration, nodeId, keys, "persist removals", e));
			}
		}
		return new Result(failures);
	}

	/** Active state and success UI may change only after every node persisted. */
	public Result resetAndCommit(Runnable onSuccess) {
		Result result = reset();
		if (result.successful()) onSuccess.run();
		return result;
	}
}
