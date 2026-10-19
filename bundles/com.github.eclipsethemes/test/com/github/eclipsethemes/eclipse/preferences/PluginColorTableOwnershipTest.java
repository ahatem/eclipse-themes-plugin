package com.github.eclipsethemes.eclipse.preferences;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.github.eclipsethemes.core.workbench.PluginColorKey;
import com.github.eclipsethemes.core.workbench.PluginColorTable;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

/**
 * The editor integrations overwrite and remove their keys; the plug-in color table uses conflict-aware ownership.
 * A key must never be under both models.
 */
public class PluginColorTableOwnershipTest {

	@Test
	public void noCoordinatedPluginKeyIsAlsoManagedByAnEditorAdapter() {
		for (EclipseThemeAdapter adapter : AdapterTestSupport.adapters()) {
			for (PluginColorKey key : PluginColorTable.keys()) {
				assertFalse(adapter.getClass().getSimpleName() + " manages " + key.id(),
						adapter.getManagedKeys().contains(key.key()));
			}
		}
	}
}
