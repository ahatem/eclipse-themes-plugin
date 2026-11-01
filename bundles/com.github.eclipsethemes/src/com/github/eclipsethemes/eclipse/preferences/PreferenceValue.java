package com.github.eclipsethemes.eclipse.preferences;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

/** A key state, preserving absence for future trustworthy ownership snapshots. */
public record PreferenceValue(Optional<String> value) {

	public PreferenceValue {
		Objects.requireNonNull(value);
	}

	public static PreferenceValue absent() {
		return new PreferenceValue(Optional.empty());
	}

	public static PreferenceValue present(String value) {
		return new PreferenceValue(Optional.of(value));
	}

	/** The instance value only; a node default is not a stored value and is reported as absent. */
	public static PreferenceValue read(IEclipsePreferences preferences, String key) {
		String value = preferences.get(key, null);
		return value == null ? absent() : present(value);
	}

	public void restore(IEclipsePreferences preferences, String key) {
		if (value.isPresent()) preferences.put(key, value.get());
		else preferences.remove(key);
	}
}
