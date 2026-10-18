package com.github.eclipsethemes.core.workbench;

import java.util.Objects;

/** The small, local-only diagnostics block users can copy into a bug report. */
public final class WorkbenchDiagnostics {

	private WorkbenchDiagnostics() {
	}

	public static String format(WorkbenchCapability capability, boolean coordinationEnabled, boolean optedIn,
			String selectedTheme, String activeWorkbenchTheme, boolean owned) {
		WorkbenchEnvironment env = capability.environment();
		StringBuilder text = new StringBuilder();
		text.append("Eclipse Themes workbench diagnostics\n");
		text.append("Capability: ").append(capability.state());
		if (capability.reason() != null) {
			text.append(" (").append(capability.reason()).append(')');
		}
		text.append('\n');
		text.append("Coordination enabled: ").append(coordinationEnabled).append('\n');
		text.append("Experimental opt-in for this combination: ").append(optedIn).append('\n');
		text.append("Selected theme: ").append(Objects.requireNonNullElse(selectedTheme, "none")).append('\n');
		text.append("Active workbench theme: ").append(Objects.requireNonNullElse(activeWorkbenchTheme, "unknown"))
				.append('\n');
		text.append("Workbench theme owned by this plugin: ").append(owned).append('\n');
		text.append("OS: ").append(env.os()).append('\n');
		text.append("Windowing system: ").append(env.ws()).append('\n');
		text.append("Theme engine bundle (org.eclipse.e4.ui.css.swt.theme): ").append(env.themeBundleVersion())
				.append('\n');
		text.append("Eclipse platform: ").append(env.platformVersion()).append('\n');
		text.append("Java: ").append(env.javaVersion()).append('\n');
		return text.toString();
	}
}
