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

	/** Where the editor font comes from: {@code keep}, {@code theme} or {@code custom}. Absent means keep. */
	String FONT_EDITOR_MODE = "FONT_EDITOR_MODE";
	/** The user's own editor font (family and size); used only while the editor font mode is custom. */
	String FONT_EDITOR_FAMILY = "FONT_EDITOR_FAMILY";
	String FONT_EDITOR_SIZE = "FONT_EDITOR_SIZE";
	/** Where the workbench font comes from: {@code keep}, {@code theme} or {@code custom}. Absent means keep. */
	String FONT_WORKBENCH_MODE = "FONT_WORKBENCH_MODE";
	/** The user's own workbench font (family and size); used only while the workbench font mode is custom. */
	String FONT_WORKBENCH_FAMILY = "FONT_WORKBENCH_FAMILY";
	String FONT_WORKBENCH_SIZE = "FONT_WORKBENCH_SIZE";
	/**
	 * The font definitions this plugin wrote, the exact value of each and what the definition held before
	 * ({@code id}, written value and prior state per line). Absent when it owns none.
	 */
	String THEME_FONTS_OWNED = "THEME_FONTS_OWNED";
}
