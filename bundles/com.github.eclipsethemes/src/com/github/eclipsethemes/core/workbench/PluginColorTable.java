package com.github.eclipsethemes.core.workbench;

import static com.github.eclipsethemes.core.workbench.PluginColorSource.ACCENT;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.ACTIVE_ACCENT;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.BACKGROUND;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.BORDER;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.DARK_FLAG;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.ERROR;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.ERROR_TINT;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.ERROR_TINT_SOFT;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.FOREGROUND;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.INFO;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.INFO_TINT;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.MUTED;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.OFF_FLAG;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.RAISED;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.SUCCESS;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.SUCCESS_TINT;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.SUCCESS_TINT_SOFT;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.WARNING;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.WARNING_TINT;
import static com.github.eclipsethemes.core.workbench.PluginColorSource.WARNING_TINT_SOFT;

import java.util.List;

/**
 * The whole contract for preference keys of other plug-ins that this plugin may write: the keys, where they live and
 * how each is derived. Nothing outside this table is ever written to a foreign node.
 *
 * <p>
 * Provenance (Eclipse 4.40 and 4.41): every key is one that stock Eclipse Dark sets through an
 * {@code IEclipsePreferences} stylesheet bound to its own theme id (inventory in
 * {@code openspec/changes/plugin-color-coordination/evidence/key-classification.md}), and whose Eclipse default is
 * unreadable or clashing on a dark workbench (default colors black, blue, light pastel; checked against the owning
 * plug-in's color definition or preference initializer). These are other plug-ins' preference names, not API, and
 * are re-checked on each supported Eclipse release. Syntax and editor-surface keys are owned by the editor
 * integrations and must not appear here (a test enforces that).
 */
public final class PluginColorTable {

	static final String WORKBENCH = "org.eclipse.ui.workbench";
	static final String EDITORS = "org.eclipse.ui.editors";
	static final String JDT = "org.eclipse.jdt.ui";
	static final String DEBUG = "org.eclipse.debug.ui";
	static final String ANT = "org.eclipse.ant.ui";
	static final String EGIT = "org.eclipse.egit.ui";
	static final String COMPARE = "org.eclipse.compare";
	static final String SEARCH = "org.eclipse.search";
	static final String ECLEMMA = "org.eclipse.eclemma.ui";
	static final String MYLYN_TASKS = "org.eclipse.mylyn.tasks.ui";
	static final String MPC = "org.eclipse.epp.mpc.ui";

	private static final List<PluginColorKey> KEYS = List.of(
			// 1. Popups: hover, information, content assist (workbench definitions).
			// Javadoc hover: a JDT definition stored in the workbench node, so only when JDT is installed. It defaults to
			// the information colors, and the color registry leaves it stale if the information key is removed first, so
			// it must stay ahead of them: keys are released in this order.
			key(WORKBENCH, "org.eclipse.jdt.ui.Javadoc.backgroundColor", JDT, RAISED),
			key(WORKBENCH, "org.eclipse.jdt.ui.Javadoc.foregroundColor", JDT, FOREGROUND),
			key(WORKBENCH, "org.eclipse.ui.workbench.HOVER_BACKGROUND", null, RAISED),
			key(WORKBENCH, "org.eclipse.ui.workbench.HOVER_FOREGROUND", null, FOREGROUND),
			key(WORKBENCH, "org.eclipse.ui.workbench.INFORMATION_BACKGROUND", null, RAISED),
			key(WORKBENCH, "org.eclipse.ui.workbench.INFORMATION_FOREGROUND", null, FOREGROUND),
			key(WORKBENCH, "CONTENT_ASSIST_BACKGROUND_COLOR", null, RAISED),
			key(WORKBENCH, "CONTENT_ASSIST_FOREGROUND_COLOR", null, FOREGROUND),
			// Hyperlinks.
			key(WORKBENCH, "HYPERLINK_COLOR", null, ACCENT),
			key(WORKBENCH, "ACTIVE_HYPERLINK_COLOR", null, ACTIVE_ACCENT),
			// JDT content-assist popups and the Javadoc dark-mode flag (JDT node).
			key(JDT, "content_assist_proposals_background", JDT, RAISED),
			key(JDT, "content_assist_proposals_foreground", JDT, FOREGROUND),
			key(JDT, "content_assist_parameters_background", JDT, RAISED),
			key(JDT, "content_assist_parameters_foreground", JDT, FOREGROUND),
			key(JDT, "javadocElementsStyling.darkModeDefaultColors", JDT, DARK_FLAG),

			// 2. Platform status colors, EGit and Compare.
			key(WORKBENCH, "ERROR_COLOR", null, ERROR),
			key(WORKBENCH, "org.eclipse.ui.workbench.FORM_HEADING_ERROR_COLOR", null, ERROR),
			key(WORKBENCH, "org.eclipse.ui.workbench.FORM_HEADING_WARNING_COLOR", null, WARNING),
			key(WORKBENCH, "org.eclipse.ui.workbench.FORM_HEADING_INFO_COLOR", null, MUTED),
			key(WORKBENCH, "INCOMING_COLOR", COMPARE, INFO),
			key(WORKBENCH, "OUTGOING_COLOR", COMPARE, FOREGROUND),
			key(WORKBENCH, "CONFLICTING_COLOR", COMPARE, ERROR),
			key(WORKBENCH, "RESOLVED_COLOR", COMPARE, SUCCESS),
			key(WORKBENCH, "EDITION_COLOR", COMPARE, FOREGROUND),
			key(WORKBENCH, "org.eclipse.egit.ui.CommitMessageCommentColor", EGIT, MUTED),
			key(WORKBENCH, "org.eclipse.egit.ui.DiffAddBackgroundColor", EGIT, SUCCESS_TINT),
			key(WORKBENCH, "org.eclipse.egit.ui.DiffAddForegroundColor", EGIT, FOREGROUND),
			key(WORKBENCH, "org.eclipse.egit.ui.DiffRemoveBackgroundColor", EGIT, ERROR_TINT),
			key(WORKBENCH, "org.eclipse.egit.ui.DiffRemoveForegroundColor", EGIT, FOREGROUND),
			key(WORKBENCH, "org.eclipse.egit.ui.DiffHunkBackgroundColor", EGIT, INFO_TINT),
			key(WORKBENCH, "org.eclipse.egit.ui.DiffHunkForegroundColor", EGIT, FOREGROUND),
			key(WORKBENCH, "org.eclipse.egit.ui.DiffHeadlineBackgroundColor", EGIT, RAISED),
			key(WORKBENCH, "org.eclipse.egit.ui.DiffHeadlineForegroundColor", EGIT, FOREGROUND),
			key(WORKBENCH, "org.eclipse.egit.ui.IgnoredResourceBackgroundColor", EGIT, BACKGROUND),
			key(WORKBENCH, "org.eclipse.egit.ui.IgnoredResourceForegroundColor", EGIT, MUTED),
			key(WORKBENCH, "org.eclipse.egit.ui.UncommittedChangeBackgroundColor", EGIT, BACKGROUND),
			key(WORKBENCH, "org.eclipse.egit.ui.UncommittedChangeForegroundColor", EGIT, ACCENT),

			// 3. Search, revision, annotations and sticky lines that the editor integrations do not manage.
			key(WORKBENCH, "org.eclipse.search.ui.match.highlight", SEARCH, WARNING_TINT),
			key(WORKBENCH, "org.eclipse.jface.REVISION_NEWEST_COLOR", null, WARNING_TINT),
			key(WORKBENCH, "org.eclipse.jface.REVISION_OLDEST_COLOR", null, WARNING_TINT_SOFT),
			key(EDITORS, "hyperlinkColor", null, ACCENT),
			key(EDITORS, "hyperlinkColor.SystemDefault", null, OFF_FLAG),
			key(EDITORS, "linked.slave.color", null, INFO_TINT),
			key(EDITORS, "breakpointIndicationColor", DEBUG, INFO),
			key(EDITORS, "fullcoverage_color", ECLEMMA, SUCCESS_TINT),
			key(EDITORS, "partialcoverage_color", ECLEMMA, WARNING_TINT),
			key(EDITORS, "nocoverage_color", ECLEMMA, ERROR_TINT),
			key(EDITORS, "org.eclipse.ui.editors.stickyLinesSeparatorColor", null, BORDER),
			// Inline compare highlights; the keys exist from Eclipse 4.41 and are inert on 4.40.
			key(EDITORS, "inlineDiffColor", COMPARE, SUCCESS_TINT_SOFT),
			key(EDITORS, "detailedInlineDiffColor", COMPARE, SUCCESS_TINT),
			key(EDITORS, "deletionInlineDiffColor", COMPARE, ERROR_TINT_SOFT),
			key(EDITORS, "detailedDeletionInlineDiffColor", COMPARE, ERROR_TINT),

			// 4. JDT, Debug and Ant auxiliary colors.
			key(WORKBENCH, "org.eclipse.jdt.ui.ColoredLabels.inherited", JDT, MUTED),
			key(WORKBENCH, "org.eclipse.jdt.ui.ColoredLabels.writeaccess_highlight", JDT, WARNING_TINT),
			key(DEBUG, "org.eclipse.debug.ui.PREF_CHANGED_VALUE_BACKGROUND", DEBUG, WARNING_TINT),
			key(DEBUG, "org.eclipse.debug.ui.MemoryHistoryKnownColor", DEBUG, FOREGROUND),
			key(DEBUG, "org.eclipse.debug.ui.changedDebugElement", DEBUG, ERROR),
			key(ANT, "org.eclipse.ant.ui.informationColor", ANT, INFO),
			key(ANT, "org.eclipse.ant.ui.debugColor", ANT, FOREGROUND),
			key(ANT, "org.eclipse.ant.ui.errorColor", ANT, ERROR),

			// 5. Mylyn tasks and the Marketplace.
			key(WORKBENCH, "org.eclipse.mylyn.tasks.ui.colors.foreground.today.scheduled", MYLYN_TASKS, INFO),
			key(WORKBENCH, "org.eclipse.mylyn.tasks.ui.colors.foreground.past.scheduled", MYLYN_TASKS, INFO),
			key(WORKBENCH, "org.eclipse.mylyn.tasks.ui.colors.foreground.thisweek.scheduled", MYLYN_TASKS, MUTED),
			key(WORKBENCH, "org.eclipse.mylyn.tasks.ui.colors.foreground.task.active", MYLYN_TASKS, ACCENT),
			key(WORKBENCH, "org.eclipse.mylyn.tasks.ui.colors.category.gradient.start", MYLYN_TASKS, RAISED),
			key(WORKBENCH, "org.eclipse.mylyn.tasks.ui.colors.category.gradient.end", MYLYN_TASKS, RAISED),
			key(WORKBENCH, "org.eclipse.mylyn.tasks.ui.color.text.quoted", MYLYN_TASKS, INFO),
			key(MPC, "native-borders", MPC, OFF_FLAG));

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
