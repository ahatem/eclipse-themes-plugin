package com.github.eclipsethemes.eclipse.fonts;

import java.io.IOException;
import java.util.Optional;

import com.github.eclipsethemes.core.models.ThemeFonts;

/**
 * The Eclipse font mechanism reduced to what {@link ThemeFontCoordinator} may do: read, put or remove the stored value
 * of one named font definition and flush. Values are Eclipse's stored font representation. A put or remove also
 * updates the running workbench; there is no way to clear a node or list its keys. Call on the UI thread.
 */
public interface WorkbenchFonts {

	/** True when the font family is installed on this system. */
	boolean installed(String family);

	/** The stored representation of a regular-weight font of this family and size. */
	String represent(ThemeFonts.Face face);

	/** The stored value of the font definition, or empty when it has none (its default applies). */
	Optional<String> get(String fontId);

	void put(String fontId, String stored);

	/** Removes the stored value so the definition's default applies again. */
	void remove(String fontId);

	/** The font the definition currently resolves to (stored value or default), for display. */
	Optional<ThemeFonts.Face> effective(String fontId);

	void flush() throws IOException;
}
