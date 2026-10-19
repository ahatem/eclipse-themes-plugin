package com.github.eclipsethemes.core.models;

import java.util.Objects;
import java.util.Optional;

/**
 * The fonts a theme package asks for. Both entries are independently optional; a theme without fonts asks for
 * nothing. Fonts are installed font families only: a package never carries font files.
 */
public record ThemeFonts(Face editor, Face workbench) {

	public static final int MIN_SIZE = 4;
	public static final int MAX_SIZE = 96;
	public static final int MAX_FAMILY_LENGTH = 128;

	public static final ThemeFonts NONE = new ThemeFonts(null, null);

	/**
	 * One requested font.
	 *
	 * @param family the font family name exactly as the author wrote it
	 * @param size   the size in points, {@value #MIN_SIZE} to {@value #MAX_SIZE}
	 */
	public record Face(String family, int size) {

		public Face {
			Objects.requireNonNull(family, "family");
			if (!validFamily(family) || size < MIN_SIZE || size > MAX_SIZE) {
				throw new IllegalArgumentException("Invalid font: '" + family + "' " + size);
			}
		}

		@Override
		public String toString() {
			return family + " " + size;
		}

		/** The presentation form, for example {@code Consolas — 11 pt}. */
		public String label() {
			return family + " — " + size + " pt";
		}
	}

	/** A family is a non-blank name of printable characters of at most {@value #MAX_FAMILY_LENGTH} characters. */
	public static boolean validFamily(String family) {
		return !family.isBlank() && family.length() <= MAX_FAMILY_LENGTH
				&& family.chars().noneMatch(Character::isISOControl);
	}

	public Optional<Face> editorFont() {
		return Optional.ofNullable(editor);
	}

	public Optional<Face> workbenchFont() {
		return Optional.ofNullable(workbench);
	}

	public boolean isEmpty() {
		return editor == null && workbench == null;
	}
}
