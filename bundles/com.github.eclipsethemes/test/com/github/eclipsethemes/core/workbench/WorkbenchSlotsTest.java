package com.github.eclipsethemes.core.workbench;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;

public class WorkbenchSlotsTest {

	@Test
	public void sameFamilyChangeAlternatesToARealIdTransition() {
		WorkbenchFamily dark = WorkbenchFamily.DARK;
		String first = WorkbenchSlots.target(dark, "org.eclipse.e4.ui.css.theme.e4_default");
		assertEquals(dark.themeId(), first);
		String second = WorkbenchSlots.target(dark, first);
		assertEquals(dark.alternateThemeId(), second);
		assertNotEquals(first, second);
		String third = WorkbenchSlots.target(dark, second);
		assertEquals(dark.themeId(), third);
		assertNotEquals(second, third);
	}

	@Test
	public void familyChangeSelectsTheOtherFamilyEvenFromItsAlternateSlot() {
		assertEquals(WorkbenchFamily.LIGHT.themeId(),
				WorkbenchSlots.target(WorkbenchFamily.LIGHT, WorkbenchFamily.DARK.alternateThemeId()));
		assertEquals(WorkbenchFamily.DARK.themeId(),
				WorkbenchSlots.target(WorkbenchFamily.DARK, WorkbenchFamily.LIGHT.themeId()));
	}

	@Test
	public void foreignOrUnknownActiveThemeSelectsSlotA() {
		assertEquals(WorkbenchFamily.DARK.themeId(), WorkbenchSlots.target(WorkbenchFamily.DARK, ""));
		assertEquals(WorkbenchFamily.LIGHT.themeId(), WorkbenchSlots.target(WorkbenchFamily.LIGHT, null));
	}

	@Test
	public void ownershipIsRecognizedOnlyForTheFourOwnIds() {
		assertEquals(4, new HashSet<>(WorkbenchSlots.ownThemeIds()).size());
		for (String id : WorkbenchSlots.ownThemeIds()) {
			assertTrue(WorkbenchSlots.isOurs(id));
		}
		assertFalse(WorkbenchSlots.isOurs("org.eclipse.e4.ui.css.theme.e4_dark"));
		assertFalse(WorkbenchSlots.isOurs(null));
		assertEquals(WorkbenchFamily.LIGHT, WorkbenchSlots.familyOf(WorkbenchFamily.LIGHT.alternateThemeId()).orElseThrow());
	}

	@Test
	public void darkIdsContainDarkAndLightIdsDoNot() {
		// Eclipse derives Display.setDarkThemePreferred from id.contains("dark") on 4.40+.
		assertTrue(WorkbenchFamily.DARK.themeId().contains("dark"));
		assertTrue(WorkbenchFamily.DARK.alternateThemeId().contains("dark"));
		assertFalse(WorkbenchFamily.LIGHT.themeId().contains("dark"));
		assertFalse(WorkbenchFamily.LIGHT.alternateThemeId().contains("dark"));
	}

	@Test
	public void colorIdsAreNamespacedPerFamilyAndCssIdsUseDashes() {
		assertEquals("com.github.eclipsethemes.workbench.dark.surface", WorkbenchColor.SURFACE.colorId(WorkbenchFamily.DARK));
		assertEquals("com-github-eclipsethemes-workbench-light-selection",
				WorkbenchColor.SELECTION.cssId(WorkbenchFamily.LIGHT));
		HashSet<String> ids = new HashSet<>();
		for (WorkbenchFamily family : WorkbenchFamily.values()) {
			for (WorkbenchColor role : WorkbenchColor.values()) {
				assertTrue(ids.add(role.colorId(family)));
				assertEquals(role.colorId(family).toLowerCase(), role.colorId(family));
			}
		}
		assertEquals(14, ids.size());
	}
}
