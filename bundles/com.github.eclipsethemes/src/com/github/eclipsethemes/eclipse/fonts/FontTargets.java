package com.github.eclipsethemes.eclipse.fonts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.github.eclipsethemes.core.models.ThemeFonts;

/**
 * Which Eclipse font definitions each semantic font coordinates, and why. Pure: it works from the font definitions the
 * running Eclipse declares ({@link WorkbenchFonts#definitions()}), so it needs neither widgets nor a workbench.
 *
 * <p>
 * Provenance, so a maintainer can answer "why is this definition here?":
 * <ol>
 * <li><b>Base.</b> {@code org.eclipse.jface.textfont} (Text Font) for the editor font and
 * {@code org.eclipse.jface.dialogfont} (Dialog Font) for the interface font.</li>
 * <li><b>Declared inheritance.</b> Every editable definition that Eclipse itself declares to default to the base,
 * directly or through other definitions ({@code defaultsTo}). For example the Java editor, console, compare and
 * terminal fonts follow Text Font. Eclipse's own declaration is the reason; a font's current family never is.</li>
 * <li><b>Proven supplements.</b> A few definitions that declare no inheritance but demonstrably serve the same
 * purpose, listed below with the evidence, and used only when their bundle is installed.</li>
 * </ol>
 * Anything else, including definitions of unknown purpose, is left alone.
 */
final class FontTargets {

	/** One definition to write: its id and how the group's base font is shaped for it. */
	record Target(String id, boolean bold, int sizeDelta) {

		/** The size is the base size plus the delta; the family is always the base family. */
		ThemeFonts.Face shape(ThemeFonts.Face base) {
			int size = Math.max(ThemeFonts.MIN_SIZE, Math.min(ThemeFonts.MAX_SIZE, base.size() + sizeDelta));
			return new ThemeFonts.Face(base.family(), size);
		}
	}

	/**
	 * Supplements for the editor font. Neither declares {@code defaultsTo}.
	 * <ul>
	 * <li>{@code blockSelectionModeFont}: used by text editors in block (column) mode. Eclipse's description asks for a
	 * monospace font, and it otherwise stays Courier New while a block selection is made.</li>
	 * <li>{@code failurePaneFont}: the JUnit failure trace, a stack trace shown as text.</li>
	 * </ul>
	 */
	private static final List<Target> EDITOR_SUPPLEMENTS = List.of(
			new Target("org.eclipse.ui.workbench.texteditor.blockSelectionModeFont", false, 0),
			new Target("org.eclipse.jdt.junit.failurePaneFont", false, 0));

	/**
	 * Supplements for the interface font. None declares {@code defaultsTo}.
	 * <ul>
	 * <li>{@code TAB_TEXT_FONT} (Part title) and {@code TREE_TABLE_FONT} (Tree and Table): the fonts the Eclipse CSS
	 * themes apply to view and editor tabs and to trees and tables.</li>
	 * <li>{@code bannerfont} and {@code headerfont}: bold wizard banners and section headers. Eclipse's own Windows
	 * defaults are bold, one and three points above the 9 pt interface font, so they keep that emphasis relative to the
	 * chosen size instead of turning into ordinary text.</li>
	 * </ul>
	 */
	private static final List<Target> INTERFACE_SUPPLEMENTS = List.of(
			new Target("org.eclipse.ui.workbench.TAB_TEXT_FONT", false, 0),
			new Target("org.eclipse.ui.workbench.TREE_TABLE_FONT", false, 0),
			new Target("org.eclipse.jface.bannerfont", true, 1),
			new Target("org.eclipse.jface.headerfont", true, 3));

	private static final int MAX_DEPTH = 16;

	private FontTargets() {
	}

	/**
	 * The definitions the group coordinates, ancestors first: the base, then the declared descendants, then the
	 * supplements that are installed and editable.
	 */
	static List<Target> resolve(FontTarget group, Map<String, WorkbenchFonts.Definition> installed) {
		List<Target> targets = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		targets.add(new Target(group.baseId(), false, 0));
		seen.add(group.baseId());

		// Ordered by depth, then id, so a parent always precedes its children and the order is stable.
		Map<String, Integer> descendants = new TreeMap<>();
		installed.forEach((id, definition) -> {
			int depth = depthBelow(group.baseId(), id, installed);
			if (depth > 0 && definition.editable() && !definition.hasValue()) {
				descendants.put(id, depth);
			}
		});
		descendants.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
				.forEach(entry -> {
					if (seen.add(entry.getKey())) {
						targets.add(new Target(entry.getKey(), false, 0));
					}
				});

		for (Target supplement : group == FontTarget.EDITOR ? EDITOR_SUPPLEMENTS : INTERFACE_SUPPLEMENTS) {
			WorkbenchFonts.Definition definition = installed.get(supplement.id());
			if (definition != null && definition.editable() && seen.add(supplement.id())) {
				targets.add(supplement);
			}
		}
		return targets;
	}

	/** How many {@code defaultsTo} steps lead from the definition up to the base; 0 when it does not reach it. */
	private static int depthBelow(String baseId, String id, Map<String, WorkbenchFonts.Definition> installed) {
		String current = id;
		for (int depth = 0; depth <= MAX_DEPTH; depth++) {
			if (current.equals(baseId)) {
				return depth;
			}
			WorkbenchFonts.Definition definition = installed.get(current);
			if (definition == null || definition.defaultsTo() == null) {
				return 0;
			}
			current = definition.defaultsTo();
		}
		return 0;
	}
}
