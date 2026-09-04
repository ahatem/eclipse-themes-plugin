package com.github.eclipsethemes.eclipse.adapters.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;

public final class GtkCssThemeAdapter extends EclipseThemeAdapter {

	@Override
	public String getPreferencesId() {
		return EclipseThemes.PLUGIN_ID;
	}

	@Override
	public int getOrder() {
		return WorkbenchCssThemeAdapter.WORKBENCH_ORDER;
	}

	@Override
	public void apply(Theme theme, IEclipsePreferences preferences) {
		// Native GTK styling needs the workbench Display supplied by applyWorkbench.
	}

	@Override
	public void applyWorkbench(Theme theme, IEclipsePreferences preferences, IWorkbench workbench)
			throws BackingStoreException {
		if (!Platform.WS_GTK.equals(Platform.getWS())
				|| !preferences.getBoolean(PreferenceKeys.APPLY_WORKBENCH_THEME, true)
				|| !preferences.getBoolean(PreferenceKeys.ENABLE_GTK_MODERNIZATION, true)
				|| workbench == null) {
			return;
		}

		String css = GtkCssGenerator.generate(theme);
		Path cssFile;
		try {
			Path generated = EclipseThemes.getPluginDataDirectory().toPath().resolve("generated");
			Files.createDirectories(generated);
			cssFile = generated.resolve("gtk-overlay.css");
			Files.writeString(cssFile, css, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Could not write the GTK CSS overlay", e);
		}

		Display display = workbench.getDisplay();
		Runnable inject = () -> {
			if (!GtkCssInjector.apply(display, css, EclipseThemes.instance().getLogger())) {
				EclipseThemes.instance().getLogger()
						.info("Generated GTK stylesheet is available at " + cssFile);
			}
		};
		if (Display.getCurrent() == display) {
			inject.run();
		} else {
			display.syncExec(inject);
		}
	}
}
