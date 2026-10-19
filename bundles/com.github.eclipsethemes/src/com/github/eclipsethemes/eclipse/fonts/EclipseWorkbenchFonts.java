package com.github.eclipsethemes.eclipse.fonts;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.ThemeFonts;

/**
 * {@link WorkbenchFonts} over the mechanism Eclipse's own Colors and Fonts page uses: the stored value lives in the
 * instance preferences of {@code org.eclipse.ui.workbench} under the font definition's id (public preference
 * node, documented key), and the running workbench is updated through the current {@code ITheme}'s public
 * {@link FontRegistry}. No widget is touched directly.
 *
 * <p>
 * Limit: a font definition that the active Eclipse CSS theme itself overrides is stored by Eclipse under a key
 * prefixed with that theme's id; this class only writes the plain key, so such a definition may keep the CSS theme's
 * font. The plugin's own workbench themes do not override fonts.
 */
public final class EclipseWorkbenchFonts implements WorkbenchFonts {

	private static final String NODE = "org.eclipse.ui.workbench";

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
	public String represent(ThemeFonts.Face face) {
		return PreferenceConverter.getStoredRepresentation(
				new FontData[] { new FontData(face.family(), face.size(), SWT.NORMAL) });
	}

	@Override
	public Optional<String> get(String fontId) {
		return Optional.ofNullable(InstanceScope.INSTANCE.getNode(NODE).get(fontId, null));
	}

	@Override
	public void put(String fontId, String stored) {
		InstanceScope.INSTANCE.getNode(NODE).put(fontId, stored);
		publish(fontId, stored);
	}

	@Override
	public void remove(String fontId) {
		InstanceScope.INSTANCE.getNode(NODE).remove(fontId);
		String fallback = DefaultScope.INSTANCE.getNode(NODE).get(fontId, null);
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
