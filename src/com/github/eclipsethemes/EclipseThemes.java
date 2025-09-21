package com.github.eclipsethemes;

import java.io.File;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.github.eclipsethemes.theme.ThemeManager;

public class EclipseThemes extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.github.eclipsethemes";

	private static EclipseThemes instance;
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
		super.stop(context);
	}

	public static EclipseThemes get() {
		return instance;
	}

	public static File getPluginDataDirectory() {
		return instance.getStateLocation().toFile();
	}
}
