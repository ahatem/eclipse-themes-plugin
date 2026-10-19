# Design

This design is written for the recommended answers to the three blocking decisions in `proposal.md`: dark family only, release on an
external theme change, no writes in editor-only mode. A different answer changes the parts marked *(D1)*, *(D2)* or *(D3)* before apply.

## Context

- R8a coordinates the workbench through four static CSS themes that read eight published palette colors. `WorkbenchCoordinator` owns
  the theme switch and its prior-theme record in the plugin's own store. `ThemeService` applies the workbench first and the editor second.
- The R8a parity pass showed that popup and hyperlink colors live in other plug-ins' preferences. The CSS routes that stock Dark uses
  (`IEclipsePreferences` and `ColorDefinition` rules) overwrite and then delete user values. They are now forbidden by
  `WorkbenchContributionConsistencyTest`.
- Measured platform behavior on 4.40/4.41 (R8a `evidence/runtime/parity`, bytecode of `org.eclipse.ui.internal.themes.Theme`):
  - Writing a plain color key to the `org.eclipse.ui.workbench` instance node updates the JFace color registry live.
  - The registry mirrors every put back into the same key (`registryColorChangeEvent`). An equal value is a no-op. A value equal to the
    definition default makes the mirror call `setToDefault`, which removes the key.
  - Colors and Fonts stores a user change under the plain key unless CSS overrides the definition. Our CSS overrides none of these
    definitions, so a user edit is visible as a plain-key change.
  - `setTheme` removes stock Dark's preference overrides before it returns, so they are gone before our editor and popup writes run.
- The existing editor adapters use overwrite-and-remove with no snapshot. This change deliberately does not reuse that model.

## Goals / Non-Goals

**Goals:**
- Fix dark-theme popups and hyperlinks with the smallest key set that matches what stock Dark changes for those surfaces.
- Conflict-aware ownership: write only absent keys, rewrite only unchanged keys, restore only unchanged keys, release everything else.
- Use the same apply ordering and failure semantics as R8a. Never report success before the writes are committed.

**Non-Goals:**
- Any other foreign key: editor `hyperlinkColor`, EGit/Mylyn/compare decorations, form headings, other stock re-tunes.
- Light-family popups *(D1)*. Editor-only writes *(D3)*.
- A general preference-transaction framework, or migrating the editor adapters to conflict-aware ownership.
- Theme-authored popup colors (the R9 package schema).

## Decisions

### 1. Key table (the whole contract)

All keys are instance scope. Provenance, checked in the 4.40 bundles: the workbench keys are set by
`org.eclipse.ui.themes/css/dark/e4-dark_preferencestyle.css`; the JDT keys and the two Javadoc keys by
`org.eclipse.jdt.ui/css/e4-dark_jdt_syntaxhighlighting.css` (the Javadoc keys under the `org-eclipse-ui-workbench` node selector).
Stock Dark uses `52,57,61` / `238,238,238` for every popup surface here. Live effect was verified on 4.40 and 4.41 for `HYPERLINK_COLOR`;
the remaining keys are a runtime task.

| Node | Key | Kind | Value (dark family) |
|---|---|---|---|
| `org.eclipse.ui.workbench` | `org.eclipse.ui.workbench.HOVER_BACKGROUND` | color | raised |
| `org.eclipse.ui.workbench` | `org.eclipse.ui.workbench.HOVER_FOREGROUND` | color | foreground |
| `org.eclipse.ui.workbench` | `org.eclipse.ui.workbench.INFORMATION_BACKGROUND` | color | raised |
| `org.eclipse.ui.workbench` | `org.eclipse.ui.workbench.INFORMATION_FOREGROUND` | color | foreground |
| `org.eclipse.ui.workbench` | `CONTENT_ASSIST_BACKGROUND_COLOR` | color | raised |
| `org.eclipse.ui.workbench` | `CONTENT_ASSIST_FOREGROUND_COLOR` | color | foreground |
| `org.eclipse.ui.workbench` | `org.eclipse.jdt.ui.Javadoc.backgroundColor` | color | raised |
| `org.eclipse.ui.workbench` | `org.eclipse.jdt.ui.Javadoc.foregroundColor` | color | foreground |
| `org.eclipse.ui.workbench` | `HYPERLINK_COLOR` | color | link |
| `org.eclipse.ui.workbench` | `ACTIVE_HYPERLINK_COLOR` | color | active link |
| `org.eclipse.jdt.ui` | `content_assist_proposals_background` | color | raised |
| `org.eclipse.jdt.ui` | `content_assist_proposals_foreground` | color | foreground |
| `org.eclipse.jdt.ui` | `content_assist_parameters_background` | color | raised |
| `org.eclipse.jdt.ui` | `content_assist_parameters_foreground` | color | foreground |
| `org.eclipse.jdt.ui` | `javadocElementsStyling.darkModeDefaultColors` | boolean | `true` |

- The Javadoc definitions belong to JDT but are stored in the workbench node, because that is where the color registry mirrors them.
  They are written only when JDT is installed, so no definition the registry does not know is ever stored.
- `content_assist_completion_replacement_*` are excluded: they are highlight colors inside proposals, not popup surfaces.
- JDT `sourceHoverBackgroundColor` stays with `JavaEditorThemeAdapter`, which already manages it from the editor theme; this change
  does not take it over.
- `INFORMATION_BACKGROUND` uses raised, the same as hovers. Stock Dark uses a lighter grey there. One background role for every popup
  keeps the derivation small, and the difference cannot be noticed at popup size. *(Engineering choice.)*
- Colors use the Eclipse `r,g,b` format. These are documented preference names, not internal classes, but they are still
  implementation-level contracts of other plug-ins. They live in one table class (`PopupColorKeys`) with this provenance comment and
  are covered by a consistency test.

### 2. Derivation (pure, in `core/workbench`)

`PopupColors.derive(WorkbenchPalette)` returns an ordered key → value map for the dark family and an empty map for light *(D1)*:
- popup background → `raised`;
- popup foreground → `foreground`. R8a already guarantees at least 4.5:1 against background, surface and raised;
- link → `accent`. R8a guarantees the same 4.5:1;
- active link → `accent` mixed 35% toward `foreground` in the perceptual space `PerceptualColor` already implements (a small `mix`
  method is added there), then checked for 4.5:1 against the three backgrounds. If the check fails it falls back to `foreground`. This keeps the hover state visibly different and readable without
  adding a new palette role.

The derivation has no SWT, JFace or activator dependency, as required for core code.

### 3. Ownership record (plugin store)

One key in the plugin's own preference store: `POPUP_COLORS_OWNED`. Its value is a line-per-key list of `node|key|writtenValue`. The
three fields contain no `|` or newline by construction: node and key names come from the fixed table, values are `r,g,b` or `true`.
An unparsable record is treated as empty: nothing is owned, nothing is removed, and every present value counts as the user's.

A separate key per entry was rejected because it makes the record non-atomic across one flush. A snapshot of the prior value was
rejected because only absent keys are ever taken, so the prior state is always "absent".

### 4. Sync algorithm (`PopupColorWriter`, in `eclipse/workbench`)

`sync(Map<key, value> desired)`, with an empty map meaning "release all":

```
for each key in the table, if its node's bundle is installed:
  current = instance value or ABSENT
  if key is owned:
    if current != recorded:        release (drop record)        // user or other plug-in changed it
    else if desired has key:       put desired, record desired  // still ours: update
    else:                          remove key, drop record      // restore only while unchanged
  else if desired has key and current == ABSENT:
    put desired, record desired                                 // take an absent key
  else if desired has key:
    count as "held by user"                                     // never write a present value
flush each touched foreign node; then persist the record (plugin store)
```

- **Failure ordering.** Foreign nodes are flushed first. If a node's flush fails, the record entries for that node are not added or
  updated (removals for that node stay recorded), and the outcome reports the failure. The plugin store is then saved. If saving the
  record fails after the foreign flushes succeeded, the written values stay in place without an owner. A later sync then sees them as
  user values: they are never removed automatically, and the failure is reported. This is conservative: no user value can be lost,
  only an orphan can remain, and diagnostics name it as held by a user value.
- **Mirror interaction.** Putting a value fires the registry mirror, which writes the equal value back as a no-op. If a derived value
  equals the definition default, the mirror removes the key again. The next sync sees ABSENT ≠ recorded, releases the entry, and takes
  the key again only if it is still absent. The effective color is the same either way. A focused test covers this.
- **Threading.** It runs on the UI thread, like the existing preference writes. The registry listeners that the puts trigger expect it.
- **No node clearing, no defaults written.** Only `put`, `remove` and `flush` on single named keys.

### 5. Where it runs

- **Apply.** `WorkbenchCoordinator.apply` calls `sync(PopupColors.derive(palette))` after the switch postcondition holds, including the
  "same family, equal palette" no-op path, so an upgrade or a lost key catches up on the next Apply. On a postcondition failure no sync
  runs. Relinquishing (coordination disabled) calls `sync(empty)` *(D3)*.
- **Light target.** `derive` returns empty, so applying a light theme releases the dark values *(D1)*.
- **External theme change** *(D2)*. One `IPreferenceChangeListener` on the engine's instance node (`org.eclipse.e4.ui.css.swt.theme`,
  key `themeid`) is registered at startup and runs `sync(empty)` when the active theme is not one of ours. It never calls the derivation,
  so it cannot reassert anything, which keeps the R8a rule that no automatic path re-coordinates. It uses only the core preferences API.
  It needs no new bundle dependency, unlike `IEventBroker` or `THEME_CHANGED`. The node is already read by `EclipseWorkbenchThemeEngine`,
  which owns the subscription. The Appearance page saves `themeid` on Apply, so the listener sees every external choice that persists.
  A non-persisted switch is caught at the next startup reconcile. Our own `select` also changes `themeid`; the listener then sees our
  theme active and does nothing.
- **Startup.** `WorkbenchStartup` reconcile calls `sync(empty)` when our theme is not active, and does nothing to popup keys otherwise.
  Keys persist, so no rewrite is needed.

### 6. Outcome and diagnostics

`WorkbenchOutcome` gains a popup summary: owned, held by user, failed, not installed. The page reports "N popup colors kept your own
values" when N > 0. Any failure makes the workbench outcome DEGRADED, never success. `WorkbenchDiagnostics` lists each key with its
state. Local only.

## Risks / Trade-offs

- **Foreign implementation keys may change between Eclipse releases.** Mitigation: one table with provenance; a missing definition
  simply has no effect; the runtime check is repeated on each supported release (4.40/4.41 now).
- **A stale value from an older tool or uninstall looks like a user value.** Intended conservatism: such a key is never coordinated,
  and the page says so.
- **Uninstalling the plugin while a dark theme is active leaves owned values behind.** The plugin has no uninstall hook. This is
  documented with a recovery note: disable coordination before uninstalling, or reset the keys in Colors and Fonts. Stock Dark has the
  same exposure.
- **Stock Dark chosen externally.** Whether stock writes its values before or after our listener removes ours, each key ends with
  stock's value and is not recorded as ours. Only a derived value exactly equal to stock's (for example `52,57,61`) could let us remove a
  stock value, if stock writes before the listener runs. The runtime task records the actual order; the consequence would be one popup
  color at its default until the next theme change, never a lost user value.
- **A user change made between our write and the record save is never observed.** Accepted: the window is one flush long on the UI
  thread.
- **The listener reverses part of the R8a "no theme-change listener" choice.** It is bounded to releasing keys and is covered by a test
  that asserts it never writes a value *(D2)*.
