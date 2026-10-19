package com.github.eclipsethemes.core.workbench;

/** What the compat boundary observed about the theme engine, reduced to the facts the classifier needs. */
public record EngineProbe(boolean linkageError, boolean engineAvailable, boolean highContrast,
		boolean ownThemesPresent) {

	public static EngineProbe usable() {
		return new EngineProbe(false, true, false, true);
	}
}
