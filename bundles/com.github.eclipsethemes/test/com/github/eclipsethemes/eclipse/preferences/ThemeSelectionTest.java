package com.github.eclipsethemes.eclipse.preferences;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Test;

import com.github.eclipsethemes.core.Constants;
import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.TokenBuilder;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome.EditorConflict;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome.EditorOutcome;
import com.github.eclipsethemes.eclipse.ThemeRecord;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchOutcome;

public class ThemeSelectionTest {
    @Test public void legacyResolutionIsUniqueAndReadOnlyAndDoesNotInterpretDefaultLabels() {
        var store = new PreferenceStore();
        var events = new ArrayList<String>();
        store.addPropertyChangeListener(event -> events.add(event.getProperty()));
        store.setDefault(PreferenceKeys.ACTIVE_THEME_ID, "GitHub Light Default");
        ThemeRecord first = theme("local:first", "source", "First", "#112233");
        assertTrue(ThemeSelection.resolve(store, List.of(first)).isEmpty());
        store.setValue(PreferenceKeys.ACTIVE_THEME_ID, "source");
        events.clear();
        assertSame(first, ThemeSelection.resolve(store, List.of(first)).orElseThrow());
        assertTrue(events.isEmpty());
        assertFalse(store.contains(PreferenceKeys.SELECTED_THEME_LOGICAL_ID));
        assertEquals("source", store.getString(PreferenceKeys.ACTIVE_THEME_ID));
        ThemeRecord second = theme("local:second", "source", "Second", "#334455");
        assertTrue(ThemeSelection.resolve(store, List.of(first, second)).isEmpty());
        assertTrue(ThemeSelection.resolve(store, List.of()).isEmpty());
        assertTrue(events.isEmpty());
        for (String missing : List.of("", " ", "missing")) {
            store.setValue(PreferenceKeys.ACTIVE_THEME_ID, missing);
            assertTrue(ThemeSelection.resolve(store, List.of(first)).isEmpty());
            assertEquals(missing, store.getString(PreferenceKeys.ACTIVE_THEME_ID));
        }
    }

    @Test public void modernIntentTakesPrecedenceAndOnlyExplicitRememberWritesIt() {
        var store = new PreferenceStore();
        store.setValue(PreferenceKeys.ACTIVE_THEME_ID, "old-source");
        store.setValue("unrelated", "kept");
        ThemeRecord old = theme("local:old", "old-source", "Old", "#112233");
        ThemeRecord selected = theme("local:new", "new-source", "New", "#334455");
        assertSame(old, ThemeSelection.resolve(store, List.of(old, selected)).orElseThrow());
        ThemeSelection.remember(store, selected);
        assertEquals("local:new", store.getString(PreferenceKeys.SELECTED_THEME_LOGICAL_ID));
        assertSame(selected, ThemeSelection.resolve(store, List.of(old, selected)).orElseThrow());
        assertEquals("old-source", store.getString(PreferenceKeys.ACTIVE_THEME_ID));
        assertEquals("kept", store.getString("unrelated"));
        assertFalse(store.contains("unrelated-absent"));
        assertTrue("Deleted modern reference must not fall back to old source", ThemeSelection.resolve(store, List.of(old)).isEmpty());
        ThemeSelection.clear(store);
        assertTrue(ThemeSelection.resolve(store, List.of(old, selected)).isEmpty());
        assertEquals("kept", store.getString("unrelated"));
        assertFalse(store.contains("unrelated-absent"));
        assertThrows("A catalog record requires an established logical identity", IllegalArgumentException.class,
                () -> ThemeRecord.installed(" ", "candidate.xml",
                        new Theme("claim", "Unestablished", "", "", "", ThemeType.LIGHT, Map.of())));
    }

    @Test public void restoreOnOpenAndReloadDoesNotDispatchMigrateOrInventFallback() throws Exception {
        var store = new PreferenceStore();
        var page = new RecordingPage(store);
        ThemeRecord first = theme("local:first", "same", "First", "#112233");
        ThemeRecord second = theme("local:second", "same", "Second", "#334455");
        page.restoreSelectedIntent(List.of(first));
        page.performOk();
        page.apply();
        assertTrue(page.received.isEmpty());
        store.setValue(PreferenceKeys.ACTIVE_THEME_ID, "same");
        page.restoreSelectedIntent(List.of(first, second));
        page.performOk();
        assertTrue(page.received.isEmpty());
        assertEquals("same", store.getString(PreferenceKeys.ACTIVE_THEME_ID));
        assertFalse(store.contains(PreferenceKeys.SELECTED_THEME_LOGICAL_ID));
        page.restoreSelectedIntent(List.of(first));
        assertTrue("Resolution itself cannot dispatch", page.received.isEmpty());
        assertFalse("Resolution itself cannot migrate", store.contains(PreferenceKeys.SELECTED_THEME_LOGICAL_ID));
        page.restoreSelectedIntent(List.of());
        page.performOk();
        assertTrue(page.received.isEmpty());
    }

    @Test public void explicitOkAndApplyDispatchChangedAndUnchangedDefinitionsWithTheSameIdentity() throws Exception {
        var store = new PreferenceStore();
        var page = new RecordingPage(store);
        ThemeRecord old = theme("local:established", "source", "Old", "#112233");
        ThemeRecord changed = theme("local:established", "source", "Renamed", "#445566");
        ThemeSelection.remember(store, old);
        page.restoreSelectedIntent(List.of(old));
        Field selection = EclipseThemesPreferencePage.class.getDeclaredField("selectedTheme");
        selection.setAccessible(true);
        selection.set(page, changed);
        assertEquals(old, changed);
        assertTrue(page.performOk());
        page.apply();
        assertTrue(page.performOk());
        assertEquals(3, page.received.size());
        for (ThemeRecord received : page.received) {
            assertSame(changed, received);
            assertEquals(Color.ofHex("#445566"), received.theme().get(TokenKey.BACKGROUND).getColor());
        }
    }

    @Test public void previewUsesSourceMetadataAndDefaultsUseCuratedLogicalIdentity() {
        ThemeRecord imported = theme("local:generated", "website-source", "GitHub Light Default", "#112233");
        assertEquals(Constants.PREVIEW_SERVER_URL + "website-source", EclipseThemesPreferencePage.previewUrl(imported).orElseThrow());
        assertFalse(EclipseThemesPreferencePage.previewUrl(imported).orElseThrow().contains("local:generated"));
        ThemeRecord curated = curated(Constants.DEFAULT_LIGHT_THEME_ID, "curated-source", "Renamed", "#445566");
        assertSame(curated, ThemeSelection.defaultTheme(List.of(imported, curated), false));
        assertNotEquals("Duplicate source claims do not imply the current record", imported,
                theme("local:other", "website-source", "Other", "#334455"));
    }

    @Test public void partialOrConflictingEditorApplyDoesNotPersistTheSelectedTheme() {
        var store = new PreferenceStore();
        ThemeRecord applied = theme("local:applied", "applied", "Applied", "#112233");
        ThemeRecord selected = theme("local:selected", "selected", "Selected", "#334455");
        ThemeSelection.remember(store, applied);

        ThemeApplyOutcome conflict = new ThemeApplyOutcome(
                new EditorOutcome(List.of(), List.of(new EditorConflict("TextEditorThemeAdapter",
                        "org.eclipse.ui.editors", "AbstractTextEditor.Color.Background", "kept"))),
                WorkbenchOutcome.notRequested("off"));
        assertFalse(ThemeSelection.rememberApplied(store, selected, conflict));
        assertEquals("local:applied", store.getString(PreferenceKeys.SELECTED_THEME_LOGICAL_ID));

        ThemeApplyOutcome failure = new ThemeApplyOutcome(new EditorOutcome(List.of("TextEditorThemeAdapter")),
                WorkbenchOutcome.notRequested("off"));
        assertFalse(ThemeSelection.rememberApplied(store, selected, failure));
        assertEquals("local:applied", store.getString(PreferenceKeys.SELECTED_THEME_LOGICAL_ID));

        ThemeApplyOutcome success = new ThemeApplyOutcome(new EditorOutcome(List.of()),
                WorkbenchOutcome.notRequested("off"));
        assertTrue(ThemeSelection.rememberApplied(store, selected, success));
        assertEquals("local:selected", store.getString(PreferenceKeys.SELECTED_THEME_LOGICAL_ID));
    }

    private static ThemeRecord theme(String logical, String source, String name, String background) {
        return ThemeRecord.installed(logical, name + ".xml", model(source, name, background));
    }

    private static ThemeRecord curated(String logical, String source, String name, String background) {
        return ThemeRecord.bundled(logical, model(source, name, background));
    }

    private static Theme model(String source, String name, String background) {
        return new Theme(source, name, "", "", "", ThemeType.LIGHT, Map.of(TokenKey.BACKGROUND,
                new TokenBuilder().setKey(TokenKey.BACKGROUND).setColor(Color.ofHex(background)).build()));
    }

    private static final class RecordingPage extends EclipseThemesPreferencePage {
        final List<ThemeRecord> received = new ArrayList<>();
        RecordingPage(PreferenceStore store) { super(store); }
        void apply() { performApply(); }
        @Override protected boolean applySelectedTheme() {
            try {
                Field field = EclipseThemesPreferencePage.class.getDeclaredField("selectedTheme");
                field.setAccessible(true);
                received.add((ThemeRecord) field.get(this));
                return true;
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }
    }
}
