package com.github.eclipsethemes.core.workbench;

/** How a coordinated plug-in key gets its value from the workbench palette. */
public enum PluginColorSource {
	/** The popup surface: the palette's raised color. */
	POPUP_BACKGROUND,
	/** Text on popup surfaces: the palette foreground. */
	FOREGROUND,
	/** Hyperlinks: the palette accent. */
	LINK,
	/** The hovered hyperlink: the accent mixed toward the foreground, or the foreground when that is unreadable. */
	ACTIVE_LINK,
	/** A boolean flag that tells the owning plug-in its defaults should suit a dark UI. */
	DARK_FLAG
}
