package com.github.eclipsethemes.eclipse.preferences;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
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
import com.github.eclipsethemes.core.models.PackageMetadata;
import com.github.eclipsethemes.core.models.ThemeFonts;
import com.github.eclipsethemes.core.parser.ParserFactory;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;
import com.github.eclipsethemes.core.validation.WebsiteUri;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.State;
import com.github.eclipsethemes.core.workbench.WorkbenchDiagnostics;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome;
import com.github.eclipsethemes.eclipse.ThemeManager;
import com.github.eclipsethemes.eclipse.ThemeOperationOutcome;
import com.github.eclipsethemes.eclipse.ThemeOrigin;
import com.github.eclipsethemes.eclipse.ThemeRecord;
import com.github.eclipsethemes.eclipse.fonts.FontChoice;
import com.github.eclipsethemes.eclipse.fonts.FontMode;
import com.github.eclipsethemes.eclipse.fonts.FontSectionModel;
import com.github.eclipsethemes.eclipse.fonts.FontSettings;
import com.github.eclipsethemes.eclipse.fonts.FontTarget;
import com.github.eclipsethemes.eclipse.fonts.ThemeFontCoordinator;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchCoordinator;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchOutcome;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchSectionModel;

/**
 * The Theme Manager: a theme library on the left, and on the right the selected theme with its preview, whether it
 * also themes the workbench, the fonts it will use and where it comes from.
 *
 * <p>
 * Everything on this page is staged page state (the selected theme, the workbench option and the font choices) until
 * the user chooses Apply or OK. Import, Export, Update and Remove act on the theme library and take effect at once.
 */
public class EclipseThemesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String APPEARANCE_PAGE_ID = "org.eclipse.ui.preferencePages.Views";
	private static final String ALL_THEMES = "All";
	private static final int WRAP_HINT = 120;

	private IWorkbench workbench;

	// Theme library
	private TableViewer themeViewer;
	private Text searchText;
	private Combo themeTypeCombo;
	private Label themeCountLabel;
	private Link downloadMoreLink;
	private Button importButton;
	private Button updateButton;
	private Button exportButton;
	private Button removeButton;

	// Selected theme
	private ScrolledComposite scroller;
	private Composite details;
	private Label themeNameLabel;
	private Label subtitleLabel;
	private Label descriptionLabel;
	private Browser browserPreview;
	private Button previewToggleButton;
	private Button workbenchCheckbox;
	private Label workbenchStatusLabel;
	private final Map<FontTarget, FontRow> fontRows = new EnumMap<>(FontTarget.class);
	private Group aboutGroup;
	private final Map<Control, Label> aboutCaptions = new HashMap<>();
	private Label authorLabel;
	private Link websiteLink;
	private Label versionLabel;
	private Label licenseLabel;
	private Label copyrightLabel;
	private Link sourceLink;

	// Data
	private List<ThemeRecord> allThemes;
	private List<ThemeRecord> filteredThemes;
	private ThemeRecord selectedTheme;
	private ThemeRecord currentTheme;
	private boolean previewVisible = true;
	private boolean listUpdating;
	/** The font choices as the user has them on the page; committed only by Apply or OK. */
	private FontSettings stagedFonts = FontSettings.load(new org.eclipse.jface.preference.PreferenceStore());

	/** The widgets of one font target. */
	private record FontRow(FontTarget target, Combo mode, Button choose, Label description) {
	}

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

		stagedFonts = EclipseThemes.instance().getManager().fonts().settings();

		SashForm sash = new SashForm(container, SWT.HORIZONTAL);
		GridData sashData = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashData.widthHint = 780;
		sashData.heightHint = 520;
		sashData.minimumHeight = 320;
		sash.setLayoutData(sashData);

		createLibrary(sash);
		createDetails(sash);
		sash.setWeights(new int[] { 32, 68 });

		// Load themes and set the initial state, then add listeners to all widgets
		initializeData();
		addListeners();

		return container;
	}

	private void createLibrary(Composite parent) {
		Composite library = new Composite(parent, SWT.NONE);
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
		themeViewer.setLabelProvider(new ThemeLabelProvider());

		Label legend = new Label(library, SWT.WRAP);
		legend.setText("* imported by you");
		legend.setLayoutData(wrapData());

		downloadMoreLink = new Link(library, SWT.NONE);
		downloadMoreLink.setText("<a>Download more themes</a>");
		downloadMoreLink.setToolTipText(Constants.MORE_THEMES_URL);
		downloadMoreLink.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		// Library actions: these change the library at once and are not part of Apply / OK.
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
	}

	private Button libraryButton(Composite parent, String text, String tooltip) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setToolTipText(tooltip);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return button;
	}

	private void createDetails(Composite parent) {
		scroller = new ScrolledComposite(parent, SWT.V_SCROLL);
		scroller.setExpandHorizontal(true);
		scroller.setExpandVertical(true);

		details = new Composite(scroller, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 0;
		layout.verticalSpacing = 8;
		details.setLayout(layout);
		scroller.setContent(details);

		createHeader(details);
		createPreviewSection(details);
		createAppearanceSection(details);
		createFontsSection(details);
		createAboutSection(details);

		scroller.addListener(SWT.Resize, event -> updateScrollSize());
		updateScrollSize();
	}

	/** Recomputes the scrollable height for the current width, since wrapped text changes height with width. */
	private void updateScrollSize() {
		if (scroller == null || scroller.isDisposed()) {
			return;
		}
		int width = Math.max(scroller.getClientArea().width, 1);
		scroller.setMinHeight(details.computeSize(width, SWT.DEFAULT).y);
	}

	private void relayoutDetails() {
		details.layout(true, true);
		updateScrollSize();
	}

	private static GridData wrapData() {
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		data.widthHint = WRAP_HINT;
		return data;
	}

	private void createHeader(Composite parent) {
		Composite header = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 2;
		header.setLayout(layout);
		header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		themeNameLabel = new Label(header, SWT.WRAP);
		themeNameLabel.setText("No theme selected");
		themeNameLabel.setFont(JFaceResources.getHeaderFont());
		themeNameLabel.setLayoutData(wrapData());

		subtitleLabel = new Label(header, SWT.WRAP);
		subtitleLabel.setLayoutData(wrapData());

		descriptionLabel = new Label(header, SWT.WRAP);
		descriptionLabel.setLayoutData(wrapData());
	}

	private void createPreviewSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Preview");
		group.setLayout(new GridLayout(1, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		previewToggleButton = new Button(group, SWT.CHECK);
		previewToggleButton.setText("Show preview");
		previewToggleButton.setSelection(true);

		browserPreview = new Browser(group, SWT.BORDER);
		GridData browserData = new GridData(SWT.FILL, SWT.FILL, true, true);
		browserData.widthHint = WRAP_HINT;
		browserData.heightHint = 200;
		browserData.minimumHeight = 140;
		browserPreview.setLayoutData(browserData);
		showDefaultPreview();
	}

	private void createAppearanceSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Appearance");
		group.setLayout(new GridLayout(1, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		workbenchCheckbox = new Button(group, SWT.CHECK);
		workbenchCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		workbenchStatusLabel = new Label(group, SWT.WRAP);
		workbenchStatusLabel.setLayoutData(wrapData());

		Label coverage = new Label(group, SWT.WRAP);
		coverage.setText("Native menus, dialogs and title bars are not themed.");
		coverage.setToolTipText(WorkbenchSectionModel.COVERAGE);
		coverage.setLayoutData(wrapData());

		Link links = new Link(group, SWT.WRAP);
		links.setText("<a href=\"appearance\">Eclipse Appearance preferences</a>    <a href=\"diagnostics\">Copy diagnostics</a>");
		links.setToolTipText("Appearance: choose the Eclipse workbench theme manually. Diagnostics: copy environment "
				+ "and workbench-theming state to the clipboard; nothing is sent");
		links.setLayoutData(wrapData());
		links.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if ("diagnostics".equals(e.text)) {
				copyDiagnostics();
			} else {
				openAppearance();
			}
		}));

		refreshWorkbenchSection(true);
	}

	private void createFontsSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Fonts");
		GridLayout layout = new GridLayout(3, false);
		layout.verticalSpacing = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		for (FontTarget target : FontTarget.values()) {
			createFontRow(group, target);
		}

		Label hint = new Label(group, SWT.WRAP);
		hint.setText(FontSectionModel.HINT);
		GridData hintData = wrapData();
		hintData.horizontalSpan = 3;
		hintData.verticalIndent = 4;
		hint.setLayoutData(hintData);
	}

	private void createFontRow(Composite group, FontTarget target) {
		String tooltip = target.description();
		Label label = new Label(group, SWT.NONE);
		label.setText(target.label() + ":");
		label.setToolTipText(tooltip);
		// The caption spans the combo row and the status line under it, so each font reads as one block.
		GridData labelData = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 2);
		labelData.verticalIndent = 4;
		label.setLayoutData(labelData);

		Combo mode = new Combo(group, SWT.READ_ONLY | SWT.DROP_DOWN);
		for (FontMode fontMode : FontMode.values()) {
			mode.add(fontMode.label());
		}
		mode.setToolTipText(tooltip);
		mode.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button choose = new Button(group, SWT.PUSH);
		choose.setText("Choose...");
		choose.setToolTipText("Choose an installed font for the " + target.role() + " font");

		Label description = new Label(group, SWT.WRAP);
		GridData descriptionData = wrapData();
		descriptionData.horizontalSpan = 2;
		descriptionData.verticalIndent = 0;
		description.setLayoutData(descriptionData);

		FontRow row = new FontRow(target, mode, choose, description);
		fontRows.put(target, row);
		mode.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> fontModeChanged(row)));
		choose.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (chooseCustomFont(target)) {
				updateFontRows();
			}
		}));
	}

	private void createAboutSection(Composite parent) {
		aboutGroup = new Group(parent, SWT.NONE);
		aboutGroup.setText("About this theme");
		aboutGroup.setLayout(new GridLayout(2, false));
		aboutGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		authorLabel = aboutRow("Author:", new Label(aboutGroup, SWT.WRAP));
		websiteLink = aboutRow("Website:", new Link(aboutGroup, SWT.WRAP));
		versionLabel = aboutRow("Version:", new Label(aboutGroup, SWT.WRAP));
		licenseLabel = aboutRow("License:", new Label(aboutGroup, SWT.WRAP));
		copyrightLabel = aboutRow("Copyright:", new Label(aboutGroup, SWT.WRAP));
		sourceLink = aboutRow("Source:", new Link(aboutGroup, SWT.WRAP));
		showAboutRows(false);
	}

	private <T extends Control> T aboutRow(String caption, T value) {
		Label captionLabel = new Label(aboutGroup, SWT.NONE);
		captionLabel.setText(caption);
		captionLabel.moveAbove(value);
		captionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		value.setLayoutData(wrapData());
		aboutCaptions.put(value, captionLabel);
		return value;
	}

	private void showAboutRow(Control value, boolean visible) {
		Label caption = aboutCaptions.get(value);
		for (Control control : new Control[] { caption, value }) {
			control.setVisible(visible);
			((GridData) control.getLayoutData()).exclude = !visible;
		}
	}

	private void showAboutRows(boolean visible) {
		aboutCaptions.keySet().forEach(row -> showAboutRow(row, visible));
		aboutGroup.setVisible(visible);
		((GridData) aboutGroup.getLayoutData()).exclude = !visible;
	}

	// ── Workbench option ──────────────────────────────────────────────────────

	private WorkbenchCoordinator workbenchCoordinator() {
		return EclipseThemes.instance().getManager().workbench();
	}

	private ThemeFontCoordinator fontCoordinator() {
		return EclipseThemes.instance().getManager().fonts();
	}

	private IPreferenceStore pluginStore() {
		return EclipseThemes.instance().getPreferenceStore();
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
		workbenchStatusLabel.setText(workbenchStatus(capability, model));
		if (initial) {
			boolean effective = enabled && (capability.state() == State.VERIFIED
					|| capability.state() == State.EXPERIMENTAL && coordinator.isOptedIn(capability));
			workbenchCheckbox.setSelection(effective);
		}
		if (details != null) {
			relayoutDetails();
		}
	}

	/** The user-facing status: what can be said about this system, without engine versions or internals. */
	static String workbenchStatus(WorkbenchCapability capability, WorkbenchSectionModel model) {
		String status = switch (capability.state()) {
		case VERIFIED -> "Supported on this system.";
		case EXPERIMENTAL -> "Not yet verified on this system; turning it on is an experiment.";
		default -> model.statusText();
		};
		return model.conflict() ? status + " Not in effect: another theme is chosen on the Appearance page." : status;
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
				coordinator.isOwned(), coordinator.ownedPluginColorKeys(), coordinator.pluginColorSummary().userKeys(),
				coordinator.pluginColorSummary().releasedKeys());
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		try {
			clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
		showStatus("Diagnostics copied to the clipboard.", IMessageProvider.INFORMATION);
	}

	// ── Fonts ─────────────────────────────────────────────────────────────────

	/** Shows the staged font choices; nothing is written to the platform here. */
	private void updateFontRows() {
		if (fontRows.isEmpty() || details == null || details.isDisposed()) {
			return;
		}
		ThemeFontCoordinator coordinator = fontCoordinator();
		for (FontRow row : fontRows.values()) {
			FontChoice choice = stagedFonts.choice(row.target());
			row.mode().select(choice.mode().ordinal());
			row.choose().setEnabled(choice.mode() == FontMode.CUSTOM);
			row.description().setText(FontSectionModel.describe(row.target(), choice,
					selectedTheme == null ? null : selectedTheme.theme(), coordinator::installed,
					coordinator.current(row.target())));
		}
		relayoutDetails();
	}

	private void fontModeChanged(FontRow row) {
		FontMode mode = FontMode.values()[Math.max(0, row.mode().getSelectionIndex())];
		FontChoice previous = stagedFonts.choice(row.target());
		stagedFonts = stagedFonts.with(row.target(), previous.withMode(mode));
		// Choosing Custom with no font yet goes straight to the chooser; cancelling leaves the previous source.
		if (mode == FontMode.CUSTOM && previous.custom() == null && !chooseCustomFont(row.target())) {
			stagedFonts = stagedFonts.with(row.target(), previous);
		}
		updateFontRows();
	}

	/**
	 * Lets the user pick an installed font with the standard dialog and stages it. Only family and size are used: the
	 * supported contract is a regular-weight font, so any style the dialog returns is ignored.
	 *
	 * @return true when a font was chosen
	 */
	private boolean chooseCustomFont(FontTarget target) {
		FontChoice choice = stagedFonts.choice(target);
		Optional<ThemeFonts.Face> start = choice.custom() != null ? Optional.of(choice.custom())
				: fontCoordinator().current(target);
		FontDialog dialog = new FontDialog(getShell());
		dialog.setText("Choose " + target.label().toLowerCase());
		dialog.setEffectsVisible(false);
		start.ifPresent(face -> dialog.setFontList(new FontData[] { new FontData(face.family(), face.size(), SWT.NORMAL) }));
		FontData chosen = dialog.open();
		if (chosen == null) {
			return false;
		}
		try {
			ThemeFonts.Face face = new ThemeFonts.Face(chosen.getName(), chosen.getHeight());
			stagedFonts = stagedFonts.with(target, choice.withCustom(face));
			return true;
		} catch (IllegalArgumentException e) {
			showErrorMessage("Font Not Supported", "That font cannot be used: the size must be between "
					+ ThemeFonts.MIN_SIZE + " and " + ThemeFonts.MAX_SIZE + " points.");
			return false;
		}
	}

	/** Writes the committed font choices; called only from Apply and OK. Returns false when they could not be saved. */
	private boolean commitFontsIntent() {
		try {
			stagedFonts.save(pluginStore());
			return true;
		} catch (IOException e) {
			EclipseThemes.instance().getLogger().error("Saving font settings failed", e);
			showErrorMessage("Fonts Not Saved", "Your font choices could not be saved: " + e.getMessage());
			return false;
		}
	}

	// ── Library ───────────────────────────────────────────────────────────────

	/** The applied theme is bold, marked and says so, so it does not depend on a legend to be recognized. */
	private class ThemeLabelProvider extends LabelProvider implements IFontProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof ThemeRecord theme) {
				String name = theme.theme().getName();
				if (theme.origin() == ThemeOrigin.INSTALLED) {
					name += " *";
				}
				return theme.equals(currentTheme) ? "● " + name + "  (applied)" : name;
			}
			return super.getText(element);
		}

		@Override
		public Font getFont(Object element) {
			// Borrowed from the JFace registry, which owns and disposes it.
			return element instanceof ThemeRecord theme && theme.equals(currentTheme)
					? JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT)
					: null;
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
			setListSelection(currentTheme);
		}
	}

	/** Selects in the list without treating it as the user choosing a theme. */
	private void setListSelection(ThemeRecord theme) {
		listUpdating = true;
		try {
			themeViewer.setSelection(new StructuredSelection(theme), true);
		} finally {
			listUpdating = false;
		}
	}

	private void refreshThemeList() {
		listUpdating = true;
		try {
			themeViewer.setInput(this.filteredThemes);
		} finally {
			listUpdating = false;
		}
		themeCountLabel.setText(this.filteredThemes.size() + " theme" + (this.filteredThemes.size() != 1 ? "s" : ""));
		themeCountLabel.getParent().layout();
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
					.collect(java.util.stream.Collectors.toList());
		}

		refreshThemeList();

		if (selectedTheme != null && filteredThemes.contains(selectedTheme)) {
			setListSelection(selectedTheme);
		} else if (selectedTheme == null) {
			ThemeRecord preferred = currentTheme != null && filteredThemes.contains(currentTheme) ? currentTheme
					: filteredThemes.isEmpty() ? null : filteredThemes.get(0);
			if (preferred == null) {
				clearSelection();
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

	private void addListeners() {
		// Theme selection
		themeViewer.addSelectionChangedListener(event -> {
			if (listUpdating) {
				return;
			}
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			ThemeRecord chosen = (ThemeRecord) selection.getFirstElement();
			if (chosen != null) {
				handleThemeSelectionChanged(chosen);
			}
		});

		themeViewer.addDoubleClickListener(event -> applySelectedTheme());

		// Search and filter
		searchText.addModifyListener(event -> filterThemes());
		themeTypeCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> filterThemes()));

		// Links
		downloadMoreLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> Program.launch(Constants.MORE_THEMES_URL)));

		websiteLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			// The same policy that decides the link markup decides the launch.
			WebsiteUri.Check check = WebsiteUri.check(selectedTheme == null ? null : selectedTheme.theme().getWebsite());
			if (check.launchable()) {
				Program.launch(check.value());
			}
		}));

		sourceLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			// Same policy as the website link: only a launchable value reaches the launcher.
			WebsiteUri.Check check = WebsiteUri.check(selectedSource());
			if (check.launchable()) {
				Program.launch(check.value());
			}
		}));

		// Buttons
		previewToggleButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> togglePreviewVisibility()));
		exportButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> exportSelectedTheme()));
		importButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> importTheme()));
		updateButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateSelectedTheme()));
		removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> removeSelectedTheme()));
	}

	// ── Selected theme ────────────────────────────────────────────────────────

	private void handleThemeSelectionChanged(ThemeRecord newTheme) {
		showStatus(null, IMessageProvider.NONE);

		if (newTheme == null) {
			clearSelection();
			return;
		}

		this.selectedTheme = newTheme;
		updateThemeDetails(newTheme);
		updatePreview(newTheme);
		updateFontRows();
		updateButtonStates();
	}

	private void clearSelection() {
		this.selectedTheme = null;
		themeNameLabel.setText("No theme selected");
		subtitleLabel.setText("");
		descriptionLabel.setText("");
		showAboutRows(false);
		showDefaultPreview();
		updateFontRows();
		updateButtonStates();
		relayoutDetails();
	}

	private void updateThemeDetails(ThemeRecord theme) {
		themeNameLabel.setText(theme.theme().getName());
		subtitleLabel.setText(subtitle(theme, theme.equals(currentTheme)));
		String description = theme.theme().getDescription();
		descriptionLabel.setText(description == null ? "" : description.strip());
		setExcluded(descriptionLabel, descriptionLabel.getText().isEmpty());

		String author = theme.theme().getAuthor();
		boolean hasAuthor = author != null && !author.isBlank();
		authorLabel.setText(hasAuthor ? author.strip() : "");
		showAboutRow(authorLabel, hasAuthor);

		String website = theme.theme().getWebsite();
		boolean hasWebsite = !WebsiteUri.check(website).value().isEmpty();
		websiteLink.setText(websiteText(website));
		websiteLink.setToolTipText(hasWebsite ? website : null);
		showAboutRow(websiteLink, hasWebsite);

		Optional<PackageMetadata> metadata = theme.theme().getPackageMetadata();
		boolean anyProvenance = metadata.isPresent() && (metadata.get().licenseText().isPresent()
				|| metadata.get().copyrightText().isPresent() || metadata.get().sourceText().isPresent());
		showAboutRow(versionLabel, metadata.flatMap(PackageMetadata::versionText).isPresent());
		showAboutRow(licenseLabel, metadata.isPresent() && (metadata.get().licenseText().isPresent() || !anyProvenance));
		showAboutRow(copyrightLabel, metadata.flatMap(PackageMetadata::copyrightText).isPresent());
		showAboutRow(sourceLink, metadata.flatMap(PackageMetadata::sourceText).isPresent());
		metadata.ifPresent(m -> {
			versionLabel.setText(stated(m.versionText()));
			licenseLabel.setText(stated(m.licenseText()));
			copyrightLabel.setText(stated(m.copyrightText()));
			sourceLink.setText(linkText(m.source(), NOT_STATED));
			sourceLink.setToolTipText(m.source());
		});

		boolean anyRow = hasAuthor || hasWebsite || metadata.isPresent();
		aboutGroup.setVisible(anyRow);
		((GridData) aboutGroup.getLayoutData()).exclude = !anyRow;
		relayoutDetails();
	}

	private static void setExcluded(Control control, boolean excluded) {
		control.setVisible(!excluded);
		((GridData) control.getLayoutData()).exclude = excluded;
	}

	/** The one line under the theme name: appearance, format, author and whether it is the applied theme. */
	static String subtitle(ThemeRecord theme, boolean applied) {
		List<String> parts = new java.util.ArrayList<>();
		parts.add(theme.theme().getType().nameCapitalized());
		parts.add(formatText(theme));
		String author = theme.theme().getAuthor();
		if (author != null && !author.isBlank()) {
			parts.add("by " + author.strip());
		}
		if (applied) {
			parts.add("Currently applied");
		}
		return String.join(" • ", parts);
	}

	/** Packages and legacy themes are different formats with different capabilities; say which one this is. */
	static String formatText(ThemeRecord theme) {
		return theme.theme().getPackageMetadata().isPresent() ? "Theme package (.ectheme)" : "Legacy XML theme";
	}

	private String selectedSource() {
		return selectedTheme == null ? null
				: selectedTheme.theme().getPackageMetadata().map(PackageMetadata::source).orElse(null);
	}

	static final String NOT_STATED = "Not stated";

	/** An absent provenance value is shown as not stated; nothing is assumed in its place. */
	static String stated(Optional<String> value) {
		return value.orElse(NOT_STATED);
	}

	/**
	 * Link markup is emitted only for a launchable value; everything else stays
	 * inert literal text so no metadata can reach the platform launcher.
	 */
	static String websiteText(String website) {
		return linkText(website, "N/A");
	}

	static String linkText(String value, String placeholder) {
		WebsiteUri.Check check = WebsiteUri.check(value);
		if (check.value().isEmpty()) {
			return placeholder;
		}
		String text = WebsiteUri.displayText(value);
		return check.launchable() ? "<a>" + text + "</a>" : text;
	}

	// ── Preview ───────────────────────────────────────────────────────────────

	private void updatePreview(ThemeRecord theme) {
		if (!previewVisible) {
			return;
		}

		Optional<String> url = previewUrl(theme);
		if (url.isEmpty()) {
			showPreviewUnavailable();
			return;
		}
		try {
			browserPreview.setUrl(url.get());
		} catch (Exception e) {
			showPreviewError("Could not load preview for this theme.");
		}
	}

	/**
	 * The remote preview exists only for themes with a legacy source id. Packages have none, so neither their package
	 * id nor any of their metadata is ever sent to the preview server.
	 */
	static Optional<String> previewUrl(ThemeRecord theme) {
		String id = theme.theme().getId();
		return id == null || id.isBlank() ? Optional.empty() : Optional.of(Constants.PREVIEW_SERVER_URL + id);
	}

	// No fixed colors: the empty states follow the browser's light or dark scheme.
	private static final String PREVIEW_STYLE = "<style>:root{color-scheme:light dark}"
			+ "body{font-family:sans-serif;padding:16px;margin:0}h3{margin-top:0}</style>";

	private void showDefaultPreview() {
		browserPreview.setText("<html><head>" + PREVIEW_STYLE + "</head><body><h3>Preview</h3>"
				+ "<p>Select a theme to see how it looks.</p></body></html>");
	}

	private void showPreviewUnavailable() {
		browserPreview.setText("<html><head>" + PREVIEW_STYLE + "</head><body><h3>No preview for this theme</h3>"
				+ "<p>Previews are available for themes from the online collection. "
				+ "Choose Apply to see this theme in your editor.</p></body></html>");
	}

	private void showPreviewError(String message) {
		browserPreview.setText("<html><head>" + PREVIEW_STYLE + "</head><body><h3>Preview error</h3><p>" + message
				+ "</p></body></html>");
	}

	private void togglePreviewVisibility() {
		previewVisible = previewToggleButton.getSelection();
		if (previewVisible && selectedTheme != null) {
			updatePreview(selectedTheme);
		} else if (!previewVisible) {
			showDefaultPreview();
		}
	}

	private void updateButtonStates() {
		boolean installedSelected = isInstalledTheme(selectedTheme);

		updateButton.setEnabled(installedSelected);
		exportButton.setEnabled(selectedTheme != null);
		removeButton.setEnabled(installedSelected);
	}

	private static boolean isInstalledTheme(ThemeRecord theme) {
		return theme != null && theme.origin() == ThemeOrigin.INSTALLED;
	}

	// ── Status ────────────────────────────────────────────────────────────────

	/** One inline message in the page's own message area; {@code null} restores the page description. */
	private void showStatus(String message, int type) {
		if (getControl() == null || getControl().isDisposed()) {
			return;
		}
		setMessage(message, type);
	}

	// ── Apply ─────────────────────────────────────────────────────────────────

	protected boolean applySelectedTheme() {
		if (selectedTheme == null) {
			return false;
		}

		try {
			commitWorkbenchIntent();
			if (!commitFontsIntent()) {
				return false;
			}
			ThemeApplyOutcome outcome = EclipseThemes.instance().getManager().applyTheme(workbench, selectedTheme);
			boolean editorApplied = ThemeSelection.rememberApplied(getPreferenceStore(), selectedTheme, outcome);

			if (editorApplied) {
				// The selected intent and the applied indicator change only after the editor half succeeded.
				currentTheme = selectedTheme;
				subtitleLabel.setText(subtitle(selectedTheme, true));
			}
			updateButtonStates();
			themeViewer.refresh(); // to update the "●" indicator
			refreshWorkbenchSection(false);
			updateFontRows();

			showStatus(WorkbenchSectionModel.summary(outcome),
					outcome.fullySucceeded() ? IMessageProvider.INFORMATION : IMessageProvider.WARNING);
			return editorApplied;
		} catch (Exception e) {
			showErrorMessage("Apply Theme Failed", "Failed to apply theme: " + e.getMessage());
			return false;
		}
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
			showStatus(verb + " '" + name + "'.", IMessageProvider.INFORMATION);
			return;
		}
		showStatus(verb + " '" + name + "' with " + warnings + (warnings == 1 ? " warning." : " warnings."),
				IMessageProvider.WARNING);
		showInfoMessage(verb + " With Warnings", "Theme '" + name + "' was " + verb.toLowerCase(Locale.ROOT) + "."
				+ warningsText(diagnostics));
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
		if (!isInstalledTheme(selectedTheme)) {
			return;
		}

		File selectedFile = chooseThemeFile("Update Selected Theme");
		if (selectedFile == null) {
			return;
		}
		boolean confirmed = showConfirmDialog("Update Theme",
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
		if (selectedTheme == null) {
			return;
		}
		boolean legacy = selectedTheme.theme().getPackageMetadata().isEmpty();
		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
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
			showStatus(legacy
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
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
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
			handleThemeSelectionChanged(match);
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
			selectedTheme = null;
			filterThemes();

			if (resetFailed) {
				showErrorMessage("Theme Removed",
						"The theme was removed, but resetting the applied theme to the default failed. "
								+ "Please select and apply a theme again.");
			} else {
				showStatus("Removed '" + removed.theme().getName() + "'.", IMessageProvider.INFORMATION);
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
		relinquishWorkbench();
		releaseFonts();
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
		refreshThemeList();
		if (currentTheme != null) {
			setListSelection(currentTheme);
			handleThemeSelectionChanged(currentTheme);
		}
	}

	private boolean stopThemeManagement() {
		PreferenceReset.Result result = new PreferenceReset(InstanceScope.INSTANCE::getNode, pluginStore())
				.resetAndCommit(() -> {
					ThemeSelection.clear(getPreferenceStore());
					currentTheme = null;
					refreshThemeList();
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
		relinquishWorkbench();
		releaseFonts();
		if (!result.conflicts().isEmpty()) {
			result.conflicts().forEach(conflict -> EclipseThemes.instance().getLogger()
					.warn("Editor preference kept: " + conflict.description()));
			showStatus("Management stopped; " + result.conflicts().size()
					+ " editor preference(s) changed outside Eclipse Themes were left alone.", IMessageProvider.WARNING);
		}
		return true;
	}

	/** Stop management also sets both fonts to Keep current and gives back the fonts this plugin replaced. */
	private void releaseFonts() {
		for (FontTarget target : FontTarget.values()) {
			stagedFonts = stagedFonts.with(target, stagedFonts.choice(target).withMode(FontMode.KEEP));
		}
		try {
			stagedFonts.save(pluginStore());
		} catch (IOException e) {
			EclipseThemes.instance().getLogger().error("Saving font settings failed", e);
		}
		ThemeFontCoordinator coordinator = fontCoordinator();
		if (coordinator.ownsFonts()) {
			var outcome = coordinator.release();
			if (!outcome.succeeded()) {
				showStatus("Fonts: " + outcome.message(), IMessageProvider.WARNING);
			}
		}
		updateFontRows();
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
			showStatus("Workbench: " + outcome.message(), IMessageProvider.INFORMATION);
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
			heading.setText("How would you like to reset your appearance?");
			heading.setFont(JFaceResources.getBannerFont());
			GridData headingData = new GridData(SWT.FILL, SWT.TOP, true, false);
			headingData.widthHint = 420;
			headingData.verticalIndent = 4;
			heading.setLayoutData(headingData);

			// Option 1 — Plugin default
			Button pluginDefaultRadio = new Button(area, SWT.RADIO);
			pluginDefaultRadio.setText("Plugin default theme");
			pluginDefaultRadio.setSelection(true);

			Label pluginDesc = new Label(area, SWT.WRAP);
			pluginDesc.setText("Applies the built-in theme matching your current Eclipse appearance (dark/light), turns workbench "
						+ "theming off and sets both fonts to Keep current font. Fonts it replaced are given back unless you changed them since.");
			GridData pluginDescData = new GridData(SWT.FILL, SWT.TOP, true, false);
			pluginDescData.horizontalIndent = 18;
			pluginDescData.verticalIndent = 0;
			pluginDesc.setLayoutData(pluginDescData);

			// Option 2 — Remove only managed overrides
			Button stopManagementRadio = new Button(area, SWT.RADIO);
			stopManagementRadio.setText("Stop Eclipse Themes management");

			Label eclipseDesc = new Label(area, SWT.WRAP);
			eclipseDesc.setText("Removes only appearance overrides managed by this plugin, leaving unrelated settings unchanged. "
					+ "Fonts it replaced are given back unless you changed them since, and both font choices are set to "
					+ "Keep current font.\n"
					+ "Eclipse resolves the remaining preference defaults. Previous custom values were not recorded and cannot be recovered.");
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
