# Architecture

Eclipse Themes is one OSGi bundle (`bundles/com.github.eclipsethemes`) plus a feature, a p2 update site and a target definition (`releng/`). It is built with Maven/Tycho.

## Module map

All code is under `com.github.eclipsethemes`:

| Package | Role | Eclipse UI / OSGi dependency |
| --- | --- | --- |
| `core.models` | Immutable `Theme`, tokens, package metadata, `Constants` (default theme ids, website URLs) | none |
| `core.parser` | `XmlThemeParser` (legacy `.xml`) and `ThemePackageParser` (`.ectheme`); `ThemeParseException`; legacy alias mapping | none |
| `core.validation` | `ThemeDiagnostic` findings reported through a caller-supplied sink | none |
| `core.workbench` | Pure conversion of a theme into the eight-role workbench palette | none |
| `eclipse` | Activator, `ThemeRepository` / `ThemeRecord` catalog, `ThemeService`, import/update/remove | yes |
| `eclipse.adapters` | Editor adapters (`editor.*`: text, JDT, CDT, PDE, Ant) and the debug console adapter, discovered through the `com.github.eclipsethemes.theme.adapters` extension point | yes |
| `eclipse.workbench` | Workbench coordination: palette registry sink, `IThemeEngine` switching, plug-in color keys | yes |
| `eclipse.fonts` | Theme font coordination | yes |
| `eclipse.preferences` | The Theme Manager preference page (`EclipseThemesPreferencePage`): a thin coordinator that holds the selected and applied theme and handles Apply, OK and Restore Defaults | yes |
| `eclipse.preferences.ui` | One class per visible section of that page: `ThemeLibraryPanel` (list, search, import/export/update/remove), `ThemeDetailsPanel` (scroller and header), `PreviewPanel`, `AppearancePanel`, `FontsPanel`, `AboutPanel`; plus `ResetChoiceDialog`. Panels reach the page only through `PageContext` | yes |

Rule: `core.*` must not import SWT, JFace, the activator or Eclipse preference APIs (`CoreLayeringTest` enforces the SWT / JFace / `org.eclipse.ui` part). Everything that talks to Eclipse lives in `eclipse.*`. Do not split the bundle unless an optional dependency, a public API or a lifecycle difference requires it.

## How a theme flows

```text
file (*.xml / *.ectheme)
  -> ParserFactory (by final extension, never by content)
  -> XmlThemeParser | ThemePackageParser -> immutable Theme (+ PackageMetadata, authored workbench roles)
  -> ThemeRepository: ThemeRecord (logical id, origin BUNDLED/INSTALLED, locator, theme)
  -> ThemeService.applyTheme(theme)
       1. WorkbenchCoordinator (opt-in): palette -> color definitions -> one of four static CSS themes
       2. editor adapters: tokens -> preference keys of Eclipse / other plug-ins
  -> ThemeApplyOutcome (editor part and workbench part reported separately)
```

The workbench step runs first on purpose: Eclipse's own theme switch resets the editor defaults the previous theme overrode, so editor values written before the switch can be lost.

Bundled themes are `themes/*.ectheme` inside the bundle. Imported themes live in the plug-in state directory as `<uuid>.xml` or `<uuid>.ectheme`, with their logical ids in `theme-identities.properties`. Selected-theme intent and ownership journals are kept in the plug-in's instance preferences.

## Behavior rules

**Preference ownership.** Theme code writes into preference nodes owned by Eclipse and other plug-ins, so:
- only the keys the operation owns are written; a foreign node is never cleared;
- for each key the plug-in records the state before it first took the key (including *absent*) and the value it last wrote;
- a key is restored or rewritten only while it still holds the value this plug-in wrote; a newer user or external change is preserved and reported;
- the other plug-in colors follow a dark workbench theme only where the user has no value of their own;
- a theme is recorded as selected or applied only after application succeeded. A failed or conflicted editor apply leaves the previous state, and editor and workbench results are reported separately.

**XML and import security.** Theme files are untrusted input.
- Every `DOCTYPE` is rejected and external resources are never resolved. If the parser cannot be configured securely, parsing fails.
- Documents are size-bounded before parsing. Structure errors (root element, missing name) refuse the document; everything else is a warning and the theme is still admitted.
- Import stages a copy, parses the staged bytes, then moves it into place atomically. A refused import or update leaves files, identity and catalog unchanged. Updates never fall back to a non-atomic write.
- Theme packages are declarative XML only; no code from an imported theme is ever executed.
- A theme's `website` is shown as a link only for an absolute `http`/`https` URI.

**Theme identity.** A theme's identity is never its display name, file name or the `id` it claims. Bundled themes are `bundled:<id>`, installed ones get a generated `local:<id>`. Importing never infers continuity with an existing theme; only an explicit update of a selected installed theme keeps identity. Two themes with the same name stay separately selectable.

**Workbench theming limits.** Workbench theming is opt-in, off by default and independent of the editor. It uses four static CSS themes that read an eight-role palette derived from the selected theme; nothing is registered at runtime and imported content never reaches CSS. Native menus, title bars, dialogs, browser content, custom-drawn third-party widgets and arbitrary icons are not themeable this way. See [WORKBENCH_THEMING_COMPATIBILITY.md](WORKBENCH_THEMING_COMPATIBILITY.md) for the support matrix and what is verified on which platform.

## Fragile integration surface

No Eclipse `internal` packages are imported, but some integrations depend on version-specific preference keys (JDT semantic highlighting, CDT, PDE/Ant keys) and on `IThemeEngine`, which is `x-friends` API confined to `EclipseWorkbenchThemeEngine`. Keep such code isolated, document where the keys come from, and test the exact writes and removals. See [EDITOR_MAPPING_COMPATIBILITY.md](EDITOR_MAPPING_COMPATIBILITY.md).

## Tests

- `bundles/com.github.eclipsethemes/test/` mirrors the source packages (`core/...`, `eclipse/adapters`, `eclipse/preferences` including `ui`, `eclipse/workbench`) and runs as plain JUnit through Tycho; no Eclipse installation is needed.
- `bundles/com.github.eclipsethemes/test-data/` holds theme fixtures.
- Add a regression test at the narrowest layer for every bug fix; parser, repository, adapter and apply/reset changes need one.
- Behavior that only a real Eclipse can show (listeners, CSS, switching themes) is checked by hand on a real IDE, and the result is recorded in the compatibility docs.
