package com.github.eclipsethemes.eclipse.preferences;

import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome;
import com.github.eclipsethemes.eclipse.ThemeRecord;
import com.github.eclipsethemes.eclipse.preferences.ui.AboutPanel;
import com.github.eclipsethemes.eclipse.preferences.ui.AppearancePanel;
import com.github.eclipsethemes.eclipse.preferences.ui.FontsPanel;
import com.github.eclipsethemes.eclipse.preferences.ui.PreviewPanel;
import com.github.eclipsethemes.eclipse.preferences.ui.ResetChoiceDialog;
import com.github.eclipsethemes.eclipse.preferences.ui.ThemeDetailsPanel;
import com.github.eclipsethemes.eclipse.preferences.ui.ThemeLibraryPanel;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchSectionModel;

/**
 * The Theme Manager: a theme library on the left, and on the right the selected theme with its preview, whether it
 * also themes the workbench, the fonts it will use and where it comes from.
 *
 * <p>
 * Everything on this page is staged page state (the selected theme, the workbench option and the font choices) until
 * the user chooses Apply or OK. Import, Export, Update and Remove act on the theme library and take effect at once.
 *
 * <p>
 * The page is the coordinator: it builds the panels in {@code eclipse.preferences.ui}, holds the selected and applied
 * theme, passes the selection between the panels, and handles Apply, OK and Restore Defaults.
 */
public class EclipseThemesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String APPEARANCE_PAGE_ID = "org.eclipse.ui.preferencePages.Views";

	private IWorkbench workbench;

	private ThemeLibraryPanel library;
	private ThemeDetailsPanel details;
	private PreviewPanel preview;
	private AppearancePanel appearance;
	private FontsPanel fonts;
	private AboutPanel about;

	private ThemeRecord selectedTheme;
	private ThemeRecord currentTheme;

	private final ThemeLibraryPanel.Host context = new ThemeLibraryPanel.Host() {
		@Override
		public ThemeRecord selectedTheme() {
			return selectedTheme;
		}

		@Override
		public ThemeRecord currentTheme() {
			return currentTheme;
		}

		@Override
		public void showStatus(String message, int type) {
			EclipseThemesPreferencePage.this.showStatus(message, type);
		}

		@Override
		public void themeSelected(ThemeRecord theme) {
			handleThemeSelectionChanged(theme);
		}

		@Override
		public void selectionCleared() {
			clearSelection();
		}

		@Override
		public void selectedThemeRemoved() {
			selectedTheme = null;
		}

		@Override
		public void applyRequested() {
			applySelectedTheme();
		}

		@Override
		public boolean resetToDefaultTheme() {
			return EclipseThemesPreferencePage.this.resetToDefaultTheme();
		}
	};

	public EclipseThemesPreferencePage() {
		this(new ScopedPreferenceStore(InstanceScope.INSTANCE, EclipseThemes.PLUGIN_ID));
	}

	EclipseThemesPreferencePage(IPreferenceStore store) {
		setPreferenceStore(store);
		setDescription("Choose a theme for your editors and, if you like, the workbench and fonts. "
				+ "Changes take effect when you choose Apply.");
	}

	@Override
	public void init(IWorkbench workbench) {
		this.workbench = workbench;
	}

	// ── Layout ────────────────────────────────────────────────────────────────

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout containerLayout = new GridLayout(1, false);
		containerLayout.marginWidth = 0;
		containerLayout.marginHeight = 0;
		container.setLayout(containerLayout);

		SashForm sash = new SashForm(container, SWT.HORIZONTAL);
		GridData sashData = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashData.widthHint = 780;
		sashData.heightHint = 520;
		sashData.minimumHeight = 320;
		sash.setLayoutData(sashData);

		library = new ThemeLibraryPanel(sash, context);
		details = new ThemeDetailsPanel(sash);
		Composite content = details.content();
		preview = new PreviewPanel(content, context);
		appearance = new AppearancePanel(content, context, details::relayout, this::openAppearance);
		fonts = new FontsPanel(content, context, details::relayout);
		about = new AboutPanel(content, context);
		details.updateScrollSize();
		sash.setWeights(new int[] { 32, 68 });

		// Load themes and set the initial state; the panels' listeners are already in place
		initializeData();

		return container;
	}

	private void initializeData() {
		library.loadThemes();

		restoreSelectedIntent(library.themes());

		library.refreshList();
		if (currentTheme != null) {
			library.select(currentTheme);
		}
		handleThemeSelectionChanged(currentTheme); // The initial selection raises no event
	}

	void restoreSelectedIntent(List<ThemeRecord> themes) {
		currentTheme = ThemeSelection.resolve(getPreferenceStore(), themes).orElse(null);
		selectedTheme = currentTheme;
	}

	private void openAppearance() {
		if (getContainer() instanceof IWorkbenchPreferenceContainer container) {
			container.openPage(APPEARANCE_PAGE_ID, null);
			return;
		}
		var dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), APPEARANCE_PAGE_ID, null, null);
		if (dialog != null) {
			dialog.open();
		}
	}

	// ── Selected theme ────────────────────────────────────────────────────────

	private void handleThemeSelectionChanged(ThemeRecord newTheme) {
		showStatus(null, IMessageProvider.NONE);

		if (newTheme == null) {
			clearSelection();
			return;
		}

		this.selectedTheme = newTheme;
		details.show(newTheme, newTheme.equals(currentTheme));
		about.show(newTheme);
		details.relayout();
		preview.show(newTheme);
		fonts.refresh();
		library.updateButtonStates();
	}

	private void clearSelection() {
		this.selectedTheme = null;
		details.clear();
		about.hide();
		preview.showDefault();
		fonts.refresh();
		library.updateButtonStates();
		details.relayout();
	}

	// ── Status ────────────────────────────────────────────────────────────────

	/** One inline message in the page's own message area; {@code null} restores the page description. */
	private void showStatus(String message, int type) {
		if (getControl() == null || getControl().isDisposed()) {
			return;
		}
		setMessage(message, type);
	}

	private void showErrorMessage(String title, String message) {
		MessageDialog.openError(getShell(), title, message);
	}

	// ── Apply ─────────────────────────────────────────────────────────────────

	protected boolean applySelectedTheme() {
		if (selectedTheme == null) {
			return false;
		}

		try {
			appearance.commit();
			if (!fonts.commit()) {
				return false;
			}
			ThemeApplyOutcome outcome = EclipseThemes.instance().getManager().applyTheme(workbench, selectedTheme);
			boolean editorApplied = ThemeSelection.rememberApplied(getPreferenceStore(), selectedTheme, outcome);

			if (editorApplied) {
				// The selected intent and the applied indicator change only after the editor half succeeded.
				currentTheme = selectedTheme;
				details.showApplied(selectedTheme);
			}
			library.updateButtonStates();
			library.refreshLabels(); // to update the "●" indicator
			appearance.refresh(false);
			fonts.refresh();

			showStatus(WorkbenchSectionModel.summary(outcome),
					outcome.fullySucceeded() ? IMessageProvider.INFORMATION : IMessageProvider.WARNING);
			return editorApplied;
		} catch (Exception e) {
			showErrorMessage("Apply Theme Failed", "Failed to apply theme: " + e.getMessage());
			return false;
		}
	}

	private boolean resetToDefaultTheme() {
		ThemeRecord defaultTheme = getDefaultThemeForCurrentMode();

		if (defaultTheme == null) {
			return false;
		}
		ThemeApplyOutcome outcome = EclipseThemes.instance().getManager().applyTheme(workbench, defaultTheme);
		if (!ThemeSelection.rememberApplied(getPreferenceStore(), defaultTheme, outcome)) {
			showStatus(WorkbenchSectionModel.summary(outcome), IMessageProvider.WARNING);
			return false;
		}
		currentTheme = defaultTheme;
		return true;
	}

	// See:
	// https://github.com/eclipse-platform/eclipse.platform.ui/blob/8dc4f460d4d4a4a44478a4b40b21c6a03c68eb19/bundles/org.eclipse.e4.ui.css.swt.theme/src/org/eclipse/e4/ui/css/swt/internal/theme/ThemeEngine.java#L578
	private boolean isEclipseDarkMode() {
		try {
			String prefThemeId = null;
			try {
				IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode("org.eclipse.e4.ui.css.swt.theme"); // THEME_PLUGIN_ID
				prefThemeId = prefs.get("themeid", null); // THEMEID_KEY
			} catch (Exception e) {
			}

			if (prefThemeId != null && prefThemeId.contains("dark")) {
				return true;
			}

			boolean systemDarkTheme = org.eclipse.swt.widgets.Display.isSystemDarkTheme();
			boolean disableInherit = "true"
					.equalsIgnoreCase(System.getProperty("org.eclipse.e4.ui.css.theme.disableOSDarkThemeInherit")); // DISABLE_OS_DARK_THEME_INHERIT
			return systemDarkTheme && !disableInherit;

		} catch (Exception e) {
			try {
				return org.eclipse.swt.widgets.Display.isSystemDarkTheme();
			} catch (Exception ex) {
				return false;
			}
		}
	}

	private ThemeRecord getDefaultThemeForCurrentMode() {
		return ThemeSelection.defaultTheme(library.themes(), isEclipseDarkMode());
	}

	// ── Apply / OK / Defaults ─────────────────────────────────────────────────

	@Override
	public boolean performOk() {
		if (selectedTheme != null && !applySelectedTheme()) {
			return false;
		}
		return super.performOk();
	}

	@Override
	protected void performApply() {
		if (selectedTheme != null) applySelectedTheme();
	}

	@Override
	protected void performDefaults() {
		ResetChoiceDialog dialog = new ResetChoiceDialog(getShell());
		if (dialog.open() != Dialog.OK) {
			return;
		}
		if (dialog.isStopManagementSelected()) {
			if (!stopThemeManagement()) return;
		} else {
			resetToPluginDefault();
		}
		super.performDefaults();
	}

	/**
	 * The plugin defaults are the default theme with workbench theming off and both fonts on Keep current font, so the
	 * workbench and any fonts this plugin replaced are handed back first, under the same ownership rules as stopping.
	 */
	private void resetToPluginDefault() {
		appearance.relinquish();
		fonts.release();
		ThemeRecord defaultTheme = getDefaultThemeForCurrentMode();
		if (defaultTheme != null) {
			ThemeApplyOutcome outcome = EclipseThemes.instance().getManager().applyTheme(workbench, defaultTheme);
			if (ThemeSelection.rememberApplied(getPreferenceStore(), defaultTheme, outcome)) {
				currentTheme = defaultTheme;
			} else {
				showStatus(WorkbenchSectionModel.summary(outcome), IMessageProvider.WARNING);
			}
		} else {
			ThemeSelection.clear(getPreferenceStore());
			currentTheme = null;
		}
		library.refreshList();
		if (currentTheme != null) {
			library.select(currentTheme);
			handleThemeSelectionChanged(currentTheme);
		}
	}

	private boolean stopThemeManagement() {
		PreferenceReset.Result result = new PreferenceReset(InstanceScope.INSTANCE::getNode,
				EclipseThemes.instance().getPreferenceStore()).resetAndCommit(() -> {
					ThemeSelection.clear(getPreferenceStore());
					currentTheme = null;
					library.refreshList();
					clearSelection();
				});
		if (!result.successful()) {
			result.failures().forEach(failure -> EclipseThemes.instance().getLogger()
					.error(failure.description(), failure.cause()));
			showStatus("Reset incomplete", IMessageProvider.WARNING);
			showErrorMessage("Reset Incomplete", "Some managed appearance overrides could not be removed or saved. "
					+ "The reset may be partial; the active theme selection has been retained. You can retry.\n\n"
					+ result.describeFailures());
			return false;
		}
		appearance.relinquish();
		fonts.release();
		if (!result.conflicts().isEmpty()) {
			result.conflicts().forEach(conflict -> EclipseThemes.instance().getLogger()
					.warn("Editor preference kept: " + conflict.description()));
			showStatus("Management stopped; " + result.conflicts().size()
					+ " editor preference(s) changed outside Eclipse Themes were left alone.", IMessageProvider.WARNING);
		}
		return true;
	}
}
