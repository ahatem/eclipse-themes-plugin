# Proposal: coordinate popup and hyperlink colors with the workbench theme (R8a follow-up, option C)

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
- Not changed: editor syntax and editor-surface keys (editor integrations), the editor `hyperlinkColor`, EGit/Mylyn decoration colors,
  light-family popups, and any theme-file schema.

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
- Preference state: up to 15 instance keys across `org.eclipse.ui.workbench` and `org.eclipse.jdt.ui`, plus one ownership record
  in the plugin's own store. JDT keys are written only when JDT is installed.
- Compatibility: the keys are other plug-ins' preference names, not internal classes, but still implementation-level contracts. Each one
  is set by the stock 4.40 Dark sheets (`org.eclipse.ui.themes` `e4-dark_preferencestyle.css`, `org.eclipse.jdt.ui`
  `e4-dark_jdt_syntaxhighlighting.css`). They are kept in one table with provenance and checked again on each supported release.
  The external-change release (decision 2A) listens to the theme engine's saved `themeid` preference with the core preferences API.
  It needs no new bundle dependency and lives in `EclipseWorkbenchThemeEngine`, which already reads that node.
  Windows evidence only; GTK/macOS not claimed.
- Rollback: turning coordination off restores every still-owned key. Uninstalling the plugin while a dark coordinated theme is active
  leaves its written values behind, the same exposure stock Dark has; this is documented.

## Decision Gate

The maintainer chose the mechanism (option C: plugin-owned, conflict-aware writes, starting with popups and hyperlinks). The
decisions below are still open. **Implementation is not authorized until the maintainer resolves decisions 1 to 3.** The spec deltas
and design are written for the recommended options; a different answer changes them before apply.

### Product / Maintainer decisions (blocking)

1. **Which theme families write the keys.**
   - *A. Dark only (recommended).* It fixes the demonstrated problem, matches stock (stock Light leaves the defaults readable), and
     keeps foreign writes to the minimum. Light themes keep the platform's pale-yellow and white popups.
   - *B. Dark and light.* Light themes (for example Solarized Light) also get popups in their own colors. Every Apply writes foreign
     keys, and ownership conflicts become more likely.
2. **When another Eclipse theme is chosen on the Appearance page.**
   - *A. Release on the platform theme-change event (recommended).* As soon as a non-coordinated theme becomes active, the keys still
     holding the plugin's values are removed. The plugin never re-applies anything, so the external choice still wins. This adds a
     bounded listener, which R8a deliberately avoided; the visible cost of not having one is described in option B.
   - *B. No listener.* The keys are released at the next Apply or restart. Until then a stock Light workbench shows dark popups and
     light-blue links.
3. **Editor-only mode (coordination off).**
   - *A. No writes (recommended).* Popup colors belong to workbench coordination, and editor-only users keep today's behavior.
   - *B. Also write in editor-only mode, from the editor theme's colors.* Popups then match a dark editor under a light workbench,
     but popups are workbench chrome and this widens editor-only behavior.

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

Light-family popups (if decision 1 is A); the editor `hyperlinkColor` (editor integrations); EGit/Mylyn/compare decoration colors and
the remaining definitions stock Dark re-tunes; theme-authored popup colors (R9 package schema); GTK/macOS evidence.
