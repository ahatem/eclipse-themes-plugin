package com.github.eclipsethemes.core.exceptions;

import java.util.List;

import com.github.eclipsethemes.core.validation.ThemeDiagnostic;

public class ThemeParseException extends Exception {
    
	private static final long serialVersionUID = 6927093648359830646L;

	private final List<ThemeDiagnostic> diagnostics;

	public ThemeParseException(String message, Throwable cause) {
        this(message, cause, List.of());
    }

	/**
	 * @param diagnostics the error findings that refused the document; may be empty
	 *                    when the failure happened before validation started
	 */
	public ThemeParseException(String message, Throwable cause, List<ThemeDiagnostic> diagnostics) {
		super(message, cause);
		this.diagnostics = List.copyOf(diagnostics);
	}

	/** Error findings attributed to this refusal, in the order they were reported. */
	public List<ThemeDiagnostic> diagnostics() {
		return diagnostics;
	}
}
