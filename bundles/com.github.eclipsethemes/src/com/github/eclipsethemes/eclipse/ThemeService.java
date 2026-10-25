package com.github.eclipsethemes.eclipse;

import java.util.function.Function;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome.EditorOutcome;
import com.github.eclipsethemes.eclipse.fonts.FontOutcome;
import com.github.eclipsethemes.eclipse.fonts.ThemeFontCoordinator;
import com.github.eclipsethemes.eclipse.preferences.EditorPreferenceOwnership;
import com.github.eclipsethemes.eclipse.preferences.EditorPreferenceWriter;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchCoordinator;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchOutcome;

public final class ThemeService {

	/** Writes the editor-side preferences for a theme and reports failures and preserved external changes. */
	interface EditorWriter {
		EditorOutcome write(Theme theme);
	}

	private final EditorWriter editor;
	private final WorkbenchCoordinator workbench;
	private final Function<Theme, FontOutcome> fonts;
	private final ILog log;

	public ThemeService(AdapterRegistry registry, WorkbenchCoordinator workbench, ThemeFontCoordinator fonts,
			ILog log, IPreferenceStore store) {
		this(theme -> new EditorPreferenceWriter(registry.getAdapters(), InstanceScope.INSTANCE::getNode,
				new EditorPreferenceOwnership(store), log).write(theme), workbench, fonts::apply, log);
	}

	ThemeService(EditorWriter editor, WorkbenchCoordinator workbench, ILog log) {
		this(editor, workbench, theme -> FontOutcome.NOT_REQUESTED, log);
	}

	ThemeService(EditorWriter editor, WorkbenchCoordinator workbench, Function<Theme, FontOutcome> fonts, ILog log) {
		this.editor = editor;
		this.workbench = workbench;
		this.fonts = fonts;
		this.log = log;
	}

	/**
	 * Applies the workbench half, then the editor preferences, then the optional fonts; each part reports for itself.
	 *
	 * <p>
	 * The order is deliberate. Switching the Eclipse CSS theme makes the theme engine synchronously reset the
	 * preference defaults the previous theme overrode, and that reset also deletes instance values for the same
	 * keys (for example {@code AbstractTextEditor.Color.Background.SystemDefault} after leaving the stock dark
	 * theme). Writing the editor preferences first therefore lost them and left the editor light after one Apply.
	 * Writing them after the switch makes the plugin's own values the last word, and only the keys the plugin owns
	 * are written.
	 */
	public ThemeApplyOutcome applyTheme(IWorkbench workbenchHandle, Theme theme) {
		WorkbenchOutcome workbenchOutcome;
		try {
			workbenchOutcome = workbench.apply(theme);
		} catch (RuntimeException | LinkageError e) {
			log.error("Workbench theming failed unexpectedly", e);
			workbenchOutcome = WorkbenchOutcome.failed(null, "Workbench theming failed unexpectedly: " + e);
		}
		if (!workbenchOutcome.succeeded()) {
			log.warn("Workbench theming: " + workbenchOutcome.status() + " " + workbenchOutcome.message());
		}
		EditorOutcome editorOutcome = editor.write(theme);
		if (!editorOutcome.succeeded()) {
			log.warn("Editor theming: " + editorOutcome.failedAdapters().size() + " adapter(s) failed, "
					+ editorOutcome.conflicts().size() + " preference(s) changed outside Eclipse Themes were kept.");
		}
		FontOutcome fontOutcome;
		try {
			fontOutcome = fonts.apply(theme);
		} catch (RuntimeException | LinkageError e) {
			log.error("Applying theme fonts failed unexpectedly", e);
			fontOutcome = new FontOutcome(FontOutcome.Status.FAILED, "Theme fonts failed unexpectedly: " + e);
		}
		if (!fontOutcome.succeeded()) {
			log.warn("Theme fonts: " + fontOutcome.status() + " " + fontOutcome.message());
		}
		return new ThemeApplyOutcome(editorOutcome, workbenchOutcome, fontOutcome);
	}
}
