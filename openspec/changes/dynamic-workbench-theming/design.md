# Design

## Flow

```text
selected Theme (editor source of truth)
   -> WorkbenchPaletteResolver      pure: 7 colors, deterministic, contrast-safe, family fallback
   -> WorkbenchCoordinator          ownership, capability, ordering, postconditions, revert
        -> PaletteSink              publish to the Colors-and-Fonts ColorRegistry (public API)
        -> WorkbenchThemeEngine     the only code that touches IThemeEngine (getThemes/getActiveTheme/setTheme(ITheme,true))
   -> Eclipse re-applies static CSS which reads '#id' colors
```

The `ColorRegistry` is a delivery mechanism. The selected theme plus the resolver is the source of truth, so every startup that
finds coordination active re-derives and republishes; nothing depends on `put` persistence.

## Gate answers (evidence: `evidence/gate-q1-q2-runtime.txt`)

**Q1, private keys without a visible color definition.** Technically yes: CSS `'#id'` resolves any key in the current
Colors-and-Fonts registry, and an undefined key is never written to preferences. Rejected as a production design: before the first
publish (every startup, or whenever the plugin cannot start) an unresolved reference paints **black on black** across the shell,
tabs, trees and tables. Chosen: declarative `colorDefinition`s with `isEditable="false"` and the family fallback as `value`.
Measured consequences: hidden from Colors and Fonts (the page's content provider skips non-editable definitions); Eclipse loads the
definition default and ignores any stored value at startup, and the entry written by `put` during a session is gone after restart.
The palette is therefore ephemeral, startup always begins at the family fallback, and the plugin republishes. The cost is that a
coordinated theme whose palette differs from the fallback restyles once shortly after startup. R8b may improve first paint.

**Q2, hiding the refresh twin.** No supported mechanism. The Appearance combo is `IThemeEngine.getThemes()` unfiltered, and the
extension schema has only `os`, `ws`, `os_version` (these remove an entry entirely, so an unselectable twin is useless). By the
maintainer's priority (no foreign-theme side effects, then reliable refresh, then a clean list) the twins are visible and plainly
labelled: "Eclipse Themes Dark", "Eclipse Themes Dark (alternate)", "Eclipse Themes Light", "Eclipse Themes Light (alternate)".
The user-facing consequence (four extra Appearance entries with the same look family) is documented in the compatibility doc.
Hopping through Eclipse's own Light/Dark theme was rejected: it runs stock CSS with its side effects on foreign preferences and
restyles twice.

## Symbols and static CSS

`WorkbenchColor` (seven constants) x `WorkbenchFamily` (DARK, LIGHT) is the single mapping to ids of the form
`com.github.eclipsethemes.workbench.<family>.<role>` (roles: background, foreground, surface, surfaceRaised, border,
mutedForeground, selection). The CSS form is the same id with dots as dashes. A consistency test reads `plugin.xml` and both CSS
files and fails if the Java mapping, the colorDefinitions (ids, fallback values, `isEditable="false"`) or the CSS references drift.
Each family has its own id set so an unpublished default is that family's fallback, never a color from the other family.

Theme ids: `com.github.eclipsethemes.workbench.dark`, `...dark.alt`, `...light`, `...light.alt`. Dark ids contain `dark` and light
ids do not, because Eclipse derives `Display.setDarkThemePreferred` from `id.contains("dark")` (4.40+).

CSS coverage is bounded to surfaces the R7/spike runs measured: shell, containers, part stacks (selected and unselected tabs), trim
bars, tool bars, status line, trees, tables. Editor/StyledText content is not styled by workbench CSS. Native menus, dialogs, title
bars, Browser content and arbitrary icons remain system-controlled.

## Palette resolution (fixed, no adaptive logic)

Family follows `ThemeType`. Source colors are `BACKGROUND`, `FOREGROUND` and, if explicit, `SELECTION_BACKGROUND`. No syntax token
participates and no accent is derived.

- Fallback palette for the whole family when background or foreground is missing or `contrast(foreground, background) < 3.0`.
- `surface` = blend(background toward foreground, 5%); `surfaceRaised` = 10%; `border` = 22%. Always toward the foreground, so dark
  themes lighten and light themes darken.
- `mutedForeground` = blend(foreground toward background, 35%); if its contrast on `surfaceRaised` is below 3.0 it becomes `foreground`.
- `selection` = the explicit selection background when `contrast(foreground, selection) >= 3.0` and it differs from the
  background, otherwise blend(background toward foreground, 25%).
- Contrast is the WCAG relative-luminance ratio. Blends round per channel. No platform constants.

## Capability and compatibility boundary

`WorkbenchThemeEngine` (interface) has one implementation, `EclipseWorkbenchThemeEngine`, the only importer of `IThemeEngine`.
It obtains the service with `PlatformUI.getWorkbench().getService(IThemeEngine.class)`, selects by exact `ITheme` from
`getThemes()` (never the `String` overload), catches `LinkageError`, uses no reflection and no registration, and reads
`Display.getHighContrast()` on the UI thread. The classifier is pure over `(WorkbenchEnvironment, EngineProbe)`:

1. `LINKAGE_ERROR`, then `NO_ENGINE`, then `HIGH_CONTRAST`, then `THEME_NOT_FOUND` (any of the four own themes missing); otherwise
2. `VERIFIED` when `(os, ws, theme-bundle major.minor.micro)` is in the measured table (`win32/win32/0.15.100`,
   `win32/win32/0.15.200`), else `EXPERIMENTAL`.
3. `POSTCONDITION_FAILED` is produced after an apply and kept for the session (in memory) so a failing combination is not retried.

The table is confidence metadata, never an execution allow-list. A future `0.15.300` is EXPERIMENTAL until measured. The
`UNSUPPORTED_ENVIRONMENT` code from the brief was dropped: an absent workbench or service is `NO_ENGINE`, and nothing else needs it.
Diagnostics record the full theme-bundle version, Eclipse Platform version, OS, WS and Java version.

The theme bundle is an **optional** `Require-Bundle`, the binding variant measured in R7 (findings 36.9). If the bundle is absent
or the class cannot link, the boundary reports it and the editor plugin is unaffected.

## Refresh slots (kept local)

Only a real theme-ID change makes Eclipse re-read the palette (measured: same-ID `setTheme` and `applyStyles` do not). A family
therefore has slots A (`<family>`) and B (`<family>.alt`). `WorkbenchSlots.target(family, activeId)` returns the family's other slot
when the active theme is one of that family's slots, else slot A. That guarantees a real ID transition for same-family changes and
the right family for light/dark changes. Nothing outside `WorkbenchSlots` and the boundary knows about A/B. When the palette is
unchanged and the matching family is already active, Apply performs no switch.

## Ownership, ordering and restoration

Plugin preference node (not foreign): `WORKBENCH_COORDINATION_ENABLED` (user intent), `WORKBENCH_EXPERIMENTAL_ACK` (the combination
string the user opted in on), and an ownership record written **only after a verified switch**: `WORKBENCH_OWNED_THEME_ID`,
`WORKBENCH_PRIOR_ACTIVE_ID`, `WORKBENCH_PRIOR_SAVED_ID`, `WORKBENCH_PRIOR_SAVED_ABSENT`.

Apply (UI thread): classify, resolve the palette, capture prior state if we do not already own, publish (remembering previous
values, compensating on partial failure), `select(target)`, check the postcondition (active id equals target and the registry holds
the palette), persist ownership. Any failure after a selection attempt runs the safe-revert rule. The editor outcome is computed and
reported independently, so editor theming stays valid when the workbench fails.

Safe revert: restore previously published values and `select(prior)` only when the prior theme is known, the active theme is still
the one we attempted, and nothing else changed it. Otherwise yield: do nothing, report the conflict, offer the Appearance page. The
session flag marks the combination unavailable, and an unconfirmed experimental opt-in is cleared.

Relinquish (disable, stop management): if the active theme is still one of ours, select the prior active theme (known and
available) and verify it. If the prior saved `themeid` was absent and the instance key still equals what the restore wrote, remove
only that key, so Eclipse resolves its own default as before. Reset our palette ids to the fallback and clear the ownership record.
If the active theme is not ours (external change), the external choice wins: nothing is selected and ownership is cleared. If the
prior theme is unknown or gone, nothing is forced: the result is YIELDED with the Appearance route. Editor selection is untouched.

External Appearance change: there is no listener. At startup the coordinator only republishes while the active theme is ours; a
foreign active theme is a yield. The page reports the conflict when it opens. An explicit Apply with coordination enabled takes
ownership again and re-captures the prior state because we no longer own.

## Application result (minimum R3 prerequisite)

`ThemeService.applyTheme` returns `ThemeApplyOutcome(EditorOutcome, WorkbenchOutcome)`. The editor outcome lists failing adapters
(today they are only logged). The workbench outcome has a status (`NOT_REQUESTED`, `APPLIED`, `RELINQUISHED`, `YIELDED`,
`UNAVAILABLE`, `FAILED`), an optional reason code and a message. `fullySucceeded` requires both parts. Selection intent remains
intent (contract unchanged); ownership is persisted only after the postcondition. `ThemeOperationOutcome` stays the import and
update result. No generic transaction type is added.

## UI

The existing page gets one compact Workbench group driven by a pure `WorkbenchSectionModel` (testable: VERIFIED, EXPERIMENTAL with
explicit opt-in, UNAVAILABLE with reason, conflict, coverage text). Changes are written at Apply/OK only, so Cancel keeps the stored
intent. Highlighting a theme never changes the workbench. A diagnostics button copies the text block. The manual route is
`PreferencesUtil.createPreferenceDialogOn(shell, "org.eclipse.ui.preferencePages.Views", null, null)`.

## Startup

`org.eclipse.ui.startup` runs `WorkbenchStartup.earlyStartup`, which hops to the display thread and calls `reconcile` once. It does
nothing when coordination is off, unavailable, not opted in, or the active theme is not ours. It republishes and switches only when
the registry does not already hold the derived palette.

## Colors and Fonts interaction

The 14 definitions are `isEditable="false"`, so the page does not list them and stored overrides are ignored. No manual per-color
override exists in R8a. While coordination is enabled, the selected theme is the only source of these colors. The fallback palette
values live in Java (`WorkbenchFamily`) and in `plugin.xml`; a test keeps them equal.

Measured side effect: `ColorRegistry.put` writes the entry into the instance `org.eclipse.ui.workbench` preferences, and Eclipse removes it again when the
plugin resets the ids to the fallback. A preference recorder that replays recorded keys (observed with Oomph's preference task in an EPP install, which
replayed another run's palette a few hundred milliseconds after an Apply) can therefore overwrite a published palette until the next Apply or
restart. R8a does not add a listener or a suppression mechanism for this; it is documented in the compatibility doc.

## Deliberately not built

Runtime registration, generated CSS, imported CSS, the R9 schema, fonts, icons, user overrides, a transaction, journal or
change-plan framework, telemetry, a theme-change listener, and any reflection or Appearance internals.
