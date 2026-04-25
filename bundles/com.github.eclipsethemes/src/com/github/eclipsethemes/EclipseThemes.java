package com.github.eclipsethemes;

import java.io.File;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.github.eclipsethemes.eclipse.ThemeManager;

public class EclipseThemes extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.github.eclipsethemes";
	public static final String ADAPTER_EXT_ID = "com.github.eclipsethemes.theme.adapters";

	private static EclipseThemes instance;

	private ILog pluginLog;
	private ThemeManager themeManager;

	public ThemeManager getManager() {
		if (this.themeManager == null) {
			this.themeManager = new ThemeManager(getLogger());
		}

		return this.themeManager;
	}

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

	public static EclipseThemes instance() {
		return instance;
	}

	public ILog getLogger() {
		if (pluginLog == null) {
			Bundle bundle = getBundle();
			pluginLog = Platform.getLog(bundle);
		}
		return pluginLog;
	}

	public static File getPluginDataDirectory() {
		Bundle bundle = FrameworkUtil.getBundle(EclipseThemes.class);
		if (bundle == null) {
			return null;
		}

		return Platform.getStateLocation(bundle).toFile();
	}
}
