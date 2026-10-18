package com.github.eclipsethemes.eclipse.workbench;

import java.util.Objects;

import com.github.eclipsethemes.core.workbench.WorkbenchCapability.Reason;

/** The workbench half of an application result. */
public record WorkbenchOutcome(Status status, Reason reason, String message) {

	public enum Status {
		/** Coordination is off, or an experimental combination has not been opted into. */
		NOT_REQUESTED,
		/** The workbench theme carries the selected theme's palette. */
		APPLIED,
		/** Coordination was turned off and the workbench was handed back. */
		RELINQUISHED,
		/** The plugin deliberately changed nothing because a newer choice or unknown prior state made that unsafe. */
		YIELDED,
		/** The capability gate refused. */
		UNAVAILABLE,
		/** An attempt failed; the message says what was and was not reverted. */
		FAILED
	}

	public WorkbenchOutcome {
		Objects.requireNonNull(status, "status");
		message = Objects.requireNonNullElse(message, "");
	}

	public static WorkbenchOutcome notRequested(String message) {
		return new WorkbenchOutcome(Status.NOT_REQUESTED, null, message);
	}

	public static WorkbenchOutcome applied(String message) {
		return new WorkbenchOutcome(Status.APPLIED, null, message);
	}

	public static WorkbenchOutcome relinquished(String message) {
		return new WorkbenchOutcome(Status.RELINQUISHED, null, message);
	}

	public static WorkbenchOutcome yielded(String message) {
		return new WorkbenchOutcome(Status.YIELDED, null, message);
	}

	public static WorkbenchOutcome unavailable(Reason reason, String message) {
		return new WorkbenchOutcome(Status.UNAVAILABLE, reason, message);
	}

	public static WorkbenchOutcome failed(Reason reason, String message) {
		return new WorkbenchOutcome(Status.FAILED, reason, message);
	}

	/** True when nothing was requested or what was requested happened. */
	public boolean succeeded() {
		return status == Status.NOT_REQUESTED || status == Status.APPLIED || status == Status.RELINQUISHED;
	}
}
