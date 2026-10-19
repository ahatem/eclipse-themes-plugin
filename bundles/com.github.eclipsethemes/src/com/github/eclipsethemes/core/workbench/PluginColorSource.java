package com.github.eclipsethemes.core.workbench;

/** How a coordinated plug-in key gets its value from the workbench palette. */
public enum PluginColorSource {
	/** The palette content background. */
	BACKGROUND,
	/** The palette raised surface (popups, headers). */
	RAISED,
	/** The palette border color. */
	BORDER,
	/** The palette foreground. */
	FOREGROUND,
	/** The palette muted foreground (secondary and de-emphasized text). */
	MUTED,
	/** The palette accent (links and emphasis). */
	ACCENT,
	/** The hovered link: the accent mixed toward the foreground, or the foreground when that is unreadable. */
	ACTIVE_ACCENT,
	/** Text-grade status colors: a fixed hue lightness-corrected to be readable on the content surfaces. */
	ERROR, WARNING, SUCCESS, INFO,
	/** Background tints: the palette background pulled toward a status hue as far as the foreground stays readable. */
	ERROR_TINT, WARNING_TINT, SUCCESS_TINT, INFO_TINT,
	/** Tints at half strength, for the weaker of two related highlights or the older end of a scale. */
	ERROR_TINT_SOFT, WARNING_TINT_SOFT, SUCCESS_TINT_SOFT,
	/** A boolean flag that tells the owning plug-in its defaults should suit a dark UI. */
	DARK_FLAG,
	/** A boolean flag that switches a plug-in feature off (for example system colors). */
	OFF_FLAG
}
