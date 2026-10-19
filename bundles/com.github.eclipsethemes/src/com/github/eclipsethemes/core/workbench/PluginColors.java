package com.github.eclipsethemes.core.workbench;

import java.util.LinkedHashMap;
import java.util.Map;

import com.github.eclipsethemes.core.models.Color;

/**
 * Derives the values of the coordinated plug-in keys from a workbench palette. Pure and deterministic; no SWT,
 * JFace or activator dependency.
 */
public final class PluginColors {

	/** Share of the way from the link color toward the foreground for a hovered link. */
	private static final double ACTIVE_LINK_MIX = 0.35;

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
			case POPUP_BACKGROUND -> palette.get(WorkbenchColor.SURFACE_RAISED).toEclipseFormat();
			case FOREGROUND -> palette.get(WorkbenchColor.FOREGROUND).toEclipseFormat();
			case LINK -> palette.get(WorkbenchColor.ACCENT).toEclipseFormat();
			case ACTIVE_LINK -> activeLink(palette).toEclipseFormat();
			case DARK_FLAG -> "true";
		};
	}

	private static Color activeLink(WorkbenchPalette palette) {
		Color foreground = palette.get(WorkbenchColor.FOREGROUND);
		Color mixed = PerceptualColor.of(palette.get(WorkbenchColor.ACCENT))
				.mixedToward(PerceptualColor.of(foreground), ACTIVE_LINK_MIX).toColor();
		for (WorkbenchColor surface : new WorkbenchColor[] { WorkbenchColor.BACKGROUND, WorkbenchColor.SURFACE,
				WorkbenchColor.SURFACE_RAISED }) {
			if (WorkbenchPaletteResolver.contrast(mixed, palette.get(surface)) < WorkbenchPaletteResolver.TEXT_CONTRAST) {
				return foreground;
			}
		}
		return mixed;
	}
}
