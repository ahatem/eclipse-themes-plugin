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

## Build baseline

- Tycho: 5.0.0.
- runtime execution environment: JavaSE-17.
- CI build JDK: 21.
- target baseline in the current POM: Eclipse 2024-09 / 4.33.

The distinction between build JVM and bundle runtime compatibility is intentional until changed by an explicit compatibility decision.
