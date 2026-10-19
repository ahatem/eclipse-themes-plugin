package com.github.eclipsethemes.eclipse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Before;
import org.junit.Test;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenBuilder;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.workbench.EngineProbe;
import com.github.eclipsethemes.core.workbench.WorkbenchEnvironment;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;
import com.github.eclipsethemes.eclipse.workbench.PaletteSink;
import com.github.eclipsethemes.eclipse.workbench.ForeignPreferences;
import com.github.eclipsethemes.eclipse.workbench.PluginColorWriter;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchCoordinator;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchThemeEngine;

/**
 * Regression for the "second Apply" defect: switching the Eclipse CSS theme synchronously resets the preference
 * defaults the previous theme overrode, deleting instance values for the same keys. The editor preferences must
 * therefore be written after the workbench switch, in every transition, so one Apply is enough.
 */
public class ThemeServiceOrderTest {

	private static final String STOCK_DARK = "org.eclipse.e4.ui.css.theme.e4_dark";
	private static final String BACKGROUND = "AbstractTextEditor.Color.Background";
	private static final String SYSTEM_DEFAULT = "AbstractTextEditor.Color.Background.SystemDefault";

	/** Stands in for the instance-scope editor preferences. */
	private final Map<String, String> editorPrefs = new HashMap<>();
	private final List<String> events = new ArrayList<>();
	private PlatformEngine engine;
	private PreferenceStore store;
	private ThemeService service;

	@Before
	public void setUp() {
		engine = new PlatformEngine();
		store = new PreferenceStore() {
			@Override
			public void save() {
				// in memory only
			}
		};
		store.setValue(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED, true);
		WorkbenchCoordinator coordinator = new WorkbenchCoordinator(engine, new MapSink(), store,
				new PluginColorWriter(new NoForeignPreferences(), store));
		service = new ThemeService(this::writeEditor, coordinator, noopLog());
	}

	@Test
	public void leavingTheStockDarkThemeDoesNotDeleteTheEditorValuesJustWritten() {
		engine.active = STOCK_DARK;
		engine.overridesEditorDefaults = true;

		service.applyTheme(null, theme(ThemeType.DARK, "#282c34", "#abb2bf"));

		assertEditor("40,44,52", "false");
	}

	@Test
	public void everyTransitionEndsWithTheSelectedEditorThemeAfterOneApply() {
		engine.active = STOCK_DARK;
		engine.overridesEditorDefaults = true;
		String[][] sequence = { { "DARK", "#282c34", "40,44,52" }, { "DARK", "#282a36", "40,42,54" },
				{ "LIGHT", "#fdf6e3", "253,246,227" }, { "LIGHT", "#ffffff", "255,255,255" },
				{ "DARK", "#1e1e2e", "30,30,46" } };
		for (String[] step : sequence) {
			service.applyTheme(null, theme(ThemeType.valueOf(step[0]), step[1], "#101010"));
			assertEditor(step[2], "false");
		}
	}

	@Test
	public void editorPreferencesAreWrittenAfterTheWorkbenchSwitchAndEvenWhenItDoesNothing() {
		engine.active = STOCK_DARK;
		service.applyTheme(null, theme(ThemeType.DARK, "#282c34", "#abb2bf"));
		assertTrue(events.indexOf("select") >= 0 && events.indexOf("editor") > events.lastIndexOf("select"));

		store.setValue(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED, false);
		store.setToDefault(PreferenceKeys.WORKBENCH_OWNED_THEME_ID);
		events.clear();
		service.applyTheme(null, theme(ThemeType.LIGHT, "#ffffff", "#000000"));
		assertEquals(List.of("editor"), events);
	}

	private void assertEditor(String background, String systemDefault) {
		assertEquals(background, editorPrefs.get(BACKGROUND));
		assertEquals(systemDefault, editorPrefs.get(SYSTEM_DEFAULT));
	}

	private List<String> writeEditor(Theme theme) {
		events.add("editor");
		Color bg = theme.get(TokenKey.BACKGROUND).getColor();
		editorPrefs.put(BACKGROUND, bg.red() + "," + bg.green() + "," + bg.blue());
		editorPrefs.put(SYSTEM_DEFAULT, "false");
		return List.of();
	}

	private static Theme theme(ThemeType type, String background, String foreground) {
		Map<TokenKey, Token> tokens = new HashMap<>();
		tokens.put(TokenKey.BACKGROUND,
				new TokenBuilder().setKey(TokenKey.BACKGROUND).setColor(Color.ofHex(background)).build());
		tokens.put(TokenKey.FOREGROUND,
				new TokenBuilder().setKey(TokenKey.FOREGROUND).setColor(Color.ofHex(foreground)).build());
		return new Theme("id", "name", "", "", "", type, tokens);
	}

	private static ILog noopLog() {
		return (ILog) Proxy.newProxyInstance(ILog.class.getClassLoader(), new Class<?>[] { ILog.class },
				(proxy, method, args) -> null);
	}

	/** Models the platform: a theme switch deletes the editor keys the previous stock theme had overridden. */
	private final class PlatformEngine implements WorkbenchThemeEngine {
		String active;
		Optional<String> saved = Optional.empty();
		boolean overridesEditorDefaults;

		@Override
		public WorkbenchEnvironment environment() {
			return new WorkbenchEnvironment("win32", "win32", "0.15.200.v1", "4.41.0", "21");
		}

		@Override
		public EngineProbe probe() {
			return EngineProbe.usable();
		}

		@Override
		public Optional<String> activeThemeId() {
			return Optional.ofNullable(active);
		}

		@Override
		public boolean select(String themeId) {
			events.add("select");
			if (overridesEditorDefaults) {
				editorPrefs.remove(BACKGROUND);
				editorPrefs.remove(SYSTEM_DEFAULT);
			}
			active = themeId;
			saved = Optional.of(themeId);
			return true;
		}

		@Override
		public Optional<String> savedThemeId() {
			return saved;
		}

		@Override
		public void clearSavedThemeId() {
			saved = Optional.empty();
		}

		@Override
		public Runnable watchSavedThemeId(java.util.function.Consumer<Optional<String>> listener) {
			return () -> {
			};
		}
	}

	/** Plug-in colors are not under test here; no bundle is installed, so no key is touched. */
	private static final class NoForeignPreferences implements ForeignPreferences {
		@Override
		public boolean installed(String bundle) {
			return false;
		}

		@Override
		public Optional<String> get(String node, String key) {
			return Optional.empty();
		}

		@Override
		public void put(String node, String key, String value) {
		}

		@Override
		public void remove(String node, String key) {
		}

		@Override
		public void flush(String node) {
		}
	}

	private static final class MapSink implements PaletteSink {
		private final Map<String, Color> values = new HashMap<>();

		@Override
		public Optional<Color> get(String colorId) {
			return Optional.ofNullable(values.get(colorId));
		}

		@Override
		public void put(String colorId, Color color) {
			values.put(colorId, color);
		}
	}
}
