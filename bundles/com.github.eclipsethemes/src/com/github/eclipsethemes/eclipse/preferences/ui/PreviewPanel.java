package com.github.eclipsethemes.eclipse.preferences.ui;

import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.github.eclipsethemes.core.models.Constants;
import com.github.eclipsethemes.eclipse.ThemeRecord;

/** The preview section: a browser showing the online preview of the selected theme. */
public class PreviewPanel {

	// No fixed colors: the empty states follow the browser's light or dark scheme.
	private static final String PREVIEW_STYLE = "<style>:root{color-scheme:light dark}"
			+ "body{font-family:sans-serif;padding:16px;margin:0}h3{margin-top:0}</style>";

	private final PageContext page;
	private final Browser browserPreview;
	private final Button previewToggleButton;
	private boolean previewVisible = true;

	public PreviewPanel(Composite parent, PageContext page) {
		this.page = page;

		Group group = new Group(parent, SWT.NONE);
		group.setText("Preview");
		group.setLayout(new GridLayout(1, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		previewToggleButton = new Button(group, SWT.CHECK);
		previewToggleButton.setText("Show preview");
		previewToggleButton.setSelection(true);

		browserPreview = new Browser(group, SWT.BORDER);
		GridData browserData = new GridData(SWT.FILL, SWT.FILL, true, true);
		browserData.widthHint = Layouts.WRAP_HINT;
		browserData.heightHint = 200;
		browserData.minimumHeight = 140;
		browserPreview.setLayoutData(browserData);
		showDefault();

		previewToggleButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> togglePreviewVisibility()));
	}

	public void show(ThemeRecord theme) {
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
	 * The remote preview exists only for themes with a legacy source id: legacy themes and bundled themes (curated id).
	 * Imported packages have none, so neither their package id nor any of their metadata is ever sent to the preview
	 * server.
	 */
	public static Optional<String> previewUrl(ThemeRecord theme) {
		String id = theme.sourceId();
		return id == null || id.isBlank() ? Optional.empty() : Optional.of(Constants.PREVIEW_SERVER_URL + id);
	}

	public void showDefault() {
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
		ThemeRecord selected = page.selectedTheme();
		if (previewVisible && selected != null) {
			show(selected);
		} else if (!previewVisible) {
			showDefault();
		}
	}
}
