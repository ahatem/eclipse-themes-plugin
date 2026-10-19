package com.github.eclipsethemes.eclipse.fonts;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.github.eclipsethemes.core.models.ThemeFonts;

/**
 * The Eclipse font mechanism reduced to what {@link ThemeFontCoordinator} may do: read, put or remove the stored value
 * of one named font definition and flush. Values are Eclipse's stored font representation. A put or remove also
 * updates the running workbench; there is no way to clear a node or list its keys. Call on the UI thread.
 */
public interface WorkbenchFonts {

	/**
	 * One font definition as the installed bundles declare it.
	 *
	 * @param defaultsTo the definition it inherits from, or null
	 * @param hasValue   true when it declares its own fixed default value, so it never inherits
	 * @param editable   false for definitions Eclipse does not let users change
	 */
	record Definition(String defaultsTo, boolean hasValue, boolean editable) {
	}

	/** Every font definition declared by the installed bundles, by id. */
	Map<String, Definition> definitions();

	/** True when the font family is installed on this system. */
	boolean installed(String family);

	/** The stored representation of a font of this family and size, regular or bold. */
	String represent(ThemeFonts.Face face, boolean bold);

	/** The stored value of the font definition, or empty when it has none (its default applies). */
	Optional<String> get(String fontId);

	void put(String fontId, String stored);

	/** Removes the stored value so the definition's default applies again. */
	void remove(String fontId);

	/** The font the definition currently resolves to (stored value or default), for display. */
	Optional<ThemeFonts.Face> effective(String fontId);

	void flush() throws IOException;
}
