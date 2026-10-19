package com.github.eclipsethemes.core.workbench;

import java.util.List;
import java.util.Objects;

/** The small, local-only diagnostics block users can copy into a bug report. */
public final class WorkbenchDiagnostics {

	private WorkbenchDiagnostics() {
	}

	public static String format(WorkbenchCapability capability, boolean coordinationEnabled, boolean optedIn,
			String selectedTheme, String activeWorkbenchTheme, boolean owned) {
		return format(capability, coordinationEnabled, optedIn, selectedTheme, activeWorkbenchTheme, owned, List.of(),
				List.of(), List.of());
	}

	/**
	 * @param ownedColors    coordinated plug-in keys this plugin currently owns ({@code node|key} ids)
	 * @param userColors     keys skipped at the last synchronization because the user already has a value
	 * @param releasedColors keys released at the last synchronization because someone else changed them
	 */
	public static String format(WorkbenchCapability capability, boolean coordinationEnabled, boolean optedIn,
			String selectedTheme, String activeWorkbenchTheme, boolean owned, List<String> ownedColors,
			List<String> userColors, List<String> releasedColors) {
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
		text.append("Plug-in colors owned by this plugin: ").append(ownedColors.size()).append('\n');
		text.append("Plug-in colors kept as the user's (last sync this session): ").append(userColors.size())
				.append('\n');
		text.append("Plug-in colors released after an outside change (last sync this session): ")
				.append(releasedColors.size()).append('\n');
		appendKeys(text, "  owned", ownedColors);
		appendKeys(text, "  user-held", userColors);
		appendKeys(text, "  released", releasedColors);
		text.append("OS: ").append(env.os()).append('\n');
		text.append("Windowing system: ").append(env.ws()).append('\n');
		text.append("Theme engine bundle (org.eclipse.e4.ui.css.swt.theme): ").append(env.themeBundleVersion())
				.append('\n');
		text.append("Eclipse platform: ").append(env.platformVersion()).append('\n');
		text.append("Java: ").append(env.javaVersion()).append('\n');
		return text.toString();
	}

	private static void appendKeys(StringBuilder text, String label, List<String> keys) {
		for (String key : keys) {
			text.append(label).append(": ").append(key).append('\n');
		}
	}
}
