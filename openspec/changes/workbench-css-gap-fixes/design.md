# Design

## Context

The four coordinated themes are declared in `plugin.xml` and share two family stylesheets. A test keeps those two stylesheets identical apart from the family name, and every color in them is a palette role id (`WorkbenchContributionConsistencyTest`).

On 4.40/4.41 the theme engine calls `display.setDarkThemePreferred(id.contains("dark"))` and reads no `isDarkTheme` attribute. The runtime `org.eclipse.e4.ui.css.swt.theme` jar ships no `.exsd`, so the extension registry stores unknown attributes without complaint. Both facts were checked against `tmp/eclipse-4.40` and `tmp/eclipse-4.41`.

`WidgetElement.computeLocalName` returns the widget's exact simple class name. A `ProgressBar` two `Composite` subclasses deep (`ProgressMonitorPart > ProgressIndicator > ProgressBar`) is therefore matched neither by our `Composite > *` / `Composite > * > *` rules nor by the `ProgressMonitorPart` and `ProgressIndicator` container rules.

## Goals / Non-Goals

**Goals:** declare appearance explicitly. Style the progress bar and separator leaf widgets wherever measurement shows SWT honors the color on Windows.

**Non-Goals:** Linux/GTK or macOS claims. Bar (fill) color as a separate role. Changing scroll bars, menus or other native surfaces. Any preference writes.

## Decisions

1. **Leaf type selectors, not container enumeration.** `ProgressBar` and `ProgressIndicator > *` reach the bar whatever its containers are called. Listing every `Composite` subclass that might host a bar is open-ended and would still miss third-party hosts. Separator labels use `Label[style~='SWT.SEPARATOR']`, the same attribute-selector style the stock sheets use.
2. **Roles.**
   - Progress bar: `background-color` is the `background` role (the content surface used for inputs and trees) and `color` is the `accent` role, so a bar is distinguishable from its trough where SWT honors the foreground.
   - Separator: `background-color: inherit` and `color` is the `border` role.

   If the probe shows that a role combination lacks contrast against `surface`, adjust it to another existing role. No new role is added.
3. **Measure first, then write CSS.** Reuse the archived probe pattern (`openspec/changes/archive/2026-10-19-dynamic-workbench-theming/evidence/runtime/harness/parity`). The probe opens a shell under each coordinated family with a `ProgressMonitorPart`, a bare `ProgressBar` and a separator `Label`, then records the widget's `getBackground()`/`getForeground()` plus a sampled pixel from a screenshot, with and without the candidate rules applied. The pixel is the evidence. A getter reporting the color while the pixel stays system-light counts as "not honored". Results are saved under this change's `evidence/`.
4. **Attribute values are literals in `plugin.xml`.** The consistency test asserts that each theme's `isDarkTheme` matches its family. No code reads the attribute.

## Risks / Trade-offs

- [Windows visual styles ignore `PBM_SETBKCOLOR`/`PBM_SETBARCOLOR`, so the bar stays light] → The selector is dropped and the doc row stays SYSTEM/NATIVE. This is the expected fallback, not a failure of the change.
- [A separator rule paints a visible band in some layouts] → Use `background-color: inherit`. The probe screenshot checks a dialog with separators (Preferences).
- [A future engine schema validates `isDarkTheme` differently] → It is the attribute name and values defined upstream in #4354, and the id heuristic remains as the fallback.

## Migration Plan

None. No persisted state. Rollback is reverting `plugin.xml`, the two stylesheets and the doc.
