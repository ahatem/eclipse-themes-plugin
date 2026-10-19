package com.github.eclipsethemes.core.workbench;

import java.util.List;

/**
 * The whole contract for preference keys of other plug-ins that this plugin may write: the keys, where they live and
 * how each is derived. Nothing outside this table is ever written to a foreign node.
 *
 * <p>
 * Provenance (Eclipse 4.40 and 4.41): every key is set by stock Eclipse Dark through an {@code IEclipsePreferences}
 * stylesheet bound to its own theme id. The workbench-node keys come from
 * {@code org.eclipse.ui.themes/css/dark/e4-dark_preferencestyle.css}; the JDT keys and the two Javadoc keys (JDT color
 * definitions that the color registry stores in the workbench node) from
 * {@code org.eclipse.jdt.ui/css/e4-dark_jdt_syntaxhighlighting.css}. These are other plug-ins' preference names, not
 * API, and are re-checked on each supported Eclipse release. Editor-surface keys are owned by the editor
 * integrations and must not appear here (a test enforces that).
 */
public final class PluginColorTable {

	static final String WORKBENCH = "org.eclipse.ui.workbench";
	static final String JDT = "org.eclipse.jdt.ui";

	private static final List<PluginColorKey> KEYS = List.of(
			// Popups: hover, information, content assist (workbench definitions).
			key(WORKBENCH, "org.eclipse.ui.workbench.HOVER_BACKGROUND", null, PluginColorSource.POPUP_BACKGROUND),
			key(WORKBENCH, "org.eclipse.ui.workbench.HOVER_FOREGROUND", null, PluginColorSource.FOREGROUND),
			key(WORKBENCH, "org.eclipse.ui.workbench.INFORMATION_BACKGROUND", null,
					PluginColorSource.POPUP_BACKGROUND),
			key(WORKBENCH, "org.eclipse.ui.workbench.INFORMATION_FOREGROUND", null, PluginColorSource.FOREGROUND),
			key(WORKBENCH, "CONTENT_ASSIST_BACKGROUND_COLOR", null, PluginColorSource.POPUP_BACKGROUND),
			key(WORKBENCH, "CONTENT_ASSIST_FOREGROUND_COLOR", null, PluginColorSource.FOREGROUND),
			// Javadoc hover: a JDT definition stored in the workbench node, so only when JDT is installed.
			key(WORKBENCH, "org.eclipse.jdt.ui.Javadoc.backgroundColor", JDT, PluginColorSource.POPUP_BACKGROUND),
			key(WORKBENCH, "org.eclipse.jdt.ui.Javadoc.foregroundColor", JDT, PluginColorSource.FOREGROUND),
			// Hyperlinks.
			key(WORKBENCH, "HYPERLINK_COLOR", null, PluginColorSource.LINK),
			key(WORKBENCH, "ACTIVE_HYPERLINK_COLOR", null, PluginColorSource.ACTIVE_LINK),
			// JDT content-assist popups and the Javadoc dark-mode flag (JDT node).
			key(JDT, "content_assist_proposals_background", JDT, PluginColorSource.POPUP_BACKGROUND),
			key(JDT, "content_assist_proposals_foreground", JDT, PluginColorSource.FOREGROUND),
			key(JDT, "content_assist_parameters_background", JDT, PluginColorSource.POPUP_BACKGROUND),
			key(JDT, "content_assist_parameters_foreground", JDT, PluginColorSource.FOREGROUND),
			key(JDT, "javadocElementsStyling.darkModeDefaultColors", JDT, PluginColorSource.DARK_FLAG));

	private PluginColorTable() {
	}

	private static PluginColorKey key(String node, String key, String requiredBundle, PluginColorSource source) {
		return new PluginColorKey(node, key, requiredBundle, source);
	}

	/** Every coordinated key, in the order values are written. */
	public static List<PluginColorKey> keys() {
		return KEYS;
	}
}
