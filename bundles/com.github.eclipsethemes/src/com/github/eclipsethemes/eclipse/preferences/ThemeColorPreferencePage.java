package com.github.eclipsethemes.eclipse.preferences;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.TokenOptions;
import com.github.eclipsethemes.eclipse.ThemeManager;

public class ThemeColorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private IWorkbench workbench;
	private Theme baseTheme;
	private final Map<TokenKey, Token> overrides = new LinkedHashMap<>();

	private TreeViewer elementViewer;
	private ColorSelector colorSelector;
	private Button boldButton;
	private Button italicButton;
	private Button underlineButton;
	private Button strikethroughButton;
	private Button restoreElementButton;
	private StyledText preview;
	private Label inheritedLabel;
	private boolean updatingControls;
	private final java.util.List<org.eclipse.swt.graphics.Color> previewColors = new java.util.ArrayList<>();

	public ThemeColorPreferencePage() {
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, EclipseThemes.PLUGIN_ID));
		setDescription("Configure individual theme colors the same way Java syntax highlighting is configured.");
	}

	@Override
	public void init(IWorkbench workbench) {
		this.workbench = workbench;
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, true));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		loadWorkingCopy();
		createElementTree(container);
		createStyleControls(container);
		createPreview(container);

		if (baseTheme != null) {
			elementViewer.expandAll();
			Object first = ColorElementCatalog.categories().get(0).keys().get(0);
			elementViewer.setSelection(new StructuredSelection(first));
			handleSelectionChanged();
			refreshPreview();
		}

		return container;
	}

	private void createElementTree(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Element");
		group.setLayout(new GridLayout(1, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		elementViewer = new TreeViewer(group, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
		GridData treeData = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeData.heightHint = 280;
		elementViewer.getTree().setLayoutData(treeData);
		elementViewer.setContentProvider(new ElementContentProvider());
		elementViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ColorElementCatalog.Category category) {
					return category.name();
				}
				if (element instanceof TokenKey key) {
					String suffix = overrides.containsKey(key) ? " *" : "";
					return key.getDisplayName() + suffix;
				}
				return super.getText(element);
			}
		});
		elementViewer.setInput(ColorElementCatalog.categories());
		elementViewer.addSelectionChangedListener(event -> handleSelectionChanged());
	}

	private void createStyleControls(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Color and style");
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		new Label(group, SWT.NONE).setText("Color:");
		colorSelector = new ColorSelector(group);
		colorSelector.addListener(event -> {
			if (!updatingControls) {
				updateSelectedToken();
			}
		});

		boldButton = styleButton(group, "Bold");
		italicButton = styleButton(group, "Italic");
		underlineButton = styleButton(group, "Underline");
		strikethroughButton = styleButton(group, "Strikethrough");

		inheritedLabel = new Label(group, SWT.WRAP);
		GridData inheritData = new GridData(SWT.FILL, SWT.TOP, true, false);
		inheritData.horizontalSpan = 2;
		inheritedLabel.setLayoutData(inheritData);

		restoreElementButton = new Button(group, SWT.PUSH);
		restoreElementButton.setText("Restore selected element");
		GridData restoreData = new GridData(SWT.LEFT, SWT.TOP, false, false);
		restoreData.horizontalSpan = 2;
		restoreElementButton.setLayoutData(restoreData);
		restoreElementButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TokenKey key = selectedKey();
				if (key != null) {
					overrides.remove(key);
					elementViewer.refresh();
					handleSelectionChanged();
					refreshPreview();
				}
			}
		});
	}

	private Button styleButton(Composite parent, String text) {
		Button button = new Button(parent, SWT.CHECK);
		button.setText(text);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		button.setLayoutData(data);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!updatingControls) {
					updateSelectedToken();
				}
			}
		});
		return button;
	}

	private void createPreview(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Preview");
		group.setLayout(new GridLayout(1, false));
		GridData groupData = new GridData(SWT.FILL, SWT.FILL, true, false);
		groupData.horizontalSpan = 2;
		group.setLayoutData(groupData);

		preview = new StyledText(group, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData previewData = new GridData(SWT.FILL, SWT.FILL, true, true);
		previewData.heightHint = 160;
		preview.setLayoutData(previewData);
		preview.setFont(org.eclipse.jface.resource.JFaceResources.getTextFont());
		preview.setText("""
				package sample;

				/**
				 * Theme preview
				 */
				public class Sample {
				    // comment
				    public void run(int count) {
				        String text = "hello";
				        return count;
				    }
				}
				""");
	}

	private void loadWorkingCopy() {
		ThemeManager manager = EclipseThemes.instance().getManager();
		manager.loadThemes();
		String themeId = getPreferenceStore().getString(PreferenceKeys.ACTIVE_THEME_ID);
		Optional<Theme> theme = manager.findById(themeId);
		if (theme.isEmpty()) {
			theme = manager.getAllThemes().stream().findFirst();
		}
		baseTheme = theme.orElse(null);
		overrides.clear();
		if (baseTheme != null) {
			overrides.putAll(ColorCustomizationStore.load(baseTheme.getId()));
		} else {
			setErrorMessage("Apply a theme on the Eclipse Themes page before editing colors.");
			noDefaultAndApplyButton();
		}
	}

	private void handleSelectionChanged() {
		TokenKey key = selectedKey();
		updatingControls = true;
		try {
			boolean enabled = key != null && baseTheme != null;
			colorSelector.getButton().setEnabled(enabled);
			restoreElementButton.setEnabled(enabled && overrides.containsKey(key));
			boolean styles = enabled && ColorElementCatalog.supportsStyle(key);
			boldButton.setEnabled(styles);
			italicButton.setEnabled(styles);
			underlineButton.setEnabled(styles);
			strikethroughButton.setEnabled(styles);
			if (!enabled) {
				inheritedLabel.setText("");
				return;
			}
			Token token = effectiveToken(key);
			org.eclipse.swt.graphics.RGB rgb = new RGB(token.getColor().red(), token.getColor().green(),
					token.getColor().blue());
			colorSelector.setColorValue(rgb);
			TokenOptions options = token.getOptions().orElse(TokenOptions.empty());
			boldButton.setSelection(options.bold());
			italicButton.setSelection(options.italic());
			underlineButton.setSelection(options.underline());
			strikethroughButton.setSelection(options.strikethrough());
			if (overrides.containsKey(key)) {
				inheritedLabel.setText("Customized for this theme.");
			} else if (baseTheme.has(key)) {
				inheritedLabel.setText("From the selected theme.");
			} else if (key.getInheritsFrom() != null) {
				inheritedLabel.setText("Inherited from " + key.getInheritsFrom().getDisplayName() + ".");
			} else {
				inheritedLabel.setText("");
			}
		} finally {
			updatingControls = false;
		}
	}

	private void updateSelectedToken() {
		TokenKey key = selectedKey();
		if (key == null || baseTheme == null) {
			return;
		}
		RGB rgb = colorSelector.getColorValue();
		Color color = Color.ofRgb(rgb.red, rgb.green, rgb.blue);
		TokenOptions options = ColorElementCatalog.supportsStyle(key)
				? new TokenOptions(boldButton.getSelection(), italicButton.getSelection(),
						underlineButton.getSelection(), strikethroughButton.getSelection())
				: TokenOptions.empty();
		overrides.put(key, new Token(key, color, options));
		elementViewer.refresh();
		restoreElementButton.setEnabled(true);
		inheritedLabel.setText("Customized for this theme.");
		refreshPreview();
	}

	private void refreshPreview() {
		if (preview == null || preview.isDisposed() || baseTheme == null) {
			return;
		}
		Theme themed = workingTheme();
		disposePreviewColors();
		Color background = themed.get(TokenKey.BACKGROUND).getColor();
		Color foreground = themed.get(TokenKey.FOREGROUND).getColor();
		preview.setBackground(previewColor(background));
		preview.setForeground(previewColor(foreground));
		preview.setStyleRanges(new StyleRange[0]);
		colorRange(themed, "package", TokenKey.KEYWORD);
		colorRange(themed, "public", TokenKey.KEYWORD);
		colorRange(themed, "class", TokenKey.KEYWORD);
		colorRange(themed, "void", TokenKey.KEYWORD);
		colorRange(themed, "int", TokenKey.KEYWORD);
		colorRange(themed, "return", TokenKey.KEYWORD);
		colorRange(themed, "Sample", TokenKey.CLASS);
		colorRange(themed, "run", TokenKey.METHOD_DECLARATION);
		colorRange(themed, "count", TokenKey.ARGUMENT);
		colorRange(themed, "text", TokenKey.LOCAL_VARIABLE);
		colorRange(themed, "\"hello\"", TokenKey.STRING);
		colorRange(themed, "// comment", TokenKey.COMMENT);
		colorRange(themed, "Theme preview", TokenKey.DOC);
	}

	private void colorRange(Theme theme, String snippet, TokenKey key) {
		int index = 0;
		String text = preview.getText();
		Token token = theme.get(key);
		Color color = token.getColor();
		TokenOptions options = token.getOptions().orElse(TokenOptions.empty());
		while ((index = text.indexOf(snippet, index)) >= 0) {
			StyleRange range = new StyleRange();
			range.start = index;
			range.length = snippet.length();
			range.foreground = previewColor(color);
			range.fontStyle = (options.bold() ? SWT.BOLD : SWT.NORMAL) | (options.italic() ? SWT.ITALIC : SWT.NORMAL);
			range.underline = options.underline();
			range.strikeout = options.strikethrough();
			preview.setStyleRange(range);
			index += snippet.length();
		}
	}

	private org.eclipse.swt.graphics.Color previewColor(Color color) {
		org.eclipse.swt.graphics.Color swtColor = new org.eclipse.swt.graphics.Color(preview.getDisplay(), color.red(),
				color.green(), color.blue());
		previewColors.add(swtColor);
		return swtColor;
	}

	private void disposePreviewColors() {
		previewColors.forEach(org.eclipse.swt.graphics.Color::dispose);
		previewColors.clear();
	}

	@Override
	public void dispose() {
		disposePreviewColors();
		super.dispose();
	}

	private Token effectiveToken(TokenKey key) {
		if (overrides.containsKey(key)) {
			return overrides.get(key);
		}
		return workingTheme().get(key);
	}

	private Theme workingTheme() {
		Theme copy = baseTheme.copy();
		overrides.values().forEach(copy::addToken);
		return copy;
	}

	private TokenKey selectedKey() {
		Object element = elementViewer.getStructuredSelection().getFirstElement();
		return element instanceof TokenKey key ? key : null;
	}

	@Override
	public boolean performOk() {
		if (baseTheme != null) {
			ColorCustomizationStore.save(baseTheme.getId(), overrides);
			EclipseThemes.instance().getManager().applyTheme(workbench, baseTheme);
		}
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		if (baseTheme != null) {
			overrides.clear();
			elementViewer.refresh();
			handleSelectionChanged();
			refreshPreview();
		}
		super.performDefaults();
	}

	private static final class ElementContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return ColorElementCatalog.categories().toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ColorElementCatalog.Category category) {
				return category.keys().toArray();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof TokenKey key) {
				return ColorElementCatalog.categories().stream()
						.filter(category -> category.keys().contains(key))
						.findFirst().orElse(null);
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof ColorElementCatalog.Category;
		}
	}
}
