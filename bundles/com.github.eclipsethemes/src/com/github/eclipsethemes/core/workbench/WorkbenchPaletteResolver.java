package com.github.eclipsethemes.core.workbench;

import java.util.EnumMap;
import java.util.Map;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;

/**
 * Derives the seven-color workbench palette from a theme's background, foreground, explicit selection
 * background and light/dark type. Pure and deterministic. No syntax token participates and no accent is derived.
 */
public final class WorkbenchPaletteResolver {

	static final double MIN_CONTRAST = 3.0;
	private static final double SURFACE = 0.05;
	private static final double RAISED = 0.10;
	private static final double BORDER = 0.22;
	private static final double MUTED = 0.35;
	private static final double SELECTION = 0.25;

	private WorkbenchPaletteResolver() {
	}

	public static WorkbenchPalette resolve(Theme theme) {
		WorkbenchFamily family = WorkbenchFamily.of(theme.getType());
		if (!theme.has(TokenKey.BACKGROUND) || !theme.has(TokenKey.FOREGROUND)) {
			return family.fallbackPalette();
		}
		Color background = theme.get(TokenKey.BACKGROUND).getColor();
		Color foreground = theme.get(TokenKey.FOREGROUND).getColor();
		if (contrast(foreground, background) < MIN_CONTRAST) {
			return family.fallbackPalette();
		}

		Color raised = blend(background, foreground, RAISED);
		Color muted = blend(foreground, background, MUTED);
		if (contrast(muted, raised) < MIN_CONTRAST) {
			muted = foreground;
		}
		Color selection = blend(background, foreground, SELECTION);
		if (theme.has(TokenKey.SELECTION_BACKGROUND)) {
			Color explicit = theme.get(TokenKey.SELECTION_BACKGROUND).getColor();
			if (!explicit.equals(background) && contrast(foreground, explicit) >= MIN_CONTRAST) {
				selection = explicit;
			}
		}

		Map<WorkbenchColor, Color> colors = new EnumMap<>(WorkbenchColor.class);
		colors.put(WorkbenchColor.BACKGROUND, background);
		colors.put(WorkbenchColor.FOREGROUND, foreground);
		colors.put(WorkbenchColor.SURFACE, blend(background, foreground, SURFACE));
		colors.put(WorkbenchColor.SURFACE_RAISED, raised);
		colors.put(WorkbenchColor.BORDER, blend(background, foreground, BORDER));
		colors.put(WorkbenchColor.MUTED_FOREGROUND, muted);
		colors.put(WorkbenchColor.SELECTION, selection);
		return new WorkbenchPalette(family, colors);
	}

	/** {@code from} moved {@code amount} (0..1) of the way toward {@code toward}, rounded per channel. */
	static Color blend(Color from, Color toward, double amount) {
		return Color.ofRgb(channel(from.red(), toward.red(), amount), channel(from.green(), toward.green(), amount),
				channel(from.blue(), toward.blue(), amount));
	}

	private static int channel(int from, int toward, double amount) {
		return (int) Math.round(from + (toward - from) * amount);
	}

	/** WCAG 2 contrast ratio from relative luminance. */
	static double contrast(Color a, Color b) {
		double la = luminance(a);
		double lb = luminance(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	private static double luminance(Color color) {
		return 0.2126 * linear(color.red()) + 0.7152 * linear(color.green()) + 0.0722 * linear(color.blue());
	}

	private static double linear(int channel) {
		double c = channel / 255.0;
		return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}
}
