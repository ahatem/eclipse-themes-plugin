package com.github.eclipsethemes.core.workbench;

/**
 * The seven semantic roles of the workbench palette. This enum and
 * {@link WorkbenchFamily} are the single mapping to the plugin-owned color ids
 * that {@code plugin.xml} declares and the static CSS references.
 */
public enum WorkbenchColor {
	BACKGROUND("background"),
	FOREGROUND("foreground"),
	SURFACE("surface"),
	SURFACE_RAISED("raised"),
	BORDER("border"),
	MUTED_FOREGROUND("muted"),
	SELECTION("selection");

	static final String ID_PREFIX = "com.github.eclipsethemes.workbench.";

	private final String idRole;

	WorkbenchColor(String idRole) {
		this.idRole = idRole;
	}

	/** The Colors-and-Fonts id, for example {@code com.github.eclipsethemes.workbench.dark.surface}. */
	public String colorId(WorkbenchFamily family) {
		return ID_PREFIX + family.key() + "." + idRole;
	}

	/** The id as written in CSS {@code '#...'} references: dots become dashes. */
	public String cssId(WorkbenchFamily family) {
		return colorId(family).replace('.', '-');
	}
}
