package com.github.eclipsethemes.eclipse.adapters;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.eclipsethemes.eclipse.adapters.editor.JavaEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.TextEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.ui.GtkCssThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.ui.WorkbenchCssThemeAdapter;

class AdapterOrderTest {

	@Test
	void workbenchStylingRunsBeforeTheEditorPreferences() {
		int editors = Math.min(new TextEditorThemeAdapter().getOrder(), new JavaEditorThemeAdapter().getOrder());

		assertTrue(new WorkbenchCssThemeAdapter().getOrder() < editors,
				"Eclipse's base stylesheet rewrites the editor colors on a theme change, "
						+ "so the workbench CSS has to be applied first");
		assertTrue(new GtkCssThemeAdapter().getOrder() < editors);
	}
}
