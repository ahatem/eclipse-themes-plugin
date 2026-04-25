package com.github.eclipsethemes.eclipse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public final class AdapterRegistry {

	private final ILog log;
	private final Map<EclipseThemeAdapter, String> adapters = new HashMap<>();

	public AdapterRegistry(ILog log) {
		this.log = log;
	}

	public void discover() {
		adapters.clear();
		var elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(EclipseThemes.ADAPTER_EXT_ID);

		for (IConfigurationElement element : elements) {
			if (!"adapter".equals(element.getName())) continue;

			try {
				Object obj = element.createExecutableExtension("class");
				if (!(obj instanceof EclipseThemeAdapter adapter)) continue;
				adapters.put(adapter, element.getAttribute("plugin"));
			} catch (CoreException e) {
				log.error("Could not register adapter " + element.getAttribute("class"), e);
			}
		}
	}

	public Map<EclipseThemeAdapter, String> getAdapters() {
		return Collections.unmodifiableMap(adapters);
	}
}
