# Design

The three blocking decisions in `proposal.md` are resolved as recommended: dark family only (*D1*), release on an external theme change
(*D2*), no writes in editor-only mode (*D3*).

## Context

- R8a coordinates the workbench through four static CSS themes that read eight published palette colors. `WorkbenchCoordinator` owns
  the theme switch and its prior-theme record in the plugin's own store. `ThemeService` applies the workbench first and the editor second.
- The R8a parity pass showed that these colors live in other plug-ins' preferences. The CSS routes that stock Dark uses
  (`IEclipsePreferences` and `ColorDefinition` rules) overwrite and then delete user values. They are forbidden by
  `WorkbenchContributionConsistencyTest`.
- Measured platform behavior on 4.40/4.41 (R8a `evidence/runtime/parity`, bytecode of `org.eclipse.ui.internal.themes.Theme`):
  - Writing a plain color key to the `org.eclipse.ui.workbench` instance node updates the JFace color registry live.
  - The registry mirrors every put back into the same key (`registryColorChangeEvent`). An equal value is a no-op. A value equal to the
    definition default makes the mirror call `setToDefault`, which removes the key.
  - Colors and Fonts stores a user change under the plain key unless CSS overrides the definition. Our CSS overrides none of these
    definitions, so a user edit is visible as a plain-key change.
  - `setTheme` removes the previous theme's preference overrides before it returns, so they are gone before our writes run.
- The existing editor adapters use overwrite-and-remove with no snapshot. This change deliberately does not reuse that model.

## Goals / Non-Goals

**Goals:**
- Match what stock Eclipse Dark does for visible plug-in surfaces (R8 closure), by palette role and without theme-name special cases.
- Conflict-aware ownership: write only absent keys, rewrite only unchanged keys, restore only unchanged keys, release everything else.
- Use the same apply ordering and failure semantics as R8a. Never report success before the writes are committed.

**Non-Goals:**
- Light-family plug-in colors *(D1)*. Editor-only writes *(D3)*.
- Syntax, semantic-highlighting and editor-surface keys: they stay with the editor adapters.
- A general preference-transaction framework, or migrating the editor adapters to conflict-aware ownership.
- Theme-authored plug-in colors or theme-supplied CSS (R9).

## Decisions

### 1. Key table (the whole contract)

`PluginColorTable` is the only list of foreign keys this plugin may write: 68 keys (on 4.40 the four inline compare
highlights do not exist and are inert if written). Each entry is `node, key, required bundle, source`. The required bundle is the
installed-bundle guard: a key is written only when its owning bundle is installed. `PluginColorTableOwnershipTest` proves the table is
disjoint from the keys the editor adapters manage.

Classification of all stock-Dark preference keys (197 on 4.40, 201 on 4.41) is in `evidence/key-classification.md`: 64/68
`REQUIRED_VISIBLE_PARITY` (coordinated), 111 `ALREADY_COVERED` by the editor adapters, 1 `NATIVE_NOT_OURS`, 21 `OUTSIDE_R8`,
0 `OBSOLETE_UNUSED`. A key is `REQUIRED_VISIBLE_PARITY` only when its Eclipse default is unreadable or clashing on a dark workbench
(read from the owning plug-in's color definition, annotation specification or preference initializer) and the surface appears in
normal use. The groups:

1. Popups and hyperlinks (15 keys, the first slice): hover, information, content-assist, Javadoc, JDT content-assist, the JDT Javadoc
   dark-mode flag, `HYPERLINK_COLOR`, `ACTIVE_HYPERLINK_COLOR`.
2. Platform status and forms (error text, form headings), Compare and EGit (incoming/outgoing/conflicting/resolved/edition, inline
   compare highlights, diff, commit comment, ignored, uncommitted).
3. Search match, revision, editor hyperlink, linked edit, breakpoint, coverage and sticky-lines separator annotation colors.
4. JDT label and write-access colors, Debug and Ant console colors.
5. Mylyn task list colors and the Marketplace native-borders flag.

Provenance is the stock preference stylesheets (`org.eclipse.ui.themes`, JDT, EGit, Mylyn, Debug, Ant, EclEmma, Marketplace, Compare)
of the 4.40/4.41 packages. These are other plug-ins' preference names, not API. They live in one class with this provenance comment
and are re-checked on each supported release.

### 2. Derivation (pure, in `core/workbench`)

`PluginColors.derive(WorkbenchPalette)` returns an ordered key id → value map for the dark family and an empty map for light *(D1)*.
Each table entry names a `PluginColorSource`:
- **Palette roles**: background, raised, border, foreground, muted, accent. R8a already guarantees the text roles 4.5:1 against
  background, surface and raised.
- **Active accent**: the accent mixed 35% toward the foreground in `PerceptualColor`, checked for 4.5:1 against the three backgrounds;
  the foreground when the check fails.
- **Status colors** (error, warning, success, information): fixed OKLCH hues at fixed lightness and chroma, then
  `WorkbenchPaletteResolver.readable` moves lightness only until 4.5:1 holds on the three backgrounds. Themes do not author them, the
  same way the accent is not authored.
- **Status tints** (background highlights for diffs, coverage, search, revision): the palette background pulled toward a status hue.
  The search gives up lightness first, then chroma, and accepts the strongest candidate that differs from the background and keeps the
  palette foreground at 4.5:1 on top of it. If none qualifies, the background. A soft variant at half strength exists for the weaker
  of two related highlights (inline compare, revision age).
- **Flags**: `DARK_FLAG` (`true`) and `OFF_FLAG` (`false`) for boolean keys.

There is no theme-name special case and no generated CSS. The derivation has no SWT, JFace or activator dependency. Measured on two
dark themes (`#282c34` and Solarized Dark `#002b36`) every tint is distinct from the background and every status color is readable.

### 3. Ownership record (plugin store)

One key in the plugin's own preference store: `PLUGIN_COLORS_OWNED`. Its value is a line-per-key list of `node|key|writtenValue`.
Fields contain no `|` or newline by construction. An unparsable record is treated as empty: nothing is owned, nothing is removed, and
every present value counts as the user's.

A separate key per entry was rejected because it makes the record non-atomic across one flush. A snapshot of the prior value was
rejected because only absent keys are ever taken, so the prior state is always "absent".

### 4. Sync algorithm (`PluginColorWriter`, in `eclipse/workbench`)

`sync(Map<key id, value> desired)`, with an empty map meaning "release all":

```
for each key in the table, if its bundle is installed:
  current = instance value or ABSENT
  if key is owned:
    if current != recorded:        release (drop record)        // user or other plug-in changed it
    else if desired has key:       put desired, record desired  // still ours: update
    else:                          remove key, drop record      // restore only while unchanged
  else if desired has key and current == ABSENT:
    put desired, record desired                                 // take an absent key
  else if desired has key:
    count as "kept as the user's"                               // never write a present value
flush each touched foreign node; then persist the record (plugin store)
```

- **Failure ordering.** Foreign nodes are flushed first. If a node's flush fails, the record entries for that node are not added or
  updated (removals for that node stay recorded), and the outcome reports the failure. The plugin store is then saved. If saving the
  record fails after the foreign flushes succeeded, the written values stay in place without an owner. A later sync then sees them as
  user values: they are never removed automatically, and the failure is reported. This is conservative: no user value can be lost,
  only an orphan can remain.
- **Mirror interaction.** Putting a value fires the registry mirror, which writes the equal value back as a no-op. The writer claims
  ownership only when the value read back equals the one written, so a value the mirror removed (equal to the definition default) is
  not recorded. A focused test covers this.
- **Threading.** It runs on the UI thread, like the existing preference writes. The registry listeners that the puts trigger expect it.
- **No node clearing, no defaults written.** Only `put`, `remove` and `flush` on single named keys.
- Foreign preference access sits behind `ForeignPreferences` (`InstancePreferences` for the real instance scope), so the writer is
  tested with in-memory nodes.

### 5. Where it runs

- **Apply.** `WorkbenchCoordinator.apply` resolves the palette, switches the workbench theme, verifies the postcondition, then calls
  `sync(derive(palette))`, including the "same family, equal palette" path, so an upgrade or a lost key catches up on the next Apply.
  On a postcondition failure no sync runs. The editor preferences are written after that. Relinquishing (coordination disabled) calls
  `sync(empty)` *(D3)*. Editor-only applies call `sync(empty)` so leftovers from an earlier run are released and nothing is written.
- **Light target.** `derive` returns empty, so applying a light theme releases the dark values *(D1)*.
- **External theme change** *(D2)*. One `IPreferenceChangeListener` on the engine's instance node (`org.eclipse.e4.ui.css.swt.theme`,
  key `themeid`) is registered by `WorkbenchStartup` and runs `sync(empty)` when the active theme is not one of ours, and is removed
  on shutdown. It never calls the derivation, so it cannot reassert anything, which keeps the R8a rule that no automatic path
  re-coordinates. It uses only the core preferences API. The Appearance page saves `themeid` on Apply, so the listener sees every
  external choice that persists. A non-persisted switch is caught at the next startup reconcile. Our own `select` also changes
  `themeid`; the listener then sees our theme active and does nothing.
- **Startup.** `WorkbenchStartup` reconcile calls `sync(empty)` when our theme is not active, and does nothing to these keys otherwise.
  Keys persist, so no rewrite is needed.

### 6. Outcome and diagnostics

`WorkbenchOutcome` carries the sync summary (`PluginColorSummary`: owned, kept as the user's, released, failed). A failed sync makes
the aggregate status DEGRADED, never success. The preference page shows aggregate counts only ("N plug-in colors kept your own
values"). `WorkbenchDiagnostics` (the copyable text) lists the key ids per state. Local only, no telemetry.

### 7. Plug-in widget CSS (same change)

Plug-in widgets that stock Dark styles by class name (EGit, Marketplace, JDT, PDE, Debug, Mylyn, Buildship, WTP) are styled in the
existing static sheets by palette role, in the same files and with the same role references; nothing here writes a preference. The CSS
type selector matches the exact widget class name only (no superclass match), so each class the stock sheets name is named here too.
Selectors that would also match an editor's `StyledText` are left out. The family files stay identical apart from the family name,
as before, and a test enforces it. The only literals are the splash rules copied from stock (test: `SPLASH_LITERALS`).

## Risks / Trade-offs

- **Foreign implementation keys may change between Eclipse releases.** Mitigation: one table with provenance; a missing definition
  simply has no effect; the runtime check is repeated on each supported release (4.40/4.41).
- **A stale value from an older tool or uninstall looks like a user value.** Intended conservatism: such a key is never coordinated,
  and the page says so.
- **Uninstalling the plugin while a dark theme is active leaves owned values behind.** The plugin has no uninstall hook. This is
  documented with a recovery note: disable coordination before uninstalling, or reset the keys in Colors and Fonts. Stock Dark has the
  same exposure.
- **A user change made between our write and the record save is never observed.** Accepted: the window is one flush long on the UI
  thread.
- **The listener reverses part of the R8a "no theme-change listener" choice.** It is bounded to releasing keys and is covered by a test
  that asserts it never writes a value *(D2)*.
- **Inline compare keys on 4.40.** Written when Compare is installed although 4.40 does not read them; harmless and removed on release.
- **Stock Dark chosen externally.** See the runtime evidence for the order of stock's writes and the listener.
