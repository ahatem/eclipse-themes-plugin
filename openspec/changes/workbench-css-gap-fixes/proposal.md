# Proposal

## Why

A review of vogellacompany/eclipse-themes (`docs/platform-styling-bugs.md`) turned up two small gaps in our coordinated workbench themes:

- From Eclipse 2026-12 (eclipse.platform.ui#4354) the `<theme>` element takes an explicit `isDarkTheme` attribute. Our four themes do not declare it, so they keep relying on the `id.contains("dark")` heuristic.
- The JFace progress bar (`ProgressMonitorPart > ProgressIndicator > ProgressBar`) and separator labels fall outside our `Composite > *` rules, because the CSS engine matches selectors by exact class name. That leaves a light bar on dark wizard and dialog surfaces. Our coverage doc lists the progress bar as SYSTEM/NATIVE, but vogella reports that naming the leaf widget reaches it. We have not measured this ourselves.

## What Changes

- Declare `isDarkTheme="true"` on the two dark-family `<theme>` elements and `isDarkTheme="false"` on the two light-family ones. The ids keep containing `dark` / not containing it, so Eclipse releases before 2026-12 behave as today.
- Add a `ProgressBar` leaf selector to both family stylesheets, using only the existing palette role ids. This was measured on Windows with Eclipse 4.40 and 4.41: it fixes the light wizard/dialog bar on the dark family and is inert on the light family, where Windows draws the bar natively. The candidate separator and `ProgressIndicator > *` selectors had no additional effect and are not shipped (`evidence/progress-separator-measurement.md`).
- Update the coverage table in `docs/WORKBENCH_THEMING_COMPATIBILITY.md` to match what was measured.

No preference writes, no new palette roles, no change to opt-in, ownership or capability gating.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `workbench-theming`: adds requirements that coordinated themes declare their appearance explicitly, and that measured progress-bar and separator surfaces follow the palette.

## Impact

- `bundles/com.github.eclipsethemes/plugin.xml` (four `<theme>` elements).
- `bundles/com.github.eclipsethemes/css/workbench-dark.css` and `workbench-light.css`, which must stay identical apart from the family name.
- `WorkbenchContributionConsistencyTest` (new assertions).
- `docs/WORKBENCH_THEMING_COMPATIBILITY.md` coverage table.
- Compatibility: `isDarkTheme` is ignored by the 4.40/4.41 theme engine. Verified: their `ThemeEngine` reads no such attribute, and the runtime jar ships no schema. CSS selectors that match nothing are inert. Rollback is reverting the files; no state is persisted.

## Decision Gate

**Product / Maintainer decisions:** none required. The change only adds styling to surfaces inside the already published coverage boundary, and only where measurement proves it works. It does not touch preferences, persisted state, supported versions or platforms.

**Evidence-dependent:**
- Do `ProgressBar`, `ProgressIndicator > *` and `Label[style~='SWT.SEPARATOR']` visibly take the palette on Windows with Eclipse 4.40 and 4.41? On Windows with visual styles, `msctls_progress32` may ignore background and bar colors. This is resolved by the runtime probe in tasks group 1, before any CSS is written. Selectors without a measured effect are not shipped, and the spec delta is trimmed to match.
- Does the bundle still resolve and register all four themes on 4.40/4.41 with the new attribute? Expected yes (the attribute is ignored); confirmed in the same runtime run.

**Engineering:** use leaf type selectors rather than enumerating every `Composite` subclass container (see design.md).

**Deferred:**
- Detecting the *current* Eclipse theme as dark through `ITheme.isDark()` (4.42+) instead of the id heuristic, in `EclipseThemesPreferencePage`. This needs a compatibility shim; owner is R5 `target-and-compatibility-matrix`.
- Linux/GTK and macOS verification. That stays with R8's open items.
