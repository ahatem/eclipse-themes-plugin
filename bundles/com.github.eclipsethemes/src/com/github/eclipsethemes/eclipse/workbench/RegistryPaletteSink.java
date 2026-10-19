package com.github.eclipsethemes.eclipse.workbench;

import java.util.Optional;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;

import com.github.eclipsethemes.core.models.Color;

/** Publishes into the current Colors-and-Fonts theme's {@link ColorRegistry} (public API only). */
public final class RegistryPaletteSink implements PaletteSink {

	private static ColorRegistry registry() {
		return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
	}

	@Override
	public Optional<Color> get(String colorId) {
		RGB rgb = registry().getRGB(colorId);
		return rgb == null ? Optional.empty() : Optional.of(Color.ofRgb(rgb.red, rgb.green, rgb.blue));
	}

	@Override
	public void put(String colorId, Color color) {
		registry().put(colorId, new RGB(color.red(), color.green(), color.blue()));
	}
}
