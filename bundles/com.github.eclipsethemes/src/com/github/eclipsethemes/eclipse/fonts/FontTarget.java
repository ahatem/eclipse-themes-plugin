package com.github.eclipsethemes.eclipse.fonts;

import java.util.Optional;

import com.github.eclipsethemes.core.models.ThemeFonts;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;

/**
 * The two semantic fonts a user can choose, and the only two. Each stands for a bounded set of Eclipse font
 * definitions that {@link FontTargets} resolves; the user never sees that list.
 *
 * <ul>
 * <li>{@link #EDITOR}: the base font for code and text surfaces, anchored on the Eclipse <em>Text Font</em>.</li>
 * <li>{@link #INTERFACE}: the base font for the Eclipse interface, anchored on the Eclipse <em>Dialog Font</em>.</li>
 * </ul>
 */
public enum FontTarget {

	EDITOR("editor", "Editor font", "Editors, consoles and supported code/text views.", "org.eclipse.jface.textfont",
			PreferenceKeys.FONT_EDITOR_MODE, PreferenceKeys.FONT_EDITOR_FAMILY, PreferenceKeys.FONT_EDITOR_SIZE),
	INTERFACE("interface", "Interface font", "Dialogs, views, trees, tables and supported Eclipse chrome.",
			"org.eclipse.jface.dialogfont", PreferenceKeys.FONT_INTERFACE_MODE, PreferenceKeys.FONT_INTERFACE_FAMILY,
			PreferenceKeys.FONT_INTERFACE_SIZE);

	private final String role;
	private final String label;
	private final String description;
	private final String baseId;
	private final String modeKey;
	private final String familyKey;
	private final String sizeKey;

	FontTarget(String role, String label, String description, String baseId, String modeKey, String familyKey,
			String sizeKey) {
		this.role = role;
		this.label = label;
		this.description = description;
		this.baseId = baseId;
		this.modeKey = modeKey;
		this.familyKey = familyKey;
		this.sizeKey = sizeKey;
	}

	/** The word used in messages and in the ownership record: {@code editor} or {@code interface}. */
	public String role() {
		return role;
	}

	/** The name shown on the preference page. */
	public String label() {
		return label;
	}

	/** One short line saying what the font is used for. */
	public String description() {
		return description;
	}

	/** The Eclipse definition this font is anchored on; the one shown as the current font. */
	String baseId() {
		return baseId;
	}

	String modeKey() {
		return modeKey;
	}

	String familyKey() {
		return familyKey;
	}

	String sizeKey() {
		return sizeKey;
	}

	/** What a theme package asks for this font, if anything. */
	public Optional<ThemeFonts.Face> themeFace(ThemeFonts fonts) {
		return this == EDITOR ? fonts.editorFont() : fonts.interfaceFont();
	}
}
