package com.github.eclipsethemes.eclipse.workbench;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.github.eclipsethemes.core.workbench.WorkbenchCapability;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.Reason;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.State;
import com.github.eclipsethemes.core.workbench.WorkbenchColor;
import com.github.eclipsethemes.core.workbench.WorkbenchEnvironment;
import com.github.eclipsethemes.core.workbench.WorkbenchFamily;
import com.github.eclipsethemes.core.workbench.WorkbenchPaletteResolver;
import com.github.eclipsethemes.core.workbench.WorkbenchSlots;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchOutcome.Status;

public class WorkbenchCoordinatorTest {

	private static final String STOCK_DARK = "org.eclipse.e4.ui.css.theme.e4_dark";
	private static final String STOCK_LIGHT = "org.eclipse.e4.ui.css.theme.e4_default";

	private final List<String> events = new ArrayList<>();
	private FakeEngine engine;
	private FakeSink sink;
	private PreferenceStore store;
	private WorkbenchCoordinator coordinator;

	@Before
	public void setUp() {
		engine = new FakeEngine();
		sink = new FakeSink();
		store = new PreferenceStore();
		coordinator = new WorkbenchCoordinator(engine, sink, store);
		engine.active = STOCK_DARK;
		engine.saved = Optional.empty();
	}

	// ---- opt-in ----

	@Test
	public void coordinationIsOffByDefaultAndNothingIsWritten() {
		WorkbenchOutcome outcome = coordinator.apply(dark("#282c34", "#abb2bf"));
		assertEquals(Status.NOT_REQUESTED, outcome.status());
		assertTrue(outcome.succeeded());
		assertTrue(events.isEmpty());
		assertFalse(coordinator.isOwned());
		assertEquals(STOCK_DARK, engine.active);
	}

	// ---- apply ----

	@Test
	public void applyPublishesBeforeSwitchingAndRecordsOwnershipAfterwards() {
		enable();
		WorkbenchOutcome outcome = coordinator.apply(dark("#282c34", "#abb2bf"));
		assertEquals(Status.APPLIED, outcome.status());
		assertEquals(WorkbenchFamily.DARK.themeId(), engine.active);
		int lastPut = events.lastIndexOf("put");
		int select = events.indexOf("select " + WorkbenchFamily.DARK.themeId());
		assertTrue(lastPut >= 0 && select > lastPut);
		assertEquals(WorkbenchFamily.DARK.themeId(), store.getString(PreferenceKeys.WORKBENCH_OWNED_THEME_ID));
		assertEquals(STOCK_DARK, store.getString(PreferenceKeys.WORKBENCH_PRIOR_ACTIVE_ID));
		assertTrue(store.getBoolean(PreferenceKeys.WORKBENCH_PRIOR_SAVED_ABSENT));
		assertEquals(Color.ofHex("#282c34"), sink.values.get(WorkbenchColor.BACKGROUND.colorId(WorkbenchFamily.DARK)));
	}

	@Test
	public void priorSavedStateIsCapturedBeforeTheSwitchRewritesIt() {
		enable();
		engine.saved = Optional.of(STOCK_DARK);
		coordinator.apply(dark("#282c34", "#abb2bf"));
		assertFalse(store.getBoolean(PreferenceKeys.WORKBENCH_PRIOR_SAVED_ABSENT));
		assertEquals(STOCK_DARK, store.getString(PreferenceKeys.WORKBENCH_PRIOR_SAVED_ID));
		assertEquals(Optional.of(WorkbenchFamily.DARK.themeId()), engine.saved);
	}

	@Test
	public void darkToDarkAlternatesToADifferentThemeIdEachTime() {
		enable();
		coordinator.apply(dark("#282c34", "#abb2bf"));
		String first = engine.active;
		coordinator.apply(dark("#1e1e2e", "#cdd6f4"));
		String second = engine.active;
		coordinator.apply(dark("#002b36", "#839496"));
		String third = engine.active;
		assertEquals(WorkbenchFamily.DARK.alternateThemeId(), second);
		assertEquals(WorkbenchFamily.DARK.themeId(), third);
		assertTrue(!first.equals(second) && !second.equals(third));
		assertEquals(Color.ofHex("#002b36"), sink.values.get(WorkbenchColor.BACKGROUND.colorId(WorkbenchFamily.DARK)));
		// ownership keeps the original prior state, not a hop through our own theme
		assertEquals(STOCK_DARK, store.getString(PreferenceKeys.WORKBENCH_PRIOR_ACTIVE_ID));
	}

	@Test
	public void darkToLightAndLightToLightSelectTheRightFamily() {
		enable();
		coordinator.apply(dark("#282c34", "#abb2bf"));
		coordinator.apply(light("#fafafa", "#383a42"));
		assertEquals(WorkbenchFamily.LIGHT.themeId(), engine.active);
		coordinator.apply(light("#fdf6e3", "#657b83"));
		assertEquals(WorkbenchFamily.LIGHT.alternateThemeId(), engine.active);
		coordinator.apply(dark("#282c34", "#abb2bf"));
		assertEquals(WorkbenchFamily.DARK.themeId(), engine.active);
	}

	@Test
	public void reapplyingTheSamePaletteSwitchesNothing() {
		enable();
		Theme theme = dark("#282c34", "#abb2bf");
		coordinator.apply(theme);
		events.clear();
		WorkbenchOutcome outcome = coordinator.apply(theme);
		assertEquals(Status.APPLIED, outcome.status());
		assertFalse(events.stream().anyMatch(e -> e.startsWith("select")));
	}

	@Test
	public void partialPublishFailureNeverSwitchesAndRestoresPreviousColors() {
		enable();
		Theme theme = dark("#282c34", "#abb2bf");
		sink.failOnPut = 3;
		WorkbenchOutcome outcome = coordinator.apply(theme);
		assertEquals(Status.FAILED, outcome.status());
		assertFalse(events.stream().anyMatch(e -> e.startsWith("select")));
		assertEquals(STOCK_DARK, engine.active);
		assertFalse(coordinator.isOwned());
		for (WorkbenchColor role : WorkbenchColor.values()) {
			assertEquals(WorkbenchFamily.DARK.fallbackPalette().get(role),
					sink.values.get(role.colorId(WorkbenchFamily.DARK)));
		}
	}

	// ---- capability and experimental opt-in ----

	@Test
	public void unavailableCapabilityDoesNotSwitchOrWrite() {
		enable();
		engine.probe = new EngineProbe(false, true, true, true);
		WorkbenchOutcome outcome = coordinator.apply(dark("#282c34", "#abb2bf"));
		assertEquals(Status.UNAVAILABLE, outcome.status());
		assertEquals(Reason.HIGH_CONTRAST, outcome.reason());
		assertFalse(outcome.succeeded());
		assertTrue(events.isEmpty());
	}

	@Test
	public void experimentalCombinationNeedsAnExplicitOptInForThatCombination() {
		enable();
		engine.env = env("0.15.300.v1");
		assertEquals(State.EXPERIMENTAL, coordinator.capability().state());
		assertEquals(Status.NOT_REQUESTED, coordinator.apply(dark("#282c34", "#abb2bf")).status());
		assertTrue(events.isEmpty());

		store.setValue(PreferenceKeys.WORKBENCH_EXPERIMENTAL_ACK, "win32/win32/0.15.200");
		assertEquals(Status.NOT_REQUESTED, coordinator.apply(dark("#282c34", "#abb2bf")).status());

		store.setValue(PreferenceKeys.WORKBENCH_EXPERIMENTAL_ACK, "win32/win32/0.15.300");
		assertEquals(Status.APPLIED, coordinator.apply(dark("#282c34", "#abb2bf")).status());
		assertEquals(WorkbenchFamily.DARK.themeId(), engine.active);
	}

	// ---- failure handling ----

	@Test
	public void failedSwitchThatLeavesTheAttemptedThemeActiveIsRevertedToThePreviousTheme() {
		enable();
		engine.throwAfterSelect = true;
		WorkbenchOutcome outcome = coordinator.apply(dark("#282c34", "#abb2bf"));
		assertEquals(Status.FAILED, outcome.status());
		assertEquals(STOCK_DARK, engine.active);
		assertFalse(coordinator.isOwned());
		assertTrue(outcome.message().contains("restored"));
		for (WorkbenchColor role : WorkbenchColor.values()) {
			assertEquals(WorkbenchFamily.DARK.fallbackPalette().get(role),
					sink.values.get(role.colorId(WorkbenchFamily.DARK)));
		}
	}

	@Test
	public void aFailedCombinationIsNotRetriedInTheSameSession() {
		enable();
		engine.throwAfterSelect = true;
		coordinator.apply(dark("#282c34", "#abb2bf"));
		engine.throwAfterSelect = false;
		events.clear();
		WorkbenchOutcome outcome = coordinator.apply(dark("#282c34", "#abb2bf"));
		assertEquals(Status.UNAVAILABLE, outcome.status());
		assertEquals(Reason.POSTCONDITION_FAILED, outcome.reason());
		assertTrue(events.isEmpty());
	}

	@Test
	public void anExperimentalFailureClearsTheUnconfirmedOptIn() {
		enable();
		engine.env = env("0.15.300.v1");
		store.setValue(PreferenceKeys.WORKBENCH_EXPERIMENTAL_ACK, "win32/win32/0.15.300");
		engine.throwAfterSelect = true;
		coordinator.apply(dark("#282c34", "#abb2bf"));
		assertEquals("", store.getString(PreferenceKeys.WORKBENCH_EXPERIMENTAL_ACK));
	}

	@Test
	public void aNewerExternalChoiceDuringAFailureIsNeverOverwritten() {
		enable();
		engine.landOn = Optional.of("user.chosen.theme");
		WorkbenchOutcome outcome = coordinator.apply(dark("#282c34", "#abb2bf"));
		assertEquals(Status.FAILED, outcome.status());
		assertEquals("user.chosen.theme", engine.active);
		assertEquals(1, events.stream().filter(e -> e.startsWith("select")).count());
	}

	// ---- relinquish and external changes ----

	@Test
	public void disableRestoresThePriorThemeAndAbsentSavedChoice() {
		enable();
		coordinator.apply(dark("#282c34", "#abb2bf"));
		store.setValue(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED, false);

		WorkbenchOutcome outcome = coordinator.apply(dark("#282c34", "#abb2bf"));
		assertEquals(Status.RELINQUISHED, outcome.status());
		assertEquals(STOCK_DARK, engine.active);
		assertEquals(Optional.empty(), engine.saved);
		assertFalse(coordinator.isOwned());
		for (WorkbenchFamily family : WorkbenchFamily.values()) {
			for (WorkbenchColor role : WorkbenchColor.values()) {
				assertEquals(family.fallbackPalette().get(role), sink.values.get(role.colorId(family)));
			}
		}
	}

	@Test
	public void disableKeepsAnExistingSavedChoiceInsteadOfRemovingIt() {
		enable();
		engine.saved = Optional.of(STOCK_DARK);
		coordinator.apply(dark("#282c34", "#abb2bf"));
		store.setValue(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED, false);
		coordinator.relinquish();
		assertEquals(STOCK_DARK, engine.active);
		assertEquals(Optional.of(STOCK_DARK), engine.saved);
	}

	@Test
	public void anExternalAppearanceChoiceWinsOnDisableAndNothingIsReasserted() {
		enable();
		coordinator.apply(dark("#282c34", "#abb2bf"));
		engine.active = STOCK_LIGHT; // the user picked another theme on the Appearance page
		events.clear();
		store.setValue(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED, false);

		WorkbenchOutcome outcome = coordinator.relinquish();
		assertEquals(Status.YIELDED, outcome.status());
		assertEquals(STOCK_LIGHT, engine.active);
		assertFalse(events.stream().anyMatch(e -> e.startsWith("select")));
		assertFalse(coordinator.isOwned());
	}

	@Test
	public void anUnknownPriorThemeIsNeverForcedOnDisable() {
		enable();
		engine.active = ""; // nothing was known when ownership started
		coordinator.apply(dark("#282c34", "#abb2bf"));
		assertEquals("", store.getString(PreferenceKeys.WORKBENCH_PRIOR_ACTIVE_ID));
		store.setValue(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED, false);
		events.clear();
		WorkbenchOutcome outcome = coordinator.relinquish();
		assertEquals(Status.YIELDED, outcome.status());
		assertFalse(events.stream().anyMatch(e -> e.startsWith("select")));
		assertFalse(coordinator.isOwned());
	}

	@Test
	public void applyingAgainAfterAnExternalChoiceRecapturesPriorStateFromTheNewTheme() {
		enable();
		coordinator.apply(dark("#282c34", "#abb2bf"));
		engine.active = STOCK_LIGHT;
		coordinator.apply(light("#fafafa", "#383a42"));
		assertEquals(STOCK_LIGHT, store.getString(PreferenceKeys.WORKBENCH_PRIOR_ACTIVE_ID));
		assertEquals(WorkbenchFamily.LIGHT.themeId(), engine.active);
	}

	// ---- startup ----

	@Test
	public void startupRepublishesFromTheSelectedThemeWhenOursIsActive() {
		enable();
		Theme theme = dark("#282c34", "#abb2bf");
		coordinator.apply(theme);
		sink.reset(); // a restart: the registry is back at the fallback, our theme is still the active one
		events.clear();

		WorkbenchOutcome outcome = coordinator.reconcile(theme);
		assertEquals(Status.APPLIED, outcome.status());
		assertEquals(Color.ofHex("#282c34"), sink.values.get(WorkbenchColor.BACKGROUND.colorId(WorkbenchFamily.DARK)));
		assertEquals(WorkbenchFamily.DARK.alternateThemeId(), engine.active);
	}

	@Test
	public void startupDoesNothingWhenAnotherThemeIsActiveOrCoordinationIsOff() {
		Theme theme = dark("#282c34", "#abb2bf");
		assertEquals(Status.NOT_REQUESTED, coordinator.reconcile(theme).status());
		enable();
		engine.active = STOCK_LIGHT;
		assertEquals(Status.YIELDED, coordinator.reconcile(theme).status());
		assertTrue(events.isEmpty());
	}

	// ---- helpers ----

	private void enable() {
		store.setValue(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED, true);
	}

	private static WorkbenchEnvironment env(String themeBundle) {
		return new WorkbenchEnvironment("win32", "win32", themeBundle, "4.40.0", "21");
	}

	private static Theme dark(String background, String foreground) {
		return theme(ThemeType.DARK, background, foreground);
	}

	private static Theme light(String background, String foreground) {
		return theme(ThemeType.LIGHT, background, foreground);
	}

	private static Theme theme(ThemeType type, String background, String foreground) {
		Map<TokenKey, Token> tokens = new HashMap<>();
		tokens.put(TokenKey.BACKGROUND, new TokenBuilder().setKey(TokenKey.BACKGROUND).setColor(Color.ofHex(background)).build());
		tokens.put(TokenKey.FOREGROUND, new TokenBuilder().setKey(TokenKey.FOREGROUND).setColor(Color.ofHex(foreground)).build());
		return new Theme("id", "name", "", "", "", type, tokens);
	}

	private final class FakeEngine implements WorkbenchThemeEngine {
		String active;
		Optional<String> saved = Optional.empty();
		WorkbenchEnvironment env = env("0.15.200.v1");
		EngineProbe probe = EngineProbe.usable();
		/** After a select the engine ends up on this theme instead of the requested one. */
		Optional<String> landOn = Optional.empty();
		boolean throwAfterSelect;

		@Override
		public WorkbenchEnvironment environment() {
			return env;
		}

		@Override
		public EngineProbe probe() {
			return probe;
		}

		@Override
		public Optional<String> activeThemeId() {
			return Optional.ofNullable(active);
		}

		@Override
		public boolean select(String themeId) {
			events.add("select " + themeId);
			if (throwAfterSelect && WorkbenchSlots.isOurs(themeId)) {
				active = themeId;
				saved = Optional.of(themeId);
				throw new IllegalStateException("engine failed");
			}
			active = landOn.orElse(themeId);
			saved = Optional.of(active);
			return true;
		}

		@Override
		public Optional<String> savedThemeId() {
			return saved;
		}

		@Override
		public void clearSavedThemeId() {
			events.add("clearSaved");
			saved = Optional.empty();
		}
	}

	private final class FakeSink implements PaletteSink {
		final Map<String, Color> values = new HashMap<>();
		int failOnPut;
		private int puts;

		FakeSink() {
			reset();
		}

		void reset() {
			for (WorkbenchFamily family : WorkbenchFamily.values()) {
				for (WorkbenchColor role : WorkbenchColor.values()) {
					values.put(role.colorId(family), family.fallbackPalette().get(role));
				}
			}
		}

		@Override
		public Optional<Color> get(String colorId) {
			return Optional.ofNullable(values.get(colorId));
		}

		@Override
		public void put(String colorId, Color color) {
			puts++;
			if (failOnPut > 0 && puts == failOnPut) {
				failOnPut = 0;
				throw new IllegalStateException("registry failure");
			}
			events.add("put");
			values.put(colorId, color);
		}
	}
}
