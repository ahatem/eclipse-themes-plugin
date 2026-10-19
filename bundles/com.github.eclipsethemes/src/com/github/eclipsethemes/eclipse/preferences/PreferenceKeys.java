package com.github.eclipsethemes.eclipse.preferences;

public interface PreferenceKeys {
	String ACTIVE_THEME_ID = "ACTIVE_THEME_ID";
	String SELECTED_THEME_LOGICAL_ID = "SELECTED_THEME_LOGICAL_ID";

	/** User intent: also coordinate the Eclipse workbench theme. Off by default. */
	String WORKBENCH_COORDINATION_ENABLED = "WORKBENCH_COORDINATION_ENABLED";
	/** The os/ws/theme-engine combination the user opted into experimentally; empty when none. */
	String WORKBENCH_EXPERIMENTAL_ACK = "WORKBENCH_EXPERIMENTAL_ACK";
	/** Ownership record, written only after a verified switch. Absent when the plugin owns nothing. */
	String WORKBENCH_OWNED_THEME_ID = "WORKBENCH_OWNED_THEME_ID";
	/** The Eclipse theme that was active before this plugin first took ownership; empty when unknown. */
	String WORKBENCH_PRIOR_ACTIVE_ID = "WORKBENCH_PRIOR_ACTIVE_ID";
	/** The theme id Eclipse had saved before ownership; meaningful only when the absent flag is false. */
	String WORKBENCH_PRIOR_SAVED_ID = "WORKBENCH_PRIOR_SAVED_ID";
	String WORKBENCH_PRIOR_SAVED_ABSENT = "WORKBENCH_PRIOR_SAVED_ABSENT";

	/**
	 * Which foreign plug-in preference keys this plugin wrote and the exact value of each ({@code node|key|value}
	 * lines). Absent when it owns none.
	 */
	String PLUGIN_COLORS_OWNED = "PLUGIN_COLORS_OWNED";

	/** User intent: apply the fonts a theme package asks for. Off by default; independent of colors. */
	String THEME_FONTS_ENABLED = "THEME_FONTS_ENABLED";
	/**
	 * The font definitions this plugin wrote, the exact value of each and what the definition held before
	 * ({@code id}, written value and prior state per line). Absent when it owns none.
	 */
	String THEME_FONTS_OWNED = "THEME_FONTS_OWNED";
}
