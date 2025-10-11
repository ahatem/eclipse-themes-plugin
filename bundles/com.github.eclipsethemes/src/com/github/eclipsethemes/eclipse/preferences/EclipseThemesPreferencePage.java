package com.github.eclipsethemes.eclipse.preferences;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.FrameworkUtil;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.Constants;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.eclipse.ThemeManager;

public class EclipseThemesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private IWorkbench workbench;

	// UI Components
	private TableViewer themeViewer;
	private Browser browserPreview;
	private Text searchText;
	private Combo themeTypeCombo;
	private Label themeCountLabel;
	private Label themeNameLabel;
	private Label authorLabel;
	private Label themeTypeLabel;
	private Link websiteLink;
	private Link downloadMoreLink;
	private Button importButton;
	private Button removeButton;
	private Button resetPluginDefaultsButton;
	private Button previewToggleButton;

	// Data
	private List<Theme> allThemes;
	private List<Theme> filteredThemes;
	private Theme selectedTheme;
	private Theme currentTheme;
	private boolean previewVisible = true;

	public EclipseThemesPreferencePage() {
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				String.valueOf(FrameworkUtil.getBundle(getClass()).getBundleId()));
		setPreferenceStore(scopedPreferenceStore);
		setDescription("Select and customize color themes for Eclipse editors and UI elements");
	}

	@Override
	public void init(IWorkbench workbench) {
		this.workbench = workbench;
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		// Search, Filter by type, Toggle Preview
		createToolbar(container);
		// Themes List and Theme Details, Preview browser
		createMainArea(container);
		// Import, Remove
		createActionButtons(container);

		// Load themes and set initial components state (eg. selected theme in list and
		// current theme)
		initializeData();
		// Add Listeners to all UI widgets
		addListeners();

		return container;
	}

	private void createToolbar(Composite parent) {
		Group toolbarGroup = new Group(parent, SWT.NONE);
		toolbarGroup.setText("Theme Browser");
		toolbarGroup.setLayout(new GridLayout(6, false));
		toolbarGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		// Search
		Label searchLabel = new Label(toolbarGroup, SWT.NONE);
		searchLabel.setText("Search:");

		searchText = new Text(toolbarGroup, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		searchText.setMessage("Filter themes by name or author...");
		GridData searchData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		searchData.widthHint = 180;
		searchText.setLayoutData(searchData);

		// Type filter
		Label typeLabel = new Label(toolbarGroup, SWT.NONE);
		typeLabel.setText("Type:");

		themeTypeCombo = new Combo(toolbarGroup, SWT.READ_ONLY);
		themeTypeCombo.setItems(new String[] { "All", "Dark", "Light" });
		themeTypeCombo.select(0);

		// Theme count
		themeCountLabel = new Label(toolbarGroup, SWT.NONE);
		themeCountLabel.setText("0 themes");
		themeCountLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		// Preview toggle
		previewToggleButton = new Button(toolbarGroup, SWT.TOGGLE);
		previewToggleButton.setText("Preview");
		previewToggleButton.setSelection(true);
	}

	private void createMainArea(Composite parent) {
		Composite mainArea = new Composite(parent, SWT.NONE);
		mainArea.setLayout(new GridLayout(2, false));
		mainArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createThemeListPanel(mainArea);
		createPreviewPanel(mainArea);
	}

	private void createThemeListPanel(Composite parent) {
		Composite leftPanel = new Composite(parent, SWT.NONE);
		leftPanel.setLayout(new GridLayout(1, false));
		GridData leftData = new GridData(SWT.FILL, SWT.FILL, false, true);
		leftData.widthHint = 280;
		leftData.minimumWidth = 250;
		leftPanel.setLayoutData(leftData);

		// Theme list
		Group themeListGroup = new Group(leftPanel, SWT.NONE);
		themeListGroup.setText("Available Themes");
		themeListGroup.setLayout(new GridLayout(1, false));
		themeListGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		themeViewer = new TableViewer(themeListGroup, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableData.heightHint = 200;
		tableData.minimumHeight = 150;
		themeViewer.getTable().setLayoutData(tableData);
		themeViewer.setContentProvider(ArrayContentProvider.getInstance());
		themeViewer.setLabelProvider(new ThemeLabelProvider());

		// Download link
		downloadMoreLink = new Link(themeListGroup, SWT.NONE);
		downloadMoreLink.setText("<a>Download more themes from eclipse-color-themes.vercel.app</a>");
		downloadMoreLink.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		createThemeDetailsPanel(leftPanel);
	}

	private void createThemeDetailsPanel(Composite parent) {
		Group detailsGroup = new Group(parent, SWT.NONE);
		detailsGroup.setText("Theme Details");
		detailsGroup.setLayout(new GridLayout(2, false));
		detailsGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		// Theme name (prominent)
		themeNameLabel = new Label(detailsGroup, SWT.WRAP);
		themeNameLabel.setText("No theme selected");
		GridData nameData = new GridData(SWT.FILL, SWT.TOP, true, false);
		nameData.horizontalSpan = 2;
		themeNameLabel.setLayoutData(nameData);
		themeNameLabel.setFont(org.eclipse.jface.resource.JFaceResources.getHeaderFont());

		// Author
		new Label(detailsGroup, SWT.NONE).setText("Author:");
		authorLabel = new Label(detailsGroup, SWT.WRAP);
		authorLabel.setText("N/A");
		authorLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		// Website
		new Label(detailsGroup, SWT.NONE).setText("Website:");
		websiteLink = new Link(detailsGroup, SWT.WRAP);
		websiteLink.setText("N/A");
		websiteLink.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		// Type
		new Label(detailsGroup, SWT.NONE).setText("Type:");
		themeTypeLabel = new Label(detailsGroup, SWT.WRAP);
		themeTypeLabel.setText("N/A");
		themeTypeLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
	}

	private void createPreviewPanel(Composite parent) {
		Group previewGroup = new Group(parent, SWT.NONE);
		previewGroup.setText("Live Preview");
		previewGroup.setLayout(new GridLayout(1, false));
		previewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label instructionLabel = new Label(previewGroup, SWT.WRAP);
		instructionLabel
				.setText("Select a theme from the list to see how it looks with Java code syntax highlighting.");
		instructionLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		browserPreview = new Browser(previewGroup, SWT.BORDER);
		GridData browserData = new GridData(SWT.FILL, SWT.FILL, true, true);
		browserData.heightHint = 300;
		browserData.minimumHeight = 200;
		browserPreview.setLayoutData(browserData);

		showDefaultPreview();
	}

	private void createActionButtons(Composite parent) {
		Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite buttonArea = new Composite(parent, SWT.NONE);
		buttonArea.setLayout(new GridLayout(5, false));
		buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Import button
		importButton = new Button(buttonArea, SWT.PUSH);
		importButton.setText("Import Theme...");
		importButton.setToolTipText("Import a new theme from file");
		setButtonWidth(importButton, 120);

		// Remove button
		removeButton = new Button(buttonArea, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.setToolTipText("Remove the selected custom theme");
		removeButton.setEnabled(false);
		setButtonWidth(removeButton, 120);

		// Reset to default button
		resetPluginDefaultsButton = new Button(buttonArea, SWT.PUSH);
		resetPluginDefaultsButton.setText("Reset Plugin Defaults");
		resetPluginDefaultsButton.setToolTipText("Reset plugin settings to default values");
		setButtonWidth(removeButton, 120);
	}

	private void setButtonWidth(Button button, int width) {
		GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.widthHint = width;
		button.setLayoutData(data);
	}

	private class ThemeLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Theme) {
				Theme theme = (Theme) element;
				String name = theme.getName();

				if (theme.equals(currentTheme)) {
					name += " (CURRENT)";
				}
				if (theme.getFile().isPresent()) {
					name += " [IMPORTED]";
				}

				return name;
			}
			return super.getText(element);
		}
	}

	private void initializeData() {
		loadThemes();

		String activeThemeId = getPreferenceStore().getString(PreferenceKeys.ACTIVE_THEME_ID);
		this.currentTheme = findThemeById(activeThemeId);

		refreshThemeList();
		selectInitialTheme();
		handleThemeSelectionChanged(currentTheme); // Important because listeners not added yet
	}

	private void loadThemes() {
		ThemeManager themeManager = EclipseThemes.instance().getManager();
		themeManager.loadThemes();

		this.allThemes = themeManager.getAllThemes();
		this.filteredThemes = this.allThemes;
	}

	private Theme findThemeById(String themeId) {
		return this.allThemes.stream().filter(theme -> theme.getId().equals(themeId)).findFirst().orElse(null);
	}

	private void selectInitialTheme() {
		if (!this.filteredThemes.isEmpty()) {
			Theme themeToSelect = this.currentTheme != null ? this.currentTheme : this.filteredThemes.get(0);
			themeViewer.setSelection(new StructuredSelection(themeToSelect));
		}
	}

	private void refreshThemeList() {
		themeViewer.setInput(this.filteredThemes);
		themeCountLabel.setText(this.filteredThemes.size() + " theme" + (this.filteredThemes.size() != 1 ? "s" : ""));
	}

	private void filterThemes() {
		String searchTerm = searchText.getText();
		String selectedType = themeTypeCombo.getText();

		if (isEmptyFilter(searchTerm, selectedType)) {
			this.filteredThemes = this.allThemes;
		} else {
			this.filteredThemes = this.allThemes.stream()
					.filter(theme -> matchesSearchCriteria(theme, searchTerm, selectedType))
					.collect(java.util.stream.Collectors.toList());
		}

		refreshThemeList();

		// Preserve selection
		if (selectedTheme != null && filteredThemes.contains(selectedTheme)) {
			themeViewer.setSelection(new StructuredSelection(selectedTheme));
		} else if (!filteredThemes.isEmpty()) {
			themeViewer.setSelection(new StructuredSelection(filteredThemes.get(0)));
		}
	}

	private boolean isEmptyFilter(String searchTerm, String selectedType) {
		return (searchTerm == null || searchTerm.trim().isEmpty()) && "All".equals(selectedType);
	}

	private boolean matchesSearchCriteria(Theme theme, String searchTerm, String selectedType) {
		// Check search term match
		if (searchTerm != null && !searchTerm.trim().isEmpty()) {
			String lowerSearch = searchTerm.toLowerCase().trim();
			boolean matchesName = theme.getName().toLowerCase().contains(lowerSearch);
			boolean matchesAuthor = theme.getAuthor() != null && theme.getAuthor().toLowerCase().contains(lowerSearch);

			// If search term doesn't match name or author, return false
			if (!matchesName && !matchesAuthor) {
				return false;
			}
		}

		// Check theme type match
		if (!"All".equals(selectedType)) {
			String themeType = theme.getType().name();
			if (!selectedType.equalsIgnoreCase(themeType)) {
				return false;
			}
		}

		return true;
	}

	private void addListeners() {
		// Theme selection
		themeViewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			handleThemeSelectionChanged((Theme) selection.getFirstElement());
		});

		// Search and filter
		searchText.addModifyListener(event -> filterThemes());
		themeTypeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				filterThemes();
			}
		});

		// Links
		downloadMoreLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch(Constants.MORE_THEMES_URL);
			}
		});

		websiteLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (selectedTheme != null && selectedTheme.getWebsite() != null) {
					Program.launch(selectedTheme.getWebsite());
				}
			}
		});

		// Buttons
		previewToggleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				togglePreviewVisibility();
			}
		});

		importButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				importTheme();
			}
		});

		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSelectedTheme();
			}
		});

		resetPluginDefaultsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				resetToDefault();
			}
		});
	}

	private void handleThemeSelectionChanged(Theme newTheme) {
		if (newTheme == null) {
			clearSelection();
			return;
		}

		this.selectedTheme = newTheme;
		updateThemeDetails(newTheme);
		updatePreview(newTheme);
		updateButtonStates();
	}

	private void clearSelection() {
		this.selectedTheme = null;
		themeNameLabel.setText("No theme selected");
		authorLabel.setText("N/A");
		websiteLink.setText("N/A");
		themeTypeLabel.setText("N/A");
		showDefaultPreview();
		updateButtonStates();
	}

	private void updateThemeDetails(Theme theme) {
		themeNameLabel.setText(theme.getName());
		authorLabel.setText(theme.getAuthor() != null ? theme.getAuthor() : "N/A");
		themeTypeLabel.setText(theme.getType().nameCapitalized());

		String website = theme.getWebsite();
		if (website != null && !website.isEmpty()) {
			websiteLink.setText("<a>" + website + "</a>");
		} else {
			websiteLink.setText("N/A");
		}

		themeNameLabel.getParent().layout();
	}

	private void updatePreview(Theme theme) {
		if (!previewVisible) {
			return;
		}

		try {
			String url = Constants.PREVIEW_SERVER_URL + theme.getId();
			browserPreview.setUrl(url);
		} catch (Exception e) {
			showPreviewError("Could not load preview for this theme.");
		}
	}

	private void showDefaultPreview() {
		// TODO: We might do offline theme preview generation
		browserPreview.setText("<html><body style='background:#f5f5f5;padding:20px;font-family:Arial;'>"
				+ "<h3>Theme Preview</h3>" + "<p>Select a theme to see the preview here.</p></body></html>");
	}

	private void showPreviewError(String message) {
		browserPreview.setText("<html><body style='background:#f5f5f5;padding:20px;font-family:Arial;color:#cc0000;'>"
				+ "<h3>Preview Error</h3>" + "<p>" + message + "</p></body></html>");
	}

	private void updateButtonStates() {
		boolean hasSelection = selectedTheme != null;
		boolean isImportedTheme = hasSelection && selectedTheme.getFile().isPresent();

		removeButton.setEnabled(isImportedTheme);
	}

	private void togglePreviewVisibility() {
		previewVisible = previewToggleButton.getSelection();
		if (previewVisible && selectedTheme != null) {
			updatePreview(selectedTheme);
		} else if (!previewVisible) {
			showDefaultPreview();
		}
	}

	private void applySelectedTheme() {
		if (selectedTheme == null) {
			return;
		}

		try {
			getPreferenceStore().setValue(PreferenceKeys.ACTIVE_THEME_ID, selectedTheme.getId());
			EclipseThemes.instance().getManager().applyTheme(workbench, selectedTheme);

			currentTheme = selectedTheme;
			updateButtonStates();
			refreshThemeList(); // to update "(CURRENT)" indicator

			// NOTE: Consider to remove it if it is annoying
			showInfoMessage("Theme Applied", "Theme '" + selectedTheme.getName() + "' has been applied successfully.");
		} catch (Exception e) {
			showErrorMessage("Apply Theme Failed", "Failed to apply theme: " + e.getMessage());
		}
	}

	private void importTheme() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setText("Import Theme");
		dialog.setFilterExtensions(new String[] { "*.xml" });
		dialog.setFilterNames(new String[] { "Eclipse Theme Files (*.xml)" });

		String selectedFilePath = dialog.open();
		if (selectedFilePath == null) {
			return;
		}

		File selectedFile = new File(selectedFilePath);
		if (!selectedFile.exists() || !selectedFile.canRead()) {
			showErrorMessage("Import Failed", "Cannot read the selected file.");
			return;
		}

		try {
			Optional<Theme> importedThemeOpt = EclipseThemes.instance().getManager().importTheme(selectedFile);
			if (importedThemeOpt.isEmpty()) {
				showErrorMessage("Import Failed",
						"Could not import theme - it may conflict with an existing theme or have invalid format.");
				return;
			}
			Theme importedTheme = importedThemeOpt.get();

			// Refresh and select the new theme
			loadThemes();
			filterThemes();

			selectedTheme = importedTheme;
			themeViewer.setSelection(new StructuredSelection(selectedTheme));

			showInfoMessage("Import Successful",
					"Theme '" + importedTheme.getName() + "' has been imported successfully.");

		} catch (Exception e) {
			EclipseThemes.instance().getLogger().error("Import Error", e);
			showErrorMessage("Import Error", "Error importing theme: " + e.getMessage());
		}
	}

	private void removeSelectedTheme() {
		if (selectedTheme == null || !selectedTheme.getFile().isPresent()) {
			return;
		}

		boolean confirmed = showConfirmDialog("Remove Theme", "Are you sure you want to remove the theme '"
				+ selectedTheme.getName() + "'?\n\n" + "This action cannot be undone.");

		if (!confirmed) {
			return;
		}

		try {
			// Delete the theme file
			selectedTheme.getFile().ifPresent(File::delete);
			// If this was the current theme, reset to default
			if (selectedTheme.equals(currentTheme)) {
				resetToDefaultTheme();
			}

			// Refresh the theme list
			loadThemes();
			filterThemes();

			// Select another theme
			if (!filteredThemes.isEmpty()) {
				themeViewer.setSelection(new StructuredSelection(filteredThemes.get(0)));
			} else {
				clearSelection();
			}

			showInfoMessage("Theme Removed", "Theme has been removed successfully.");
		} catch (Exception e) {
			showErrorMessage("Remove Failed", "Failed to remove theme: " + e.getMessage());
		}
	}

	private void resetToDefault() {
		boolean confirmed = showConfirmDialog("Reset to Default",
				"This will:\n" + "• Reset to Eclipse default theme (matching your current Eclipse appearance mode)\n"
						+ "• Remove all imported themes\n\n" + "This action cannot be undone. Continue?");

		if (!confirmed) {
			return;
		}

		try {
			Theme defaultTheme = getDefaultThemeForCurrentMode();
			if (defaultTheme != null) {
				getPreferenceStore().setValue(PreferenceKeys.ACTIVE_THEME_ID, defaultTheme.getId());
			} else {
				getPreferenceStore().setToDefault(PreferenceKeys.ACTIVE_THEME_ID);
			}

			// Remove all imported theme files
			allThemes.stream().filter(theme -> theme.getFile().isPresent())
					.forEach(theme -> theme.getFile().ifPresent(File::delete));

			// Apply the default theme
			if (defaultTheme != null) {
				EclipseThemes.instance().getManager().applyTheme(workbench, defaultTheme);
				currentTheme = defaultTheme;
			}

			// Reload and refresh
			loadThemes();
			filterThemes();

			if (!filteredThemes.isEmpty()) {
				Theme themeToSelect = currentTheme != null ? currentTheme : filteredThemes.get(0);
				themeViewer.setSelection(new StructuredSelection(themeToSelect));
			}

			showInfoMessage("Reset Complete", "Themes have been reset to "
					+ (isEclipseDarkMode() ? Constants.DEFAULT_DARK_THEME_NAME : Constants.DEFAULT_LIGHT_THEME_NAME)
					+ " theme.");

		} catch (Exception e) {
			showErrorMessage("Reset Failed", "Failed to reset themes: " + e.getMessage());
		}
	}

	private void resetToDefaultTheme() {
		Theme defaultTheme = getDefaultThemeForCurrentMode();

		if (defaultTheme != null) {
			EclipseThemes.instance().getManager().applyTheme(workbench, defaultTheme);
			currentTheme = defaultTheme;
			getPreferenceStore().setValue(PreferenceKeys.ACTIVE_THEME_ID, defaultTheme.getId());
		}
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

			boolean systemDarkTheme = Display.isSystemDarkTheme();
			boolean disableInherit = "true"
					.equalsIgnoreCase(System.getProperty("org.eclipse.e4.ui.css.theme.disableOSDarkThemeInherit")); // DISABLE_OS_DARK_THEME_INHERIT
			return systemDarkTheme && !disableInherit;

		} catch (Exception e) {
			try {
				return Display.isSystemDarkTheme();
			} catch (Exception ex) {
				return false;
			}
		}
	}

	private Theme getDefaultThemeForCurrentMode() {
		boolean isDarkMode = isEclipseDarkMode();
		String targetThemeName = isDarkMode ? Constants.DEFAULT_DARK_THEME_NAME : Constants.DEFAULT_LIGHT_THEME_NAME;

		Optional<Theme> defaultTheme = allThemes.stream().filter(theme -> targetThemeName.equals(theme.getName()))
				.findFirst();

		if (defaultTheme.isPresent()) {
			return defaultTheme.get();
		}

		return allThemes.stream().filter(theme -> {
			String type = theme.getType().name().toLowerCase();
			return isDarkMode ? "dark".equals(type) : "light".equals(type);
		}).findFirst().orElse(allThemes.isEmpty() ? null : allThemes.get(0));
	}

	// Dialog helper methods
	private void showInfoMessage(String title, String message) {
		MessageDialog.openInformation(getShell(), title, message);
	}

	private void showErrorMessage(String title, String message) {
		MessageDialog.openError(getShell(), title, message);
	}

	private boolean showConfirmDialog(String title, String message) {
		return MessageDialog.openConfirm(getShell(), title, message);
	}

	@Override
	public boolean performOk() {
		if (selectedTheme != null && !selectedTheme.equals(currentTheme)) {
			applySelectedTheme();
		}
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		EclipseThemes.instance().getManager().clearTheme();
		super.performDefaults();
	}
}