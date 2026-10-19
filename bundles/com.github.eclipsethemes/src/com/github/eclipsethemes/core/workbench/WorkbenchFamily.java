package com.github.eclipsethemes.core.workbench;

import java.util.EnumMap;
import java.util.Map;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.ThemeType;

/**
 * The two static CSS families. The dark theme ids contain {@code dark} and the light ids do not:
 * Eclipse derives its dark-preferred setting from {@code id.contains("dark")}.
 */
public enum WorkbenchFamily {
	DARK("dark", ThemeType.DARK, "#1e1e1e", "#d4d4d4", "#252526", "#2d2d30", "#3c3c3c", "#9d9d9d", "#264f78", "#5aa2e8"),
	LIGHT("light", ThemeType.LIGHT, "#ffffff", "#1f1f1f", "#f3f3f3", "#e8e8e8", "#cecece", "#6b6b6b", "#cce4f7", "#0b61c4");

	private static final String THEME_ID_PREFIX = "com.github.eclipsethemes.workbench.";

	private final String key;
	private final ThemeType type;
	private final Map<WorkbenchColor, Color> fallback = new EnumMap<>(WorkbenchColor.class);

	WorkbenchFamily(String key, ThemeType type, String background, String foreground, String surface, String raised,
			String border, String muted, String selection, String accent) {
		this.key = key;
		this.type = type;
		fallback.put(WorkbenchColor.BACKGROUND, Color.ofHex(background));
		fallback.put(WorkbenchColor.FOREGROUND, Color.ofHex(foreground));
		fallback.put(WorkbenchColor.SURFACE, Color.ofHex(surface));
		fallback.put(WorkbenchColor.SURFACE_RAISED, Color.ofHex(raised));
		fallback.put(WorkbenchColor.BORDER, Color.ofHex(border));
		fallback.put(WorkbenchColor.MUTED_FOREGROUND, Color.ofHex(muted));
		fallback.put(WorkbenchColor.SELECTION, Color.ofHex(selection));
		fallback.put(WorkbenchColor.ACCENT, Color.ofHex(accent));
	}

	public static WorkbenchFamily of(ThemeType type) {
		return type == ThemeType.DARK ? DARK : LIGHT;
	}

	public String key() {
		return key;
	}

	/** Slot A theme id. */
	public String themeId() {
		return THEME_ID_PREFIX + key;
	}

	/** Slot B theme id (see {@link WorkbenchSlots}). */
	public String alternateThemeId() {
		return themeId() + ".alt";
	}

	/** The palette that {@code plugin.xml} declares as the colorDefinition default of this family. */
	public WorkbenchPalette fallbackPalette() {
		return new WorkbenchPalette(this, fallback);
	}
}
