package com.github.eclipsethemes.eclipse.preferences.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import com.github.eclipsethemes.core.models.PackageMetadata;
import com.github.eclipsethemes.core.validation.WebsiteUri;
import com.github.eclipsethemes.eclipse.ThemeRecord;

/** The "About this theme" section: author, website and the provenance a theme package states. */
public class AboutPanel {

	static final String NOT_STATED = "Not stated";

	private final PageContext page;
	private final Group aboutGroup;
	private final Map<Control, Label> aboutCaptions = new HashMap<>();
	private final Label authorLabel;
	private final Link websiteLink;
	private final Label versionLabel;
	private final Label licenseLabel;
	private final Label copyrightLabel;
	private final Link sourceLink;

	public AboutPanel(Composite parent, PageContext page) {
		this.page = page;

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
		hide();

		websiteLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			// The same policy that decides the link markup decides the launch.
			ThemeRecord selected = page.selectedTheme();
			WebsiteUri.Check check = WebsiteUri.check(selected == null ? null : selected.theme().getWebsite());
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
	}

	private <T extends Control> T aboutRow(String caption, T value) {
		Label captionLabel = new Label(aboutGroup, SWT.NONE);
		captionLabel.setText(caption);
		captionLabel.moveAbove(value);
		captionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		value.setLayoutData(Layouts.wrapData());
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

	/** Hides the whole section, as when no theme is selected. */
	public void hide() {
		aboutCaptions.keySet().forEach(row -> showAboutRow(row, false));
		aboutGroup.setVisible(false);
		((GridData) aboutGroup.getLayoutData()).exclude = true;
	}

	/** Shows the rows the theme has something to say for; the caller lays the details pane out again. */
	public void show(ThemeRecord theme) {
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
	}

	private String selectedSource() {
		ThemeRecord selected = page.selectedTheme();
		return selected == null ? null
				: selected.theme().getPackageMetadata().map(PackageMetadata::source).orElse(null);
	}

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
}
