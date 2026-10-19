package com.github.eclipsethemes.core.workbench;

import java.util.LinkedHashMap;
import java.util.Map;

import com.github.eclipsethemes.core.models.Color;

/**
 * Derives the values of the coordinated plug-in keys from a workbench palette. Pure and deterministic; no SWT,
 * JFace or activator dependency.
 *
 * <p>
 * Status colors (error, warning, success, info) are not authored by themes. They use fixed OKLCH hues, like the
 * palette accent, and are corrected for readability on the content surfaces. Tints pull the content background toward
 * a status hue as far as the palette foreground stays readable on top of them.
 */
public final class PluginColors {

	/** Share of the way from the link color toward the foreground for a hovered link. */
	private static final double ACTIVE_LINK_MIX = 0.35;

	private static final double STATUS_LIGHTNESS = 0.74;
	private static final double STATUS_CHROMA = 0.15;
	private static final double ERROR_HUE = Math.toRadians(25);
	private static final double WARNING_HUE = Math.toRadians(85);
	private static final double SUCCESS_HUE = Math.toRadians(150);
	private static final double INFO_HUE = Math.toRadians(225);

	/** The strongest tint sits this much lighter than the background, with this chroma. */
	private static final double TINT_LIGHTNESS_STEP = 0.14;
	private static final double TINT_CHROMA = 0.09;
	private static final double TINT_STEP = 0.2;
	private static final double SOFT_TINT_SHARE = 0.5;

	private PluginColors() {
	}

	/**
	 * The key id ({@link PluginColorKey#id()}) to value map for this palette. Only the dark family coordinates
	 * foreign plug-in colors, so a light palette yields an empty map, which releases everything.
	 */
	public static Map<String, String> derive(WorkbenchPalette palette) {
		Map<String, String> values = new LinkedHashMap<>();
		if (palette.family() != WorkbenchFamily.DARK) {
			return values;
		}
		for (PluginColorKey key : PluginColorTable.keys()) {
			values.put(key.id(), value(key.source(), palette));
		}
		return values;
	}

	private static String value(PluginColorSource source, WorkbenchPalette palette) {
		return switch (source) {
			case BACKGROUND -> palette.get(WorkbenchColor.BACKGROUND).toEclipseFormat();
			case RAISED -> palette.get(WorkbenchColor.SURFACE_RAISED).toEclipseFormat();
			case BORDER -> palette.get(WorkbenchColor.BORDER).toEclipseFormat();
			case FOREGROUND -> palette.get(WorkbenchColor.FOREGROUND).toEclipseFormat();
			case MUTED -> palette.get(WorkbenchColor.MUTED_FOREGROUND).toEclipseFormat();
			case ACCENT -> palette.get(WorkbenchColor.ACCENT).toEclipseFormat();
			case ACTIVE_ACCENT -> activeAccent(palette).toEclipseFormat();
			case ERROR -> status(palette, ERROR_HUE).toEclipseFormat();
			case WARNING -> status(palette, WARNING_HUE).toEclipseFormat();
			case SUCCESS -> status(palette, SUCCESS_HUE).toEclipseFormat();
			case INFO -> status(palette, INFO_HUE).toEclipseFormat();
			case ERROR_TINT -> tint(palette, ERROR_HUE, 1.0).toEclipseFormat();
			case WARNING_TINT -> tint(palette, WARNING_HUE, 1.0).toEclipseFormat();
			case SUCCESS_TINT -> tint(palette, SUCCESS_HUE, 1.0).toEclipseFormat();
			case INFO_TINT -> tint(palette, INFO_HUE, 1.0).toEclipseFormat();
			case ERROR_TINT_SOFT -> tint(palette, ERROR_HUE, SOFT_TINT_SHARE).toEclipseFormat();
			case SUCCESS_TINT_SOFT -> tint(palette, SUCCESS_HUE, SOFT_TINT_SHARE).toEclipseFormat();
			case WARNING_TINT_SOFT -> tint(palette, WARNING_HUE, SOFT_TINT_SHARE).toEclipseFormat();
			case DARK_FLAG -> "true";
			case OFF_FLAG -> "false";
		};
	}

	private static Color activeAccent(WorkbenchPalette palette) {
		Color foreground = palette.get(WorkbenchColor.FOREGROUND);
		Color mixed = PerceptualColor.of(palette.get(WorkbenchColor.ACCENT))
				.mixedToward(PerceptualColor.of(foreground), ACTIVE_LINK_MIX).toColor();
		if (worstContrast(mixed, palette) < WorkbenchPaletteResolver.TEXT_CONTRAST) {
			return foreground;
		}
		return mixed;
	}

	private static Color status(WorkbenchPalette palette, double hue) {
		Color start = PerceptualColor.of(STATUS_LIGHTNESS, STATUS_CHROMA, hue).toColor();
		return WorkbenchPaletteResolver.readable(start, WorkbenchPaletteResolver.TEXT_CONTRAST, 1.0,
				palette.get(WorkbenchColor.BACKGROUND), palette.get(WorkbenchColor.SURFACE),
				palette.get(WorkbenchColor.SURFACE_RAISED));
	}

	/**
	 * The background pulled toward {@code hue}, as strongly as keeps the foreground readable on top of it. The
	 * search first gives up lightness, then chroma, so a theme whose foreground has little contrast to spare still
	 * gets a hue-only tint. {@code strength} (0..1) scales the strongest candidate. The background itself is the
	 * last resort.
	 */
	private static Color tint(WorkbenchPalette palette, double hue, double strength) {
		Color background = palette.get(WorkbenchColor.BACKGROUND);
		Color foreground = palette.get(WorkbenchColor.FOREGROUND);
		PerceptualColor base = PerceptualColor.of(background);
		for (double chromaShare = 1.0; chromaShare > 0.0; chromaShare -= TINT_STEP) {
			for (double lightnessShare = 1.0; lightnessShare >= 0.0; lightnessShare -= TINT_STEP) {
				PerceptualColor candidate = PerceptualColor.of(
						Math.min(1.0, base.l + TINT_LIGHTNESS_STEP * strength * lightnessShare),
						TINT_CHROMA * strength * chromaShare, hue);
				Color color = candidate.toColor();
				if (!color.equals(background)
						&& WorkbenchPaletteResolver.contrast(foreground, color) >= WorkbenchPaletteResolver.TEXT_CONTRAST) {
					return color;
				}
			}
		}
		return background;
	}

	private static double worstContrast(Color color, WorkbenchPalette palette) {
		return WorkbenchPaletteResolver.worstContrast(color, palette.get(WorkbenchColor.BACKGROUND),
				palette.get(WorkbenchColor.SURFACE), palette.get(WorkbenchColor.SURFACE_RAISED));
	}
}
