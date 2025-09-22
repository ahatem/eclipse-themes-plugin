package com.github.eclipsethemes.preferences;

import java.util.List;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.FrameworkUtil;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.theme.models.Theme;

public class EclipseThemesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String PREVIEW_SERVER_URL = "https://eclipse-color-themes.vercel.app/preview/";

	private IWorkbench workbench;

	// --- UI Widgets ---
	private TableViewer themeViewer;
	private Browser browserPreview;
	private Label authorLabel;
	private Link websiteLink;
	private Button removeButton;

	private List<Theme> allThemes;
	private Theme selectedTheme;

	public EclipseThemesPreferencePage() {
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				String.valueOf(FrameworkUtil.getBundle(getClass()).getBundleId()));
		setPreferenceStore(scopedPreferenceStore);
	}

	@Override
	public void init(IWorkbench workbench) {
		this.workbench = workbench;
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		createTopPane(container);
		createBottomBar(container);

		initializeData();
		addListeners();

		return container;
	}

	/**
	 * Creates the top section containing the theme list, details, and preview.
	 */
	private void createTopPane(Composite parent) {
		Composite topPane = new Composite(parent, SWT.NONE);
		topPane.setLayout(new GridLayout(2, false));
		topPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createLeftPane(topPane);
		createRightPane(topPane);
	}

	/**
	 * Creates the left pane with the theme list and details.
	 */
	private void createLeftPane(Composite parent) {
		Composite leftPane = new Composite(parent, SWT.NONE);
		leftPane.setLayout(new GridLayout(1, false));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true);
		gd.widthHint = 220; // Give the list a comfortable width
		leftPane.setLayoutData(gd);

		new Label(leftPane, SWT.NONE).setText("Available Themes:");
		themeViewer = new TableViewer(leftPane, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
		themeViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		themeViewer.setContentProvider(ArrayContentProvider.getInstance());
		themeViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Theme) element).getName();
			}
		});

		Composite detailsGroup = new Composite(leftPane, SWT.NONE);
		detailsGroup.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		detailsGroup.setLayout(new GridLayout(2, false));

		new Label(detailsGroup, SWT.NONE).setText("Author:");
		authorLabel = new Label(detailsGroup, SWT.NONE);
		authorLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		new Label(detailsGroup, SWT.NONE).setText("Website:");
		websiteLink = new Link(detailsGroup, SWT.NONE);
		websiteLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	/**
	 * Creates the right pane with the code preview.
	 */
	private void createRightPane(Composite parent) {
		Composite rightPane = new Composite(parent, SWT.NONE);
		rightPane.setLayout(new GridLayout(1, false));
		rightPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		new Label(rightPane, SWT.NONE).setText("Live Preview:");

		browserPreview = new Browser(rightPane, SWT.BORDER | SWT.NONE);
		browserPreview.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	/**
	 * Creates the bottom action bar with the buttons.
	 */
	private void createBottomBar(Composite parent) {
		// A separator line for clean visual division
		Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Composite bottomBar = new Composite(parent, SWT.NONE);
		bottomBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		bottomBar.setLayout(new GridLayout(4, true)); // 4 buttons, equal width

		Button importButton = new Button(bottomBar, SWT.PUSH);
		importButton.setText("Import...");
		importButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		removeButton = new Button(bottomBar, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		removeButton.setEnabled(false);
	}

	/**
	 * Loads themes from the ThemeManager and populates the UI.
	 */
	private void initializeData() {
		this.allThemes = EclipseThemes.instance().getManager().getAllThemes();

		themeViewer.setInput(this.allThemes);

		if (!this.allThemes.isEmpty()) {
			String activeThemeId = getPreferenceStore().getString(Preferences.ACTIVE_THEME_ID);

			Theme activeTheme = this.allThemes.stream().filter(theme -> theme.getId().equals(activeThemeId)).findFirst()
					.orElse(this.allThemes.get(0));

			themeViewer.setSelection(new org.eclipse.jface.viewers.StructuredSelection(activeTheme));
		}
	}

	/**
	 * Attaches all necessary listeners to the UI widgets.
	 */
	private void addListeners() {
		themeViewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			handleThemeSelectionChanged((Theme) selection.getFirstElement());
		});

		// TODO: Add listener for Import button
		// TODO: Add listener for Remove button
	}

	/**
	 * Central handler for when a new theme is selected in the list.
	 */
	private void handleThemeSelectionChanged(Theme newTheme) {
		if (newTheme == null)
			return;
		this.selectedTheme = newTheme;

		updatePreviewPane(newTheme);
		updateDetailsPane(newTheme);

		// Enable the remove button only if the theme came from a user file
		removeButton.setEnabled(newTheme.getFile().isPresent());
	}

	/**
	 * Applies the selected theme's styles to the preview text widget.
	 */
	private void updatePreviewPane(Theme theme) {
		String themeId = theme.getId();
		String url = PREVIEW_SERVER_URL + themeId;

		// This is the core action: tell the browser to navigate to the new URL.
		browserPreview.setUrl(url);
	}

	/**
	 * Updates the author and website labels.
	 */
	private void updateDetailsPane(Theme theme) {
		authorLabel.setText(theme.getAuthor() != null ? theme.getAuthor() : "N/A");

		String website = theme.getWebsite();
		if (website != null && !website.isEmpty()) {
			websiteLink.setText("<a>" + website + "</a>");
		} else {
			websiteLink.setText("N/A");
		}

		websiteLink.getParent().layout();
	}

	// --- Standard Preference Page Methods ---
	@Override
	public boolean performOk() {
		if (selectedTheme != null) {
			System.out.println("Applying: " + selectedTheme.getName() );
			getPreferenceStore().setValue(Preferences.ACTIVE_THEME_ID, selectedTheme.getId());
			EclipseThemes.instance().getManager().applyTheme(workbench, selectedTheme);
		}
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		// This is called when the user clicks "Restore Defaults".
		// Select the default theme in the list.
		super.performDefaults();
	}

}
