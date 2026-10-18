package com.github.eclipsethemes.eclipse.workbench;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.workbench.EngineProbe;
import com.github.eclipsethemes.core.workbench.WorkbenchEnvironment;
import com.github.eclipsethemes.core.workbench.WorkbenchSlots;

/**
 * The single user of {@code org.eclipse.e4.ui.css.swt.theme.IThemeEngine}.
 *
 * <p>
 * Provenance: that package is exported {@code x-friends} by the theme bundle, so this is accepted
 * compatibility debt (R7, P4). The boundary uses only {@code getThemes()}, {@code getActiveTheme()} and
 * {@code setTheme(ITheme, true)} with the exact {@link ITheme} taken from {@code getThemes()}; never the
 * {@code String} overload, reflection, or runtime theme registration. The engine's own preference node
 * ({@code org.eclipse.e4.ui.css.swt.theme}, key {@code themeid}) is read, and the key is removed only to undo a
 * restore this plugin performed. Measured on Eclipse 4.40 (theme bundle 0.15.100) and 4.41 (0.15.200) on Windows.
 */
public final class EclipseWorkbenchThemeEngine implements WorkbenchThemeEngine {

	private static final String ENGINE_BUNDLE = "org.eclipse.e4.ui.css.swt.theme";
	private static final String SAVED_THEME_KEY = "themeid";

	@Override
	public WorkbenchEnvironment environment() {
		return new WorkbenchEnvironment(Platform.getOS(), Platform.getWS(), bundleVersion(ENGINE_BUNDLE),
				bundleVersion("org.eclipse.platform"), System.getProperty("java.runtime.version"));
	}

	private static String bundleVersion(String symbolicName) {
		Bundle bundle = Platform.getBundle(symbolicName);
		return bundle == null ? null : bundle.getVersion().toString();
	}

	@Override
	public EngineProbe probe() {
		try {
			IThemeEngine engine = engine();
			if (engine == null) {
				return new EngineProbe(false, false, false, false);
			}
			boolean highContrast = PlatformUI.getWorkbench().getDisplay().getHighContrast();
			List<String> ids = engine.getThemes().stream().map(ITheme::getId).toList();
			return new EngineProbe(false, true, highContrast, ids.containsAll(WorkbenchSlots.ownThemeIds()));
		} catch (LinkageError e) {
			return new EngineProbe(true, false, false, false);
		}
	}

	@Override
	public Optional<String> activeThemeId() {
		try {
			IThemeEngine engine = engine();
			ITheme active = engine == null ? null : engine.getActiveTheme();
			return active == null ? Optional.empty() : Optional.of(active.getId());
		} catch (LinkageError e) {
			return Optional.empty();
		}
	}

	@Override
	public boolean select(String themeId) {
		try {
			IThemeEngine engine = engine();
			if (engine == null) {
				return false;
			}
			for (ITheme theme : engine.getThemes()) {
				if (theme.getId().equals(themeId)) {
					engine.setTheme(theme, true);
					return true;
				}
			}
			return false;
		} catch (LinkageError e) {
			throw new IllegalStateException("The Eclipse theme engine cannot be linked", e);
		}
	}

	@Override
	public Optional<String> savedThemeId() {
		return Optional.ofNullable(InstanceScope.INSTANCE.getNode(ENGINE_BUNDLE).get(SAVED_THEME_KEY, null));
	}

	@Override
	public void clearSavedThemeId() {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(ENGINE_BUNDLE);
		node.remove(SAVED_THEME_KEY);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			throw new IllegalStateException("Could not persist removal of the saved Eclipse theme id", e);
		}
	}

	private static IThemeEngine engine() {
		return PlatformUI.isWorkbenchRunning() ? PlatformUI.getWorkbench().getService(IThemeEngine.class) : null;
	}
}
