package com.github.eclipsethemes.core.workbench;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.github.eclipsethemes.core.models.Color;

/** An immutable set of exactly one color per palette role for one family. */
public final class WorkbenchPalette {

	private final WorkbenchFamily family;
	private final Map<WorkbenchColor, Color> colors;

	public WorkbenchPalette(WorkbenchFamily family, Map<WorkbenchColor, Color> colors) {
		this.family = Objects.requireNonNull(family, "family");
		this.colors = new EnumMap<>(WorkbenchColor.class);
		for (WorkbenchColor role : WorkbenchColor.values()) {
			this.colors.put(role, Objects.requireNonNull(colors.get(role), "missing palette color " + role));
		}
	}

	public WorkbenchFamily family() {
		return family;
	}

	public Color get(WorkbenchColor role) {
		return colors.get(role);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof WorkbenchPalette p && family == p.family && colors.equals(p.colors);
	}

	@Override
	public int hashCode() {
		return Objects.hash(family, colors);
	}

	@Override
	public String toString() {
		return family + colors.toString();
	}
}
