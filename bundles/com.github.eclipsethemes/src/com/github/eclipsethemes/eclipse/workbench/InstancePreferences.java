package com.github.eclipsethemes.eclipse.workbench;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

/** {@link ForeignPreferences} over the Eclipse instance scope. Call on the UI thread. */
final class InstancePreferences implements ForeignPreferences {

	@Override
	public boolean installed(String bundle) {
		return Platform.getBundle(bundle) != null;
	}

	@Override
	public Optional<String> get(String node, String key) {
		return Optional.ofNullable(InstanceScope.INSTANCE.getNode(node).get(key, null));
	}

	@Override
	public void put(String node, String key, String value) {
		InstanceScope.INSTANCE.getNode(node).put(key, value);
	}

	@Override
	public void remove(String node, String key) {
		InstanceScope.INSTANCE.getNode(node).remove(key);
	}

	@Override
	public void flush(String node) throws IOException {
		try {
			InstanceScope.INSTANCE.getNode(node).flush();
		} catch (BackingStoreException e) {
			throw new IOException("Could not save preferences of " + node, e);
		}
	}
}
