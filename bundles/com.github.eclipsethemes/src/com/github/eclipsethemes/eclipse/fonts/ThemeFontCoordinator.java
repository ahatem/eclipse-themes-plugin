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
 * Applies and hands back the fonts of the two targets (editor, workbench) according to the user's
 * {@link FontSettings}, under a conflict-aware ownership model. Call on the UI thread, after the workbench theme
 * switch.
 *
 * <ul>
 * <li>Each target is {@link FontMode#KEEP}, {@link FontMode#THEME} or {@link FontMode#CUSTOM}, independently. Applying
 * colors alone never changes a font, and a target whose resolved font does not exist (keep, or a theme that names none)
 * is not managed.</li>
 * <li>A definition not yet owned is taken over: its prior state (a stored value, or none) is recorded with the exact
 * value written. An owned definition is updated while it still holds the recorded value. When it holds something else
 * (another page or the user changed it), that newer value is the new prior state and is replaced only when the
 * request itself changed; an unchanged request leaves it alone.</li>
 * <li>A target that stops being managed gets its prior state back, again only while the definition still holds the
 * recorded value; a newer value is left as found.</li>
 * <li>A requested family that is not installed is reported and changes nothing.</li>
 * </ul>
 */
public final class ThemeFontCoordinator {

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

	public FontSettings settings() {
		return FontSettings.load(store);
	}

	public boolean ownsFonts() {
		return !read().isEmpty();
	}

	/** True when the family is installed; used to describe what is requested. */
	public boolean installed(String family) {
		return fonts.installed(family);
	}

	/** The font a target is using right now, for display. */
	public Optional<ThemeFonts.Face> current(FontTarget target) {
		return fonts.effective(target.definitionIds().get(0));
	}

	/** Applies the committed font settings for the theme, handing back any target that is no longer managed. */
	public FontOutcome apply(Theme theme) {
		FontSettings settings = settings();
		Map<String, Owned> record = read();
		List<String> missing = new ArrayList<>();
		List<String> kept = new ArrayList<>();
		boolean applied = false;
		boolean released = false;
		boolean themeWithoutFont = false;
		boolean touched = false;
		try {
			for (FontTarget target : FontTarget.values()) {
				FontChoice choice = settings.choice(target);
				Optional<ThemeFonts.Face> wanted = choice.resolve(target.themeFace(theme.getFonts()));
				if (wanted.isEmpty()) {
					themeWithoutFont |= choice.mode() == FontMode.THEME;
					released |= giveBack(target.definitionIds(), record);
					continue;
				}
				ThemeFonts.Face face = wanted.get();
				if (!fonts.installed(face.family())) {
					missing.add("Requested " + target.role() + " font \"" + face.family() + "\" is not installed; the "
							+ target.role() + " font was not changed.");
					continue;
				}
				applied |= take(target, fonts.represent(face), record, kept);
				touched = true;
			}
			if (touched || released) {
				fonts.flush();
				save(record);
			}
		} catch (IOException | RuntimeException e) {
			return new FontOutcome(FontOutcome.Status.FAILED, "Fonts could not be applied: " + e.getMessage());
		}
		if (!missing.isEmpty()) {
			return new FontOutcome(FontOutcome.Status.INCOMPLETE, String.join(" ", missing));
		}
		if (applied) {
			return new FontOutcome(FontOutcome.Status.APPLIED,
					kept.isEmpty() ? "Fonts applied." : "Fonts applied. " + String.join(" ", kept));
		}
		if (!kept.isEmpty()) {
			return new FontOutcome(FontOutcome.Status.APPLIED, String.join(" ", kept));
		}
		if (released) {
			return new FontOutcome(FontOutcome.Status.RELEASED, "Previous fonts were restored.");
		}
		return themeWithoutFont ? new FontOutcome(FontOutcome.Status.NOTHING_TO_APPLY, "This theme does not specify fonts.")
				: FontOutcome.NOT_REQUESTED;
	}

	/** Gives back every definition whose stored value is still the one this plugin wrote, for every target. */
	public FontOutcome release() {
		Map<String, Owned> record = read();
		try {
			for (FontTarget target : FontTarget.values()) {
				giveBack(target.definitionIds(), record);
			}
			fonts.flush();
			save(record);
		} catch (IOException | RuntimeException e) {
			return new FontOutcome(FontOutcome.Status.FAILED, "Previous fonts could not be restored: " + e.getMessage());
		}
		return new FontOutcome(FontOutcome.Status.RELEASED, "Previous fonts were restored.");
	}

	/**
	 * Writes the font to the target's definitions. Returns true when at least one definition was written; a definition
	 * the user changed since and that is requested unchanged is left and noted in {@code kept}.
	 */
	private boolean take(FontTarget target, String stored, Map<String, Owned> record, List<String> kept) {
		boolean wrote = false;
		boolean keptAny = false;
		for (String id : target.definitionIds()) {
			Optional<String> current = fonts.get(id);
			Owned owned = record.get(id);
			if (owned == null) {
				record.put(id, new Owned(stored, current.orElse(null)));
			} else if (current.filter(owned.written()::equals).isPresent()) {
				record.put(id, new Owned(stored, owned.prior()));
			} else if (!stored.equals(owned.written())) {
				// The request changed, so this is an explicit new choice; what the definition holds now is what
				// would be given back later.
				record.put(id, new Owned(stored, current.orElse(null)));
			} else {
				keptAny = true;
				continue;
			}
			fonts.put(id, stored);
			wrote = true;
		}
		if (keptAny) {
			kept.add("Your own " + target.role() + " font choice was kept; choose the font again to replace it.");
		}
		return wrote;
	}

	/** Restores each owned definition still holding the written value and forgets it; true if any was owned. */
	private boolean giveBack(List<String> ids, Map<String, Owned> record) {
		boolean any = false;
		for (String id : ids) {
			Owned owned = record.remove(id);
			if (owned == null) {
				continue;
			}
			any = true;
			if (fonts.get(id).filter(owned.written()::equals).isPresent()) {
				if (owned.prior() == null) {
					fonts.remove(id);
				} else {
					fonts.put(id, owned.prior());
				}
			}
		}
		return any;
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
