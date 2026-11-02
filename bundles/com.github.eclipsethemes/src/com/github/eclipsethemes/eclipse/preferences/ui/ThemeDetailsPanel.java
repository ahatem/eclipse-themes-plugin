package com.github.eclipsethemes.eclipse.preferences.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.github.eclipsethemes.eclipse.ThemeRecord;

/**
 * The scrolling right-hand side of the Theme Manager and the header at its top: the selected theme's name, subtitle and
 * description. The preview, appearance, fonts and about sections are created in {@link #content()}.
 */
public class ThemeDetailsPanel {

	private final ScrolledComposite scroller;
	private final Composite details;
	private final Label themeNameLabel;
	private final Label subtitleLabel;
	private final Label descriptionLabel;

	public ThemeDetailsPanel(Composite parent) {
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

		Composite header = new Composite(details, SWT.NONE);
		GridLayout headerLayout = new GridLayout(1, false);
		headerLayout.marginWidth = 0;
		headerLayout.marginHeight = 0;
		headerLayout.verticalSpacing = 2;
		header.setLayout(headerLayout);
		header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		themeNameLabel = new Label(header, SWT.WRAP);
		themeNameLabel.setText("No theme selected");
		themeNameLabel.setFont(JFaceResources.getHeaderFont());
		themeNameLabel.setLayoutData(Layouts.wrapData());

		subtitleLabel = new Label(header, SWT.WRAP);
		subtitleLabel.setLayoutData(Layouts.wrapData());

		descriptionLabel = new Label(header, SWT.WRAP);
		descriptionLabel.setLayoutData(Layouts.wrapData());

		scroller.addListener(SWT.Resize, event -> updateScrollSize());
	}

	/** The composite the sections are created in. */
	public Composite content() {
		return details;
	}

	/** Recomputes the scrollable height for the current width, since wrapped text changes height with width. */
	public void updateScrollSize() {
		if (scroller.isDisposed()) {
			return;
		}
		int width = Math.max(scroller.getClientArea().width, 1);
		scroller.setMinHeight(details.computeSize(width, SWT.DEFAULT).y);
	}

	public void relayout() {
		details.layout(true, true);
		updateScrollSize();
	}

	public void show(ThemeRecord theme, boolean applied) {
		themeNameLabel.setText(theme.theme().getName());
		subtitleLabel.setText(subtitle(theme, applied));
		String description = theme.theme().getDescription();
		descriptionLabel.setText(description == null ? "" : description.strip());
		setExcluded(descriptionLabel, descriptionLabel.getText().isEmpty());
	}

	/** Updates the subtitle of the shown theme once it has become the applied one. */
	public void showApplied(ThemeRecord theme) {
		subtitleLabel.setText(subtitle(theme, true));
	}

	public void clear() {
		themeNameLabel.setText("No theme selected");
		subtitleLabel.setText("");
		descriptionLabel.setText("");
	}

	private static void setExcluded(Control control, boolean excluded) {
		control.setVisible(!excluded);
		((GridData) control.getLayoutData()).exclude = excluded;
	}

	/** The one line under the theme name: appearance, format, author and whether it is the applied theme. */
	static String subtitle(ThemeRecord theme, boolean applied) {
		List<String> parts = new ArrayList<>();
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
}
