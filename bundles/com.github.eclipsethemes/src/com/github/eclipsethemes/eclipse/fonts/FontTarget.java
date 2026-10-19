package com.github.eclipsethemes.eclipse.fonts;

import java.util.List;
import java.util.Optional;

import com.github.eclipsethemes.core.models.ThemeFonts;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;

/**
 * The two independent things a font can be chosen for. Each is mapped to the platform font definitions it changes, and
 * to the preference keys that hold the user's choice.
 *
 * <p>
 * The editor font is the platform <em>Text Font</em>. The workbench font is the platform <em>Part title</em>
 * ({@code TAB_TEXT_FONT}), <em>Tree and Table</em> and <em>Dialog</em> font definitions, all given the one chosen font.
 */
public enum FontTarget {

	EDITOR("editor", List.of("org.eclipse.jface.textfont"), PreferenceKeys.FONT_EDITOR_MODE,
			PreferenceKeys.FONT_EDITOR_FAMILY, PreferenceKeys.FONT_EDITOR_SIZE),
	WORKBENCH("workbench", List.of("org.eclipse.ui.workbench.TAB_TEXT_FONT", "org.eclipse.ui.workbench.TREE_TABLE_FONT",
			"org.eclipse.jface.dialogfont"), PreferenceKeys.FONT_WORKBENCH_MODE, PreferenceKeys.FONT_WORKBENCH_FAMILY,
			PreferenceKeys.FONT_WORKBENCH_SIZE);

	private final String role;
	private final List<String> definitionIds;
	private final String modeKey;
	private final String familyKey;
	private final String sizeKey;

	FontTarget(String role, List<String> definitionIds, String modeKey, String familyKey, String sizeKey) {
		this.role = role;
		this.definitionIds = definitionIds;
		this.modeKey = modeKey;
		this.familyKey = familyKey;
		this.sizeKey = sizeKey;
	}

	/** The word used in messages: {@code editor} or {@code workbench}. */
	public String role() {
		return role;
	}

	/** The platform font definitions this target changes; the first is the one shown as the current font. */
	List<String> definitionIds() {
		return definitionIds;
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

	/** What a theme package asks for this target, if anything. */
	public Optional<ThemeFonts.Face> themeFace(ThemeFonts fonts) {
		return this == EDITOR ? fonts.editorFont() : fonts.workbenchFont();
	}
}
