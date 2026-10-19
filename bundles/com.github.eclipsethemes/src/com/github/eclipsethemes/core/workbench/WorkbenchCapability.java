package com.github.eclipsethemes.core.workbench;

import java.util.Objects;

/** Whether, and with what confidence, this workbench can be themed. UNAVAILABLE always carries a reason. */
public record WorkbenchCapability(State state, Reason reason, WorkbenchEnvironment environment) {

	public enum State {
		VERIFIED, EXPERIMENTAL, UNAVAILABLE
	}

	public enum Reason {
		NO_ENGINE, LINKAGE_ERROR, THEME_NOT_FOUND, HIGH_CONTRAST, POSTCONDITION_FAILED
	}

	public WorkbenchCapability {
		Objects.requireNonNull(state, "state");
		Objects.requireNonNull(environment, "environment");
		if ((state == State.UNAVAILABLE) != (reason != null)) {
			throw new IllegalArgumentException("Exactly the UNAVAILABLE state carries a reason");
		}
	}

	public static WorkbenchCapability unavailable(Reason reason, WorkbenchEnvironment environment) {
		return new WorkbenchCapability(State.UNAVAILABLE, Objects.requireNonNull(reason), environment);
	}

	public boolean usable() {
		return state != State.UNAVAILABLE;
	}
}
