package com.github.eclipsethemes.eclipse.fonts;

import java.util.Objects;

/** The font part of an application result. */
public record FontOutcome(Status status, String message) {

	public enum Status {
		/** Theme fonts are off, or were off and the plugin owns nothing. */
		NOT_REQUESTED,
		/** Theme fonts are on but the theme asks for none. */
		NOTHING_TO_APPLY,
		/** Every requested font was applied. */
		APPLIED,
		/** Some requested font was not applied; the message says which and why. */
		INCOMPLETE,
		/** Theme fonts were turned off and the plugin's fonts were handed back. */
		RELEASED,
		/** Writing or saving fonts failed. */
		FAILED
	}

	public static final FontOutcome NOT_REQUESTED = new FontOutcome(Status.NOT_REQUESTED, "");

	public FontOutcome {
		Objects.requireNonNull(status, "status");
		message = Objects.requireNonNullElse(message, "");
	}

	/** True when nothing was requested or everything requested happened. */
	public boolean succeeded() {
		return status != Status.INCOMPLETE && status != Status.FAILED;
	}
}
