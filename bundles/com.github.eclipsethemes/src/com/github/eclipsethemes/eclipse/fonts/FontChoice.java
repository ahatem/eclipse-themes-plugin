package com.github.eclipsethemes.eclipse.fonts;

import java.util.Objects;
import java.util.Optional;

import com.github.eclipsethemes.core.models.ThemeFonts;

/**
 * The user's choice for one font target: where the font comes from and, for {@link FontMode#CUSTOM}, which font. The
 * custom font is remembered while another mode is selected, so switching back does not lose it. It belongs to the
 * user and is never part of a theme package.
 *
 * @param custom the user's own font, or {@code null} when none has been chosen
 */
public record FontChoice(FontMode mode, ThemeFonts.Face custom) {

	public static final FontChoice KEEP = new FontChoice(FontMode.KEEP, null);

	public FontChoice {
		Objects.requireNonNull(mode, "mode");
	}

	public FontChoice withMode(FontMode newMode) {
		return new FontChoice(newMode, custom);
	}

	public FontChoice withCustom(ThemeFonts.Face face) {
		return new FontChoice(mode, face);
	}

	/** The font to apply, given what the selected theme asks for; empty means the target is not managed. */
	public Optional<ThemeFonts.Face> resolve(Optional<ThemeFonts.Face> themeFace) {
		return switch (mode) {
		case KEEP -> Optional.empty();
		case THEME -> themeFace;
		case CUSTOM -> Optional.ofNullable(custom);
		};
	}
}
