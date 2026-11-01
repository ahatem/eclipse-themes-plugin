# Proposal: extend plug-in color coverage to keys whose owners bind dark values to the stock theme id only

## Why

R8 (`plugin-color-coordination`) coordinates the 68 preference keys that stock Eclipse Dark re-tunes. Some plug-ins keep further color
definitions in `org.eclipse.ui.workbench` whose defaults are unreadable on a dark workbench. Their dark values come either from a
stylesheet the plug-in binds to `org.eclipse.e4.ui.css.theme.e4_dark` alone, or from no stylesheet at all. Under our coordinated dark
themes, which have their own theme ids, these colors keep their light defaults: dark-red or dark-blue text and pastel backgrounds on a
dark view.

The vogellacompany/eclipse-themes project listed such keys in its `plugins/*/css/*_preferences.css`. That project writes foreign
preferences unconditionally, which we reject. We re-verified each candidate against the owning plug-in's color definitions and the
code that reads them (`evidence/key-classification.md`). Only keys with a live reader and an unreadable default qualify.

## What Changes

- Add to the declared plug-in color table, in `org.eclipse.ui.workbench`, gated by the owning bundle being installed:
  - Mylyn Tasks, the three task-list colors no dark sheet re-tunes (Mylyn's own `e4-dark_mylyn_prefstyle.css` sets 7 of 12):
    `foreground.past.due` (ERROR), `foreground.overdue.for.others` (WARNING), `incoming.background` (INFO_TINT). Verified in the
    4.40 and 4.41 packages.
  - Spring Tools, eight keys whose dark values Spring binds to `e4_dark` alone (verified from source, decision 1A): Boot
    Dashboard `TextDecorColor` (SUCCESS) and `AltTextDecorColor` (INFO), `STS4BootMarkerIndicationColor` (SUCCESS_TINT), and the
    embedded terminal `xterm.background` (BACKGROUND), `foreground`, `selection` and `cursor` (FOREGROUND), `cursorAccent` (BACKGROUND,
    always equal to the terminal background).
- Reuse the existing ownership, gating and release rules and the existing palette roles. No new mechanism, role or preference node.
- Broaden the capability's inclusion criterion (decision 2A). Today a key qualifies only when stock Eclipse Dark re-tunes it.
  It would qualify when its default is unreadable on a dark workbench and a live reader exists. The re-tuning may come from stock Dark,
  from the owning plug-in's dark sheet bound to the stock dark id alone, or from no sheet at all.
- Not added, with evidence:
  - Mylyn Context `org.eclipse.mylyn.context.ui.colors.foreground.*`: no reader. Mylyn 4.11 and 4.12 hard-code these colors in
    `ColorMap`.
  - e4 tools `org.eclipse.e4.tools.emf.ui.color.*`: no reader. The XMI editor takes its colors from its own resource pool.
  - PDE target editor `org.eclipse.pde.genericeditor.extension.editor.color.*`: syntax colors. The current spec keeps those with the
    editor integrations, so they are deferred there.
  - Readable defaults (`today.completed`, `completed`, Boot Dashboard `MutedTextDecorColor`) and Mylyn WikiText CSS fragments.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `plugin-color-coordination`: the inclusion criterion of the declared table is broadened as described above, and the scenario set
  covers a plug-in whose dark sheet is bound to the stock dark id alone. Ownership, timing and truthfulness requirements are unchanged.

## Impact

- Code: `core/workbench/PluginColorTable` gains up to 11 entries and new owning-bundle constants. The table's provenance comment and
  table tests change with it. `PluginColorWriter`, `PluginColors` and `PluginColorSource` stay as they are.
- Preference state: up to 11 more instance keys in `org.eclipse.ui.workbench`, written only under a dark coordinated theme, only when
  absent, and only when the owning bundle is installed. They fall under the existing ownership record. Existing users gain the keys on
  their next explicit Apply of a dark coordinated theme. Nothing is written on upgrade by itself.
- Compatibility: the keys are other plug-ins' color-definition ids, not API. Mylyn keys were checked against the 4.40 and 4.41
  packages. Spring Tools keys were checked against `spring-projects/spring-tools` at commit `83e669e` (source only, no runtime check).
  Windows evidence only, as for R8.
- Rollback: unchanged. Light themes, turning coordination off, or another Eclipse theme release still-owned keys exactly as before.
  Downgrading to a build without the new keys leaves their written values behind, as uninstalling does. A light Apply or turning
  coordination off before the downgrade avoids that (design.md, Migration / Rollback).
- Docs: `docs/WORKBENCH_THEMING_COMPATIBILITY.md` key count and list.

## Decision Gate

The maintainer resolved both blocking decisions on 2026-11-01: **1A** (include Spring Tools, source-verified) and **2A** (broaden the
criterion). Implementation is authorized for those options. The spec delta, design and evidence already describe them.

### Product / Maintainer decisions (resolved)

1. **Coordinate plug-ins outside the verified install (Spring Tools)?** Spring Tools is not part of the Eclipse 4.40/4.41 packages R8
   verified. Its keys and readers were checked from source only. The e4 tools candidates need no decision: they have no reader and are
   excluded on evidence.
   - **A. Include, source-verified (recommended).** Add the 8 Spring keys gated on their bundles. Docs mark them source-verified, not
     runtime-verified. Without Spring Tools installed, nothing is written. With it installed, a wrong key would at worst be an inert
     owned value, released by the same paths.
   - **B. Exclude.** Keep the table to the verified install and record the Spring keys as deferred. Spring Tools users keep light
     Boot Dashboard and terminal colors under our dark themes.
   - **C. Include after runtime verification.** Install Spring Tools into 4.40, check live recolor and release, then add the keys.
     This costs a third-party install in the verification set and repeats on each release check.
   - Recommendation: A. Bundle gating and value-exact ownership make a wrong key harmless and reversible, and the readers are simple
     color-registry lookups with theme listeners. C can follow if a defect is reported.
2. **Broaden the inclusion criterion beyond "stock Eclipse Dark re-tunes it"?** The current spec text admits only stock-Dark keys. Mylyn
   `past.due`, `overdue.for.others` and `incoming.background` are re-tuned by no sheet, so stock Dark shows them light too.
   - **A. Broaden (recommended).** A key qualifies when its default is unreadable on a dark workbench (the R8 test) and a live reader
     exists, wherever its dark value would otherwise come from. Under a coordinated dark theme we may then look better than stock Dark
     on these surfaces.
   - **B. Narrow broadening.** A key qualifies only when some dark sheet bound to the stock dark id re-tunes it (Spring keys qualify,
     the three Mylyn keys do not).
   - **C. No change.** Only Spring's keys would also fail the current text, so with 1A this collapses to B's wording anyway.
   - Recommendation: A. The R8 rationale is readability, not parity for its own sake, and the three Mylyn keys show 1.03:1 to 2.2:1
     contrast on a dark view.

### Evidence-dependent

- Owner defaults and readers: resolved in `evidence/key-classification.md` (Mylyn and PDE from the 4.40/4.41 jars and sources; Spring
  from source at a pinned commit).
- Spring Tools runtime behavior (live recolor and the terminal picking up colors at view creation) is not observed. With 1A this stays
  a documented limit. With 1C it becomes a task.

### Engineering (recorded in design.md)

- Role choices: `overdue.for.others` WARNING versus `past.due` ERROR, terminal selection FOREGROUND (Spring draws it at 20% alpha),
  `cursorAccent` BACKGROUND.
- Placement and ordering of the new table entries.

### Deferred

- PDE target editor syntax colors: editor integrations (`editor-integrations` spec, next to the PDE editor adapter), mapped from the
  theme's XML tokens.
- Mylyn Context and e4 tools XMI colors: re-check on each supported release, since a future build could start reading those ids.
- Runtime verification on Linux/macOS: unchanged R8 limit.
