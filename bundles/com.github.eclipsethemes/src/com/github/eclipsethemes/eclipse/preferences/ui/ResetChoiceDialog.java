package com.github.eclipsethemes.eclipse.preferences.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** Restore Defaults asks whether to apply the plugin default theme or only stop Eclipse Themes management. */
public class ResetChoiceDialog extends Dialog {

	private boolean stopManagementSelected = false;

	public ResetChoiceDialog(Shell shell) {
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

	public boolean isStopManagementSelected() {
		return stopManagementSelected;
	}
}
