package com.github.eclipsethemes.eclipse;

import java.util.List;
import java.util.Objects;

import com.github.eclipsethemes.core.validation.ThemeDiagnostic;

/**
 * Admission result of one import or update attempt. The findings are the
 * validation findings of the candidate document; the record is present only
 * when the attempt committed.
 */
public record ThemeOperationOutcome(ThemeRecord record, List<ThemeDiagnostic> diagnostics) {

	private static final String REFUSED =
			"The theme file could not be read or validated. Nothing in the catalog was changed.";

	public ThemeOperationOutcome {
		diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
	}

	public static ThemeOperationOutcome committed(ThemeRecord record, List<ThemeDiagnostic> diagnostics) {
		return new ThemeOperationOutcome(Objects.requireNonNull(record, "record"), diagnostics);
	}

	public static ThemeOperationOutcome refused(List<ThemeDiagnostic> diagnostics) {
		return new ThemeOperationOutcome(null, diagnostics);
	}

	public boolean successful() {
		return record != null;
	}

	/** The first error finding, or a neutral description when none was produced. */
	public String describeFailure() {
		return diagnostics.stream()
				.filter(ThemeDiagnostic::error)
				.map(ThemeDiagnostic::message)
				.findFirst()
				.orElse(REFUSED);
	}
}
