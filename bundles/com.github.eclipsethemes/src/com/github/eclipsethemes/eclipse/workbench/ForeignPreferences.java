package com.github.eclipsethemes.eclipse.workbench;

import java.io.IOException;
import java.util.Optional;

/**
 * Instance-scope preferences of other plug-ins, reduced to what {@link PluginColorWriter} may do to them: read, put or
 * remove one named key, and flush a node. There is deliberately no way to clear a node or list its keys.
 */
public interface ForeignPreferences {

	/** True when the bundle that owns the keys is installed. */
	boolean installed(String bundle);

	Optional<String> get(String node, String key);

	void put(String node, String key, String value);

	void remove(String node, String key);

	void flush(String node) throws IOException;
}
