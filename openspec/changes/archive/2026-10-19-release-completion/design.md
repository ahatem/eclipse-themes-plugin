# Design — Release completion

## Fonts

- **Model.** `ThemeFonts(editor, workbench)` with `Face(family, size)`; sizes 4–96, family at most 128 characters with no control
  characters. It lives in `core.models` and has no SWT or JFace dependency. `Theme` carries it; a theme built without it has none.
- **Parser.** `<fonts>` is a known single-occurrence section. An invalid entry is a `font` warning and the entry is ignored; an
  unknown child is an unknown-element warning; a repeated entry refuses the package like other repeated sections.
- **Mechanism (public API only).** Write the stored `FontData` string under the plain font-definition id in the
  `org.eclipse.ui.workbench` instance node and flush; publish through the current theme's `FontRegistry`. Editor: Text Font.
  Workbench: Part title, Tree and Table, Dialog, all to the one font. Installed check: `Display.getFontList`, case-insensitive.
  Not done: restyle via the event broker (needs a manifest change) and keys prefixed with a CSS theme id, so a CSS-overridden
  definition and some surfaces may update only after restart; the UI says so.
- **Ownership (`ThemeFontCoordinator`).** Record per definition: written value and prior state (absent or a value). Not owned:
  take over, record prior. Owned and still holding the written value: update. Owned but changed by the user: leave it and report.
  Release: restore prior (or remove the key) only while the value is still the one written, then clear the record. Same semantics
  as the plug-in color writer. The record is saved only after the flush succeeds; a failure is `FAILED`, never success.
- **Order and independence.** Apply order is workbench, editor, then fonts. Fonts are skipped when the toggle is off (releasing
  anything owned) and when the theme has none (`NOTHING_TO_APPLY`). The fonts outcome joins `ThemeApplyOutcome`; the existing
  summary reports it.

## Export

`ThemePackageWriter` writes the package deterministically from a `Theme` (token order: background, foreground, then
alphabetical; flags only when true, `bold="false"` when options exist and are all false). Authored workbench values and fonts are
written only when present. A legacy theme gets `legacy.<slug>`; no workbench values are inferred; provenance is only written for
packages that have it. `ThemeExporter` parses the output with `ThemePackageParser` before writing a temp file and moving it into
place, so an invalid package is never produced and a partial file never appears.

## UI

Page changes are additive: a Format row, a Fonts group, an **Export as Package...** button and warning text in import/update
success dialogs. Preview behavior is unchanged: packages never send identifiers to the preview server.
