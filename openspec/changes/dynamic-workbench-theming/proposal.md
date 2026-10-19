# Proposal: dynamic coordinated workbench theming (R8a)

## Why

R7 closed with P4 GO and Option D GO (`archive/2026-10-19-workbench-theme-engine-spike`, findings §36–§39). A selected Eclipse
Color Theme can drive workbench colors without runtime theme registration, runtime stylesheet registration or generated CSS: one
static declarative CSS family reads symbolic `'#id'` colors, the plugin publishes a small derived palette to the Colors-and-Fonts
`ColorRegistry`, and a real workbench theme-ID change makes Eclipse re-read it. Today the product only coordinates editor
preferences, so "Atom One Dark" gives an Atom editor inside a stock Eclipse chrome.

## What Changes

- New opt-in **workbench coordination**: when enabled, Apply/OK also converts the selected theme into an eight-role semantic palette from the selected
  theme, publishes it, and switches Eclipse to a plugin-contributed static CSS theme (dark or light family).
- Two static CSS families (dark, light). Each family contributes a primary and an alternate "twin"
  `org.eclipse.e4.ui.css.swt.theme` entry (only a real theme-ID change refreshes the colors) and eight non-editable
  `colorDefinition` entries whose defaults are the safe family fallback palette.
- A deterministic semantic palette conversion (suitable authored non-syntax candidates, then OKLCH derivation, then readability correction, then family fallback; no syntax token ever maps to chrome).
- One isolated compatibility boundary around `org.eclipse.e4.ui.css.swt.theme.IThemeEngine` (friend-exported, accepted under P4)
  and a capability model: VERIFIED / EXPERIMENTAL / UNAVAILABLE with reason codes.
- Ownership and truthful results: prior workbench theme captured at first takeover, restored on disable only when still ours,
  external Appearance choices always win, apply returns an aggregate editor + workbench outcome.
- A compact "Workbench" section on the existing preference page, a copyable diagnostics text and a public route to the Eclipse
  Appearance page.

Not changed: legacy XML schema, editor adapters and their preference mappings, ROADMAP sequencing beyond marking R8a at the end.

## Capabilities

- New: `workbench-theming`.
- Modified: `theme-application` (aggregate application outcome), `preference-ui` (workbench section).

## Compatibility surface and risk

- `MANIFEST.MF` gains an **optional** `Require-Bundle: org.eclipse.e4.ui.css.swt.theme`; `plugin.xml` gains two extensions
  (`org.eclipse.e4.ui.css.swt.theme`, `org.eclipse.ui.themes`) and one `org.eclipse.ui.startup`. Without the theme bundle the
  editor features keep working and the workbench section reports UNAVAILABLE.
- `IThemeEngine` is `x-friends`: formally not API. Accepted debt (P4): used only for `getThemes()`, `getActiveTheme()`,
  `setTheme(ITheme, true)`, in one class. The `'#id'` resolver is implementation-specific.
- VERIFIED confidence is a measured table keyed by OS + WS + theme-bundle `major.minor.micro`: win32/win32 with 0.15.100
  (Eclipse 4.40) and 0.15.200 (4.41). Everything else that passes the runtime probe is EXPERIMENTAL and OFF until the user opts in.
- State risk: the plugin owns its own palette keys (ephemeral; `isEditable=false` colors are hidden from Colors and Fonts and
  never restored from stored values) and a small ownership record in its own preference node. Foreign preference nodes are never
  cleared. The only foreign key touched is the theme bundle's instance `themeid`, and only to restore its original absence.
- Rollback: disabling coordination restores the previous workbench theme when ours is still active, otherwise leaves the user's
  choice. Removing the plugin leaves Eclipse's own fallback for an unavailable saved theme.

## Decision Gate

All Product / Maintainer decisions were resolved by the maintainer before this change (Option D; VERIFIED vs EXPERIMENTAL policy;
experimental failure yield rule; legacy XML stays editor-only until opt-in; no 4.33 evidence in the UI). No blocking decision remains.

### Evidence-dependent (investigated, see `design.md` and `evidence/`)

- Can CSS `'#id'` resolve a registry key with no `colorDefinition`? Yes, but an unresolved reference paints **black**, and nothing
  persists, so a startup before republish is unreadable. Decision: declarative `colorDefinition`s.
- Can the refresh twin be hidden from Appearance? No supported mechanism. Decision: visible, plainly labelled entries.
- Production behavior on Windows 4.40/4.41: measured during apply; results recorded at the end of `tasks.md`.

### Engineering (recorded in `design.md`)

Package layout, class split, the A/B slot rule, blend constants, the contrast threshold, and the in-memory session flag.

### Deferred

R8b generated/imported workbench CSS, user palette overrides and a Colors-and-Fonts policy; R9 package schema with explicit
`<workbench>` values; fonts and icons; real high-contrast, GTK and macOS certification (pre-release items); broad R3.
