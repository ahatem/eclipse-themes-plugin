package com.github.eclipsethemes.eclipse;

import java.util.List;
import java.util.Objects;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;

/**
 * Admission result of one import or update attempt. The findings are the
 * validation findings of the candidate document; the theme is present only when
 * the attempt committed.
 */
public record ThemeOperationOutcome(Theme theme, List<ThemeDiagnostic> diagnostics) {

	private static final String REFUSED =
			"The theme file could not be read or validated. Nothing in the catalog was changed.";

	public ThemeOperationOutcome {
		diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
	}

	public static ThemeOperationOutcome committed(Theme theme, List<ThemeDiagnostic> diagnostics) {
		return new ThemeOperationOutcome(Objects.requireNonNull(theme, "theme"), diagnostics);
	}

	public static ThemeOperationOutcome refused(List<ThemeDiagnostic> diagnostics) {
		return new ThemeOperationOutcome(null, diagnostics);
	}

	public boolean successful() {
		return theme != null;
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
