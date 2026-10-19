package com.github.eclipsethemes.core.workbench;

import java.util.Arrays;
import java.util.Objects;

/**
 * Where the plugin runs. The theme-bundle version is the identity that matters: the measured table matches on
 * its major.minor.micro and ignores the build qualifier. The other fields are recorded for diagnostics.
 */
public record WorkbenchEnvironment(String os, String ws, String themeBundleVersion, String platformVersion,
		String javaVersion) {

	public WorkbenchEnvironment {
		os = Objects.requireNonNullElse(os, "unknown");
		ws = Objects.requireNonNullElse(ws, "unknown");
		themeBundleVersion = Objects.requireNonNullElse(themeBundleVersion, "unknown");
		platformVersion = Objects.requireNonNullElse(platformVersion, "unknown");
		javaVersion = Objects.requireNonNullElse(javaVersion, "unknown");
	}

	/** {@code major.minor.micro} of the theme bundle, without the build qualifier. */
	public String themeBundleMajorMinorMicro() {
		String[] parts = themeBundleVersion.split("[.]");
		return String.join(".", Arrays.copyOf(parts, Math.min(parts.length, 3)));
	}

	/** The identity of this combination, also used to remember an experimental opt-in. */
	public String combination() {
		return os + "/" + ws + "/" + themeBundleMajorMinorMicro();
	}
}
