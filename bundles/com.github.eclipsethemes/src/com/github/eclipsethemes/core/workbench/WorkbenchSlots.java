package com.github.eclipsethemes.core.workbench;

import java.util.List;
import java.util.Optional;

/**
 * The refresh-twin rule. Eclipse re-reads published colors only on a real theme-id change, so each family has
 * a slot A and a slot B and a switch always moves to a different id. Nothing else knows about the slots.
 */
public final class WorkbenchSlots {

	private WorkbenchSlots() {
	}

	/** The theme id to select so that the family is active and the id really changes when it already is. */
	public static String target(WorkbenchFamily family, String activeId) {
		return family.themeId().equals(activeId) ? family.alternateThemeId() : family.themeId();
	}

	public static List<String> ownThemeIds() {
		return List.of(WorkbenchFamily.DARK.themeId(), WorkbenchFamily.DARK.alternateThemeId(),
				WorkbenchFamily.LIGHT.themeId(), WorkbenchFamily.LIGHT.alternateThemeId());
	}

	public static Optional<WorkbenchFamily> familyOf(String themeId) {
		for (WorkbenchFamily family : WorkbenchFamily.values()) {
			if (family.themeId().equals(themeId) || family.alternateThemeId().equals(themeId)) {
				return Optional.of(family);
			}
		}
		return Optional.empty();
	}

	public static boolean isOurs(String themeId) {
		return familyOf(themeId).isPresent();
	}
}
