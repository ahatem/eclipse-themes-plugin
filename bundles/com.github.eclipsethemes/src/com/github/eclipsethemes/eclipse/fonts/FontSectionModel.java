package com.github.eclipsethemes.eclipse.fonts;

import java.util.Optional;
import java.util.function.Predicate;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeFonts;

/** What the Fonts section of the preference page says about each target; pure so it needs no widgets. */
public final class FontSectionModel {

	public static final String HINT = "Fonts are separate from colors and only change when you choose Apply. "
			+ "Keep current leaves a font alone. Some workbench surfaces may update only after restarting Eclipse.";

	public static final String NOT_SPECIFIED = "Not specified by this theme";

	private FontSectionModel() {
	}

	/**
	 * One line describing where the target's font comes from and what it is.
	 *
	 * @param theme   the selected theme, or null when none is selected
	 * @param current the font the target uses right now
	 */
	public static String describe(FontTarget target, FontChoice choice, Theme theme, Predicate<String> installed,
			Optional<ThemeFonts.Face> current) {
		return switch (choice.mode()) {
		case KEEP -> current.map(face -> "Using current Eclipse font: " + face.label())
				.orElse("Using the current Eclipse font");
		case THEME -> {
			if (theme == null) {
				yield "No theme selected";
			}
			Optional<ThemeFonts.Face> face = target.themeFace(theme.getFonts());
			yield face.isEmpty() ? NOT_SPECIFIED : line("Theme: ", face.get(), installed);
		}
		case CUSTOM -> choice.custom() == null ? "No font chosen yet"
				: line("Custom: ", choice.custom(), installed);
		};
	}

	private static String line(String prefix, ThemeFonts.Face face, Predicate<String> installed) {
		return installed.test(face.family()) ? prefix + face.label()
				: "Requested font \"" + face.family() + "\" is not installed";
	}
}
