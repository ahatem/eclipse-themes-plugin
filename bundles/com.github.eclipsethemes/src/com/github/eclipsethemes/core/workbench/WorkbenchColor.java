package com.github.eclipsethemes.core.workbench;

/**
 * The semantic roles of the workbench palette: five surfaces and borders, three text and accent colors. Each role
 * exists because a concrete workbench surface needs it (see the CSS). This enum and
 * {@link WorkbenchFamily} are the single mapping to the plugin-owned color ids
 * that {@code plugin.xml} declares and the static CSS references.
 */
public enum WorkbenchColor {
	BACKGROUND("background", "background"),
	FOREGROUND("foreground", "foreground"),
	SURFACE("surface", "surface"),
	SURFACE_RAISED("raised", "surfaceRaised"),
	BORDER("border", "border"),
	MUTED_FOREGROUND("muted", "mutedForeground"),
	SELECTION("selection", "selection"),
	ACCENT("accent", "accent");

	static final String ID_PREFIX = "com.github.eclipsethemes.workbench.";

	private final String idRole;
	private final String schemaName;

	WorkbenchColor(String idRole, String schemaName) {
		this.idRole = idRole;
		this.schemaName = schemaName;
	}

	/** The role's element name in a package {@code <workbench>} section (see {@code Theme.WORKBENCH_ROLES}). */
	public String schemaName() {
		return schemaName;
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
