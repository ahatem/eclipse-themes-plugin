package com.github.eclipsethemes.eclipse.fonts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeFonts;

/** What the Fonts section of the preference page says about the selected theme; pure so it needs no widgets. */
public final class FontSectionModel {

	public static final String CHECKBOX_LABEL = "Apply theme fonts";
	public static final String HINT = "Optional and separate from colors. Turning it off restores the fonts it replaced. "
			+ "Some workbench surfaces may update only after restarting Eclipse.";

	private FontSectionModel() {
	}

	/** One line per requested font, or one line saying the theme asks for none; missing families are called out. */
	public static String describe(Theme theme, Predicate<String> installed) {
		if (theme == null) {
			return "No theme selected.";
		}
		ThemeFonts fonts = theme.getFonts();
		if (fonts.isEmpty()) {
			return "Theme does not specify fonts";
		}
		List<String> lines = new ArrayList<>();
		fonts.editorFont().ifPresent(face -> lines.add(line("Editor font", face, installed)));
		fonts.workbenchFont().ifPresent(face -> lines.add(line("Workbench font", face, installed)));
		return String.join("\n", lines);
	}

	private static String line(String label, ThemeFonts.Face face, Predicate<String> installed) {
		return installed.test(face.family()) ? label + ": " + face
				: "Requested font \"" + face.family() + "\" is not installed (" + label.toLowerCase(Locale.ROOT) + ")";
	}
}
