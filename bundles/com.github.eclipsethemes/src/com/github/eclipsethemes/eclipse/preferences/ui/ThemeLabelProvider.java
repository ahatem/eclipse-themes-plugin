package com.github.eclipsethemes.eclipse.preferences.ui;

import java.util.function.Supplier;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;

import com.github.eclipsethemes.eclipse.ThemeOrigin;
import com.github.eclipsethemes.eclipse.ThemeRecord;

/** The applied theme is bold, marked and says so, so it does not depend on a legend to be recognized. */
class ThemeLabelProvider extends LabelProvider implements IFontProvider {

	private final Supplier<ThemeRecord> applied;

	ThemeLabelProvider(Supplier<ThemeRecord> applied) {
		this.applied = applied;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof ThemeRecord theme) {
			String name = theme.theme().getName();
			if (theme.origin() == ThemeOrigin.INSTALLED) {
				name += " *";
			}
			return theme.equals(applied.get()) ? "● " + name + "  (applied)" : name;
		}
		return super.getText(element);
	}

	@Override
	public Font getFont(Object element) {
		// Borrowed from the JFace registry, which owns and disposes it.
		return element instanceof ThemeRecord theme && theme.equals(applied.get())
				? JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT)
				: null;
	}
}
