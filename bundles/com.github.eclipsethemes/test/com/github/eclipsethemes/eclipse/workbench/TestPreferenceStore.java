package com.github.eclipsethemes.eclipse.workbench;

import java.io.IOException;

import org.eclipse.jface.preference.PreferenceStore;

/** A {@link PreferenceStore} that never touches the file system: {@code save()} counts, or fails on demand. */
public final class TestPreferenceStore extends PreferenceStore {

	public boolean failSave;
	public int saves;

	@Override
	public void save() throws IOException {
		if (failSave) {
			throw new IOException("save failed");
		}
		saves++;
	}
}
