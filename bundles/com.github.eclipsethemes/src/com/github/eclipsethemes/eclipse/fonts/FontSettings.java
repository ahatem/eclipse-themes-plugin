package com.github.eclipsethemes.eclipse.fonts;

import java.io.IOException;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;

import com.github.eclipsethemes.core.models.ThemeFonts;

/** The user's font choices for the editor and interface fonts, as stored in the plugin's preferences. */
public record FontSettings(FontChoice editor, FontChoice interfaceChoice) {

	public FontChoice choice(FontTarget target) {
		return target == FontTarget.EDITOR ? editor : interfaceChoice;
	}

	public FontSettings with(FontTarget target, FontChoice choice) {
		return target == FontTarget.EDITOR ? new FontSettings(choice, interfaceChoice)
				: new FontSettings(editor, choice);
	}

	public static FontSettings load(IPreferenceStore store) {
		return new FontSettings(load(store, FontTarget.EDITOR), load(store, FontTarget.INTERFACE));
	}

	private static FontChoice load(IPreferenceStore store, FontTarget target) {
		FontMode mode = FontMode.parse(store.getString(target.modeKey()));
		String family = store.getString(target.familyKey());
		int size = store.getInt(target.sizeKey());
		ThemeFonts.Face custom = null;
		if (ThemeFonts.validFamily(family) && size >= ThemeFonts.MIN_SIZE && size <= ThemeFonts.MAX_SIZE) {
			custom = new ThemeFonts.Face(family, size);
		}
		return new FontChoice(mode, custom);
	}

	/** Writes the choices and saves them. Call only when the user commits (Apply or OK). */
	public void save(IPreferenceStore store) throws IOException {
		for (FontTarget target : FontTarget.values()) {
			FontChoice choice = choice(target);
			store.setValue(target.modeKey(), choice.mode().stored());
			if (choice.custom() == null) {
				store.setToDefault(target.familyKey());
				store.setToDefault(target.sizeKey());
			} else {
				store.setValue(target.familyKey(), choice.custom().family());
				store.setValue(target.sizeKey(), choice.custom().size());
			}
		}
		if (store instanceof IPersistentPreferenceStore persistent && persistent.needsSaving()) {
			persistent.save();
		}
	}
}
