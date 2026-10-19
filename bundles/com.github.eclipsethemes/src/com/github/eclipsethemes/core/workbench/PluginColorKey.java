package com.github.eclipsethemes.core.workbench;

import java.util.Objects;

/**
 * One instance-scope preference key owned by another plug-in that follows a coordinated dark workbench theme.
 *
 * @param node           preference node (a bundle symbolic name) that stores the key
 * @param key            the key inside that node
 * @param requiredBundle bundle that must be installed for the key to mean anything, or {@code null} when the node's
 *                       own bundle is always present
 * @param source         how the value is derived from the workbench palette
 */
public record PluginColorKey(String node, String key, String requiredBundle, PluginColorSource source) {

	public PluginColorKey {
		Objects.requireNonNull(node, "node");
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(source, "source");
		if (node.indexOf('|') >= 0 || key.indexOf('|') >= 0 || node.indexOf('\n') >= 0 || key.indexOf('\n') >= 0) {
			throw new IllegalArgumentException("node and key must not contain '|' or a newline: " + node + "/" + key);
		}
	}

	/** Stable identity used in the ownership record: {@code node|key}. */
	public String id() {
		return node + "|" + key;
	}
}
