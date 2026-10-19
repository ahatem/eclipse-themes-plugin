package com.github.eclipsethemes.eclipse.fonts;

/** Where one font target gets its font from. */
public enum FontMode {

	/** The plugin does not manage this font. */
	KEEP("keep", "Keep current font"),
	/** The font the selected theme package asks for, if it asks for one. */
	THEME("theme", "Use theme font"),
	/** A font the user chose, independent of the selected theme. */
	CUSTOM("custom", "Custom font");

	private final String stored;
	private final String label;

	FontMode(String stored, String label) {
		this.stored = stored;
		this.label = label;
	}

	/** The value written to the preference store. */
	public String stored() {
		return stored;
	}

	/** The text of the choice shown to the user. */
	public String label() {
		return label;
	}

	/** Anything unrecognized, including an absent value, means {@link #KEEP}: nothing is managed by accident. */
	public static FontMode parse(String stored) {
		for (FontMode mode : values()) {
			if (mode.stored.equals(stored)) {
				return mode;
			}
		}
		return KEEP;
	}
}
