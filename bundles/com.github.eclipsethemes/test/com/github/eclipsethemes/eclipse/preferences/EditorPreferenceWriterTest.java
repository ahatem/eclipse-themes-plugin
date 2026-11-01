package com.github.eclipsethemes.eclipse.preferences;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenBuilder;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome;
import com.github.eclipsethemes.eclipse.ThemeApplyOutcome.EditorOutcome;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.preferences.EditorPreferenceOwnership.Key;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchOutcome;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchSectionModel;

public class EditorPreferenceWriterTest {

	private static final String NODE = "test.editor";
	private static final String COLOR = "color";
	private static final String OPTIONAL = "optional";

	@Test
	public void originalValueSurvivesThemeSwitchesAndIsRestoredOnStop() throws Exception {
		MemoryPreferences prefs = new MemoryPreferences();
		prefs.values.put(COLOR, "user-value");
		PreferenceStore store = memoryStore();
		EditorPreferenceWriter writer = writer(prefs, store);

		assertTrue(writer.write(theme("0a141e", null)).succeeded());
		assertEquals("10,20,30", prefs.values.get(COLOR));
		assertEquals(PreferenceValue.present("user-value"),
				new EditorPreferenceOwnership(store).read().get(new Key(NODE, COLOR)).original());

		assertTrue(writer.write(theme("28323c", null)).succeeded());
		assertEquals("40,50,60", prefs.values.get(COLOR));
		assertEquals(PreferenceValue.present("user-value"),
				new EditorPreferenceOwnership(store).read().get(new Key(NODE, COLOR)).original());

		PreferenceReset.Result result = new PreferenceReset(id -> prefs.node, store).reset();
		assertTrue(result.describeFailures(), result.successful());
		assertEquals("user-value", prefs.values.get(COLOR));
		assertTrue(new EditorPreferenceOwnership(store).read().isEmpty());
	}

	@Test
	public void absentOriginalIsRemovedAndNoDefaultIsGuessed() throws Exception {
		MemoryPreferences prefs = new MemoryPreferences();
		PreferenceStore store = memoryStore();
		EditorPreferenceWriter writer = writer(prefs, store);

		assertTrue(writer.write(theme("0a141e", null)).succeeded());
		assertEquals("10,20,30", prefs.values.get(COLOR));

		PreferenceReset.Result result = new PreferenceReset(id -> prefs.node, store).reset();
		assertTrue(result.describeFailures(), result.successful());
		assertFalse(prefs.values.containsKey(COLOR));
	}

	@Test
	public void externalChangeAfterApplyIsPreservedOnStop() throws Exception {
		MemoryPreferences prefs = new MemoryPreferences();
		prefs.values.put(COLOR, "user-value");
		PreferenceStore store = memoryStore();
		EditorPreferenceWriter writer = writer(prefs, store);

		assertTrue(writer.write(theme("0a141e", null)).succeeded());
		prefs.values.put(COLOR, "user-changed");

		PreferenceReset.Result result = new PreferenceReset(id -> prefs.node, store).reset();
		assertTrue(result.describeFailures(), result.successful());
		assertEquals(1, result.conflicts().size());
		assertEquals("user-changed", prefs.values.get(COLOR));
		assertTrue(new EditorPreferenceOwnership(store).read().isEmpty());
	}

	@Test
	public void externalChangeDuringThemeSwitchIsNotOverwrittenAndReleasesOwnership() throws Exception {
		MemoryPreferences prefs = new MemoryPreferences();
		prefs.values.put(COLOR, "user-value");
		PreferenceStore store = memoryStore();
		EditorPreferenceWriter writer = writer(prefs, store);
		assertTrue(writer.write(theme("0a141e", null)).succeeded());

		prefs.values.put(COLOR, "user-changed");
		EditorOutcome outcome = writer.write(theme("28323c", null));

		assertFalse(outcome.succeeded());
		assertEquals(1, outcome.conflicts().size());
		assertEquals("user-changed", prefs.values.get(COLOR));
		assertFalse(new EditorPreferenceOwnership(store).read().containsKey(new Key(NODE, COLOR)));

		ThemeApplyOutcome aggregate = new ThemeApplyOutcome(outcome, WorkbenchOutcome.notRequested("off"));
		assertFalse(aggregate.fullySucceeded());
		assertFalse(WorkbenchSectionModel.summary(aggregate).startsWith("✓"));
		assertTrue(WorkbenchSectionModel.summary(aggregate).contains("changed outside Eclipse Themes"));
	}

	@Test
	public void anIntentionallyRemovedOptionalPreferenceIsRestored() throws Exception {
		MemoryPreferences prefs = new MemoryPreferences();
		prefs.values.put(OPTIONAL, "user-optional");
		PreferenceStore store = memoryStore();
		EditorPreferenceWriter writer = writer(prefs, store);

		assertTrue(writer.write(theme("0a141e", null)).succeeded());
		assertFalse(prefs.values.containsKey(OPTIONAL));
		assertEquals(PreferenceValue.absent(),
				new EditorPreferenceOwnership(store).read().get(new Key(NODE, OPTIONAL)).written());

		PreferenceReset.Result result = new PreferenceReset(id -> prefs.node, store).reset();
		assertTrue(result.describeFailures(), result.successful());
		assertEquals("user-optional", prefs.values.get(OPTIONAL));
	}

	@Test
	public void flushFailureDoesNotCreateOwnership() throws Exception {
		MemoryPreferences prefs = new MemoryPreferences();
		prefs.failFlush = true;
		PreferenceStore store = memoryStore();

		EditorOutcome outcome = writer(prefs, store).write(theme("0a141e", null));

		assertFalse(outcome.succeeded());
		assertTrue(outcome.failedAdapters().contains("TestEditorAdapter"));
		assertTrue(new EditorPreferenceOwnership(store).read().isEmpty());
	}

	private static PreferenceStore memoryStore() {
		return new PreferenceStore() {
			@Override
			public void save() {
				// in memory only
			}
		};
	}

	private static EditorPreferenceWriter writer(MemoryPreferences prefs, PreferenceStore store) {
		Map<EclipseThemeAdapter, String> adapters = new LinkedHashMap<>();
		adapters.put(new TestEditorAdapter(), null);
		return new EditorPreferenceWriter(adapters, id -> prefs.node,
				new EditorPreferenceOwnership(store), noopLog());
	}

	private static Theme theme(String background, String selection) {
		Map<TokenKey, Token> tokens = new LinkedHashMap<>();
		tokens.put(TokenKey.BACKGROUND, token(TokenKey.BACKGROUND, background));
		if (selection != null) {
			tokens.put(TokenKey.SELECTION_BACKGROUND, token(TokenKey.SELECTION_BACKGROUND, selection));
		}
		return new Theme("id", "name", "", "", "", ThemeType.DARK, tokens);
	}

	private static Token token(TokenKey key, String color) {
		return new TokenBuilder().setKey(key).setColor(Color.ofHex("#" + color)).build();
	}

	private static ILog noopLog() {
		return (ILog) Proxy.newProxyInstance(ILog.class.getClassLoader(), new Class<?>[] { ILog.class },
				(proxy, method, args) -> null);
	}

	private static final class TestEditorAdapter extends EclipseThemeAdapter {

		@Override
		public String getPreferencesId() {
			return NODE;
		}

		@Override
		public Set<String> getManagedKeys() {
			return Set.of(COLOR, OPTIONAL);
		}

		@Override
		public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
			if (theme.has(TokenKey.BACKGROUND)) {
				preferences.put(COLOR, theme.get(TokenKey.BACKGROUND).getColor().toEclipseFormat());
			} else {
				preferences.remove(COLOR);
			}
			if (theme.has(TokenKey.SELECTION_BACKGROUND)) {
				preferences.put(OPTIONAL, theme.get(TokenKey.SELECTION_BACKGROUND).getColor().toEclipseFormat());
			} else {
				preferences.remove(OPTIONAL);
			}
			preferences.flush();
		}
	}
}
