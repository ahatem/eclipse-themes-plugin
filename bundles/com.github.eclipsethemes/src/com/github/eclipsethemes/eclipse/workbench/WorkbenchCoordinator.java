package com.github.eclipsethemes.eclipse.workbench;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.workbench.PluginColors;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.Reason;
import com.github.eclipsethemes.core.workbench.WorkbenchCapability.State;
import com.github.eclipsethemes.core.workbench.WorkbenchCapabilityClassifier;
import com.github.eclipsethemes.core.workbench.WorkbenchColor;
import com.github.eclipsethemes.core.workbench.WorkbenchEnvironment;
import com.github.eclipsethemes.core.workbench.WorkbenchFamily;
import com.github.eclipsethemes.core.workbench.WorkbenchPalette;
import com.github.eclipsethemes.core.workbench.WorkbenchPaletteResolver;
import com.github.eclipsethemes.core.workbench.WorkbenchSlots;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;

/**
 * Owns the workbench half of applying a theme: ordering (publish, then switch, then verify, then record),
 * ownership, safe restoration and the per-session failure flag. Call on the UI thread.
 *
 * <p>
 * The selected theme and the palette resolver are the source of truth; the color registry is only a delivery
 * mechanism and is republished whenever coordination is active. The only listener is the one on the saved theme
 * id, and it only releases plug-in colors this plugin still owns: nothing ever reapplies, so a newer external
 * Appearance choice is never fought.
 *
 * <p>
 * Order of one apply: resolve the palette, switch the workbench theme (Eclipse's own switch resets preference
 * defaults that the previous theme overrode, so anything written to other plug-ins' preferences must come after
 * it), verify the switch, write the plug-in colors, then record ownership.
 */
public final class WorkbenchCoordinator {

	private final WorkbenchThemeEngine engine;
	private final PaletteSink sink;
	private final IPreferenceStore store;
	private final PluginColorWriter pluginColors;
	private final Set<String> failedCombinations = new HashSet<>();
	private Runnable stopWatching;

	public WorkbenchCoordinator(WorkbenchThemeEngine engine, PaletteSink sink, IPreferenceStore store,
			PluginColorWriter pluginColors) {
		this.engine = engine;
		this.sink = sink;
		this.store = store;
		this.pluginColors = pluginColors;
	}

	// ---- state read by the UI and startup ----

	public WorkbenchCapability capability() {
		WorkbenchEnvironment environment = engine.environment();
		if (failedCombinations.contains(environment.combination())) {
			return WorkbenchCapability.unavailable(Reason.POSTCONDITION_FAILED, environment);
		}
		return WorkbenchCapabilityClassifier.classify(environment, engine.probe());
	}

	public boolean isEnabled() {
		return store.getBoolean(PreferenceKeys.WORKBENCH_COORDINATION_ENABLED);
	}

	public boolean isOptedIn(WorkbenchCapability capability) {
		return capability.environment().combination()
				.equals(store.getString(PreferenceKeys.WORKBENCH_EXPERIMENTAL_ACK));
	}

	public boolean isOwned() {
		return !store.getString(PreferenceKeys.WORKBENCH_OWNED_THEME_ID).isEmpty();
	}

	public Optional<String> activeThemeId() {
		return engine.activeThemeId();
	}

	/** The result of the latest plug-in color synchronization, for diagnostics. */
	public PluginColorSummary pluginColorSummary() {
		return pluginColors.lastSummary();
	}

	/** The coordinated plug-in keys the ownership record says are still this plugin's. */
	public List<String> ownedPluginColorKeys() {
		return pluginColors.ownedKeys();
	}

	/** True when the ownership record says some plug-in color is still this plugin's. */
	public boolean ownsPluginColors() {
		return pluginColors.hasOwned();
	}

	// ---- external theme change: release only ----

	/**
	 * Starts, once, watching for another Eclipse theme being chosen. The reaction is to release the plug-in colors
	 * this plugin still owns; it never reapplies anything.
	 */
	public void startWatching() {
		if (stopWatching == null) {
			stopWatching = engine.watchSavedThemeId(this::onSavedThemeChanged);
		}
	}

	/** Removes the registration made by {@link #startWatching()}. */
	public void dispose() {
		if (stopWatching != null) {
			stopWatching.run();
			stopWatching = null;
		}
	}

	private void onSavedThemeChanged(Optional<String> newId) {
		if (newId.filter(WorkbenchSlots::isOurs).isEmpty() && pluginColors.hasOwned()) {
			pluginColors.release();
		}
	}

	// ---- apply, relinquish, reconcile ----

	/** Applies the workbench half for the committed intent: coordinates, hands back, or does nothing. */
	public WorkbenchOutcome apply(Theme theme) {
		if (!isEnabled()) {
			return isOwned() ? relinquish()
					: WorkbenchOutcome.notRequested("Workbench coordination is off.").withColors(releaseColors());
		}
		WorkbenchCapability capability = capability();
		if (!capability.usable()) {
			return WorkbenchOutcome.unavailable(capability.reason(), "Workbench theming is unavailable: "
					+ capability.reason() + " (" + capability.environment().combination() + ").")
					.withColors(releaseColors());
		}
		if (capability.state() == State.EXPERIMENTAL && !isOptedIn(capability)) {
			return WorkbenchOutcome.notRequested("Experimental workbench theming is not enabled for "
					+ capability.environment().combination() + ".").withColors(releaseColors());
		}

		WorkbenchPalette palette = WorkbenchPaletteResolver.resolve(theme);
		WorkbenchFamily family = palette.family();
		String active = engine.activeThemeId().orElse("");
		boolean keepOwnership = isOwned() && WorkbenchSlots.isOurs(active);
		if (keepOwnership && WorkbenchSlots.familyOf(active).orElseThrow() == family && publishedEquals(palette)) {
			startWatching();
			return WorkbenchOutcome.applied("Workbench already carries this theme's palette.")
					.withColors(syncColors(palette));
		}

		// Prior state is captured before the switch, because the switch rewrites the saved theme id.
		Optional<String> savedBefore = engine.savedThemeId();
		String target = WorkbenchSlots.target(family, active);
		Map<String, Color> previous = snapshot(family);
		try {
			publish(palette, previous);
		} catch (RuntimeException e) {
			return WorkbenchOutcome.failed(null, "Could not publish the workbench palette: " + e.getMessage()
					+ " Nothing was switched and previous colors were restored.");
		}

		try {
			if (!engine.select(target)) {
				return failAfterAttempt(capability, active, target, previous, Reason.THEME_NOT_FOUND,
						"Workbench theme " + target + " is not available.");
			}
			String now = engine.activeThemeId().orElse("");
			if (!target.equals(now)) {
				return failAfterAttempt(capability, active, target, previous, Reason.POSTCONDITION_FAILED,
						"Eclipse reports " + (now.isEmpty() ? "no active theme" : now) + " instead of " + target + ".");
			}
		} catch (RuntimeException e) {
			return failAfterAttempt(capability, active, target, previous, Reason.POSTCONDITION_FAILED,
					"The Eclipse theme engine failed: " + e.getMessage());
		}

		PluginColorSummary colors = syncColors(palette);
		if (!keepOwnership) {
			store.setValue(PreferenceKeys.WORKBENCH_PRIOR_ACTIVE_ID, WorkbenchSlots.isOurs(active) ? "" : active);
			store.setValue(PreferenceKeys.WORKBENCH_PRIOR_SAVED_ID, savedBefore.orElse(""));
			store.setValue(PreferenceKeys.WORKBENCH_PRIOR_SAVED_ABSENT, savedBefore.isEmpty());
		}
		store.setValue(PreferenceKeys.WORKBENCH_OWNED_THEME_ID, target);
		persist();
		startWatching();
		return WorkbenchOutcome.applied("Workbench theme set to " + target + ".").withColors(colors);
	}

	/**
	 * Startup: republish from the selected theme when, and only when, coordination is on and one of this
	 * plugin's workbench themes is still the active Eclipse theme. A different active theme is a newer choice.
	 */
	public WorkbenchOutcome reconcile(Theme theme) {
		if (!isEnabled()) {
			return WorkbenchOutcome.notRequested("Workbench coordination is off.").withColors(releaseColors());
		}
		String active = engine.activeThemeId().orElse("");
		if (!WorkbenchSlots.isOurs(active)) {
			startWatching();
			return WorkbenchOutcome.yielded("Another Eclipse theme is active; the plugin changed nothing.")
					.withColors(releaseColors());
		}
		return apply(theme);
	}

	/**
	 * Hands the workbench back: restores the prior theme only while this plugin's theme is still the active
	 * one, so a newer external Appearance choice always wins.
	 */
	public WorkbenchOutcome relinquish() {
		String prior = store.getString(PreferenceKeys.WORKBENCH_PRIOR_ACTIVE_ID);
		boolean savedAbsent = store.getBoolean(PreferenceKeys.WORKBENCH_PRIOR_SAVED_ABSENT);
		String active = engine.activeThemeId().orElse("");
		WorkbenchCapability capability = capability();
		PluginColorSummary colors = releaseColors();
		return relinquishTheme(prior, savedAbsent, active, capability).withColors(colors);
	}

	private WorkbenchOutcome relinquishTheme(String prior, boolean savedAbsent, String active,
			WorkbenchCapability capability) {
		if (!WorkbenchSlots.isOurs(active)) {
			resetPalette();
			clearOwnership();
			return WorkbenchOutcome.yielded("A different Eclipse theme is active, so it was left as chosen.");
		}
		if (prior.isEmpty() || !capability.usable()) {
			clearOwnership();
			return WorkbenchOutcome.yielded("The previous Eclipse theme is not known or cannot be restored; choose "
					+ "a theme on the Appearance page.");
		}
		try {
			if (!engine.select(prior) || !prior.equals(engine.activeThemeId().orElse(""))) {
				clearOwnership();
				return WorkbenchOutcome.yielded("The previous Eclipse theme '" + prior
						+ "' could not be restored; choose a theme on the Appearance page.");
			}
			if (savedAbsent && engine.savedThemeId().filter(prior::equals).isPresent()) {
				engine.clearSavedThemeId();
			}
		} catch (RuntimeException e) {
			clearOwnership();
			return WorkbenchOutcome.failed(Reason.POSTCONDITION_FAILED,
					"Restoring the previous Eclipse theme failed: " + e.getMessage());
		}
		resetPalette();
		clearOwnership();
		return WorkbenchOutcome.relinquished("The previous Eclipse theme was restored.");
	}

	// ---- helpers ----

	/** Writes the dark-family plug-in colors for this palette; a light palette derives none and so releases. */
	private PluginColorSummary syncColors(WorkbenchPalette palette) {
		try {
			return pluginColors.sync(PluginColors.derive(palette));
		} catch (RuntimeException e) {
			return failedColors("Updating plug-in colors failed: " + e.getMessage());
		}
	}

	private PluginColorSummary releaseColors() {
		if (!pluginColors.hasOwned()) {
			return PluginColorSummary.EMPTY;
		}
		try {
			return pluginColors.release();
		} catch (RuntimeException e) {
			return failedColors("Releasing plug-in colors failed: " + e.getMessage());
		}
	}

	private static PluginColorSummary failedColors(String message) {
		return new PluginColorSummary(List.of(), List.of(), List.of(), List.of(message));
	}

	private WorkbenchOutcome failAfterAttempt(WorkbenchCapability capability, String before, String attempted,
			Map<String, Color> previous, Reason reason, String detail) {
		failedCombinations.add(capability.environment().combination());
		if (capability.state() == State.EXPERIMENTAL) {
			store.setValue(PreferenceKeys.WORKBENCH_EXPERIMENTAL_ACK, "");
			persist();
		}
		String now = engine.activeThemeId().orElse("");
		String outcome;
		if (now.equals(before)) {
			restore(previous);
			outcome = "The previous theme remains active.";
		} else if (now.equals(attempted) && !before.isEmpty()) {
			boolean reverted;
			try {
				reverted = engine.select(before) && before.equals(engine.activeThemeId().orElse(""));
			} catch (RuntimeException e) {
				reverted = false;
			}
			if (reverted) {
				restore(previous);
			}
			outcome = reverted ? "The previous theme was restored."
					: "The previous theme could not be restored; choose a theme on the Appearance page.";
		} else {
			outcome = "Another Eclipse theme is active, so nothing was reverted.";
		}
		return WorkbenchOutcome.failed(reason, detail + " " + outcome);
	}

	private Map<String, Color> snapshot(WorkbenchFamily family) {
		Map<String, Color> values = new LinkedHashMap<>();
		for (WorkbenchColor role : WorkbenchColor.values()) {
			String id = role.colorId(family);
			sink.get(id).ifPresent(value -> values.put(id, value));
		}
		return values;
	}

	private boolean publishedEquals(WorkbenchPalette palette) {
		for (WorkbenchColor role : WorkbenchColor.values()) {
			if (!Optional.of(palette.get(role)).equals(sink.get(role.colorId(palette.family())))) {
				return false;
			}
		}
		return true;
	}

	private void publish(WorkbenchPalette palette, Map<String, Color> previous) {
		try {
			for (WorkbenchColor role : WorkbenchColor.values()) {
				String id = role.colorId(palette.family());
				if (!Optional.of(palette.get(role)).equals(Optional.ofNullable(previous.get(id)))) {
					sink.put(id, palette.get(role));
				}
			}
		} catch (RuntimeException e) {
			restore(previous);
			throw e;
		}
	}

	private void restore(Map<String, Color> previous) {
		previous.forEach((id, color) -> {
			try {
				sink.put(id, color);
			} catch (RuntimeException e) {
				// Best effort: the colors are ephemeral and are republished from the selected theme.
			}
		});
	}

	private void resetPalette() {
		for (WorkbenchFamily family : WorkbenchFamily.values()) {
			WorkbenchPalette fallback = family.fallbackPalette();
			for (WorkbenchColor role : WorkbenchColor.values()) {
				String id = role.colorId(family);
				if (!Optional.of(fallback.get(role)).equals(sink.get(id))) {
					sink.put(id, fallback.get(role));
				}
			}
		}
	}

	private void clearOwnership() {
		store.setToDefault(PreferenceKeys.WORKBENCH_OWNED_THEME_ID);
		store.setToDefault(PreferenceKeys.WORKBENCH_PRIOR_ACTIVE_ID);
		store.setToDefault(PreferenceKeys.WORKBENCH_PRIOR_SAVED_ID);
		store.setToDefault(PreferenceKeys.WORKBENCH_PRIOR_SAVED_ABSENT);
		persist();
	}

	private void persist() {
		if (store instanceof IPersistentPreferenceStore persistent && persistent.needsSaving()) {
			try {
				persistent.save();
			} catch (IOException e) {
				// The in-memory value is authoritative for this session; the store is saved again at shutdown.
			}
		}
	}
}
