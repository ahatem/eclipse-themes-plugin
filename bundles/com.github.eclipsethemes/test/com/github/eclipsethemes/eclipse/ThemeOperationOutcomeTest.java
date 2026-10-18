package com.github.eclipsethemes.eclipse;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;

/** Outcome reporting contract used by the import and update dialogs. */
public class ThemeOperationOutcomeTest {

	private static ThemeDiagnostic finding(ThemeDiagnostic.Severity severity, String message) {
		return new ThemeDiagnostic(severity, ThemeDiagnostic.TYPE, message, null, "colorTheme", "type");
	}

	@Test public void committedOutcomeCarriesTheThemeAndItsWarnings() {
		Theme theme = new Theme("id", "Name", "Author", "", "", null, ThemeType.LIGHT);

		ThemeOperationOutcome outcome = ThemeOperationOutcome.committed(theme,
				List.of(finding(ThemeDiagnostic.Severity.WARNING, "warned")));

		assertTrue(outcome.successful());
		assertSame(theme, outcome.theme());
		assertEquals(1, outcome.diagnostics().size());
	}

	@Test public void refusedOutcomeCarriesNoTheme() {
		ThemeOperationOutcome outcome = ThemeOperationOutcome.refused(
				List.of(finding(ThemeDiagnostic.Severity.ERROR, "root is wrong")));

		assertFalse(outcome.successful());
		assertNull(outcome.theme());
		assertEquals("root is wrong", outcome.describeFailure());
	}

	@Test public void describeFailurePrefersTheErrorOverWarnings() {
		ThemeOperationOutcome outcome = ThemeOperationOutcome.refused(List.of(
				finding(ThemeDiagnostic.Severity.WARNING, "warned"),
				finding(ThemeDiagnostic.Severity.ERROR, "refused")));

		assertEquals("refused", outcome.describeFailure());
	}

	@Test public void describeFailureIsNeutralWhenNoFindingExplainsTheRefusal() {
		String described = ThemeOperationOutcome.refused(List.of()).describeFailure();

		assertTrue(described, described.contains("could not be read or validated"));
		assertTrue("The neutral text states that nothing changed: " + described,
				described.contains("Nothing in the catalog was changed"));
	}

	@Test public void diagnosticsAreDefensivelyCopied() {
		var mutable = new java.util.ArrayList<ThemeDiagnostic>();
		mutable.add(finding(ThemeDiagnostic.Severity.WARNING, "warned"));
		ThemeOperationOutcome outcome = ThemeOperationOutcome.refused(mutable);
		mutable.clear();

		assertEquals(1, outcome.diagnostics().size());
	}
}
