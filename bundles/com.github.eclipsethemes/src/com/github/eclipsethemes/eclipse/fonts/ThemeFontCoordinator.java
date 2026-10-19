package com.github.eclipsethemes.eclipse.fonts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeFonts;
import com.github.eclipsethemes.eclipse.preferences.PreferenceKeys;

/**
 * Applies and hands back the fonts a theme package asks for, under a conflict-aware ownership model. Call on the UI
 * thread, after the workbench theme switch.
 *
 * <ul>
 * <li>Fonts are an independent opt-in ({@link PreferenceKeys#THEME_FONTS_ENABLED}); applying colors never changes a
 * font, and a theme without fonts changes nothing.</li>
 * <li>The editor font is the platform <em>Text Font</em>. The workbench font is the platform <em>Part title</em>
 * ({@code TAB_TEXT_FONT}), <em>Tree and Table</em> and <em>Dialog</em> font definitions, all given the one requested
 * font.</li>
 * <li>A definition not yet owned is taken over: its prior state (a stored value, or none) is recorded with the exact
 * value written. An owned definition is updated only while it still holds the recorded value; a different value is the
 * user's newer choice and is left as found.</li>
 * <li>Turning fonts off restores each owned definition's prior state, again only while it still holds the recorded
 * value.</li>
 * <li>A requested family that is not installed is reported and changes nothing.</li>
 * </ul>
 */
public final class ThemeFontCoordinator {

	static final String TEXT_FONT = "org.eclipse.jface.textfont";
	static final String TAB_TEXT_FONT = "org.eclipse.ui.workbench.TAB_TEXT_FONT";
	static final String TREE_TABLE_FONT = "org.eclipse.ui.workbench.TREE_TABLE_FONT";
	static final String DIALOG_FONT = "org.eclipse.jface.dialogfont";

	static final List<String> EDITOR_FONTS = List.of(TEXT_FONT);
	static final List<String> WORKBENCH_FONTS = List.of(TAB_TEXT_FONT, TREE_TABLE_FONT, DIALOG_FONT);

	private static final String ABSENT = "A";
	private static final String VALUE = "V";

	/** One owned definition: the value this plugin wrote and what the definition held before ({@code null}: nothing). */
	private record Owned(String written, String prior) {
	}

	private final WorkbenchFonts fonts;
	private final IPreferenceStore store;

	public ThemeFontCoordinator(WorkbenchFonts fonts, IPreferenceStore store) {
		this.fonts = fonts;
		this.store = store;
	}

	public boolean isEnabled() {
		return store.getBoolean(PreferenceKeys.THEME_FONTS_ENABLED);
	}

	public boolean ownsFonts() {
		return !read().isEmpty();
	}

	/** True when the family is installed; used to describe what a theme asks for. */
	public boolean installed(String family) {
		return fonts.installed(family);
	}

	/** Applies the committed intent for fonts: applies the theme's fonts, hands fonts back, or does nothing. */
	public FontOutcome apply(Theme theme) {
		if (!isEnabled()) {
			return ownsFonts() ? release() : FontOutcome.NOT_REQUESTED;
		}
		ThemeFonts requested = theme.getFonts();
		if (requested.isEmpty()) {
			return new FontOutcome(FontOutcome.Status.NOTHING_TO_APPLY, "This theme does not specify fonts.");
		}
		Map<String, Owned> record = read();
		List<String> notes = new ArrayList<>();
		try {
			take("editor", requested.editorFont(), EDITOR_FONTS, record, notes);
			take("workbench", requested.workbenchFont(), WORKBENCH_FONTS, record, notes);
			fonts.flush();
			save(record);
		} catch (IOException | RuntimeException e) {
			return new FontOutcome(FontOutcome.Status.FAILED, "Theme fonts could not be applied: " + e.getMessage());
		}
		return notes.isEmpty() ? new FontOutcome(FontOutcome.Status.APPLIED, "Theme fonts applied.")
				: new FontOutcome(FontOutcome.Status.INCOMPLETE, String.join(" ", notes));
	}

	/** Gives back every definition whose stored value is still the one this plugin wrote. */
	public FontOutcome release() {
		Map<String, Owned> record = read();
		try {
			for (Map.Entry<String, Owned> entry : record.entrySet()) {
				if (fonts.get(entry.getKey()).filter(entry.getValue().written()::equals).isPresent()) {
					String prior = entry.getValue().prior();
					if (prior == null) {
						fonts.remove(entry.getKey());
					} else {
						fonts.put(entry.getKey(), prior);
					}
				}
			}
			fonts.flush();
			save(Map.of());
		} catch (IOException | RuntimeException e) {
			return new FontOutcome(FontOutcome.Status.FAILED, "Previous fonts could not be restored: " + e.getMessage());
		}
		return new FontOutcome(FontOutcome.Status.RELEASED, "Previous fonts were restored.");
	}

	private void take(String role, Optional<ThemeFonts.Face> face, List<String> ids, Map<String, Owned> record,
			List<String> notes) {
		if (face.isEmpty()) {
			return;
		}
		ThemeFonts.Face wanted = face.get();
		if (!fonts.installed(wanted.family())) {
			notes.add("Requested " + role + " font \"" + wanted.family() + "\" is not installed; the " + role
					+ " font was not changed.");
			return;
		}
		String stored = fonts.represent(wanted);
		boolean kept = false;
		for (String id : ids) {
			Optional<String> current = fonts.get(id);
			Owned owned = record.get(id);
			if (owned == null) {
				record.put(id, new Owned(stored, current.orElse(null)));
			} else if (current.filter(owned.written()::equals).isPresent()) {
				record.put(id, new Owned(stored, owned.prior()));
			} else {
				kept = true;
				continue;
			}
			fonts.put(id, stored);
		}
		if (kept) {
			notes.add("Your own " + role + " font choice was kept; turn theme fonts off and on again to use the theme's.");
		}
	}

	// ---- ownership record: one line per definition, "id TAB written TAB prior", prior being "A" or "V" + value ----

	private Map<String, Owned> read() {
		Map<String, Owned> record = new LinkedHashMap<>();
		for (String line : store.getString(PreferenceKeys.THEME_FONTS_OWNED).split("\n")) {
			String[] parts = line.split("\t", -1);
			if (parts.length == 3 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
				String prior = parts[2].startsWith(VALUE) ? parts[2].substring(1) : null;
				record.put(parts[0], new Owned(parts[1], prior));
			}
		}
		return record;
	}

	private void save(Map<String, Owned> record) throws IOException {
		if (record.isEmpty()) {
			store.setToDefault(PreferenceKeys.THEME_FONTS_OWNED);
		} else {
			List<String> lines = new ArrayList<>();
			record.forEach((id, owned) -> lines.add(id + "\t" + owned.written() + "\t"
					+ (owned.prior() == null ? ABSENT : VALUE + owned.prior())));
			store.setValue(PreferenceKeys.THEME_FONTS_OWNED, String.join("\n", lines));
		}
		if (store instanceof IPersistentPreferenceStore persistent && persistent.needsSaving()) {
			persistent.save();
		}
	}
}
