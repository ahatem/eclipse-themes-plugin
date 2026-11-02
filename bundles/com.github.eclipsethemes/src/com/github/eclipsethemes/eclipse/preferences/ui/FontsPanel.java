package com.github.eclipsethemes.eclipse.preferences.ui;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.models.ThemeFonts;
import com.github.eclipsethemes.eclipse.fonts.FontChoice;
import com.github.eclipsethemes.eclipse.fonts.FontMode;
import com.github.eclipsethemes.eclipse.fonts.FontSectionModel;
import com.github.eclipsethemes.eclipse.fonts.FontSettings;
import com.github.eclipsethemes.eclipse.fonts.FontTarget;
import com.github.eclipsethemes.eclipse.fonts.ThemeFontCoordinator;

/**
 * The fonts section: where each font comes from. The choices are staged here until {@link #commit()}; nothing is
 * written to the platform before that.
 */
public class FontsPanel {

	/** The widgets of one font target. */
	private record FontRow(FontTarget target, Combo mode, Button choose, Label description) {
	}

	private final PageContext page;
	private final Runnable relayout;
	private final Group group;
	private final Map<FontTarget, FontRow> fontRows = new EnumMap<>(FontTarget.class);
	/** The font choices as the user has them on the page; committed only by Apply or OK. */
	private FontSettings stagedFonts;

	/** @param relayout lays the details pane out again after the section's text changed */
	public FontsPanel(Composite parent, PageContext page, Runnable relayout) {
		this.page = page;
		this.relayout = relayout;
		stagedFonts = coordinator().settings();

		group = new Group(parent, SWT.NONE);
		group.setText("Fonts");
		GridLayout layout = new GridLayout(3, false);
		layout.verticalSpacing = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		for (FontTarget target : FontTarget.values()) {
			createFontRow(target);
		}

		Label hint = new Label(group, SWT.WRAP);
		hint.setText(FontSectionModel.HINT);
		GridData hintData = Layouts.wrapData();
		hintData.horizontalSpan = 3;
		hintData.verticalIndent = 4;
		hint.setLayoutData(hintData);
	}

	private static ThemeFontCoordinator coordinator() {
		return EclipseThemes.instance().getManager().fonts();
	}

	private static IPreferenceStore pluginStore() {
		return EclipseThemes.instance().getPreferenceStore();
	}

	private void createFontRow(FontTarget target) {
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
		GridData descriptionData = Layouts.wrapData();
		descriptionData.horizontalSpan = 2;
		descriptionData.verticalIndent = 0;
		description.setLayoutData(descriptionData);

		FontRow row = new FontRow(target, mode, choose, description);
		fontRows.put(target, row);
		mode.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> fontModeChanged(row)));
		choose.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (chooseCustomFont(target)) {
				refresh();
			}
		}));
	}

	/** Shows the staged font choices; nothing is written to the platform here. */
	public void refresh() {
		if (fontRows.isEmpty() || group.isDisposed()) {
			return;
		}
		ThemeFontCoordinator coordinator = coordinator();
		for (FontRow row : fontRows.values()) {
			FontChoice choice = stagedFonts.choice(row.target());
			row.mode().select(choice.mode().ordinal());
			row.choose().setEnabled(choice.mode() == FontMode.CUSTOM);
			row.description().setText(FontSectionModel.describe(row.target(), choice,
					page.selectedTheme() == null ? null : page.selectedTheme().theme(), coordinator::installed,
					coordinator.current(row.target())));
		}
		relayout.run();
	}

	private void fontModeChanged(FontRow row) {
		FontMode mode = FontMode.values()[Math.max(0, row.mode().getSelectionIndex())];
		FontChoice previous = stagedFonts.choice(row.target());
		stagedFonts = stagedFonts.with(row.target(), previous.withMode(mode));
		// Choosing Custom with no font yet goes straight to the chooser; cancelling leaves the previous source.
		if (mode == FontMode.CUSTOM && previous.custom() == null && !chooseCustomFont(row.target())) {
			stagedFonts = stagedFonts.with(row.target(), previous);
		}
		refresh();
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
				: coordinator().current(target);
		FontDialog dialog = new FontDialog(group.getShell());
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
			MessageDialog.openError(group.getShell(), "Font Not Supported",
					"That font cannot be used: the size must be between " + ThemeFonts.MIN_SIZE + " and "
							+ ThemeFonts.MAX_SIZE + " points.");
			return false;
		}
	}

	/** Writes the committed font choices; called only from Apply and OK. Returns false when they could not be saved. */
	public boolean commit() {
		try {
			stagedFonts.save(pluginStore());
			return true;
		} catch (IOException e) {
			EclipseThemes.instance().getLogger().error("Saving font settings failed", e);
			MessageDialog.openError(group.getShell(), "Fonts Not Saved",
					"Your font choices could not be saved: " + e.getMessage());
			return false;
		}
	}

	/** Stop management also sets both fonts to Keep current and gives back the fonts this plugin replaced. */
	public void release() {
		for (FontTarget target : FontTarget.values()) {
			stagedFonts = stagedFonts.with(target, stagedFonts.choice(target).withMode(FontMode.KEEP));
		}
		try {
			stagedFonts.save(pluginStore());
		} catch (IOException e) {
			EclipseThemes.instance().getLogger().error("Saving font settings failed", e);
		}
		ThemeFontCoordinator coordinator = coordinator();
		if (coordinator.ownsFonts()) {
			var outcome = coordinator.release();
			if (!outcome.succeeded()) {
				page.showStatus("Fonts: " + outcome.message(), IMessageProvider.WARNING);
			}
		}
		refresh();
	}
}
