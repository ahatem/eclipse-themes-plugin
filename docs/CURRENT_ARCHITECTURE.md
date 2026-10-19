# Current Architecture Baseline

## Status

This document is a compact current-state map derived from the October 2026 audit and verified repository metadata. It is descriptive evidence, not a promise that every current behavior is desirable. Re-check code before changing production behavior.

## Build/module structure

- `bundles/com.github.eclipsethemes` — the single runtime bundle containing model, parser, repository, adapter discovery/application, preference UI, and bundled themes.
- `features/com.github.eclipsethemes.feature` — installable feature.
- `releng/com.github.eclipsethemes.target` — Eclipse target definition.
- `releng/com.github.eclipsethemes.updatesite` — p2 repository/category.
- root `pom.xml` — Tycho reactor and target/environment configuration.

The repository is not currently a multi-runtime-bundle architecture. Future logical boundaries must not be interpreted as an instruction to split bundles prematurely.

## Runtime flow

```text
Preference page
  -> EclipseThemes activator / ThemeManager
  -> ThemeRepository.reload()
      -> bundled XML themes (themes/*.xml only)
      -> workspace-installed themes (*.xml and *.ectheme)
      -> ParserFactory (by final extension, never by content)
           *.xml     -> XmlThemeParser      -> immutable Theme/Token model
           *.ectheme -> ThemePackageParser  -> the same model + PackageMetadata + authored workbench roles
      -> ThemeRecord catalog (logical identity, origin, optional managed locator)
  -> AdapterRegistry.discover()
  -> ThemeService.applyTheme(ThemeRecord.theme())  -> ThemeApplyOutcome (editor part + workbench part)
      1. WorkbenchCoordinator.apply(theme)          (opt-in; no-op unless "Also theme the Eclipse workbench" is on)
          -> WorkbenchPaletteResolver (pure, core.workbench): deterministic semantic conversion to an 8-role palette (package-authored roles verbatim -> authored non-syntax candidates -> OKLCH derivation -> readability correction -> family fallback)
          -> RegistryPaletteSink: non-editable color definitions in the Colors-and-Fonts registry
          -> EclipseWorkbenchThemeEngine: IThemeEngine.setTheme(ITheme, true) between this plugin's four static CSS themes
      2. then the available editor adapter(s)
          -> target Eclipse/plugin InstanceScope preference nodes
```

The order is deliberate: Eclipse's own theme switch synchronously resets the preference defaults the previous stock theme overrode and deletes the instance values for the same editor keys, so editor values written before the switch can be lost (observed leaving stock Dark). Writing them after the switch makes one Apply sufficient; only keys this plugin owns are written.

See [workbench theming compatibility](WORKBENCH_THEMING_COMPATIBILITY.md) for the support matrix and coverage boundary.

The preference page may trigger an additional repository reload, so first opening can parse the collection more than once.

## Theme model and catalog records

`core.models.Theme` is one immutable, validated visual definition: source metadata, appearance
classification, and a `Map.copyOf` token snapshot. It owns no logical identity, managed file, origin or
catalog equality, and exposes no operation that adds, replaces or removes tokens. Two themes compare by
identity, never by logical ID or visual content. The adapter contract `apply(Theme, prefs)` therefore still
receives only visual data; `ThemeRecord` never appears in adapter APIs.

`eclipse.ThemeRecord(logicalId, origin, locator, theme)` is the catalog entry: a nonblank logical identity is
required, `ThemeOrigin.BUNDLED` records carry no locator, `ThemeOrigin.INSTALLED` records carry a
repository-relative managed filename, and equality/hash use the logical ID. Lifecycle actions branch on
explicit origin, and the repository resolves `installed root + locator` instead of inferring storage from
logical-ID prefixes or theme file presence. Persistence formats, including `theme-identities.properties` and
selected-intent preference keys, are unchanged.

## Adapter system

The current adapter system maps canonical theme tokens into editor/plugin preference keys. Built-in integrations cover the base text editor plus JDT, CDT, PDE, Ant, and the debug console. Editors are expected to react through their own preference listeners; the plugin does not own a universal editor-refresh mechanism.

## Extension points

Current contributions include:

- custom `com.github.eclipsethemes.theme.adapters` extension point;
- `org.eclipse.ui.preferencePages`;
- `org.eclipse.core.runtime.preferences`;
- `org.eclipse.e4.ui.css.swt.theme` — four static workbench themes (`...workbench.dark`, `.dark.alt`, `.light`, `.light.alt`) over two stylesheets;
- `org.eclipse.ui.themes` — sixteen non-editable color definitions (eight roles per family);
- `org.eclipse.ui.startup` — `WorkbenchStartup`, a no-op unless workbench coordination is enabled.

The project does not contribute an e4 model fragment or processor, and registers no theme or stylesheet at runtime.

## Layering concern

The audit identified a parser -> activator -> manager -> repository -> parser back-edge caused by parser diagnostics flowing through the UI/plugin activator. That dependency is gone: parsing reports `core.validation.ThemeDiagnostic` findings through a `Consumer` sink supplied by the caller, so `core` has no activator, SWT or preference dependency and the parser retains no findings. `ThemeRepository` owns both sinks — an admission sink that returns findings to the caller and logs them, and a reload sink that only logs — while logging findings from repository admission and reload remains at this boundary.

## Fragile integration surface

There are no direct Java imports from Eclipse `internal` packages in the audited code, but there are version-fragile string-key dependencies and implementation assumptions, including editor semantic-highlighting layouts, CDT internal preference identifiers, PDE/Ant/LSP4E-style keys, and dark-mode inference from theme IDs/preferences, and `IThemeEngine` (`x-friends` API of `org.eclipse.e4.ui.css.swt.theme`), which is confined to `EclipseWorkbenchThemeEngine`. Eclipse derives its dark preference from `id.contains("dark")` on 4.40+, so this plugin's dark workbench theme ids contain `dark` and its light ids do not. These must be isolated, documented, and integration-tested rather than treated as stable APIs.

## Persistence/state

Current state is distributed:

- imported theme files: plugin workspace state directory, as `<uuid>.xml` or `<uuid>.ectheme` (the locator keeps the source format, and an update may not change it);
- selected theme intent: plugin `InstanceScope` preferences;
- applied editor colors/styles: preference nodes owned by Eclipse/other plugins;
- workbench coordination: intent, ownership (owned theme id, prior active theme id, prior saved theme-id state including absence) and the experimental opt-in in plugin `InstanceScope` preferences, written only after the switch postcondition holds; the published palette is ephemeral and re-derived from the selected theme at startup;
- theme collection: in memory;
- page selection/filter/preview state: UI fields;
- e4 context/application model: no durable theme state currently stored there.

The selected theme ID records intent, not proof that target preferences still equal that theme. There is no current ownership journal, rollback snapshot, startup reconciliation, or uninstall restoration.

### Logical theme identity

The import lifecycle allocates a repository-owned `<UUID>.xml` locator and never derives destination or continuity from source basename, display name, source ID, or content. A new import stages one copy, parses the staged bytes, moves them without replacement, persists one new binding, and only then admits the catalog record. Existing basename-named locators remain valid and are not migrated. Update targets one selected established `local:` record and atomically replaces its existing locator while preserving identity and binding. Removal removes only that record's binding, observes the managed file's absence, and then removes the catalog record; a file-delete failure restores the exact prior binding.

Admitted themes have logical IDs independent of display names and source XML `ect-id` claims. Bundled IDs are `bundled:<curated ect-id>`; installed records receive `local:<generated value>` IDs. The raw source ID remains separate for legacy selection lookup and the existing website preview. Parser-only candidates have no established logical ID and compare equal only to themselves. Catalog equality/hash use the record's logical ID, not current contents, while a `Theme` itself uses identity equality. Catalog presentation sorts by name, then logical ID; independent same-name or same-source-ID records remain selectable.

Installed identity bindings live in `theme-identities.properties` in the existing plugin workspace state directory, mapping managed relative filenames to opaque logical IDs. They are introduced lazily without rewriting or relocating XML. The filename locates a record; it does not generate its ID. A generated ID is exposed only after staged atomic replacement of this small binding file succeeds. Failure preserves old bindings and XML, diagnoses the affected admission and does not invent a transient stable ID. Unsupported metadata atomic replacement fails admission rather than falling back to a destructive write. Arbitrary manual moves, deletion/recreation of managed files, metadata loss and cross-workspace transport are not an identity recovery protocol; a future supported relocation must carry its binding.

New selected intent uses `SELECTED_THEME_LOGICAL_ID`. With no modern explicit reference, a nonblank explicit legacy `ACTIVE_THEME_ID` resolves only when exactly one loaded candidate claims it. Ambiguous/missing/blank intent remains unresolved. Opening/restoring selection does not rewrite old intent, apply colors or select a guessed fallback. An explicit application writes modern intent and dispatches current content, including when its identity is unchanged. Unchanged explicit Apply/OK can therefore reapply. Default actions use the curated bundled identities. This remains selected intent, not successful-applied-state tracking; R3 owns application truth and recovery.

No revision ID, production digest, history or content-comparison mechanism is present. Downgrades can read unchanged XML but may require reselection. Versions before theme packages list only `*.xml`, so an installed `.ectheme` is ignored there (its file and binding are kept) and reappears after upgrading again. New imports use fresh repository-allocated locators; removed locators are not silently reassigned to a different identity. Staging files under `installed_themes/.staging` are never catalogued, and parse/copy/binding failures clean only attempt-created artifacts while preserving prior records, bindings and selected intent. Update requires one atomic replacement and fails without a non-atomic fallback. Removal may succeed when the exact managed file is already absent, but fails closed for an unexpected path type; a crash between binding removal and file deletion can leave an unbound file that is later re-admitted under a new identity. Offline preview, R3 applied-state truth and the conflict/diagnostic UI remain later work.

## Validation and diagnostics

One validation boundary covers bundled loading, installed-reload, import and update. Structural failures
refuse the document: the root element must be `colorTheme` and `name` must be present and non-blank. These are
the only errors; every other defect is a warning attached to an admitted theme, including unknown and nested
elements, tokens with an absent, blank or unparseable `color`, non-boolean style flags, mapping conflicts, a
present-but-unrecognised `type`, and a `website` that is not an absolute `http`/`https` URI. Warnings never
block admission, so a sparse or slightly non-conforming historical theme still imports.

Findings travel as a small `core.validation.ThemeDiagnostic` value (severity, stable kind, message, source,
element/attribute) through an injected sink; the parser holds no findings of its own. Refusals also carry their
error findings on `ThemeParseException.diagnostics()`, and import/update report a small
`ThemeOperationOutcome` (committed `ThemeRecord`, findings, `describeFailure()`) shaped like `PreferenceReset.Result`.
Refused admission leaves bytes, bindings, identity and catalog unchanged on every source; an installed file
that no longer validates is skipped on reload without being deleted or rebound.

Website metadata is untrusted display data. A single SWT-free check decides launchability by trim, absolute
URI syntax and a case-insensitive `http`/`https` scheme; the details pane renders link markup only for a
launchable value and escaped inert text otherwise, and the selection listener reuses that same check before
`Program.launch`.

Accepted for now: the parser still applies its hard-coded dark root defaults when a light theme omits them
(PM-V7). Correcting those defaults changes rendered colors for existing themes; the immutable theme model
retains them exactly at the parser boundary rather than folding a change into validation.

## Build baseline

- Tycho: 5.0.0.
- runtime execution environment: JavaSE-17.
- CI build JDK: 21.
- target baseline in the current POM: Eclipse 2024-09 / 4.33.

The distinction between build JVM and bundle runtime compatibility is intentional until changed by an explicit compatibility decision.
