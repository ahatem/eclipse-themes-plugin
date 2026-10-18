package com.github.eclipsethemes.core.validation;

import java.util.Objects;

/**
 * One validation finding produced where a theme document is read. Findings are
 * internal to the bundle: they carry no numeric code registry and are never
 * exported as API.
 *
 * @param severity  whether the finding refuses the document or only reports it
 * @param kind      stable machine-readable category, for example {@link #INVALID_COLOR}
 * @param message   human-readable description naming the offending element and value
 * @param source    file the document came from, or {@code null} when it was read from a stream
 * @param element   XML element involved, or {@code null} when the finding is not element-specific
 * @param attribute XML attribute involved, or {@code null} when no attribute is involved
 */
public record ThemeDiagnostic(Severity severity, String kind, String message, String source,
		String element, String attribute) {

	public enum Severity {
		WARNING,
		ERROR
	}

	public static final String ROOT_ELEMENT = "root-element";
	public static final String NAME = "name";
	public static final String TYPE = "type";
	public static final String WEBSITE = "website";
	public static final String UNKNOWN_ELEMENT = "unknown-element";
	public static final String MISSING_COLOR = "missing-color";
	public static final String INVALID_COLOR = "invalid-color";
	public static final String STYLE_FLAG = "style-flag";
	public static final String NESTED_ELEMENTS = "nested-elements";
	public static final String MAPPING_CONFLICT = "mapping-conflict";

	public ThemeDiagnostic {
		Objects.requireNonNull(severity, "severity");
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(message, "message");
	}

	public boolean error() {
		return severity == Severity.ERROR;
	}
}
