package com.github.eclipsethemes.eclipse.workbench;

import java.util.Optional;

import com.github.eclipsethemes.core.models.Color;

/** Where palette values are published so the static CSS can read them. */
public interface PaletteSink {

	Optional<Color> get(String colorId);

	void put(String colorId, Color color);
}
