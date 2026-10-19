package com.github.eclipsethemes.eclipse.workbench;

import com.github.eclipsethemes.core.workbench.WorkbenchCapability;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.State;
import com.github.eclipsethemes.core.workbench.WorkbenchEnvironment;
import com.github.eclipsethemes.core.workbench.WorkbenchSlots;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome;

/** What the Workbench section of the preference page shows; pure so the states can be tested without widgets. */
public record WorkbenchSectionModel(boolean checkboxEnabled, String checkboxLabel, String statusText,
		boolean conflict) {

	public static final String COVERAGE = "Coordinates core workbench surfaces (window, tabs, toolbars, trees, "
			+ "tables, status line). Native menus, dialogs, title bars, Browser content and icons may not change.";

	/**
	 * @param enabled the stored coordination intent
	 * @param activeThemeId the active Eclipse theme id, or null when unknown
	 */
	public static WorkbenchSectionModel of(WorkbenchCapability capability, boolean enabled, String activeThemeId) {
		WorkbenchEnvironment env = capability.environment();
		boolean conflict = enabled && capability.usable() && activeThemeId != null
				&& !WorkbenchSlots.isOurs(activeThemeId);
		String label = "Also theme the Eclipse workbench";
		String status;
		switch (capability.state()) {
		case VERIFIED:
			status = "Verified on " + env.os() + "/" + env.ws() + " with theme engine " + env.themeBundleMajorMinorMicro()
					+ ".";
			break;
		case EXPERIMENTAL:
			label = "Also theme the Eclipse workbench (experimental)";
			status = "Not yet verified on " + env.os() + "/" + env.ws() + " with theme engine "
					+ env.themeBundleMajorMinorMicro() + " (Eclipse " + env.platformVersion() + "). Checking this box is an "
					+ "experiment on that combination; the plugin reverts when it safely can.";
			break;
		default:
			status = unavailableText(capability);
			break;
		}
		if (conflict) {
			status += " Coordination is not the current effective state: another Eclipse theme was chosen on the "
					+ "Appearance page.";
		}
		return new WorkbenchSectionModel(capability.state() != State.UNAVAILABLE, label, status, conflict);
	}

	private static String unavailableText(WorkbenchCapability capability) {
		return switch (capability.reason()) {
		case NO_ENGINE -> "Unavailable: the Eclipse theme engine was not found.";
		case LINKAGE_ERROR -> "Unavailable: the Eclipse theme engine is not compatible with this plugin.";
		case THEME_NOT_FOUND -> "Unavailable: the plugin's workbench themes were not registered by Eclipse.";
		case HIGH_CONTRAST -> "Unavailable: the operating system is in high-contrast mode, which is left untouched.";
		case POSTCONDITION_FAILED -> "Unavailable for this session: a previous attempt on "
				+ capability.environment().combination() + " did not complete.";
		};
	}

	/** The one-line result shown after Apply or OK; never reads as complete success when a part did not succeed. */
	public static String summary(ThemeApplyOutcome outcome) {
		if (outcome.fullySucceeded()) {
			String line = switch (outcome.workbench().status()) {
			case APPLIED -> "✓ Applied (editors and workbench)";
			case RELINQUISHED -> "✓ Applied (editors); workbench handed back";
			default -> "✓ Applied (editors)";
			};
			int kept = outcome.workbench().colors().userKeys().size();
			return kept == 0 ? line : line + "; " + kept + " plug-in " + (kept == 1 ? "color kept your own value"
					: "colors kept your own values");
		}
		StringBuilder text = new StringBuilder();
		if (!outcome.editor().succeeded()) {
			text.append("Editor colors were not fully applied (").append(String.join(", ", outcome.editor().failedAdapters()))
					.append("). ");
		} else {
			text.append("Editor colors applied. ");
		}
		if (!outcome.workbench().succeeded()) {
			text.append("Workbench: ").append(outcome.workbench().message());
		}
		return text.toString().trim();
	}
}
