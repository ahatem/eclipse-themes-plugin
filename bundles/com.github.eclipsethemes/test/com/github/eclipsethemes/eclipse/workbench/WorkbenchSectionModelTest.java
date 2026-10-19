package com.github.eclipsethemes.eclipse.workbench;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.github.eclipsethemes.core.workbench.EngineProbe;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.Reason;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.State;
import com.github.eclipsethemes.core.workbench.WorkbenchCapabilityClassifier;
import com.github.eclipsethemes.core.workbench.WorkbenchEnvironment;
import com.github.eclipsethemes.core.workbench.WorkbenchFamily;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome.EditorOutcome;

public class WorkbenchSectionModelTest {

	private static WorkbenchCapability capability(String themeBundle, EngineProbe probe) {
		return WorkbenchCapabilityClassifier.classify(
				new WorkbenchEnvironment("win32", "win32", themeBundle, "4.40.0", "21"), probe);
	}

	@Test
	public void verifiedEnablesTheCheckboxWithAVerifiedNote() {
		WorkbenchSectionModel model = WorkbenchSectionModel.of(capability("0.15.200.v1", EngineProbe.usable()), false,
				"org.eclipse.e4.ui.css.theme.e4_dark");
		assertTrue(model.checkboxEnabled());
		assertFalse(model.conflict());
		assertTrue(model.statusText().startsWith("Verified on win32/win32"));
		assertFalse(model.checkboxLabel().contains("experimental"));
	}

	@Test
	public void experimentalNamesTheExactCombinationAndIsLabelledExperimental() {
		WorkbenchSectionModel model = WorkbenchSectionModel.of(capability("0.15.300.v1", EngineProbe.usable()), false, null);
		assertTrue(model.checkboxEnabled());
		assertTrue(model.checkboxLabel().contains("experimental"));
		assertTrue(model.statusText().contains("win32/win32"));
		assertTrue(model.statusText().contains("0.15.300"));
		assertTrue(model.statusText().contains("4.40.0"));
	}

	@Test
	public void unavailableDisablesTheCheckboxAndStatesTheReason() {
		for (Reason reason : Reason.values()) {
			WorkbenchCapability unavailable = WorkbenchCapability.unavailable(reason,
					new WorkbenchEnvironment("win32", "win32", "0.15.200", "4.40.0", "21"));
			WorkbenchSectionModel model = WorkbenchSectionModel.of(unavailable, true, null);
			assertFalse(reason.name(), model.checkboxEnabled());
			assertTrue(reason.name(), model.statusText().startsWith("Unavailable"));
		}
		assertTrue(WorkbenchSectionModel.of(capability("0.15.200", new EngineProbe(false, true, true, true)), false, null)
				.statusText().contains("high-contrast"));
	}

	@Test
	public void anotherActiveThemeWhileEnabledIsReportedAsAConflict() {
		WorkbenchCapability verified = capability("0.15.200", EngineProbe.usable());
		assertTrue(WorkbenchSectionModel.of(verified, true, "org.eclipse.e4.ui.css.theme.e4_default").conflict());
		assertFalse(WorkbenchSectionModel.of(verified, true, WorkbenchFamily.DARK.alternateThemeId()).conflict());
		assertFalse(WorkbenchSectionModel.of(verified, false, "org.eclipse.e4.ui.css.theme.e4_default").conflict());
		assertTrue(WorkbenchSectionModel.of(verified, true, "org.eclipse.e4.ui.css.theme.e4_default").statusText()
				.contains("not the current effective state"));
	}

	@Test
	public void coverageLineDoesNotClaimTheWholeIde() {
		String coverage = WorkbenchSectionModel.COVERAGE.toLowerCase();
		assertFalse(coverage.contains("every pixel"));
		assertFalse(coverage.contains("entire ide"));
		assertTrue(coverage.contains("native"));
	}

	@Test
	public void summaryOnlyReadsAsCompleteSuccessWhenEveryRequestedPartSucceeded() {
		EditorOutcome ok = new EditorOutcome(List.of());
		assertEquals("✓ Applied (editors and workbench)",
				WorkbenchSectionModel.summary(new ThemeApplyOutcome(ok, WorkbenchOutcome.applied("x"))));
		assertEquals("✓ Applied (editors)",
				WorkbenchSectionModel.summary(new ThemeApplyOutcome(ok, WorkbenchOutcome.notRequested("off"))));

		String partial = WorkbenchSectionModel.summary(new ThemeApplyOutcome(ok,
				WorkbenchOutcome.failed(Reason.POSTCONDITION_FAILED, "Eclipse reports a different theme.")));
		assertFalse(partial.startsWith("✓"));
		assertTrue(partial.contains("Editor colors applied"));
		assertTrue(partial.contains("Eclipse reports a different theme."));

		String editorFailure = WorkbenchSectionModel.summary(
				new ThemeApplyOutcome(new EditorOutcome(List.of("JavaEditorThemeAdapter")), WorkbenchOutcome.applied("x")));
		assertFalse(editorFailure.startsWith("✓"));
		assertTrue(editorFailure.contains("JavaEditorThemeAdapter"));
	}

	@Test
	public void aggregateOutcomeIsFullySuccessfulOnlyWhenBothPartsSucceeded() {
		EditorOutcome ok = new EditorOutcome(List.of());
		EditorOutcome failed = new EditorOutcome(List.of("TextEditorThemeAdapter"));
		assertTrue(new ThemeApplyOutcome(ok, WorkbenchOutcome.applied("")).fullySucceeded());
		assertTrue(new ThemeApplyOutcome(ok, WorkbenchOutcome.notRequested("")).fullySucceeded());
		assertTrue(new ThemeApplyOutcome(ok, WorkbenchOutcome.relinquished("")).fullySucceeded());
		assertFalse(new ThemeApplyOutcome(failed, WorkbenchOutcome.applied("")).fullySucceeded());
		assertFalse(new ThemeApplyOutcome(ok, WorkbenchOutcome.yielded("")).fullySucceeded());
		assertFalse(new ThemeApplyOutcome(ok, WorkbenchOutcome.unavailable(Reason.NO_ENGINE, "")).fullySucceeded());
		assertFalse(new ThemeApplyOutcome(ok, WorkbenchOutcome.failed(null, "")).fullySucceeded());
		assertTrue(new ThemeApplyOutcome(ok, WorkbenchOutcome.failed(null, "")).editor().succeeded());
		assertEquals(State.UNAVAILABLE, capability("0.15.200", new EngineProbe(true, false, false, false)).state());
	}

	@Test
	public void keptUserColorsAreNamedAsACountAndADegradedWriteIsNeverASuccess() {
		EditorOutcome ok = new EditorOutcome(List.of());
		WorkbenchOutcome kept = WorkbenchOutcome.applied("x").withColors(
				new PluginColorSummary(List.of(), List.of("a|b", "c|d"), List.of(), List.of()));
		assertEquals("✓ Applied (editors and workbench); 2 plug-in colors kept your own values",
				WorkbenchSectionModel.summary(new ThemeApplyOutcome(ok, kept)));
		WorkbenchOutcome one = WorkbenchOutcome.applied("x").withColors(
				new PluginColorSummary(List.of(), List.of("a|b"), List.of(), List.of()));
		assertTrue(WorkbenchSectionModel.summary(new ThemeApplyOutcome(ok, one)).endsWith("1 plug-in color kept your own value"));

		WorkbenchOutcome degraded = WorkbenchOutcome.applied("Workbench theme set.").withColors(
				new PluginColorSummary(List.of(), List.of(), List.of(), List.of("flush failed")));
		assertEquals(WorkbenchOutcome.Status.DEGRADED, degraded.status());
		assertFalse(degraded.succeeded());
		String text = WorkbenchSectionModel.summary(new ThemeApplyOutcome(ok, degraded));
		assertFalse(text.startsWith("✓"));
		assertTrue(text.contains("flush failed"));
	}
}
