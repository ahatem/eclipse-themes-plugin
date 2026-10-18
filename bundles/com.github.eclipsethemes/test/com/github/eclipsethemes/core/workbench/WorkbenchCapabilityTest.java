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

	@Test
	public void measuredCombinationsAreVerifiedIgnoringTheBuildQualifier() {
		assertEquals(State.VERIFIED,
				classify(env("win32", "win32", "0.15.100.v20260507-0340"), EngineProbe.usable()).state());
		assertEquals(State.VERIFIED,
				classify(env("win32", "win32", "0.15.200.v20260801-1200"), EngineProbe.usable()).state());
		assertEquals(State.VERIFIED, classify(env("win32", "win32", "0.15.200"), EngineProbe.usable()).state());
	}

	@Test
	public void aNewerMicroVersionIsExperimentalNotVerified() {
		WorkbenchCapability capability = classify(env("win32", "win32", "0.15.300.v1"), EngineProbe.usable());
		assertEquals(State.EXPERIMENTAL, capability.state());
		assertNull(capability.reason());
		assertTrue(capability.usable());
	}

	@Test
	public void otherPlatformsAndMinorVersionsAreExperimental() {
		assertEquals(State.EXPERIMENTAL, classify(env("linux", "gtk", "0.15.200.v1"), EngineProbe.usable()).state());
		assertEquals(State.EXPERIMENTAL, classify(env("macosx", "cocoa", "0.15.200.v1"), EngineProbe.usable()).state());
		assertEquals(State.EXPERIMENTAL, classify(env("win32", "win32", "0.16.0.v1"), EngineProbe.usable()).state());
	}

	@Test
	public void theMeasuredTableIsConfidenceNotAnAllowList() {
		WorkbenchCapability unknown = classify(env("solaris", "motif", "9.9.9"), EngineProbe.usable());
		assertTrue(unknown.usable());
		assertEquals(State.EXPERIMENTAL, unknown.state());
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
	public void environmentNormalizesTheVersionAndNamesTheCombination() {
		WorkbenchEnvironment environment = env("win32", "win32", "0.15.200.v20260801-1200");
		assertEquals("0.15.200", environment.themeBundleMajorMinorMicro());
		assertEquals("win32/win32/0.15.200", environment.combination());
		assertEquals("0.15", env("a", "b", "0.15").themeBundleMajorMinorMicro());
		assertEquals("unknown", new WorkbenchEnvironment(null, null, null, null, null).os());
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
}
