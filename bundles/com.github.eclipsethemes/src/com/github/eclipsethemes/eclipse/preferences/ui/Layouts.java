package com.github.eclipsethemes.eclipse.preferences.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

/** Layout data shared by the panels. */
final class Layouts {

	static final int WRAP_HINT = 120;

	private Layouts() {
	}

	static GridData wrapData() {
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		data.widthHint = WRAP_HINT;
		return data;
	}
}
