package com.github.eclipsethemes.core.models;

import java.util.Objects;
import java.util.Optional;

/**
 * The two semantic fonts a theme package asks for: the editor font and the interface font. Both are independently
 * optional; a theme without fonts asks for nothing. They are intents, not platform definitions: the plugin maps each to
 * the Eclipse font definitions it coordinates. Fonts are installed font families only: a package never carries font
 * files.
 */
public record ThemeFonts(Face editor, Face interfaceFace) {

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

	public Optional<Face> interfaceFont() {
		return Optional.ofNullable(interfaceFace);
	}

	public boolean isEmpty() {
		return editor == null && interfaceFace == null;
	}
}
