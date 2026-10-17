package com.github.eclipsethemes.eclipse.adapters.editor;

import java.util.function.Supplier;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;

/** Test-only live-store injection: product never constructs a substitute scoped store. */
public final class PdeAdapterFixture {
    private PdeAdapterFixture() {}
    public static PluginEditorThemeAdapter create() {
        return create(PreferenceStore::new);
    }
    public static PluginEditorThemeAdapter create(Supplier<IPreferenceStore> store) {
        return new PluginEditorThemeAdapter(store);
    }
}
