package com.github.eclipsethemes.eclipse.adapters.ui;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;

/**
 * Maps the handful of surface colors an editor color theme defines onto the
 * roles the workbench chrome needs (trim bars, tab area, input fields, borders,
 * headers, disabled text, ...).
 *
 * <p>
 * Editor themes only carry a background, a foreground, a current-line and a
 * selection pair. Everything else is blended from those so that a theme never
 * falls back to the grey hard-coded in Eclipse's own dark stylesheet.
 * </p>
 */
final class WorkbenchPalette {

	private final Color background;
	private final Color foreground;
	private final Color selectionBackground;
	private final Color selectionForeground;
	private final Color chrome;
	private final Color elevated;
	private final Color hover;
	private final Color input;
	private final Color border;
	private final Color muted;
	private final Color disabled;
	private final Color link;
	private final boolean dark;

	private WorkbenchPalette(Theme theme) {
		background = theme.get(TokenKey.BACKGROUND).getColor();
		foreground = theme.get(TokenKey.FOREGROUND).getColor();
		selectionBackground = theme.get(TokenKey.SELECTION_BACKGROUND).getColor();
		selectionForeground = theme.get(TokenKey.SELECTION_FOREGROUND).getColor();
		dark = luminance(background) < 0.5;

		chrome = shade(background, dark ? -0.22 : -0.05);
		elevated = mix(background, foreground, 0.09);
		hover = mix(background, foreground, 0.16);
		input = dark ? elevated : shade(background, 0.45);
		border = mix(background, foreground, 0.25);
		muted = mix(foreground, background, 0.32);
		disabled = mix(foreground, background, 0.55);
		link = accent(theme);
	}

	static WorkbenchPalette of(Theme theme) {
		return new WorkbenchPalette(theme);
	}

	boolean isDark() {
		return dark;
	}

	/** Editors, views and dialog bodies. */
	String background() {
		return background.toString();
	}

	String foreground() {
		return foreground.toString();
	}

	/** Trim bars, status line, tab area and unselected tabs. */
	String chrome() {
		return chrome.toString();
	}

	/** Table/tree headers, section title bars and other raised surfaces. */
	String elevated() {
		return elevated.toString();
	}

	String hover() {
		return hover.toString();
	}

	/** Text, Combo, Spinner and other editable controls. */
	String input() {
		return input.toString();
	}

	String border() {
		return border.toString();
	}

	/** Secondary text such as unselected tab labels. */
	String muted() {
		return muted.toString();
	}

	String disabled() {
		return disabled.toString();
	}

	String link() {
		return link.toString();
	}

	String selectionBackground() {
		return selectionBackground.toString();
	}

	String selectionForeground() {
		return selectionForeground.toString();
	}

	/**
	 * Picks a syntax color that reads as a hyperlink. Falls back to the plain
	 * foreground when the theme has no usable accent.
	 */
	private Color accent(Theme theme) {
		for (TokenKey key : new TokenKey[] { TokenKey.DOC_LINK, TokenKey.METHOD, TokenKey.KEYWORD }) {
			if (!theme.has(key)) {
				continue;
			}
			Color candidate = theme.get(key).getColor();
			if (contrast(candidate, background) >= 3.0) {
				return candidate;
			}
		}
		return foreground;
	}

	private static Color mix(Color from, Color to, double ratio) {
		return Color.ofRgb(
				interpolate(from.red(), to.red(), ratio),
				interpolate(from.green(), to.green(), ratio),
				interpolate(from.blue(), to.blue(), ratio));
	}

	/** Moves a color towards white for a positive amount and towards black otherwise. */
	private static Color shade(Color color, double amount) {
		return mix(color, amount >= 0 ? Color.WHITE : Color.BLACK, Math.abs(amount));
	}

	private static int interpolate(int from, int to, double ratio) {
		return clamp((int) Math.round(from + (to - from) * ratio));
	}

	private static int clamp(int value) {
		return Math.max(0, Math.min(255, value));
	}

	private static double contrast(Color first, Color second) {
		double lighter = Math.max(luminance(first), luminance(second));
		double darker = Math.min(luminance(first), luminance(second));
		return (lighter + 0.05) / (darker + 0.05);
	}

	private static double luminance(Color color) {
		return 0.2126 * channel(color.red()) + 0.7152 * channel(color.green()) + 0.0722 * channel(color.blue());
	}

	private static double channel(int value) {
		double normalized = value / 255.0;
		return normalized <= 0.03928 ? normalized / 12.92 : Math.pow((normalized + 0.055) / 1.055, 2.4);
	}
}
