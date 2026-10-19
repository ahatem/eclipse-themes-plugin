package com.github.eclipsethemes.eclipse.workbench;

import java.util.Optional;
import java.util.function.Consumer;

import com.github.eclipsethemes.core.workbench.EngineProbe;
import com.github.eclipsethemes.core.workbench.WorkbenchEnvironment;

/**
 * The compat boundary to Eclipse's theme engine. {@link EclipseWorkbenchThemeEngine} is its only
 * implementation and the only place that touches the engine API. All calls are made on the UI thread.
 */
public interface WorkbenchThemeEngine {

	WorkbenchEnvironment environment();

	EngineProbe probe();

	Optional<String> activeThemeId();

	/**
	 * Selects the enumerated theme with exactly this id and persists the choice.
	 *
	 * @return false when no such theme is enumerated
	 * @throws RuntimeException when the engine fails
	 */
	boolean select(String themeId);

	/** The theme id Eclipse has saved for the next start, empty when none is saved. */
	Optional<String> savedThemeId();

	/** Removes the saved theme id so that Eclipse resolves its own default again. */
	void clearSavedThemeId();

	/**
	 * Registers {@code listener} for changes of the saved theme id (the value is empty when the id was removed). It runs
	 * on the thread that changed the preference, which is the UI thread for the Appearance page and for
	 * {@link #select}.
	 *
	 * @return the call that removes the registration
	 */
	Runnable watchSavedThemeId(Consumer<Optional<String>> listener);
}
