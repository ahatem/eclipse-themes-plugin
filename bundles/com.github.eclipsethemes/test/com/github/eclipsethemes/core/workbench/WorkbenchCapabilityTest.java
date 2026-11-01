package com.github.eclipsethemes.core.workbench;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.eclipsethemes.core.workbench.WorkbenchCapability.Reason;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.State;

public class WorkbenchCapabilityTest {

	private static WorkbenchEnvironment env(String os, String ws, String themeBundle) {
		return new WorkbenchEnvironment(os, ws, themeBundle, "4.40.0", "21.0.12+7");
	}

	private static WorkbenchCapability classify(WorkbenchEnvironment env, EngineProbe probe) {
		return WorkbenchCapabilityClassifier.classify(env, probe);
	}

	/** Only measured combinations are VERIFIED (the build qualifier is ignored); the table is confidence, not an allow-list. */
	@Test
	public void measuredCombinationsAreVerifiedAndEverythingElseIsUsableButExperimental() {
		String[][] cases = {
				// os, ws, theme bundle, expected state
				{ "win32", "win32", "0.15.100.v20260507-0340", "VERIFIED" },
				{ "win32", "win32", "0.15.200.v20260801-1200", "VERIFIED" },
				{ "win32", "win32", "0.15.200", "VERIFIED" },
				{ "win32", "win32", "0.15.300.v1", "EXPERIMENTAL" },
				{ "win32", "win32", "0.16.0.v1", "EXPERIMENTAL" },
				{ "linux", "gtk", "0.15.200.v1", "EXPERIMENTAL" },
				{ "macosx", "cocoa", "0.15.200.v1", "EXPERIMENTAL" },
				{ "solaris", "motif", "9.9.9", "EXPERIMENTAL" },
		};
		for (String[] c : cases) {
			WorkbenchCapability capability = classify(env(c[0], c[1], c[2]), EngineProbe.usable());
			String label = c[0] + "/" + c[1] + "/" + c[2];
			assertEquals(label, State.valueOf(c[3]), capability.state());
			assertTrue(label, capability.usable());
			assertNull(label, capability.reason());
		}
	}

	@Test
	public void hardGatesAreUnavailableWithTheirOwnReason() {
		WorkbenchEnvironment win = env("win32", "win32", "0.15.200");
		assertEquals(Reason.LINKAGE_ERROR, classify(win, new EngineProbe(true, false, false, false)).reason());
		assertEquals(Reason.NO_ENGINE, classify(win, new EngineProbe(false, false, false, false)).reason());
		assertEquals(Reason.HIGH_CONTRAST, classify(win, new EngineProbe(false, true, true, true)).reason());
		assertEquals(Reason.THEME_NOT_FOUND, classify(win, new EngineProbe(false, true, false, false)).reason());
		for (EngineProbe probe : new EngineProbe[] { new EngineProbe(true, false, false, false),
				new EngineProbe(false, false, false, false), new EngineProbe(false, true, true, true),
				new EngineProbe(false, true, false, false) }) {
			WorkbenchCapability capability = classify(win, probe);
			assertEquals(State.UNAVAILABLE, capability.state());
			assertFalse(capability.usable());
		}
	}

	@Test
	public void linkageErrorTakesPrecedenceAndHighContrastBeatsMissingThemes() {
		WorkbenchEnvironment win = env("win32", "win32", "0.15.200");
		assertEquals(Reason.LINKAGE_ERROR, classify(win, new EngineProbe(true, true, true, false)).reason());
		assertEquals(Reason.HIGH_CONTRAST, classify(win, new EngineProbe(false, true, true, false)).reason());
	}

	@Test
	public void onlyUnavailableCarriesAReason() {
		WorkbenchEnvironment win = env("win32", "win32", "0.15.200");
		try {
			new WorkbenchCapability(State.VERIFIED, Reason.NO_ENGINE, win);
			org.junit.Assert.fail();
		} catch (IllegalArgumentException expected) {
			// reason on a usable state
		}
		try {
			new WorkbenchCapability(State.UNAVAILABLE, null, win);
			org.junit.Assert.fail();
		} catch (IllegalArgumentException expected) {
			// UNAVAILABLE needs a reason
		}
	}

	@Test
	public void diagnosticsRecordTheFullEnvironment() {
		WorkbenchEnvironment environment = new WorkbenchEnvironment("win32", "win32", "0.15.200.v20260801-1200",
				"4.41.0.v20260901", "21.0.12+7");
		String text = WorkbenchDiagnostics.format(classify(environment, EngineProbe.usable()), true, false, "Atom One Dark",
				"com.github.eclipsethemes.workbench.dark", true);
		for (String expected : new String[] { "VERIFIED", "win32", "0.15.200.v20260801-1200", "4.41.0.v20260901",
				"21.0.12+7", "Atom One Dark", "com.github.eclipsethemes.workbench.dark", "Coordination enabled: true" }) {
			assertTrue(expected, text.contains(expected));
		}
		String unavailable = WorkbenchDiagnostics.format(
				classify(environment, new EngineProbe(false, true, true, true)), false, false, null, null, false);
		assertTrue(unavailable.contains("UNAVAILABLE (HIGH_CONTRAST)"));
	}

	@Test
	public void diagnosticsCountPluginColorsAndNameTheKeysOnlyInTheCopyableText() {
		WorkbenchEnvironment environment = new WorkbenchEnvironment("win32", "win32", "0.15.200", "4.41.0", "21");
		String text = WorkbenchDiagnostics.format(classify(environment, EngineProbe.usable()), true, false, "t", "a",
				true, java.util.List.of("org.eclipse.ui.workbench|HYPERLINK_COLOR", "x|y"),
				java.util.List.of("org.eclipse.ui.workbench|HOVER"), java.util.List.of());
		assertTrue(text.contains("Plug-in colors owned by this plugin: 2"));
		assertTrue(text.contains("user's (last sync this session): 1"));
		assertTrue(text.contains("released after an outside change (last sync this session): 0"));
		assertTrue(text.contains("  owned: org.eclipse.ui.workbench|HYPERLINK_COLOR"));
		assertTrue(WorkbenchDiagnostics.format(classify(environment, EngineProbe.usable()), true, false, "t", "a", true)
				.contains("Plug-in colors owned by this plugin: 0"));
	}
}
