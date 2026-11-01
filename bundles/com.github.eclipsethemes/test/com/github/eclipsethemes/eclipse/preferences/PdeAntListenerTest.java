package com.github.eclipsethemes.eclipse.preferences;

import static com.github.eclipsethemes.eclipse.preferences.AdapterTestSupport.adapters;
import static com.github.eclipsethemes.eclipse.preferences.AdapterTestSupport.sparse;
import static com.github.eclipsethemes.eclipse.preferences.AdapterTestSupport.token;

import static org.junit.Assert.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.osgi.service.prefs.BackingStoreException;
import org.junit.Test;
import com.github.eclipsethemes.core.models.*;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.*;

public class PdeAntListenerTest {
    @Test public void actualStoreReceivesTypedFlagsOnEveryTransitionIncludingEqualDefaults() throws Exception {
        PreferenceStore store = new PreferenceStore();
        String base = "editor.color.xml_comment";
        store.setDefault(base + "_bold", true);
        store.setDefault(base + "_italic", true);
        List<PropertyChangeEvent> events = new ArrayList<>();
        store.addPropertyChangeListener(events::add);
        PluginEditorThemeAdapter adapter = PdeAdapterFixture.create(() -> store);
        MemoryPreferences prefs = new MemoryPreferences();
        prefs.values.put(base + "_underline", "old unsupported true");
        prefs.values.put(base + "_strikethrough", "old unsupported false");
        prefs.values.put("unrelated", "keep");
        for (int step : new int[] {1, 2, 0, 3, 3, 1}) {
            var theme = sparse();
            if (step != 0) theme.add(new TokenBuilder()
                    .setKey(step == 2 ? TokenKey.COMMENT : TokenKey.XML_COMMENT)
                    .setColor(Color.ofRgb(10, 20, step))
                    .setBold(step == 1).setItalic(step == 2).setUnderline(true).setStrikethrough(true).build());
            events.clear();
            adapter.apply(theme.build(), prefs.node);
            assertEquals(22, events.size()); // Two supported flags for each of eleven existing mappings.
            for (PropertyChangeEvent event : events) {
                assertTrue(event.getNewValue() instanceof Boolean);
                assertNull(event.getOldValue());
                assertTrue(event.getProperty().endsWith("_bold") || event.getProperty().endsWith("_italic"));
                assertEquals(Boolean.toString((Boolean) event.getNewValue()), prefs.values.get(event.getProperty()));
            }
            assertEquals("10,20," + (step == 0 ? 2 : step), prefs.values.get(base));
            assertEquals(Boolean.toString(step == 1), prefs.values.get(base + "_bold"));
            assertEquals(Boolean.toString(step == 2), prefs.values.get(base + "_italic"));
            assertTrue(prefs.values.containsKey(base + "_bold"));
            assertEquals("old unsupported true", prefs.values.get(base + "_underline"));
            assertEquals("old unsupported false", prefs.values.get(base + "_strikethrough"));
            assertEquals("keep", prefs.values.get("unrelated"));
            assertFalse(prefs.values.containsKey("unrelated.absent"));
        }
    }

    @Test public void unsupportedHistoricalValuesSurviveApplyAndRepeatedReset() throws Exception {
        Map<String, MemoryPreferences> nodes = new HashMap<>();
        PreferenceStore store = new PreferenceStore() {
            @Override public void save() {
                // in memory only
            }
        };
        EditorPreferenceOwnership ownership = new EditorPreferenceOwnership(store);
        Map<EditorPreferenceOwnership.Key, EditorPreferenceOwnership.Owned> journal = new HashMap<>();
        for (EclipseThemeAdapter adapter : adapters()) {
            MemoryPreferences prefs = new MemoryPreferences();
            nodes.put(adapter.getPreferencesId(), prefs);
            if (adapter instanceof PluginEditorThemeAdapter || adapter instanceof AntEditorThemeAdapter) {
                String base = adapter instanceof PluginEditorThemeAdapter ? "editor.color.xml_comment" : "org.eclipse.ant.ui.commentsColor";
                prefs.values.put(base + "_underline", "preserve true");
                prefs.values.put(base + "_strikethrough", "preserve false");
                prefs.values.put("unrelated", "keep");
                adapter.apply(sparse().build(), prefs.node);
                for (String key : adapter.getManagedKeys()) {
                    journal.put(new EditorPreferenceOwnership.Key(adapter.getPreferencesId(), key),
                            new EditorPreferenceOwnership.Owned(PreferenceValue.absent(),
                                    PreferenceValue.read(prefs.node, key)));
                }
            }
        }
        ownership.save(journal);
        PreferenceReset reset = new PreferenceReset(id -> nodes.get(id).node, store);
        for (int i = 0; i < 2; i++) assertTrue(reset.reset().successful());
        for (String id : List.of("org.eclipse.pde.ui", "org.eclipse.ant.ui")) {
            MemoryPreferences prefs = nodes.get(id);
            String base = id.equals("org.eclipse.pde.ui") ? "editor.color.xml_comment" : "org.eclipse.ant.ui.commentsColor";
            assertEquals(Map.of(base + "_underline", "preserve true", base + "_strikethrough", "preserve false", "unrelated", "keep"), prefs.values);
            assertEquals(0, prefs.clears);
        }
    }

    @Test public void absentIncompatibleOrNullStoreFailsBeforeMutationAndOtherAdaptersStillApply() throws Exception {
        for (Supplier<IPreferenceStore> supplier : List.<Supplier<IPreferenceStore>>of(
                () -> { throw new IllegalStateException("PDE absent"); },
                () -> { throw new NoClassDefFoundError("PDE incompatible"); }, () -> null)) {
            MemoryPreferences prefs = new MemoryPreferences();
            prefs.values.put("unrelated", "keep");
            BackingStoreException error = assertThrows(BackingStoreException.class,
                    () -> PdeAdapterFixture.create(supplier).apply(sparse().build(), prefs.node));
            assertTrue(error.getMessage().contains("no PDE mappings applied"));
            assertNotNull(error.getCause());
            assertTrue(prefs.mutations.isEmpty());
            assertEquals(0, prefs.flushes);
            assertEquals(Map.of("unrelated", "keep"), prefs.values);
            new TextEditorThemeAdapter().apply(sparse().build(), prefs.node);
            assertEquals("10,20,1", prefs.values.get("AbstractTextEditor.Color.Background"));
        }
    }

    @Test public void typedDispatchAndFlushFailuresStayDiagnostic() throws Exception {
        IPreferenceStore broken = (IPreferenceStore) java.lang.reflect.Proxy.newProxyInstance(
                IPreferenceStore.class.getClassLoader(), new Class<?>[] {IPreferenceStore.class},
                (proxy, method, args) -> { throw new AbstractMethodError("PDE store incompatible"); });
        MemoryPreferences prefs = new MemoryPreferences();
        BackingStoreException error = assertThrows(BackingStoreException.class,
                () -> PdeAdapterFixture.create(() -> broken).apply(sparse().build(), prefs.node));
        assertTrue(error.getMessage().contains("editor.color.default_bold"));
        assertTrue(error.getCause() instanceof AbstractMethodError);
        assertEquals(0, prefs.flushes);
        MemoryPreferences flush = new MemoryPreferences();
        flush.failFlush = true;
        assertThrows(BackingStoreException.class, () -> PdeAdapterFixture.create().apply(sparse().build(), flush.node));
        assertEquals(1, flush.flushes);
        assertEquals(0, flush.clears);
    }

    @Test public void unsupportedSuffixesNeverBecomeColorNotifications() throws Exception {
        for (EclipseThemeAdapter adapter : List.of(PdeAdapterFixture.create(), new AntEditorThemeAdapter())) {
            MemoryPreferences prefs = new MemoryPreferences();
            var theme = sparse();
            token(theme, TokenKey.XML_COMMENT, 45, true);
            adapter.apply(theme.build(), prefs.node);
            // R4_33 XMLConfiguration/AntSourceViewerConfiguration dispatch these as colors.
            assertFalse(adapter.getPreferencesId(), prefs.mutations.stream().anyMatch(
                    m -> m.contains("_underline=") || m.contains("_strikethrough=")));
        }
    }

    @Test public void unsupportedSuffixesAreExcludedFromResetResponsibility() {
        for (EclipseThemeAdapter adapter : List.of(PdeAdapterFixture.create(), new AntEditorThemeAdapter())) {
            assertFalse(adapter.getPreferencesId(), adapter.getManagedKeys().stream().anyMatch(
                    key -> key.endsWith("_underline") || key.endsWith("_strikethrough")));
        }
    }
}
