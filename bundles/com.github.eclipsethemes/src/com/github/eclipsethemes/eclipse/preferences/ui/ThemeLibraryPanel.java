package com.github.eclipsethemes.eclipse.preferences.ui;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.models.Constants;
import com.github.eclipsethemes.core.parser.ParserFactory;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;
import com.github.eclipsethemes.eclipse.ThemeManager;
import com.github.eclipsethemes.eclipse.ThemeOperationOutcome;
import com.github.eclipsethemes.eclipse.ThemeOrigin;
import com.github.eclipsethemes.eclipse.ThemeRecord;

/**
 * The theme library: the searchable, filterable list of themes and the Import, Export, Update and Remove actions.
 * Library actions change the library at once and are not part of Apply / OK.
 */
public class ThemeLibraryPanel {

	/** What the library needs from the page that hosts it, besides the shared {@link PageContext}. */
	public interface Host extends PageContext {

		/** The user chose a theme in the list, or an import or update selected one. */
		void themeSelected(ThemeRecord theme);

		/** The filter left nothing to select. */
		void selectionCleared();

		/** The selected theme was removed; the page forgets it without touching the details. */
		void selectedThemeRemoved();

		/** The user asked to apply the selected theme (double-click). */
		void applyRequested();

		/** Applies the default theme for the current mode; false when that failed. */
		boolean resetToDefaultTheme();
	}

	private static final String ALL_THEMES = "All";

	private final Host host;
	private final Composite library;
	private final TableViewer themeViewer;
	private final Text searchText;
	private final Combo themeTypeCombo;
	private final Label themeCountLabel;
	private final Button importButton;
	private final Button updateButton;
	private final Button exportButton;
	private final Button removeButton;

	private List<ThemeRecord> allThemes;
	private List<ThemeRecord> filteredThemes;
	private boolean listUpdating;

	public ThemeLibraryPanel(Composite parent, Host host) {
		this.host = host;

		library = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.marginRight = 8;
		library.setLayout(layout);

		Label heading = new Label(library, SWT.NONE);
		heading.setText("Theme Library");
		heading.setFont(JFaceResources.getBannerFont());

		searchText = new Text(library, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		searchText.setMessage("Search by name or author");
		searchText.setToolTipText("Filter the library by theme name or author");
		searchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite filterRow = new Composite(library, SWT.NONE);
		GridLayout filterLayout = new GridLayout(3, false);
		filterLayout.marginWidth = 0;
		filterLayout.marginHeight = 0;
		filterRow.setLayout(filterLayout);
		filterRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		new Label(filterRow, SWT.NONE).setText("Show:");
		themeTypeCombo = new Combo(filterRow, SWT.READ_ONLY);
		themeTypeCombo.setItems(new String[] { ALL_THEMES, "Dark", "Light" });
		themeTypeCombo.select(0);
		themeCountLabel = new Label(filterRow, SWT.RIGHT);
		themeCountLabel.setText("0 themes");
		themeCountLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		themeViewer = new TableViewer(library, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableData.widthHint = 200;
		tableData.heightHint = 200;
		themeViewer.getTable().setLayoutData(tableData);
		themeViewer.getTable().setToolTipText("Select a theme to see it on the right. Double-click to apply it.");
		themeViewer.setContentProvider(ArrayContentProvider.getInstance());
		themeViewer.setLabelProvider(new ThemeLabelProvider(host::currentTheme));

		Label legend = new Label(library, SWT.WRAP);
		legend.setText("* imported by you");
		legend.setLayoutData(Layouts.wrapData());

		Link downloadMoreLink = new Link(library, SWT.NONE);
		downloadMoreLink.setText("<a>Download more themes</a>");
		downloadMoreLink.setToolTipText(Constants.MORE_THEMES_URL);
		downloadMoreLink.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Composite actions = new Composite(library, SWT.NONE);
		GridLayout actionsLayout = new GridLayout(2, true);
		actionsLayout.marginWidth = 0;
		actionsLayout.marginHeight = 0;
		actions.setLayout(actionsLayout);
		actions.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

		importButton = libraryButton(actions, "Import...",
				"Add a theme package (.ectheme) or a legacy theme (.xml) to your library");
		exportButton = libraryButton(actions, "Export...", "Save the selected theme as a new .ectheme theme package. "
				+ "A legacy theme is converted into the new file; the installed theme is not changed");
		updateButton = libraryButton(actions, "Update...", "Replace the content of the selected imported theme with a "
				+ "file of the same format, keeping its place in your library");
		removeButton = libraryButton(actions, "Remove", "Remove the selected imported theme from your library");
		exportButton.setEnabled(false);
		updateButton.setEnabled(false);
		removeButton.setEnabled(false);

		addListeners(downloadMoreLink);
	}

	private static Button libraryButton(Composite parent, String text, String tooltip) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setToolTipText(tooltip);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return button;
	}

	private void addListeners(Link downloadMoreLink) {
		themeViewer.addSelectionChangedListener(event -> {
			if (listUpdating) {
				return;
			}
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			ThemeRecord chosen = (ThemeRecord) selection.getFirstElement();
			if (chosen != null) {
				host.themeSelected(chosen);
			}
		});

		themeViewer.addDoubleClickListener(event -> host.applyRequested());

		searchText.addModifyListener(event -> filterThemes());
		themeTypeCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> filterThemes()));

		downloadMoreLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> Program.launch(Constants.MORE_THEMES_URL)));

		exportButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> exportSelectedTheme()));
		importButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> importTheme()));
		updateButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateSelectedTheme()));
		removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> removeSelectedTheme()));
	}

	// ── List ──────────────────────────────────────────────────────────────────

	public void loadThemes() {
		ThemeManager themeManager = EclipseThemes.instance().getManager();
		themeManager.loadThemes();

		this.allThemes = themeManager.getAllThemes();
		this.filteredThemes = this.allThemes;
	}

	/** Every theme in the library, whatever the filter shows. */
	public List<ThemeRecord> themes() {
		return allThemes;
	}

	/** Selects in the list without treating it as the user choosing a theme. */
	public void select(ThemeRecord theme) {
		listUpdating = true;
		try {
			themeViewer.setSelection(new StructuredSelection(theme), true);
		} finally {
			listUpdating = false;
		}
	}

	public void refreshList() {
		listUpdating = true;
		try {
			themeViewer.setInput(this.filteredThemes);
		} finally {
			listUpdating = false;
		}
		themeCountLabel.setText(this.filteredThemes.size() + " theme" + (this.filteredThemes.size() != 1 ? "s" : ""));
		themeCountLabel.getParent().layout();
	}

	/** Redraws the rows, for the applied-theme indicator. */
	public void refreshLabels() {
		themeViewer.refresh();
	}

	public void updateButtonStates() {
		ThemeRecord selected = host.selectedTheme();
		boolean installedSelected = isInstalledTheme(selected);

		updateButton.setEnabled(installedSelected);
		exportButton.setEnabled(selected != null);
		removeButton.setEnabled(installedSelected);
	}

	private static boolean isInstalledTheme(ThemeRecord theme) {
		return theme != null && theme.origin() == ThemeOrigin.INSTALLED;
	}

	/**
	 * Applies the search and type filter. The selected theme stays the page's selection even when the filter hides
	 * it, so typing in the search box never changes what Apply will apply.
	 */
	private void filterThemes() {
		String searchTerm = searchText.getText();
		String selectedType = themeTypeCombo.getText();

		if (isEmptyFilter(searchTerm, selectedType)) {
			this.filteredThemes = this.allThemes;
		} else {
			this.filteredThemes = this.allThemes.stream()
					.filter(theme -> matchesSearchCriteria(theme, searchTerm, selectedType))
					.collect(Collectors.toList());
		}

		refreshList();

		ThemeRecord selectedTheme = host.selectedTheme();
		ThemeRecord currentTheme = host.currentTheme();
		if (selectedTheme != null && filteredThemes.contains(selectedTheme)) {
			select(selectedTheme);
		} else if (selectedTheme == null) {
			ThemeRecord preferred = currentTheme != null && filteredThemes.contains(currentTheme) ? currentTheme
					: filteredThemes.isEmpty() ? null : filteredThemes.get(0);
			if (preferred == null) {
				host.selectionCleared();
			} else {
				themeViewer.setSelection(new StructuredSelection(preferred), true);
			}
		}
	}

	private static boolean isEmptyFilter(String searchTerm, String selectedType) {
		return (searchTerm == null || searchTerm.trim().isEmpty()) && ALL_THEMES.equals(selectedType);
	}

	private static boolean matchesSearchCriteria(ThemeRecord theme, String searchTerm, String selectedType) {
		if (searchTerm != null && !searchTerm.trim().isEmpty()) {
			String lowerSearch = searchTerm.toLowerCase(Locale.ROOT).trim();
			boolean matchesName = theme.theme().getName().toLowerCase(Locale.ROOT).contains(lowerSearch);
			boolean matchesAuthor = theme.theme().getAuthor() != null
					&& theme.theme().getAuthor().toLowerCase(Locale.ROOT).contains(lowerSearch);
			if (!matchesName && !matchesAuthor) {
				return false;
			}
		}
		if (!ALL_THEMES.equals(selectedType)) {
			return selectedType.equalsIgnoreCase(theme.theme().getType().name());
		}
		return true;
	}

	// ── Library actions ───────────────────────────────────────────────────────

	private void importTheme() {
		File selectedFile = chooseThemeFile("Import Theme");
		if (selectedFile == null) {
			return;
		}

		try {
			ThemeOperationOutcome outcome = EclipseThemes.instance().getManager().importTheme(selectedFile);
			if (!outcome.successful()) {
				showErrorMessage("Import Failed", outcome.describeFailure());
				return;
			}

			String identity = outcome.record().logicalId();
			ThemeRecord admitted = reloadAndSelect(identity);
			if (admitted == null) {
				showErrorMessage("Import Failed", "The imported theme could not be admitted to the catalog.");
				return;
			}
			reportAdmitted("Imported", admitted, outcome.diagnostics());
		} catch (Exception e) {
			EclipseThemes.instance().getLogger().error("Import Error", e);
			showErrorMessage("Import Error", "Error importing theme: " + e.getMessage());
		}
	}

	/** Reloads the catalog with no filter, so the theme that was just admitted is visible, and selects it. */
	private ThemeRecord reloadAndSelect(String logicalId) {
		loadThemes();
		searchText.setText("");
		themeTypeCombo.select(0);
		filterThemes();
		return selectThemeByIdentity(logicalId);
	}

	/** An accepted file says so inline; accepted warnings are listed in a dialog so they are not missed. */
	private void reportAdmitted(String verb, ThemeRecord admitted, List<ThemeDiagnostic> diagnostics) {
		String name = admitted.theme().getName();
		int warnings = warningMessages(diagnostics).size();
		if (warnings == 0) {
			host.showStatus(verb + " '" + name + "'.", IMessageProvider.INFORMATION);
			return;
		}
		host.showStatus(verb + " '" + name + "' with " + warnings + (warnings == 1 ? " warning." : " warnings."),
				IMessageProvider.WARNING);
		MessageDialog.openInformation(shell(), verb + " With Warnings",
				"Theme '" + name + "' was " + verb.toLowerCase(Locale.ROOT) + "." + warningsText(diagnostics));
	}

	private static List<String> warningMessages(List<ThemeDiagnostic> diagnostics) {
		return diagnostics.stream().filter(finding -> !finding.error()).map(ThemeDiagnostic::message).distinct().toList();
	}

	/** The first few warnings of an admitted file, so a package that was only partly understood says so. */
	static String warningsText(List<ThemeDiagnostic> diagnostics) {
		List<String> warnings = warningMessages(diagnostics);
		if (warnings.isEmpty()) {
			return "";
		}
		StringBuilder text = new StringBuilder("\n\nIt was accepted with ").append(warnings.size())
				.append(warnings.size() == 1 ? " warning:" : " warnings:");
		warnings.stream().limit(5).forEach(message -> text.append("\n- ").append(message));
		if (warnings.size() > 5) {
			text.append("\n- and ").append(warnings.size() - 5).append(" more (see the Eclipse error log)");
		}
		return text.toString();
	}

	private void updateSelectedTheme() {
		ThemeRecord selectedTheme = host.selectedTheme();
		if (!isInstalledTheme(selectedTheme)) {
			return;
		}

		File selectedFile = chooseThemeFile("Update Selected Theme");
		if (selectedFile == null) {
			return;
		}
		boolean confirmed = MessageDialog.openConfirm(shell(), "Update Theme",
				"Replace the installed theme content of '" + selectedTheme.theme().getName() + "'?\n\n"
						+ "The theme keeps its identity and remains selected. This cannot be undone.");
		if (!confirmed) {
			return;
		}

		String identity = selectedTheme.logicalId();
		try {
			ThemeOperationOutcome outcome = EclipseThemes.instance().getManager().updateTheme(selectedTheme, selectedFile);
			if (!outcome.successful()) {
				showErrorMessage("Update Failed", outcome.describeFailure());
				return;
			}

			ThemeRecord admitted = reloadAndSelect(identity);
			if (admitted == null) {
				showErrorMessage("Update Failed", "The replaced theme could not be reloaded from managed storage.");
				return;
			}
			reportAdmitted("Updated", admitted, outcome.diagnostics());
		} catch (Exception e) {
			EclipseThemes.instance().getLogger().error("Update Error", e);
			showErrorMessage("Update Error", "Error updating theme: " + e.getMessage());
		}
	}

	private void exportSelectedTheme() {
		ThemeRecord selectedTheme = host.selectedTheme();
		if (selectedTheme == null) {
			return;
		}
		boolean legacy = selectedTheme.theme().getPackageMetadata().isEmpty();
		FileDialog dialog = new FileDialog(shell(), SWT.SAVE);
		dialog.setText(legacy ? "Convert to Theme Package" : "Export Theme Package");
		dialog.setFilterExtensions(new String[] { "*" + ParserFactory.PACKAGE_EXTENSION });
		dialog.setFilterNames(new String[] { "Theme Packages (*.ectheme)" });
		dialog.setFileName(exportFileName(selectedTheme.theme().getName()));
		dialog.setOverwrite(true);
		String path = dialog.open();
		if (path == null) {
			return;
		}
		if (!path.toLowerCase(Locale.ROOT).endsWith(ParserFactory.PACKAGE_EXTENSION)) {
			path += ParserFactory.PACKAGE_EXTENSION;
		}
		File target = new File(path);
		String name = selectedTheme.theme().getName();
		try {
			EclipseThemes.instance().getManager().exportTheme(selectedTheme, target);
			host.showStatus(legacy
					? "Converted '" + name + "' to a theme package: " + target.getPath()
							+ ". The installed theme was not changed."
					: "Exported '" + name + "' to " + target.getPath() + ".", IMessageProvider.INFORMATION);
		} catch (IOException e) {
			EclipseThemes.instance().getLogger().error("Export Error", e);
			showErrorMessage("Export Failed", "The theme was not exported: " + e.getMessage());
		}
	}

	/** A file-system-friendly default name for the exported package. */
	static String exportFileName(String themeName) {
		String base = themeName.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
		return (base.isEmpty() ? "theme" : base) + ParserFactory.PACKAGE_EXTENSION;
	}

	private File chooseThemeFile(String dialogTitle) {
		FileDialog dialog = new FileDialog(shell(), SWT.OPEN);
		dialog.setText(dialogTitle);
		dialog.setFilterExtensions(new String[] { "*.ectheme;*.xml;*.ECTHEME;*.XML", "*.ectheme;*.ECTHEME", "*.xml;*.XML" });
		dialog.setFilterNames(new String[] { "Eclipse Themes (*.ectheme, *.xml)", "Theme Packages (*.ectheme)",
				"Legacy Theme Files (*.xml)" });

		String selectedFilePath = dialog.open();
		if (selectedFilePath == null) {
			return null;
		}

		File selectedFile = new File(selectedFilePath);
		if (!selectedFile.exists() || !selectedFile.canRead()) {
			showErrorMessage(dialogTitle, "Cannot read the selected file.");
			return null;
		}
		return selectedFile;
	}

	private ThemeRecord selectThemeByIdentity(String logicalId) {
		ThemeRecord match = allThemes.stream()
				.filter(theme -> logicalId.equals(theme.logicalId()))
				.findFirst()
				.orElse(null);
		if (match != null) {
			themeViewer.setSelection(new StructuredSelection(match), true);
			// A selection the list already had raises no event; make sure the details follow the reloaded record.
			host.themeSelected(match);
		}
		return match;
	}

	private void removeSelectedTheme() {
		ThemeRecord selectedTheme = host.selectedTheme();
		if (!isInstalledTheme(selectedTheme)) {
			return;
		}

		boolean confirmed = MessageDialog.openConfirm(shell(), "Remove Theme", "Are you sure you want to remove the theme '"
				+ selectedTheme.theme().getName() + "'?\n\n"
				+ "This permanently deletes its installed theme file. This action cannot be undone.");

		if (!confirmed) {
			return;
		}

		ThemeRecord removed = selectedTheme;
		try {
			boolean removedFromStorage = EclipseThemes.instance().getManager().removeTheme(removed);
			if (!removedFromStorage) {
				showErrorMessage("Remove Failed",
						"The theme was not removed. Its installed file and identity are unchanged.");
				return;
			}

			// Reset only after removal is committed; a reset failure does not
			// turn the committed removal into a removal failure.
			boolean resetFailed = false;
			if (removed.equals(host.currentTheme())) {
				try {
					resetFailed = !host.resetToDefaultTheme();
				} catch (Exception e) {
					EclipseThemes.instance().getLogger().error("Reset After Removal Error", e);
					resetFailed = true;
				}
			}

			// Reload without reselecting the removed identity, and verify the
			// refreshed catalog no longer contains it before reporting success.
			loadThemes();
			if (allThemes.stream().anyMatch(removed::equals)) {
				showErrorMessage("Remove Verification Failed",
						"Removal was committed, but the refreshed catalog still contains the theme. "
								+ "Reopen the preference page to reload the catalog.");
				return;
			}
			host.selectedThemeRemoved();
			filterThemes();

			if (resetFailed) {
				showErrorMessage("Theme Removed",
						"The theme was removed, but resetting the applied theme to the default failed. "
								+ "Please select and apply a theme again.");
			} else {
				host.showStatus("Removed '" + removed.theme().getName() + "'.", IMessageProvider.INFORMATION);
			}
		} catch (Exception e) {
			showErrorMessage("Remove Failed", "Failed to remove theme: " + e.getMessage());
		}
	}

	private Shell shell() {
		return library.getShell();
	}

	private void showErrorMessage(String title, String message) {
		MessageDialog.openError(shell(), title, message);
	}
}
