package com.github.eclipsethemes.eclipse.workbench;

import java.util.List;

/**
 * What one synchronization of the coordinated plug-in keys did. Key lists carry {@code node|key} ids and are shown
 * only in the copyable diagnostics text; the page and the status line use the counts.
 *
 * @param ownedKeys    keys whose current value this plugin wrote and still owns
 * @param userKeys     keys the plugin wanted to write but skipped because the user already has a value
 * @param releasedKeys keys the plugin owned but found changed or removed by someone else; left untouched
 * @param failures     problems that stopped a flush or the ownership record from being saved
 */
public record PluginColorSummary(List<String> ownedKeys, List<String> userKeys, List<String> releasedKeys,
		List<String> failures) {

	public static final PluginColorSummary EMPTY = new PluginColorSummary(List.of(), List.of(), List.of(), List.of());

	public PluginColorSummary {
		ownedKeys = List.copyOf(ownedKeys);
		userKeys = List.copyOf(userKeys);
		releasedKeys = List.copyOf(releasedKeys);
		failures = List.copyOf(failures);
	}

	public boolean failed() {
		return !failures.isEmpty();
	}
}
