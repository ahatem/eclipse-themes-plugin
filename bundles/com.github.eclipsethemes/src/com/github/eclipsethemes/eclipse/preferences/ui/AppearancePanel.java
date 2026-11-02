package com.github.eclipsethemes.eclipse.preferences.ui;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.State;
import com.github.eclipsethemes.core.workbench.WorkbenchDiagnostics;
import com.github.eclipsethemes.eclipse.ThemeRecord;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchCoordinator;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchOutcome;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchSectionModel;

/**
 * The appearance section: whether the selected theme also themes the workbench. The checkbox is the user's staged
 * choice until {@link #commit()}.
 */
public class AppearancePanel {

	private final PageContext page;
	private final Runnable relayout;
	private final Button workbenchCheckbox;
	private final Label workbenchStatusLabel;

	/**
	 * @param relayout            lays the details pane out again after the section's text changed
	 * @param openAppearancePage  opens Eclipse's own Appearance preference page
	 */
	public AppearancePanel(Composite parent, PageContext page, Runnable relayout, Runnable openAppearancePage) {
		this.page = page;
		this.relayout = relayout;

		Group group = new Group(parent, SWT.NONE);
		group.setText("Appearance");
		group.setLayout(new GridLayout(1, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		workbenchCheckbox = new Button(group, SWT.CHECK);
		workbenchCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		workbenchStatusLabel = new Label(group, SWT.WRAP);
		workbenchStatusLabel.setLayoutData(Layouts.wrapData());

		Label coverage = new Label(group, SWT.WRAP);
		coverage.setText("Native menus, dialogs and title bars are not themed.");
		coverage.setToolTipText(WorkbenchSectionModel.COVERAGE);
		coverage.setLayoutData(Layouts.wrapData());

		Link links = new Link(group, SWT.WRAP);
		links.setText("<a href=\"appearance\">Eclipse Appearance preferences</a>    <a href=\"diagnostics\">Copy diagnostics</a>");
		links.setToolTipText("Appearance: choose the Eclipse workbench theme manually. Diagnostics: copy environment "
				+ "and workbench-theming state to the clipboard; nothing is sent");
		links.setLayoutData(Layouts.wrapData());
		links.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if ("diagnostics".equals(e.text)) {
				copyDiagnostics();
			} else {
				openAppearancePage.run();
			}
		}));

		refresh(true);
	}

	private static WorkbenchCoordinator coordinator() {
		return EclipseThemes.instance().getManager().workbench();
	}

	private static IPreferenceStore pluginStore() {
		return EclipseThemes.instance().getPreferenceStore();
	}

	/**
	 * Shows the capability and stored intent. Only on first display does it set the checkbox from the stored
	 * intent; afterwards the checkbox is the user's uncommitted choice until Apply or OK.
	 */
	public void refresh(boolean initial) {
		if (workbenchCheckbox.isDisposed()) {
			return;
		}
		WorkbenchCoordinator coordinator = coordinator();
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
		relayout.run();
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
	public void commit() {
		if (!workbenchCheckbox.getEnabled()) {
			return;
		}
		WorkbenchCoordinator coordinator = coordinator();
		WorkbenchCapability capability = coordinator.capability();
		boolean checked = workbenchCheckbox.getSelection();
		IPreferenceStore store = pluginStore();
		store.setValue(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED, checked);
		if (capability.state() == State.EXPERIMENTAL) {
			store.setValue(PreferenceKeys.WORKBENCH_EXPERIMENTAL_ACK,
					checked ? capability.environment().combination() : "");
		}
	}

	/** Stop management also hands the workbench back, under the ownership rules (an external choice wins). */
	public void relinquish() {
		IPreferenceStore store = pluginStore();
		store.setValue(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED, false);
		store.setValue(PreferenceKeys.WORKBENCH_EXPERIMENTAL_ACK, "");
		WorkbenchCoordinator coordinator = coordinator();
		WorkbenchOutcome outcome = coordinator.isOwned() ? coordinator.relinquish() : WorkbenchOutcome.notRequested("");
		if (!workbenchCheckbox.isDisposed()) {
			workbenchCheckbox.setSelection(false);
		}
		refresh(false);
		if (outcome.status() != WorkbenchOutcome.Status.NOT_REQUESTED) {
			page.showStatus("Workbench: " + outcome.message(), IMessageProvider.INFORMATION);
		}
	}

	private void copyDiagnostics() {
		WorkbenchCoordinator coordinator = coordinator();
		WorkbenchCapability capability = coordinator.capability();
		ThemeRecord theme = page.selectedTheme() != null ? page.selectedTheme() : page.currentTheme();
		String text = WorkbenchDiagnostics.format(capability, coordinator.isEnabled(), coordinator.isOptedIn(capability),
				theme == null ? null : theme.theme().getName(), coordinator.activeThemeId().orElse(null),
				coordinator.isOwned(), coordinator.ownedPluginColorKeys(), coordinator.pluginColorSummary().userKeys(),
				coordinator.pluginColorSummary().releasedKeys());
		Clipboard clipboard = new Clipboard(workbenchCheckbox.getDisplay());
		try {
			clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
		page.showStatus("Diagnostics copied to the clipboard.", IMessageProvider.INFORMATION);
	}
}
