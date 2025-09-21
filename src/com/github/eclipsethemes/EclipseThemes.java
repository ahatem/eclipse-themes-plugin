package com.github.eclipsethemes;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class EclipseThemes extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.github.eclipsethemes"; 
	
	private static EclipseThemes plugin;
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static EclipseThemes getDefault() {
		return plugin;
	}
}
