# Proposal: coordinate the colors of other plug-ins with the workbench theme (R8 completion)

## Why

Under the coordinated dark workbench themes from R8a, the colors of hover, content-assist and Javadoc popups and of hyperlinks keep
their light defaults. Popups stay `#ffffe1` or white with black text, links stay `#0066cc`, and a dark workbench shows bright popups
with poor link contrast. Stock Eclipse Dark changes these colors with `IEclipsePreferences` stylesheets. Measured on 4.40 in R8a
(`dynamic-workbench-theming/evidence/runtime/README.md`, "Plug-in color definitions"), that mechanism and the equivalent
`ColorDefinition` rules overwrite a value the user had set and delete it on the next theme change. The maintainer chose option C from
the R8a Decision Gate: the plugin writes a small, declared set of keys itself, and only where the user has no value of their own.

## What Changes

- While workbench coordination is active with a dark coordinated theme, the plugin derives popup and hyperlink colors from the
  selected theme's workbench palette and writes them to a declared set of instance preference keys in other plug-ins' nodes:
  - workbench hover, information and content-assist popup background and foreground;
  - JDT Javadoc hover background and foreground (stored in `org.eclipse.ui.workbench`);
  - JDT content-assist proposal and parameter popup background and foreground;
  - JDT's Javadoc dark-mode element-color flag;
  - workbench hyperlink and active-hyperlink colors.
- Writes are conflict-aware:
  - a key that already has an instance value when the plugin would first write it belongs to the user and is never written;
  - the plugin records each value it wrote;
  - on a later Apply it overwrites a key only while the stored value is still exactly its own;
  - a key whose value was changed by anyone else is released and left alone.
- Restoration removes only keys whose stored value is still the plugin's own: when coordination is turned off, when the family
  changes to light, and when another Eclipse theme becomes active (proposed: on the platform theme-change event, see Decision Gate).
  Every other key keeps its value, or stays absent if it was absent.
- The order is the one R8a established: the workbench switch first, then these writes. Stock Dark's own values are removed by Eclipse
  during the switch, so they are never mistaken for user values.
- Diagnostics report how many keys are owned and how many were skipped because of user values.
- The same table and ownership model then cover the other visible stock-Dark preference keys (platform status and form colors,
  Compare, EGit, search/revision/breakpoint/coverage annotations, JDT/Debug/Ant, Mylyn, Marketplace): 68 keys in all.
- Plug-in widgets that stock Dark styles by class (EGit, Marketplace, JDT, PDE, Debug, Mylyn, Buildship) are styled by palette role in
  the existing static sheets; no preference is written for them.
- Not changed: editor syntax and editor-surface keys (editor integrations), light-family plug-in colors, and any theme-file schema.

## Capabilities

### New Capabilities
- `plugin-color-coordination`: which color keys owned by other plug-ins follow a coordinated workbench theme, when, and how their
  values are derived from the workbench palette.

### Modified Capabilities
- `preference-ownership`: adds conflict-aware ownership for coordinated plug-in color keys. These keys are written only when absent,
  rewritten only while unchanged, released on any external change, and restored only while still the plugin's value.

Depends on `dynamic-workbench-theming` (capability `workbench-theming`) being archived first. This change reads its palette, family
and ownership state and does not alter its requirements.

## Impact

- Code: `eclipse/workbench`, with a new key writer next to `WorkbenchCoordinator` and an extension of the coordinator's
  apply/relinquish/reconcile paths; `core/workbench`, with pure derivation of popup and link colors from `WorkbenchPalette`;
  `PreferenceKeys` for the ownership record; diagnostics and the preference-page text.
- Preference state: up to 68 declared instance keys across the nodes of the owning plug-ins, plus one ownership record in the
  plugin's own store. A key is written only when its owning bundle is installed.
- Compatibility: the keys are other plug-ins' preference names, not internal classes, but still implementation-level contracts. Each one
  is set by the stock 4.40 Dark sheets (`org.eclipse.ui.themes` `e4-dark_preferencestyle.css`, `org.eclipse.jdt.ui`
  `e4-dark_jdt_syntaxhighlighting.css`). They are kept in one table with provenance and checked again on each supported release.
  The external-change release (decision 2A) listens to the theme engine's saved `themeid` preference with the core preferences API.
  It needs no new bundle dependency and lives in `EclipseWorkbenchThemeEngine`, which already reads that node.
  Windows evidence only; GTK/macOS not claimed.
- Rollback: turning coordination off restores every still-owned key. Uninstalling the plugin while a dark coordinated theme is active
  leaves its written values behind, the same exposure stock Dark has; this is documented.

## Decision Gate

The maintainer resolved the three blocking decisions. Implementation is authorized for the options below; the spec deltas and design
already describe them.

### Product / Maintainer decisions (resolved)

1. **Which theme families write the keys: A, dark only.** Foreign plug-in popup, hyperlink and related colors are coordinated only
   while a coordinated dark workbench theme is active. A light coordinated theme writes no foreign key (and releases any it owns);
   its normal workbench CSS theming is unaffected.
2. **When another Eclipse theme is chosen on the Appearance page: A, release immediately.** A bounded listener on the engine's saved
   `themeid` releases every still-owned key and never reapplies. It is registered once, removed on shutdown, and lives next to the
   Eclipse compatibility boundary.
3. **Editor-only mode (coordination off): A, no foreign writes.** Editor theming continues; popup and plug-in colors belong to
   workbench coordination.

### Ownership contract (maintainer-final)

- A key with an instance value before the plugin's first write is user-owned and is never written.
- If absent, the plugin may acquire it, write it and record the exact written value.
- On a later Apply, if the stored value still equals the recorded value the plugin may update or remove it; otherwise it releases
  ownership and leaves the value untouched.
- On relinquish only values that still exactly equal the plugin's recorded value are removed.
- Never delete an explicit user customization, never overwrite an external change, never restore a guessed default, never reset a node.

### Scope of this change (R8 completion)

The maintainer widened the change from the first popup/hyperlink slice to the whole remaining R8 plug-in coordination scope, in
stages that each build on the proven ownership model: slice 1 (the 15 keys below), then the stock-Dark preference keys classified in
`evidence/key-classification.md` as visible parity (EGit, compare, search/annotation, JDT/debug/Ant, Mylyn/WikiText/EclEmma/TM4E/Marketplace) in the same
table and the same ownership model, plus the remaining safe plug-in widget CSS rules. Nothing outside R8 is added.

### Evidence-dependent (investigated)

- *Do these keys take effect live?* Yes for the `org.eclipse.ui.workbench` color keys: a preference write updates the JFace color
  registry immediately (R8a parity log `p3`, `HYPERLINK_COLOR`). JDT content-assist keys are applied live by stock Dark in the same
  way. Runtime verification of every popup is a task.
- *Will the platform mirror our writes back?* `Theme.registryColorChangeEvent` writes the same value into the same key, so the
  stored value stays equal to ours and ownership is not lost. Removing the key makes the registry return to the default, and the
  mirror leaves the key absent. A test and a runtime check confirm this.
- *Do stock Dark values look like user values?* No, if the writes happen after the switch. `EclipsePreferencesHelper` removes stock
  overrides inside `setTheme` (the R8a single-Apply evidence).
- *What does `javadocElementsStyling.darkModeDefaultColors` do?* JDT's `JavaElementLinks` uses it to pick dark defaults for Javadoc
  type-parameter coloring. Without it those colors are tuned for light popups.

### Engineering (decided in `design.md`)

Role mapping from the palette to each key, active-hyperlink derivation, record format in the plugin store, writer placement, and
test seams.

### Deferred

Light-family plug-in colors (decision 1); editor-surface keys (editor integrations), except the editor `hyperlinkColor`, which the
widened scope coordinated in the table; native and
operating-system colors; theme-authored plug-in colors and theme CSS (R9 package schema); GTK/macOS evidence.
