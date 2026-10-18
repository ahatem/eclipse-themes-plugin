package com.github.eclipsethemes.core.workbench;

import java.util.Set;

import com.github.eclipsethemes.core.workbench.WorkbenchCapability.Reason;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.State;

/**
 * Classifies a probe result. The measured table is confidence metadata, never an execution allow-list:
 * a combination that is not in it is EXPERIMENTAL, not refused.
 */
public final class WorkbenchCapabilityClassifier {

	/** {@code os/ws/theme-bundle major.minor.micro} combinations measured in R7 (Eclipse 4.40 and 4.41). */
	static final Set<String> VERIFIED = Set.of("win32/win32/0.15.100", "win32/win32/0.15.200");

	private WorkbenchCapabilityClassifier() {
	}

	public static WorkbenchCapability classify(WorkbenchEnvironment environment, EngineProbe probe) {
		if (probe.linkageError()) {
			return WorkbenchCapability.unavailable(Reason.LINKAGE_ERROR, environment);
		}
		if (!probe.engineAvailable()) {
			return WorkbenchCapability.unavailable(Reason.NO_ENGINE, environment);
		}
		if (probe.highContrast()) {
			return WorkbenchCapability.unavailable(Reason.HIGH_CONTRAST, environment);
		}
		if (!probe.ownThemesPresent()) {
			return WorkbenchCapability.unavailable(Reason.THEME_NOT_FOUND, environment);
		}
		return new WorkbenchCapability(VERIFIED.contains(environment.combination()) ? State.VERIFIED : State.EXPERIMENTAL,
				null, environment);
	}
}
