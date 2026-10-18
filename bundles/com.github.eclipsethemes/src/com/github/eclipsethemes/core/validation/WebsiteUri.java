package com.github.eclipsethemes.core.validation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * The single policy for the optional {@code website} theme attribute. It is pure
 * Java so the parser and the preference page agree without needing a display.
 */
public final class WebsiteUri {

	private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

	public enum Status {
		/** The attribute is not present at all. */
		ABSENT,
		/** The value is an absolute URI whose scheme may be launched. */
		LAUNCHABLE,
		/** The attribute is present but holds only whitespace. */
		BLANK,
		/** The value is not an absolute URI, so no scheme was recognised. */
		RELATIVE,
		/** The value cannot be parsed as a URI. */
		MALFORMED,
		/** The value contains characters that must never reach an external launch. */
		CONTROL_CHARACTERS,
		/** The value is an absolute URI with a scheme outside the allow-list. */
		DISALLOWED_SCHEME
	}

	/**
	 * @param status classification of the raw value
	 * @param value  the value with surrounding whitespace removed
	 * @param scheme the scheme as written, or {@code null} when none was recognised
	 */
	public record Check(Status status, String value, String scheme) {
		public boolean launchable() {
			return status == Status.LAUNCHABLE;
		}

		public boolean present() {
			return status != Status.ABSENT;
		}
	}

	private WebsiteUri() {
	}

	/** True only when the value is an absolute {@code http}/{@code https} URI. */
	public static boolean isLaunchable(String website) {
		return check(website).launchable();
	}

	/**
	 * Literal display text for a value, escaped so a markup-rendering widget
	 * cannot interpret the value itself as link markup.
	 */
	public static String displayText(String website) {
		String value = check(website).value();
		if (value.indexOf('<') < 0 && value.indexOf('>') < 0) {
			return value;
		}
		return value.replace("<", "&lt;").replace(">", "&gt;");
	}

	public static Check check(String website) {
		if (website == null) {
			return new Check(Status.ABSENT, "", null);
		}
		String value = website.strip();
		if (value.isEmpty()) {
			return new Check(Status.BLANK, value, null);
		}
		for (int i = 0; i < value.length(); i++) {
			if (Character.isISOControl(value.charAt(i))) {
				return new Check(Status.CONTROL_CHARACTERS, value, null);
			}
		}
		URI uri;
		try {
			uri = new URI(value);
		} catch (URISyntaxException e) {
			return new Check(Status.MALFORMED, value, null);
		}
		String scheme = uri.getScheme();
		if (scheme == null || !uri.isAbsolute()) {
			return new Check(Status.RELATIVE, value, null);
		}
		return new Check(ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))
				? Status.LAUNCHABLE
				: Status.DISALLOWED_SCHEME, value, scheme);
	}
}
