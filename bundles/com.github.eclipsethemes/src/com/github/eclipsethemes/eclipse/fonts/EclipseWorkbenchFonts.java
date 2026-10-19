package com.github.eclipsethemes.eclipse.fonts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.IThemeManager;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.ThemeFonts;

/**
 * {@link WorkbenchFonts} over the mechanism Eclipse's own Colors and Fonts page uses: the stored value lives in the
 * instance preferences of {@code org.eclipse.ui.workbench} under the font definition's key (see {@link #key}), and
 * the running workbench is updated through the current {@code ITheme}'s public {@link FontRegistry}, the very
 * registry the Colors and Fonts page shows. No widget is touched directly. The definitions themselves are read from the
 * {@code org.eclipse.ui.themes} extension point, the declaration Eclipse builds its own font definitions from.
 *
 * <p>
 * Limit: a font definition that the active Eclipse CSS theme itself overrides is stored by Eclipse under a key
 * prefixed with that CSS theme's id; this class does not write that key, so such a definition may keep the CSS
 * theme's font. The plugin's own workbench themes do not override fonts.
 */
public final class EclipseWorkbenchFonts implements WorkbenchFonts {

	private static final String NODE = "org.eclipse.ui.workbench";
	private static final String THEMES_POINT = "org.eclipse.ui.themes";

	@Override
	public Map<String, Definition> definitions() {
		Map<String, Definition> definitions = new HashMap<>();
		for (IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(THEMES_POINT)) {
			String id = element.getAttribute("id");
			if ("fontDefinition".equals(element.getName()) && id != null) {
				definitions.put(id, new Definition(element.getAttribute("defaultsTo"),
						element.getAttribute("value") != null, !"false".equals(element.getAttribute("isEditable"))));
			}
		}
		return definitions;
	}

	@Override
	public boolean installed(String family) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		for (boolean scalable : new boolean[] { true, false }) {
			for (FontData data : display.getFontList(null, scalable)) {
				if (data.getName().equalsIgnoreCase(family)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String represent(ThemeFonts.Face face, boolean bold) {
		return PreferenceConverter.getStoredRepresentation(
				new FontData[] { new FontData(face.family(), face.size(), bold ? SWT.BOLD : SWT.NORMAL) });
	}

	/**
	 * The key under which the Colors and Fonts page stores and reads a definition: the plain id for the default
	 * workbench theme, otherwise the id prefixed with the current workbench theme's id (as
	 * {@code ThemeElementHelper.createPreferenceKey} does). Writing the same key keeps the page and the value
	 * restored at the next start in step with what this plugin applies.
	 */
	private static String key(String fontId) {
		try {
			String themeId = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getId();
			if (themeId != null && !themeId.equals(IThemeManager.DEFAULT_THEME)) {
				return themeId + '.' + fontId;
			}
		} catch (RuntimeException e) {
			// Fall back to the plain id, which is the default workbench theme's key.
		}
		return fontId;
	}

	@Override
	public Optional<String> get(String fontId) {
		return Optional.ofNullable(InstanceScope.INSTANCE.getNode(NODE).get(key(fontId), null));
	}

	@Override
	public void put(String fontId, String stored) {
		InstanceScope.INSTANCE.getNode(NODE).put(key(fontId), stored);
		publish(fontId, stored);
	}

	@Override
	public void remove(String fontId) {
		String key = key(fontId);
		InstanceScope.INSTANCE.getNode(NODE).remove(key);
		Definition definition = definitions().get(fontId);
		if (definition != null && definition.defaultsTo() != null && !definition.hasValue()) {
			// It inherits again: show what its parent currently shows (the parent is restored before its children).
			try {
				FontData[] inherited = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry()
						.getFontData(definition.defaultsTo());
				PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry().put(fontId, inherited);
			} catch (RuntimeException e) {
				// The inherited font is shown at the next start.
			}
			return;
		}
		String fallback = DefaultScope.INSTANCE.getNode(NODE).get(key, null);
		if (fallback != null) {
			publish(fontId, fallback);
		}
	}

	@Override
	public Optional<ThemeFonts.Face> effective(String fontId) {
		try {
			FontData[] data = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry()
					.getFontData(fontId);
			if (data != null && data.length > 0 && ThemeFonts.validFamily(data[0].getName())) {
				int size = Math.max(ThemeFonts.MIN_SIZE, Math.min(ThemeFonts.MAX_SIZE, data[0].getHeight()));
				return Optional.of(new ThemeFonts.Face(data[0].getName(), size));
			}
		} catch (RuntimeException e) {
			// Display only: an unknown current font is simply not shown.
		}
		return Optional.empty();
	}

	@Override
	public void flush() throws IOException {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(NODE);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			throw new IOException("Could not save the workbench font preferences", e);
		}
	}

	/** Best effort: the stored value is authoritative and is read again at the next start. */
	private static void publish(String fontId, String stored) {
		try {
			FontData[] data = PreferenceConverter.basicGetFontData(stored);
			PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry().put(fontId, data);
		} catch (RuntimeException e) {
			// The font is applied at the next start.
		}
	}
}
