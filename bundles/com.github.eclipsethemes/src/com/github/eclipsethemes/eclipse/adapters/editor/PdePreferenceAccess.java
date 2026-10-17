package com.github.eclipsethemes.eclipse.adapters.editor;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

/** The single PM6-approved optional PDE internal access; never a core/application service. */
final class PdePreferenceAccess {
    private PdePreferenceAccess() {}

    static IPreferenceStore liveStore() {
        // Eclipse R4_33 / PDE UI 3.15.300.v20240815-2201:
        // org.eclipse.pde.internal.ui.PDEPlugin.getDefault().getPreferenceStore().
        // Version-fragile, explicitly approved. A public/upstream replacement is R6 work.
        // Load through the optional bundle; no mandatory PDE import or fallback store.
        Bundle bundle = Platform.getBundle("org.eclipse.pde.ui");
        if (bundle == null) throw new IllegalStateException("PDE is not installed");
        try {
            Class<?> pluginClass = bundle.loadClass("org.eclipse.pde.internal.ui.PDEPlugin");
            Object plugin = pluginClass.getMethod("getDefault").invoke(null);
            if (!(plugin instanceof AbstractUIPlugin uiPlugin)) {
                throw new IllegalStateException("PDE live plugin is unavailable or incompatible");
            }
            return uiPlugin.getPreferenceStore();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("PDE R4_33 live preference-store accessor is incompatible", e);
        }
    }
}
