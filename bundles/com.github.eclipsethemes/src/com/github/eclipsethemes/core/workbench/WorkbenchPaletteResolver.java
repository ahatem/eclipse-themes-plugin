package com.github.eclipsethemes.core.workbench;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;

/**
 * Deterministic semantic conversion of a theme into the workbench palette. Roles authored by a theme package are taken
 * verbatim; every other role is resolved as described below.
 *
 * <p>
 * Each role is resolved in this order: a suitable authored value, a suitable authored value of a related non-syntax
 * role, a perceptual derivation from the theme's own background and foreground (OKLCH, so hue and chroma character is
 * kept), a readability correction, and finally the family fallback palette. No syntax token (keyword, string, class,
 * method, ...) is ever read, and no random or external input is involved.
 *
 * <p>
 * Authored candidates are only taken when they pass a suitability test, so a neon current-line color never turns the
 * tab bar neon and a low-contrast line-number color never becomes secondary text.
 */
public final class WorkbenchPaletteResolver {

	/** Minimum contrast for text roles against every surface they are drawn on (WCAG AA, normal text). */
	static final double TEXT_CONTRAST = 4.5;
	/** Minimum contrast between the foreground and the selection background. */
	static final double SELECTION_CONTRAST = 3.0;

	private static final double SURFACE_MIN_DELTA = 0.012;
	private static final double SURFACE_MAX_DELTA = 0.08;
	private static final double RAISED_MAX_FROM_BACKGROUND = 0.13;
	private static final double SELECTION_MIN_DELTA = 0.04;
	private static final double CHROMA_SLACK = 0.05;
	private static final double SELECTION_CHROMA_SLACK = 0.12;
	private static final double DERIVED_SURFACE = 0.03;
	private static final double DERIVED_RAISED = 0.03;
	private static final double DERIVED_SELECTION = 0.10;
	private static final double BORDER_DISTANCE = 0.12;
	private static final double MUTED_MIX = 0.35;
	private static final double ACCENT_HUE = Math.toRadians(250);
	private static final double ACCENT_CHROMA = 0.13;
	private static final double LIGHTNESS_STEP = 0.005;

	private WorkbenchPaletteResolver() {
	}

	public static WorkbenchPalette resolve(Theme theme) {
		WorkbenchFamily family = WorkbenchFamily.of(theme.getType());
		if (!theme.has(TokenKey.BACKGROUND) || !theme.has(TokenKey.FOREGROUND)) {
			return family.fallbackPalette();
		}
		Map<String, Color> authored = theme.getAuthoredWorkbench();
		// Authored background and foreground replace the editor base for chrome only.
		Color background = authored.getOrDefault(WorkbenchColor.BACKGROUND.schemaName(),
				theme.get(TokenKey.BACKGROUND).getColor());
		Color authoredForeground = authored.getOrDefault(WorkbenchColor.FOREGROUND.schemaName(),
				theme.get(TokenKey.FOREGROUND).getColor());
		PerceptualColor bg = PerceptualColor.of(background);
		// Chrome moves toward the foreground side of the lightness range: lighter on dark themes, darker on light.
		double direction = bg.l < 0.5 ? 1.0 : -1.0;

		// Each authored role is taken verbatim; derivations that depend on a role use its final value.
		Color surface = authored(authored, WorkbenchColor.SURFACE,
				() -> surface(theme, background, authoredForeground, bg, direction));
		Color raised = authored(authored, WorkbenchColor.SURFACE_RAISED,
				() -> raised(theme, background, surface, authoredForeground, bg, direction));
		Color foreground = authored(authored, WorkbenchColor.FOREGROUND,
				() -> readable(authoredForeground, TEXT_CONTRAST, direction, background, surface, raised));
		Color border = authored(authored, WorkbenchColor.BORDER, () -> border(background, surface, raised, direction));
		Color selection = authored(authored, WorkbenchColor.SELECTION,
				() -> selection(theme, background, foreground, bg, direction));
		Color muted = authored(authored, WorkbenchColor.MUTED_FOREGROUND,
				() -> muted(theme, foreground, direction, background, surface));
		Color accent = authored(authored, WorkbenchColor.ACCENT,
				() -> readable(PerceptualColor.of(direction > 0 ? 0.74 : 0.48, ACCENT_CHROMA, ACCENT_HUE).toColor(),
						TEXT_CONTRAST, direction, background, surface, raised));

		Map<WorkbenchColor, Color> colors = new EnumMap<>(WorkbenchColor.class);
		colors.put(WorkbenchColor.BACKGROUND, background);
		colors.put(WorkbenchColor.FOREGROUND, foreground);
		colors.put(WorkbenchColor.SURFACE, surface);
		colors.put(WorkbenchColor.SURFACE_RAISED, raised);
		colors.put(WorkbenchColor.BORDER, border);
		colors.put(WorkbenchColor.MUTED_FOREGROUND, muted);
		colors.put(WorkbenchColor.SELECTION, selection);
		colors.put(WorkbenchColor.ACCENT, accent);
		WorkbenchPalette palette = new WorkbenchPalette(family, colors);
		if (isCoherent(palette)) {
			return palette;
		}
		if (authored.isEmpty()) {
			return family.fallbackPalette();
		}
		// Authored palettes keep their surfaces: only the text roles are corrected, by lightness, before falling back.
		colors.put(WorkbenchColor.FOREGROUND, readable(foreground, TEXT_CONTRAST, direction, background, surface, raised));
		colors.put(WorkbenchColor.MUTED_FOREGROUND, readable(muted, TEXT_CONTRAST, direction, background, surface));
		colors.put(WorkbenchColor.ACCENT, readable(accent, TEXT_CONTRAST, direction, background, surface, raised));
		WorkbenchPalette corrected = new WorkbenchPalette(family, colors);
		return isCoherent(corrected) ? corrected : family.fallbackPalette();
	}

	/**
	 * The authored roles the resolved palette does not show verbatim, because they were corrected for readability or the
	 * palette fell back to the family palette. Empty for themes without authored roles.
	 */
	public static Set<WorkbenchColor> unreadableAuthoredRoles(Theme theme) {
		Map<String, Color> authored = theme.getAuthoredWorkbench();
		if (authored.isEmpty()) {
			return Set.of();
		}
		WorkbenchPalette palette = resolve(theme);
		Set<WorkbenchColor> changed = EnumSet.noneOf(WorkbenchColor.class);
		for (WorkbenchColor role : WorkbenchColor.values()) {
			Color value = authored.get(role.schemaName());
			if (value != null && !value.equals(palette.get(role))) {
				changed.add(role);
			}
		}
		return changed;
	}

	private static Color authored(Map<String, Color> authored, WorkbenchColor role, Supplier<Color> derived) {
		Color value = authored.get(role.schemaName());
		return value != null ? value : derived.get();
	}

	/** The chrome base: the authored secondary surface when it reads as one, otherwise the background nudged. */
	private static Color surface(Theme theme, Color background, Color foreground, PerceptualColor bg, double direction) {
		for (TokenKey key : new TokenKey[] { TokenKey.SOURCE_HOVER_BACKGROUND, TokenKey.CURRENT_LINE }) {
			if (theme.has(key)) {
				Color candidate = theme.get(key).getColor();
				PerceptualColor c = PerceptualColor.of(candidate);
				double delta = Math.abs(c.l - bg.l);
				if (delta >= SURFACE_MIN_DELTA && delta <= SURFACE_MAX_DELTA && c.c <= bg.c + CHROMA_SLACK
						&& contrast(foreground, candidate) >= SELECTION_CONTRAST) {
					return candidate;
				}
			}
		}
		return bg.shifted(direction * DERIVED_SURFACE).toColor();
	}

	/** The selected-tab surface: further toward the foreground than the surface, and visibly distinct from it. */
	private static Color raised(Theme theme, Color background, Color surface, Color foreground, PerceptualColor bg,
			double direction) {
		PerceptualColor s = PerceptualColor.of(surface);
		for (TokenKey key : new TokenKey[] { TokenKey.CURRENT_LINE, TokenKey.SOURCE_HOVER_BACKGROUND }) {
			if (theme.has(key)) {
				Color candidate = theme.get(key).getColor();
				PerceptualColor c = PerceptualColor.of(candidate);
				if ((c.l - s.l) * direction >= SURFACE_MIN_DELTA && Math.abs(c.l - bg.l) <= RAISED_MAX_FROM_BACKGROUND
						&& c.c <= bg.c + CHROMA_SLACK && contrast(foreground, candidate) >= SELECTION_CONTRAST) {
					return candidate;
				}
			}
		}
		return s.shifted(direction * DERIVED_RAISED).toColor();
	}

	/** Always derived: no authored role means a separator. Takes the tint of the most prominent chrome color. */
	private static Color border(Color background, Color surface, Color raised, double direction) {
		PerceptualColor base = PerceptualColor.of(background);
		for (Color candidate : new Color[] { surface, raised }) {
			PerceptualColor c = PerceptualColor.of(candidate);
			if ((c.l - base.l) * direction > 0) {
				base = c;
			}
		}
		return base.shifted(direction * BORDER_DISTANCE).toColor();
	}

	/** The authored selection background when it is visible and readable under the foreground, otherwise derived. */
	private static Color selection(Theme theme, Color background, Color foreground, PerceptualColor bg,
			double direction) {
		if (theme.has(TokenKey.SELECTION_BACKGROUND)) {
			Color candidate = theme.get(TokenKey.SELECTION_BACKGROUND).getColor();
			PerceptualColor c = PerceptualColor.of(candidate);
			if (Math.abs(c.l - bg.l) >= SELECTION_MIN_DELTA && c.c <= bg.c + SELECTION_CHROMA_SLACK
					&& contrast(foreground, candidate) >= SELECTION_CONTRAST) {
				return candidate;
			}
		}
		PerceptualColor derived = bg.shifted(direction * DERIVED_SELECTION);
		for (int i = 0; i < 20; i++) {
			Color color = derived.withChroma(Math.max(derived.c, 0.03)).toColor();
			if (contrast(foreground, color) >= SELECTION_CONTRAST) {
				return color;
			}
			derived = derived.shifted(-direction * 0.01);
		}
		return background;
	}

	/**
	 * The authored line-number color when it is readable on the background and the surface (where unselected tabs and
	 * secondary labels are drawn), otherwise the foreground faded toward the background.
	 */
	private static Color muted(Theme theme, Color foreground, double direction, Color... surfaces) {
		if (theme.has(TokenKey.LINE_NUMBER)) {
			Color candidate = theme.get(TokenKey.LINE_NUMBER).getColor();
			if (worstContrast(candidate, surfaces) >= TEXT_CONTRAST && contrast(candidate, foreground) > 1.15) {
				return candidate;
			}
		}
		PerceptualColor fg = PerceptualColor.of(foreground);
		PerceptualColor bg = PerceptualColor.of(surfaces[0]);
		return readable(fg.mixedToward(bg, MUTED_MIX).toColor(), TEXT_CONTRAST, direction, surfaces);
	}

	/**
	 * Moves the color's lightness (keeping hue and chroma) away from the surfaces until it reaches the contrast on
	 * every one of them. Falls back to black or white only if no lightness works.
	 */
	static Color readable(Color color, double minimum, double direction, Color... surfaces) {
		if (worstContrast(color, surfaces) >= minimum) {
			return color;
		}
		PerceptualColor start = PerceptualColor.of(color);
		Color best = color;
		double bestContrast = worstContrast(color, surfaces);
		for (double lightness = start.l; lightness >= 0.0 && lightness <= 1.0; lightness += direction * LIGHTNESS_STEP) {
			Color trial = start.withLightness(lightness).toColor();
			double worst = worstContrast(trial, surfaces);
			if (worst > bestContrast) {
				best = trial;
				bestContrast = worst;
			}
			if (worst >= minimum) {
				return trial;
			}
		}
		Color extreme = direction > 0 ? Color.WHITE : Color.BLACK;
		return worstContrast(extreme, surfaces) > bestContrast ? extreme : best;
	}

	private static boolean isCoherent(WorkbenchPalette palette) {
		Color bg = palette.get(WorkbenchColor.BACKGROUND);
		Color surface = palette.get(WorkbenchColor.SURFACE);
		Color raised = palette.get(WorkbenchColor.SURFACE_RAISED);
		return worstContrast(palette.get(WorkbenchColor.FOREGROUND), bg, surface, raised) >= TEXT_CONTRAST
				&& worstContrast(palette.get(WorkbenchColor.MUTED_FOREGROUND), bg, surface) >= TEXT_CONTRAST
				&& worstContrast(palette.get(WorkbenchColor.ACCENT), bg, surface, raised) >= TEXT_CONTRAST
				&& contrast(palette.get(WorkbenchColor.FOREGROUND),
						palette.get(WorkbenchColor.SELECTION)) >= SELECTION_CONTRAST;
	}

	static double worstContrast(Color color, Color... surfaces) {
		double worst = Double.MAX_VALUE;
		for (Color surface : surfaces) {
			worst = Math.min(worst, contrast(color, surface));
		}
		return worst;
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
