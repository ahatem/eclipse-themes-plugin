package com.github.eclipsethemes.eclipse.preferences;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.Constants;
import com.github.eclipsethemes.core.validation.WebsiteUri;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.State;
import com.github.eclipsethemes.core.workbench.WorkbenchDiagnostics;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome;
import com.github.eclipsethemes.eclipse.ThemeManager;
import com.github.eclipsethemes.eclipse.ThemeOperationOutcome;
import com.github.eclipsethemes.eclipse.ThemeOrigin;
import com.github.eclipsethemes.eclipse.ThemeRecord;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchCoordinator;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchOutcome;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchSectionModel;

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
	private Button updateButton;
	private Button removeButton;
	private Button previewToggleButton;
	private Label statusLabel;
	private Button workbenchCheckbox;
	private Label workbenchStatusLabel;

	// Data
	private List<ThemeRecord> allThemes;
	private List<ThemeRecord> filteredThemes;
	private ThemeRecord selectedTheme;
	private ThemeRecord currentTheme;
	private boolean previewVisible = true;

	public EclipseThemesPreferencePage() {
		this(new ScopedPreferenceStore(InstanceScope.INSTANCE, EclipseThemes.PLUGIN_ID));
	}

	EclipseThemesPreferencePage(IPreferenceStore store) {
		setPreferenceStore(store);
		setDescription("Select and apply color themes for Eclipse editors");
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
		// Opt-in coordination of the Eclipse workbench theme
		createWorkbenchSection(container);
		// Import, Update, Remove
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
		downloadMoreLink.setText("<a>Download more themes →</a>");
		downloadMoreLink.setToolTipText(Constants.MORE_THEMES_URL);
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

	private static final String APPEARANCE_PAGE_ID = "org.eclipse.ui.preferencePages.Views";

	private WorkbenchCoordinator workbenchCoordinator() {
		return EclipseThemes.instance().getManager().workbench();
	}

	private IPreferenceStore pluginStore() {
		return EclipseThemes.instance().getPreferenceStore();
	}

	private void createWorkbenchSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Workbench");
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		workbenchCheckbox = new Button(group, SWT.CHECK);
		GridData checkboxData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		checkboxData.horizontalSpan = 3;
		workbenchCheckbox.setLayoutData(checkboxData);

		workbenchStatusLabel = new Label(group, SWT.WRAP);
		GridData statusData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		statusData.horizontalSpan = 3;
		statusData.widthHint = 600;
		workbenchStatusLabel.setLayoutData(statusData);

		Label coverage = new Label(group, SWT.WRAP);
		coverage.setText(WorkbenchSectionModel.COVERAGE);
		GridData coverageData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		coverageData.horizontalSpan = 3;
		coverageData.widthHint = 600;
		coverage.setLayoutData(coverageData);

		Button appearance = new Button(group, SWT.PUSH);
		appearance.setText("Open Appearance...");
		appearance.setToolTipText("Choose the Eclipse workbench theme manually on the Appearance preference page");
		appearance.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> openAppearance()));

		Button diagnostics = new Button(group, SWT.PUSH);
		diagnostics.setText("Copy diagnostics");
		diagnostics.setToolTipText("Copy environment and workbench-theming state to the clipboard; nothing is sent");
		diagnostics.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> copyDiagnostics()));

		refreshWorkbenchSection(true);
	}

	/**
	 * Shows the capability and stored intent. Only on first display does it set the checkbox from the stored
	 * intent; afterwards the checkbox is the user's uncommitted choice until Apply or OK.
	 */
	private void refreshWorkbenchSection(boolean initial) {
		if (workbenchCheckbox == null || workbenchCheckbox.isDisposed()) {
			return;
		}
		WorkbenchCoordinator coordinator = workbenchCoordinator();
		WorkbenchCapability capability = coordinator.capability();
		boolean enabled = coordinator.isEnabled();
		WorkbenchSectionModel model = WorkbenchSectionModel.of(capability, enabled,
				coordinator.activeThemeId().orElse(null));
		workbenchCheckbox.setText(model.checkboxLabel());
		workbenchCheckbox.setEnabled(model.checkboxEnabled());
		workbenchStatusLabel.setText(model.statusText());
		if (initial) {
			boolean effective = enabled && (capability.state() == State.VERIFIED
					|| capability.state() == State.EXPERIMENTAL && coordinator.isOptedIn(capability));
			workbenchCheckbox.setSelection(effective);
		}
		workbenchStatusLabel.getParent().layout(true);
	}

	/** Writes the committed intent for the workbench half; called only from Apply and OK. */
	private void commitWorkbenchIntent() {
		if (workbenchCheckbox == null || !workbenchCheckbox.getEnabled()) {
			return;
		}
		WorkbenchCoordinator coordinator = workbenchCoordinator();
		WorkbenchCapability capability = coordinator.capability();
		boolean checked = workbenchCheckbox.getSelection();
		IPreferenceStore store = pluginStore();
		store.setValue(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED, checked);
		if (capability.state() == State.EXPERIMENTAL) {
			store.setValue(PreferenceKeys.WORKBENCH_EXPERIMENTAL_ACK,
					checked ? capability.environment().combination() : "");
		}
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

	private void copyDiagnostics() {
		WorkbenchCoordinator coordinator = workbenchCoordinator();
		WorkbenchCapability capability = coordinator.capability();
		ThemeRecord theme = selectedTheme != null ? selectedTheme : currentTheme;
		String text = WorkbenchDiagnostics.format(capability, coordinator.isEnabled(), coordinator.isOptedIn(capability),
				theme == null ? null : theme.theme().getName(), coordinator.activeThemeId().orElse(null),
				coordinator.isOwned());
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		try {
			clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
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

		// Update button
		updateButton = new Button(buttonArea, SWT.PUSH);
		updateButton.setText("Update Selected Theme...");
		updateButton.setToolTipText("Replace the XML of the selected installed theme, keeping its identity");
		updateButton.setEnabled(false);
		setButtonWidth(updateButton, 150);

		// Remove button
		removeButton = new Button(buttonArea, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.setToolTipText("Remove the selected installed theme (* marks installed themes)");
		removeButton.setEnabled(false);
		setButtonWidth(removeButton, 120);

		// Hint label — fills remaining space
		Label hintLabel = new Label(buttonArea, SWT.NONE);
		hintLabel.setText("Double-click a theme, or select one and click Apply / OK");
		hintLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		hintLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Status label — right-aligned, shows feedback after apply
		statusLabel = new Label(buttonArea, SWT.NONE);
		statusLabel.setText("");
		statusLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
	}

	private void setButtonWidth(Button button, int width) {
		GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		data.widthHint = width;
		button.setLayoutData(data);
	}

	private class ThemeLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof ThemeRecord theme) {
				String name = theme.theme().getName();

				if (theme.equals(currentTheme)) {
					name = "● " + name;
				}
				if (theme.origin() == ThemeOrigin.INSTALLED) {
					name += " *";
				}

				return name;
			}
			return super.getText(element);
		}
	}

	private void initializeData() {
		loadThemes();

		restoreSelectedIntent(allThemes);

		refreshThemeList();
		selectInitialTheme();
		handleThemeSelectionChanged(currentTheme); // Important because listeners not added yet
	}

	void restoreSelectedIntent(List<ThemeRecord> themes) {
		currentTheme = ThemeSelection.resolve(getPreferenceStore(), themes).orElse(null);
		selectedTheme = currentTheme;
	}

	private void loadThemes() {
		ThemeManager themeManager = EclipseThemes.instance().getManager();
		themeManager.loadThemes();

		this.allThemes = themeManager.getAllThemes();
		this.filteredThemes = this.allThemes;
	}

	private void selectInitialTheme() {
		if (this.currentTheme != null) {
			themeViewer.setSelection(new StructuredSelection(currentTheme));
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

	private boolean matchesSearchCriteria(ThemeRecord theme, String searchTerm, String selectedType) {
		// Check search term match
		if (searchTerm != null && !searchTerm.trim().isEmpty()) {
			String lowerSearch = searchTerm.toLowerCase().trim();
			boolean matchesName = theme.theme().getName().toLowerCase().contains(lowerSearch);
			boolean matchesAuthor = theme.theme().getAuthor() != null
					&& theme.theme().getAuthor().toLowerCase().contains(lowerSearch);

			// If search term doesn't match name or author, return false
			if (!matchesName && !matchesAuthor) {
				return false;
			}
		}

		// Check theme type match
		if (!"All".equals(selectedType)) {
			String themeType = theme.theme().getType().name();
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
			handleThemeSelectionChanged((ThemeRecord) selection.getFirstElement());
		});

		themeViewer.addDoubleClickListener(event -> applySelectedTheme());

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
				// The same policy that decides the link markup decides the launch.
				WebsiteUri.Check check = WebsiteUri.check(selectedTheme == null ? null : selectedTheme.theme().getWebsite());
				if (check.launchable()) {
					Program.launch(check.value());
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

		updateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSelectedTheme();
			}
		});

		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSelectedTheme();
			}
		});
	}

	private void handleThemeSelectionChanged(ThemeRecord newTheme) {
		if (statusLabel != null && !statusLabel.isDisposed()) {
			statusLabel.setText("");
		}

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

	private void updateThemeDetails(ThemeRecord theme) {
		themeNameLabel.setText(theme.theme().getName());
		authorLabel.setText(theme.theme().getAuthor() != null ? theme.theme().getAuthor() : "N/A");
		themeTypeLabel.setText(theme.theme().getType().nameCapitalized());
		websiteLink.setText(websiteText(theme.theme().getWebsite()));

		themeNameLabel.getParent().layout();
	}

	/**
	 * Link markup is emitted only for a launchable value; everything else stays
	 * inert literal text so no metadata can reach the platform launcher.
	 */
	static String websiteText(String website) {
		WebsiteUri.Check check = WebsiteUri.check(website);
		if (check.value().isEmpty()) {
			return "N/A";
		}
		String text = WebsiteUri.displayText(website);
		return check.launchable() ? "<a>" + text + "</a>" : text;
	}

	private void updatePreview(ThemeRecord theme) {
		if (!previewVisible) {
			return;
		}

		try {
			String url = previewUrl(theme);
			browserPreview.setUrl(url);
		} catch (Exception e) {
			showPreviewError("Could not load preview for this theme.");
		}
	}

	static String previewUrl(ThemeRecord theme) {
		return Constants.PREVIEW_SERVER_URL + theme.theme().getId();
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
		boolean installedSelected = isInstalledTheme(selectedTheme);

		updateButton.setEnabled(installedSelected);
		removeButton.setEnabled(installedSelected);
	}

	private static boolean isInstalledTheme(ThemeRecord theme) {
		return theme != null && theme.origin() == ThemeOrigin.INSTALLED;
	}

	private void togglePreviewVisibility() {
		previewVisible = previewToggleButton.getSelection();
		if (previewVisible && selectedTheme != null) {
			updatePreview(selectedTheme);
		} else if (!previewVisible) {
			showDefaultPreview();
		}
	}

	protected void applySelectedTheme() {
		if (selectedTheme == null) {
			return;
		}

		try {
			ThemeSelection.remember(getPreferenceStore(), selectedTheme);
			commitWorkbenchIntent();
			ThemeApplyOutcome outcome = EclipseThemes.instance().getManager().applyTheme(workbench, selectedTheme);

			currentTheme = selectedTheme;
			updateButtonStates();
			refreshThemeList(); // to update "●" indicator
			refreshWorkbenchSection(false);

			statusLabel.setText(WorkbenchSectionModel.summary(outcome));
			statusLabel.getParent().layout();
			if (outcome.fullySucceeded()) {
				Display.getCurrent().timerExec(3000, () -> {
					if (!statusLabel.isDisposed()) {
						statusLabel.setText("");
						statusLabel.getParent().layout();
					}
				});
			}
		} catch (Exception e) {
			showErrorMessage("Apply Theme Failed", "Failed to apply theme: " + e.getMessage());
		}
	}

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
			loadThemes();
			filterThemes();
			ThemeRecord admitted = selectThemeByIdentity(identity);
			if (admitted == null) {
				showErrorMessage("Import Failed", "The imported theme could not be admitted to the catalog.");
				return;
			}

			showInfoMessage("Import Successful",
					"Theme '" + admitted.theme().getName() + "' has been imported successfully.");
		} catch (Exception e) {
			EclipseThemes.instance().getLogger().error("Import Error", e);
			showErrorMessage("Import Error", "Error importing theme: " + e.getMessage());
		}
	}

	private void updateSelectedTheme() {
		if (!isInstalledTheme(selectedTheme)) {
			return;
		}

		File selectedFile = chooseThemeFile("Update Selected Theme");
		if (selectedFile == null) {
			return;
		}
		boolean confirmed = showConfirmDialog("Update Theme",
				"Replace the installed XML content of '" + selectedTheme.theme().getName() + "'?\n\n"
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

			loadThemes();
			filterThemes();
			ThemeRecord admitted = selectThemeByIdentity(identity);
			if (admitted == null) {
				showErrorMessage("Update Failed", "The replaced theme could not be reloaded from managed storage.");
				return;
			}

			showInfoMessage("Update Successful", "Theme '" + admitted.theme().getName() + "' has been updated.");
		} catch (Exception e) {
			EclipseThemes.instance().getLogger().error("Update Error", e);
			showErrorMessage("Update Error", "Error updating theme: " + e.getMessage());
		}
	}

	private File chooseThemeFile(String dialogTitle) {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setText(dialogTitle);
		dialog.setFilterExtensions(new String[] { "*.xml", "*.XML" });
		dialog.setFilterNames(new String[] { "Eclipse Theme Files (*.xml)", "Eclipse Theme Files (*.XML)" });

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
			themeViewer.setSelection(new StructuredSelection(match));
		}
		return match;
	}

	private void removeSelectedTheme() {
		if (!isInstalledTheme(selectedTheme)) {
			return;
		}

		boolean confirmed = showConfirmDialog("Remove Theme", "Are you sure you want to remove the theme '"
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
			if (removed.equals(currentTheme)) {
				try {
					resetFailed = !resetToDefaultTheme();
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
			filterThemes();
			if (filteredThemes.isEmpty()) {
				clearSelection();
			}

			if (resetFailed) {
				showErrorMessage("Theme Removed",
						"The theme was removed, but resetting the applied theme to the default failed. "
								+ "Please select and apply a theme again.");
			} else {
				showInfoMessage("Theme Removed", "Theme has been removed successfully.");
			}
		} catch (Exception e) {
			showErrorMessage("Remove Failed", "Failed to remove theme: " + e.getMessage());
		}
	}

	private boolean resetToDefaultTheme() {
		ThemeRecord defaultTheme = getDefaultThemeForCurrentMode();

		if (defaultTheme == null) {
			return false;
		}
		EclipseThemes.instance().getManager().applyTheme(workbench, defaultTheme);
		currentTheme = defaultTheme;
		ThemeSelection.remember(getPreferenceStore(), defaultTheme);
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

	private ThemeRecord getDefaultThemeForCurrentMode() {
		return ThemeSelection.defaultTheme(allThemes, isEclipseDarkMode());
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
		if (selectedTheme != null) {
			applySelectedTheme();
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

	private void resetToPluginDefault() {
		ThemeRecord defaultTheme = getDefaultThemeForCurrentMode();
		if (defaultTheme != null) {
			ThemeSelection.remember(getPreferenceStore(), defaultTheme);
			EclipseThemes.instance().getManager().applyTheme(workbench, defaultTheme);
			currentTheme = defaultTheme;
		} else {
			ThemeSelection.clear(getPreferenceStore());
			currentTheme = null;
		}
		refreshThemeList();
		if (currentTheme != null) {
			themeViewer.setSelection(new StructuredSelection(currentTheme));
		}
	}

	private boolean stopThemeManagement() {
		PreferenceReset.Result result = new PreferenceReset(InstanceScope.INSTANCE::getNode).resetAndCommit(() -> {
			ThemeSelection.clear(getPreferenceStore());
			currentTheme = null;
			refreshThemeList();
			clearSelection();
		});
		if (!result.successful()) {
			result.failures().forEach(failure -> EclipseThemes.instance().getLogger()
					.error(failure.description(), failure.cause()));
			statusLabel.setText("Reset incomplete");
			statusLabel.getParent().layout();
			showErrorMessage("Reset Incomplete", "Some managed appearance overrides could not be removed or saved. "
					+ "The reset may be partial; the active theme selection has been retained. You can retry.\n\n"
					+ result.describeFailures());
			return false;
		}
		relinquishWorkbench();
		return true;
	}

	/** Stop management also hands the workbench back, under the ownership rules (an external choice wins). */
	private void relinquishWorkbench() {
		IPreferenceStore store = pluginStore();
		store.setValue(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED, false);
		store.setValue(PreferenceKeys.WORKBENCH_EXPERIMENTAL_ACK, "");
		WorkbenchCoordinator coordinator = workbenchCoordinator();
		WorkbenchOutcome outcome = coordinator.isOwned() ? coordinator.relinquish() : WorkbenchOutcome.notRequested("");
		if (workbenchCheckbox != null && !workbenchCheckbox.isDisposed()) {
			workbenchCheckbox.setSelection(false);
		}
		refreshWorkbenchSection(false);
		if (outcome.status() != WorkbenchOutcome.Status.NOT_REQUESTED) {
			statusLabel.setText("Workbench: " + outcome.message());
			statusLabel.getParent().layout();
		}
	}

	// ── Reset choice dialog ───────────────────────────────────────────────────

	private static class ResetChoiceDialog extends Dialog {

		private boolean stopManagementSelected = false;

		ResetChoiceDialog(Shell shell) {
			super(shell);
		}

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			shell.setText("Restore Defaults");
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite area = (Composite) super.createDialogArea(parent);
			GridLayout layout = new GridLayout(1, false);
			layout.marginWidth = 16;
			layout.marginHeight = 12;
			layout.verticalSpacing = 6;
			area.setLayout(layout);

			// Question heading
			Label heading = new Label(area, SWT.WRAP);
			heading.setText("How would you like to reset editor appearance?");
			heading.setFont(org.eclipse.jface.resource.JFaceResources.getBannerFont());
			GridData headingData = new GridData(SWT.FILL, SWT.TOP, true, false);
			headingData.widthHint = 420;
			headingData.verticalIndent = 4;
			heading.setLayoutData(headingData);

			// Option 1 — Plugin default
			Button pluginDefaultRadio = new Button(area, SWT.RADIO);
			pluginDefaultRadio.setText("Plugin default theme");
			pluginDefaultRadio.setSelection(true);

			Label pluginDesc = new Label(area, SWT.WRAP);
			pluginDesc.setText("Applies the built-in theme matching your current Eclipse appearance (dark/light).");
			pluginDesc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			GridData pluginDescData = new GridData(SWT.FILL, SWT.TOP, true, false);
			pluginDescData.horizontalIndent = 18;
			pluginDescData.verticalIndent = 0;
			pluginDesc.setLayoutData(pluginDescData);

			// Option 2 — Remove only managed overrides
			Button stopManagementRadio = new Button(area, SWT.RADIO);
			stopManagementRadio.setText("Stop Eclipse Themes management");

			Label eclipseDesc = new Label(area, SWT.WRAP);
			eclipseDesc.setText("Removes only appearance overrides managed by this plugin, leaving unrelated settings unchanged.\n"
					+ "Eclipse resolves the remaining preference defaults. Previous custom values were not recorded and cannot be recovered.");
			eclipseDesc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			GridData eclipseDescData = new GridData(SWT.FILL, SWT.TOP, true, false);
			eclipseDescData.horizontalIndent = 18;
			eclipseDesc.setLayoutData(eclipseDescData);

			stopManagementRadio.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					stopManagementSelected = stopManagementRadio.getSelection();
				}
			});

			return area;
		}

		boolean isStopManagementSelected() {
			return stopManagementSelected;
		}
	}
}
