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
      -> bundled XML themes
      -> workspace-installed XML themes
      -> ParserFactory -> XmlThemeParser -> Theme/Token model
  -> AdapterRegistry.discover()
  -> ThemeService.applyTheme()
      -> available adapter(s)
      -> target Eclipse/plugin InstanceScope preference nodes
```

The preference page may trigger an additional repository reload, so first opening can parse the collection more than once.

## Adapter system

The current adapter system maps canonical theme tokens into editor/plugin preference keys. Built-in integrations cover the base text editor plus JDT, CDT, PDE, Ant, and the debug console. Editors are expected to react through their own preference listeners; the plugin does not own a universal editor-refresh mechanism.

## Extension points

Current contributions include:

- custom `com.github.eclipsethemes.theme.adapters` extension point;
- `org.eclipse.ui.preferencePages`;
- `org.eclipse.core.runtime.preferences`.

The project currently does not contribute a workbench CSS theme, `org.eclipse.ui.themes` theme, e4 model fragment, or processor.

## Layering concern

The audit identified a parser -> activator -> manager -> repository -> parser back-edge caused by parser diagnostics flowing through the UI/plugin activator. Target architecture removes this dependency by returning diagnostics or injecting a sink.

## Fragile integration surface

There are no direct Java imports from Eclipse `internal` packages in the audited code, but there are version-fragile string-key dependencies and implementation assumptions, including editor semantic-highlighting layouts, CDT internal preference identifiers, PDE/Ant/LSP4E-style keys, and dark-mode inference from theme IDs/preferences. These must be isolated, documented, and integration-tested rather than treated as stable APIs.

## Persistence/state

Current state is distributed:

- imported theme files: plugin workspace state directory;
- selected theme intent: plugin `InstanceScope` preferences;
- applied editor colors/styles: preference nodes owned by Eclipse/other plugins;
- theme collection: in memory;
- page selection/filter/preview state: UI fields;
- e4 context/application model: no durable theme state currently stored there.

The selected theme ID records intent, not proof that target preferences still equal that theme. There is no current ownership journal, rollback snapshot, startup reconciliation, or uninstall restoration.

### Logical theme identity

The R2 identity slice includes approved PM6-A: import refuses an occupied managed destination or a locator reserved by an existing binding, before XML/binding/catalog/selection mutation. A reservation with a missing file is diagnosed and preserved rather than recycled. Reload can reuse established bindings; an incoming import cannot inherit identity through a basename match.

Admitted themes have logical IDs independent of display names and source XML `ect-id` claims. Bundled IDs are `bundled:<curated ect-id>`; installed records receive `local:<generated value>` IDs. The raw source ID remains separate for legacy selection lookup and the existing website preview. Parser-only candidates have no established logical ID and compare equal only to themselves. Established equality/hash use the logical ID, not current contents. Catalog presentation sorts by name, then logical ID; independent same-name or same-source-ID records remain selectable.

Installed identity bindings live in `theme-identities.properties` in the existing plugin workspace state directory, mapping managed relative filenames to opaque logical IDs. They are introduced lazily without rewriting or relocating XML. The filename locates a record; it does not generate its ID. A generated ID is exposed only after staged atomic replacement of this small binding file succeeds. Failure preserves old bindings and XML, diagnoses the affected admission and does not invent a transient stable ID. Unsupported metadata atomic replacement fails admission rather than falling back to a destructive write. Arbitrary manual moves, deletion/recreation of managed files, metadata loss and cross-workspace transport are not an identity recovery protocol; a future supported relocation must carry its binding.

New selected intent uses `SELECTED_THEME_LOGICAL_ID`. With no modern explicit reference, a nonblank explicit legacy `ACTIVE_THEME_ID` resolves only when exactly one loaded candidate claims it. Ambiguous/missing/blank intent remains unresolved. Opening/restoring selection does not rewrite old intent, apply colors or select a guessed fallback. An explicit application writes modern intent and dispatches current content, including when its identity is unchanged. Unchanged explicit Apply/OK can therefore reapply. Default actions use the curated bundled identities. This remains selected intent, not successful-applied-state tracking; R3 owns application truth and recovery.

No revision ID, production digest, history or content-comparison mechanism is present. Downgrades can read unchanged XML but may require reselection. Import still derives its prospective locator from the source basename but accepts only genuinely unused locators. Re-import onto an existing/reserved basename is temporarily refused, with no update/replace/keep-both policy or conflict UI. Occupancy/reservation checks precede parsing; the destination is exclusively created/copied before a new binding is persisted, and the catalog changes only after both succeed. The guard also prevents importing a managed file onto itself. If exclusive creation/copying fails, only a destination created by that attempt is removed (best effort) and no binding or catalog entry exists. If binding persistence fails after the copy, that new destination is removed and prior bindings, catalog and selected intent are preserved. Cleanup is best-effort rather than an XML transaction, and source changes during import remain later import work. Removal failure reporting/paired binding cleanup, broader validation, immutable definitions and offline preview also remain later work.

## Build baseline

- Tycho: 5.0.0.
- runtime execution environment: JavaSE-17.
- CI build JDK: 21.
- target baseline in the current POM: Eclipse 2024-09 / 4.33.

The distinction between build JVM and bundle runtime compatibility is intentional until changed by an explicit compatibility decision.
