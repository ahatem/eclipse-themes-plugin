package com.github.eclipsethemes.eclipse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public final class AdapterRegistry {

	private final ILog log;
	private final Map<EclipseThemeAdapter, String> adapters = new LinkedHashMap<>();

	public AdapterRegistry(ILog log) {
		this.log = log;
	}

	public void discover() {
		adapters.clear();
		var elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(EclipseThemes.ADAPTER_EXT_ID);

		List<Registration> discovered = new ArrayList<>();
		for (IConfigurationElement element : elements) {
			if (!"adapter".equals(element.getName())) continue;

			try {
				Object obj = element.createExecutableExtension("class");
				if (!(obj instanceof EclipseThemeAdapter adapter)) continue;
				discovered.add(new Registration(adapter, element.getAttribute("plugin")));
			} catch (CoreException e) {
				log.error("Could not register adapter " + element.getAttribute("class"), e);
			}
		}

		// The apply order matters, so it must not depend on extension registry or
		// hash ordering. See EclipseThemeAdapter#getOrder().
		discovered.sort(Comparator.comparingInt((Registration registration) -> registration.adapter().getOrder())
				.thenComparing(registration -> registration.adapter().getClass().getName()));
		discovered.forEach(registration -> adapters.put(registration.adapter(), registration.plugin()));
	}

	private record Registration(EclipseThemeAdapter adapter, String plugin) {
	}

	public Map<EclipseThemeAdapter, String> getAdapters() {
		return Collections.unmodifiableMap(adapters);
	}
}
