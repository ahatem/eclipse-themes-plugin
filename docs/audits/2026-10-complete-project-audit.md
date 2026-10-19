# Eclipse Themes — complete project audit

**Verdict:** this is a small, understandable editor-theme plugin with a sensible starting structure. It is **not yet a safe foundation for a full-IDE customization system**. The immediate priorities are preference preservation, secure XML parsing, theme identity, reliable imports, and honest apply/error reporting.

The most serious finding is that restoring “Eclipse original colors” clears entire preference nodes belonging to other Eclipse plugins. That can erase settings unrelated to themes.

## Scope and confidence

Reviewed the working tree at commit `ebec341b7c6580ffa5ab7340eeef861a5f32be68`:

- All **28 Java files**, approximately **2,307 lines**.
- All **33 bundled XML themes**.
- Bundle manifest, extension declarations and schema, Maven/Tycho configuration, feature, update site, workflow, and documentation.
- Current Eclipse API documentation and selected upstream implementations.

I ran isolated Java probes against the production parser, repository, model, and adapter helper code. Eclipse logging/bundle access was stubbed where necessary.

**Limitations:** Maven is unavailable in this shell, so I did not run a full Tycho build. I did not launch a runtime Eclipse instance, test native rendering, or inspect remote repository settings such as branch protection. Existing local build artifacts do not establish that the current checkout builds cleanly.

No project source was changed. Your existing README modification was preserved.

### Findings verified by executable probes

| Probe | Result |
|---|---|
| External XML entity referencing a harmless local XML file | Production parser loaded it and used its background color |
| Two legacy themes with different `id` attributes but no `ect-id` | Compared equal |
| Invalid replacement import using an existing filename | Previous valid stored file was deleted |
| Arbitrary XML root, `<notATheme/>` | Accepted as a theme with empty identity/name |
| Invalid background in a light theme | Accepted with dark fallback `#1e1e1e` |
| Filename ending in `.XML` | Parser lookup failed |
| Missing optional semantic token | Explicit `.enabled=false` was subsequently removed |
| GitHub Dark Dimmed selection background | Parsed as `#2d333b`, despite XML specifying `#3d4149` |
| Jupyter static-final style | Later alias discarded the explicit bold/italic options |
| Bundled themes | All 33 parsed; this does **not** establish semantic correctness |

---

# PART 1 — ARCHITECTURE AUDIT

## 1. Module structure

| Module | Responsibility | Assessment |
|---|---|---|
| [bundles/com.github.eclipsethemes](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/pom.xml) | Models, parsing, theme storage, adapter discovery, preference application, preference page, bundled themes | Reasonable for the current size, but combines domain, infrastructure, UI, and public extension contracts |
| [features/com.github.eclipsethemes.feature](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/features/com.github.eclipsethemes.feature/feature.xml) | Installable feature containing the runtime bundle | Correct basic separation |
| [releng/com.github.eclipsethemes.target](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/releng/com.github.eclipsethemes.target/eclipse-2024-09.target) | Target platform definition | Correct location; reproducibility and coverage need improvement |
| [releng/com.github.eclipsethemes.updatesite](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/releng/com.github.eclipsethemes.updatesite/category.xml) | Categorized p2 repository | Correct basic separation |
| [Root POM](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/pom.xml) | Reactor, Tycho version, target selection, platform environments | Appropriately small |

There is **one runtime bundle**, not a multi-bundle application architecture.

The current separation is not inherently contrary to OSGi/Tycho practice. Splitting every small adapter into a bundle would add overhead without much present benefit. However, an independently testable core and a stable adapter API become worthwhile before adding full-workbench theming.

### Architecture issues

| ID / Severity | Location | Problem | Recommended fix |
|---|---|---|---|
| A1 — **High** | [MANIFEST.MF](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/META-INF/MANIFEST.MF:7), adapter extension contract | The bundle exports no packages. External bundles cannot normally extend `EclipseThemeAdapter` or reference its `Theme` contract through OSGi wiring. The advertised adapter system works internally but is not a usable supported third-party API. | Export a deliberately scoped, versioned API containing the adapter contract and required model types. Keep implementation packages private. Add an external-contributor integration test. |
| A2 — **Medium** | [XmlThemeParser.parse](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/parser/XmlThemeParser.java:59) | The `core` parser calls the UI bundle activator to log diagnostics. This defeats the apparent domain/platform separation and makes standalone parsing depend on bundle lifecycle. | Return structured parser diagnostics or inject a diagnostic sink. Keep the parser independent of Eclipse. |
| A3 — **Medium** | `MANIFEST.MF`; adapters | Required Eclipse bundles have no version ranges, while optional editor compatibility is checked only by bundle presence. Installation success is not evidence that preference mappings are compatible. | Declare tested minimum API versions and reasonable ranges. Give adapters explicit supported bundle versions and integration tests. |
| A4 — **Medium** | [AdapterRegistry.discover](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/AdapterRegistry.java:24) | Discovery happens once, uses unordered `HashMap` iteration, and instantiates contributions before testing target-plugin availability. There is no change tracking, ownership metadata, ordering, or collision policy. | Discover descriptors first; validate availability/version before instantiation. Define stable ordering and collision handling. Add registry-change handling only if live contribution changes are supported. |

**Not a finding:** the fully qualified extension-point ID in `plugin.xml` is not automatically a doubled-namespace bug. Equinox recognizes qualified IDs with the declared manifest schema version. [Equinox parser implementation](https://raw.githubusercontent.com/eclipse-equinox/equinox/master/bundles/org.eclipse.equinox.registry/src/org/eclipse/core/internal/registry/ExtensionsParser.java)

## 2. Runtime architecture

### Loading and parsing

```text
Preference page opens
  → EclipseThemes.instance().getManager()
  → ThemeManager construction
      → ThemeRepository.reload()
          → bundled themes/*.xml
          → workspace installed_themes/*.xml
          → ParserFactory
          → XmlThemeParser
          → mutable Theme + Token objects
      → AdapterRegistry.discover()

Preference page loadThemes()
  → repository.reload() again
```

The first page opening can therefore parse the collection twice.

`XmlThemeParser`:

1. Builds a DOM document.
2. Reads metadata, including `ect-id`.
3. Inserts fallback foreground/background tokens.
4. Converts legacy XML tags into canonical token keys.
5. Parses colors and style flags.
6. Adds tokens to the theme, overwriting previous tokens with the same mapped key.

The repository sorts themes by **display name**, using a `TreeMap<String, Theme>`.

### Applying themes

```text
Apply / OK / double-click
  → preference page saves selected theme ID
  → ThemeManager.applyTheme()
  → ThemeService.applyTheme()
  → each available adapter
      → InstanceScope preference node
      → write editor-specific preference keys
      → flush node
```

There is no CSS theme registration or e4 model styling.

### What the adapter system actually is

It is a **token-to-preference mapping system**:

- `TextEditorThemeAdapter`: editor backgrounds, selections, rulers, annotations.
- `JavaEditorThemeAdapter`: JDT lexical and semantic highlighting.
- `CppEditorThemeAdapter`: CDT lexical and semantic highlighting.
- `PluginEditorThemeAdapter`: PDE manifest/XML editor settings.
- `AntEditorThemeAdapter`: Ant editor settings.
- `DebugConsoleThemeAdapter`: debug console colors.

Editors are expected to react through their existing preference-store listeners. The plugin does not explicitly refresh editor instances.

The `IWorkbench` argument is passed through manager/service methods but is unused by the service.

**Scope clarification:** this already styles some editor-adjacent UI and the debug console. It does not theme workbench views, toolbars, menus, dialogs, or tabs.

## 3. Extension points

| Extension point | Defined / consumed here | Role |
|---|---|---|
| `com.github.eclipsethemes.theme.adapters` | Defined in `plugin.xml`; consumed by `AdapterRegistry`; six built-in contributions | Adapter discovery |
| `org.eclipse.ui.preferencePages` | Contributed to | Appearance preference page |
| `org.eclipse.core.runtime.preferences` | Contributed to | Default preference initialization |

The adapter schema has placeholder documentation for version, examples, and API contract. Its `plugin` attribute is optional, but the consequences of omitting it are undocumented.

There are no contributions to:

- `org.eclipse.e4.ui.css.swt.theme`
- `org.eclipse.ui.themes`
- e4 model fragments or processors

## 4. Dependency graph and layering

```text
Build reactor
  parent
    ├── target definition
    ├── runtime bundle
    ├── feature ───────────────→ runtime bundle
    └── p2 repository ─────────→ feature

Declared runtime bundle dependencies
  com.github.eclipsethemes
    ├── org.eclipse.core.runtime
    └── org.eclipse.ui
          └── transitive platform/JFace/SWT dependencies

Runtime integration by preference-node ID
  adapters
    ├── org.eclipse.ui.editors
    ├── org.eclipse.jdt.ui
    ├── org.eclipse.cdt.ui
    ├── org.eclipse.pde.ui
    ├── org.eclipse.ant.ui
    └── org.eclipse.debug.ui

Application dependencies
  preference page → manager → repository → parser → models
                           → registry → adapters → models/preferences
                           → service → registry

Layering back-edge
  parser → EclipseThemes activator → manager → repository → parser

Preview dependency
  preference page → SWT Browser → external preview website
```

**No inter-bundle cycle exists in this repository.** The problematic cycle is a code-layer dependency through the activator.

The important “leak” is ownership: the plugin writes other plugins’ settings without recording which values it owns or what existed before.

## 5. Internal and fragile Eclipse dependencies

**There are no direct imports of Eclipse `internal` packages in the project’s Java code.** That is a positive finding.

However, avoiding internal imports does not eliminate implementation coupling.

| Coupling | Location | Fragility |
|---|---|---|
| CSS theme preference node `org.eclipse.e4.ui.css.swt.theme`, key `themeid`, OS-inheritance system property | `EclipseThemesPreferencePage.isEclipseDarkMode()` | Reads implementation details instead of the effective theme service; ignores other preference scopes and can misclassify explicit light themes |
| `org.eclipse.cdt.internal.ui.text.doctools.doxygen.*` | `CppEditorThemeAdapter.apply()` | Internal identifiers embedded as strings; no compile-time compatibility checking |
| `semanticHighlighting.*` preference layouts | Java/CDT adapters | Implementation-derived portions of editor highlighting contracts; changes can silently stop working |
| PDE `editor.color.*`, Ant color/style suffixes, LSP4E annotation keys | Respective adapters | String mappings must be checked against each supported editor version |
| Theme ID containing `"dark"` | `isEclipseDarkMode()` | Naming convention is not a reliable appearance contract |

Some lexical preference keys have public constant equivalents. Those should be distinguished from genuinely implementation-derived keys rather than labeling every string as an internal API.

**Severity: Medium. Recommended fix:** centralize mappings with provenance, supported versions, and tests. Use public constants when practical; keep editor dependencies optional through separate integration bundles where needed.

Also, **absence of `@since` alone does not establish that an Eclipse type is non-API**. Export declarations, API documentation, and restrictions matter.

## 6. Persistence

| State | Storage |
|---|---|
| Imported XML files | Bundle state directory, normally under workspace `.metadata/.plugins/com.github.eclipsethemes/installed_themes/` |
| Selected theme ID | Plugin `InstanceScope` preferences through `ScopedPreferenceStore` |
| Applied colors/styles | Other plugins’ `InstanceScope` preference nodes, flushed by adapters |
| Theme collection | In-memory repository map |
| Selected/current theme, filtering, preview visibility | Preference-page fields |
| e4 context/application model | No theme state stored there |

The selected ID is a **record of intent**, not proof of effective editor colors. Other plugins or Eclipse CSS can subsequently change the same settings.

There is no startup reconciliation, preference ownership journal, rollback snapshot, or uninstall restoration. Persisted editor values can survive restart without reapplying the XML; conversely, updating the XML does not automatically update those values.

---

# PART 2 — CODE QUALITY AUDIT

## 1. Package and class review

The model/repository/service/registry distinction is mostly understandable. `Color` and `TokenOptions` are useful immutable records. Adapter classes are small and cohesive.

The largest architectural problem is the **821-line preference page**, roughly 36% of the Java code. It combines widget creation, remote preview, filtering, import/removal, apply policy, dark-mode detection, persistence, and destructive reset behavior.

### Confirmed defects and quality issues

| ID / Severity | Location | Problem | Recommended fix |
|---|---|---|---|
| Q1 — **Critical** | [resetToEclipseOriginal](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/preferences/EclipseThemesPreferencePage.java:728) | Calls `prefs.clear()` on six whole preference nodes. This deletes non-theme settings in JDT, CDT, PDE, Ant, debug, and editors. The dialog’s warning describes color resets, not the full effect. | Remove whole-node clearing immediately. Restore only recorded theme-owned keys, preserving original value **and absence**. Existing installations without snapshots need a conservative, explicitly documented recovery policy. |
| Q2 — **High** | [XmlThemeParser.parse](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/parser/XmlThemeParser.java:30) | DOM parsing allows external entities. A production-parser probe loaded a local file. Network resolution and resource exhaustion are additional risks, although not reproduced here. | Reject DOCTYPE; disable external general/parameter entities and external DTD loading; disable XInclude; restrict external DTD/schema access; set processing limits. Fail closed if required security settings cannot be applied. [JAXP security guidance](https://docs.oracle.com/en/java/javase/25/security/java-api-xml-processing-jaxp-security-guide.html) |
| Q3 — **High** | [Theme.equals](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/models/Theme.java:97); parser metadata; repository keys; `performOk()` | Parser reads only `ect-id`, so legacy themes without it receive `""` and compare equal. Repository identity is instead display name. Same-ID replacements also compare equal to the old active object, allowing OK to skip applying updated content. | Define stable package identity, separate legacy external IDs, display names, versions, and content hashes. Generate and persist identities for legacy files lacking `ect-id`. Compare active revisions, not just IDs, when deciding whether application is needed. |
| Q4 — **High** | [ThemeRepository.importTheme](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/ThemeRepository.java:49) | Copies over the destination before validation. Invalid replacement deletes the previously valid file. Different themes sharing a basename can overwrite each other. Importing the stored file itself can truncate it. | Parse into a staged candidate first. Store by stable identity, not original basename. Write to a temporary file and atomically replace where supported. Retain the previous revision on failure. |
| Q5 — **High** | `ThemeRepository.importTheme()` / `loadInstalledThemes()`; preference-page `importTheme()` | Import replaces a duplicate display name in memory; reload skips that duplicate. Bundled themes win during reload. A reported successful replacement can disappear immediately or resolve differently later. | Use one deterministic identity/version replacement policy for import and reload. Require an explicit conflict choice and ensure the selected result is the repository’s committed object. |
| Q6 — **High** | [TextEditorThemeAdapter.apply](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/adapters/editor/TextEditorThemeAdapter.java:55) | Writes RGB strings into `currentIPIndication` and `secondaryIPIndication`, which are text-indication settings rather than color keys. Correct color keys are `currentIPColor` and `secondaryIPColor`. | Correct mappings and restore previously corrupted indication settings conservatively. Verify annotation mappings against upstream declarations. [Debug annotation declarations](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/debug/org.eclipse.debug.ui/plugin.xml) |
| Q7 — **High** | [LegacyEclipsePropertyMapper](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/parser/mapper/LegacyEclipsePropertyMapper.java:84); parser token insertion | `sourceHoverBackground` overwrites `selectionBackground`. Other aliases collide: `xmlComment` overwrites generic comment; `staticFinalField` and `constant` collapse into one token. Bundled themes lose colors/styles through document order. | Introduce distinct roles where semantics differ. Define explicit legacy precedence for true aliases and report collisions. Add assertions for expected resolved colors/styles of bundled themes. |
| Q8 — **Medium** | [mapSemanticStyleOptional](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/adapters/EclipseThemeAdapter.java:38), `clearSemanticStyle()` | Missing token writes `.enabled=false`, then clearing removes that flag. Effective enabled state falls back to the editor default, potentially reintroducing unwanted highlighting. | Clear style properties without removing the explicit disabled flag, or model “inherit editor default” as a separate policy. Test transitions between rich and sparse themes. |
| Q9 — **High** | [ThemeService.applyTheme](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/ThemeService.java:23); page `applySelectedTheme()` / `performOk()` | Service logs `BackingStoreException` and returns normally. Page saves active ID before applying, marks the theme current, and shows “Applied.” Runtime exceptions can abort remaining adapters. `performOk()` still returns success after the page catches an apply failure. | Return an aggregate application result. Stage changes, snapshot originals, handle failures per adapter, and roll back where possible. Commit active state only after success; keep the dialog open on failure. |
| Q10 — **Medium** | `XmlThemeParser.parse()`; `ThemeType.from()` | No root/schema/identity/name validation. Invalid colors are skipped; malformed booleans silently become false; unknown/missing type becomes light. Even light themes receive dark root fallbacks. | Validate required metadata and tokens. Produce actionable diagnostics with element/attribute context. Use a documented legacy compatibility mode rather than accepting arbitrary XML. |
| Q11 — **Medium** | Page `createContents()`, `loadThemes()`, `importTheme()`, `removeSelectedTheme()`, apply/reset methods | Parsing, copying, directory listing, and preference flushes run in UI callbacks. Unbounded input increases freeze risk. | Move file work and validation into cancellable Jobs; marshal widget updates to the display. Keep preference application on the thread required by its listeners and measure flush latency. |
| Q12 — **Medium** | Page `removeSelectedTheme()`; repository constructor/import cleanup | `File.delete()` and `mkdirs()` return values are ignored. Removal can report success when the file remains. Import failures can leave partial/unsupported files. | Use `Files.createDirectories`, `Files.delete`, staged writes, and explicit result handling. Report failure accurately. |
| Q13 — **Medium** | `EclipseThemesPreferencePage` | UI and domain/application policy are entangled. Changes to import/reset/apply behavior require exercising SWT code. | Extract an application coordinator and page-state/presentation model. Keep widget construction and event wiring in the page. |
| Q14 — **Low** | `Theme`, `Token`, `TokenKey`, activator lazy fields | `Theme` remains mutable after parsing; unmodifiable map getters return live views. Public `Token` constructor allows null required fields. `TokenKey.register()` can silently replace global keys. Lazy manager initialization has no synchronization or thread contract. | Freeze validated themes; expose immutable snapshots; validate constructors; reject duplicate token registrations. Document UI-thread confinement or provide synchronization. |
| Q15 — **Low** | `ParserFactory`; page search; `ThemeType` | Case-sensitive `.xml` lookup contradicts repository filtering. Locale-sensitive case conversions affect searching/classification. Constant-holder interfaces and mixed formatting add small maintenance friction. | Normalize extensions and machine identifiers with `Locale.ROOT`; use static utility classes where appropriate; apply a consistent formatter. |

### Important detail about Q1

Removing a preference override does **not necessarily restore a factory default**. Eclipse CSS can modify defaults, and product customization can supply defaults too.

The TODO suggestion involving `defaultValueBeforeOverriddenFromCSS` is not a replacement for an ownership journal: it concerns CSS’s default-value bookkeeping, not a complete snapshot of user settings. [Upstream preference helper](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.ui/master/bundles/org.eclipse.e4.ui.css.swt/src/org/eclipse/e4/ui/css/swt/helpers/EclipsePreferencesHelper.java)

## 2. Method complexity and side effects

No cyclomatic-complexity tool was run, so I will not invent numeric scores.

The main review targets are:

| Method | Assessment |
|---|---|
| `XmlThemeParser.parse()` | Approximately 70 lines with metadata extraction, fallback policy, alias conversion, validation, style parsing, logging, and exception wrapping |
| `ThemeRepository.importTheme()` | File mutation and domain validation interleaved; destructive ordering is the central problem |
| Page `applySelectedTheme()` | Persistence, application, UI state, list refresh, and timed feedback in one method |
| Page `performDefaults()` | A normally reversible preference action initiates immediate mutations |
| Adapter `apply()` methods | Some are long, but mostly declarative mapping lists; length alone is not the problem |
| `Theme.get()` | Small and understandable fallback traversal |

The existing style/color helpers are useful extraction. The next extraction should be **declarative preference changes**, which enable ownership, preview, rollback, and testing—not a generic plugin framework.

## 3. Error handling

Positive: repository and registry failures generally use `ILog`; file streams preserve underlying exceptions.

Gaps:

- Dark-mode detection contains empty catch blocks.
- Apply/remove page catches can surface a message without logging the exception.
- Import collapses multiple causes into `Optional.empty()`.
- Parser warnings disappear into logs while the UI declares success.
- Wrong adapter type is skipped without a diagnostic.
- Browser load failure is not comprehensively observed.

**Severity: Medium. Location:** parser/repository/service result contracts and page error handlers.  
**Recommended fix:** return structured diagnostics and `IStatus`/`MultiStatus` at Eclipse boundaries. Surface actionable failures through the page or `StatusManager`, retaining the cause and affected adapter/file/key.

`Optional` is appropriate for a lookup that might not find a theme. It is inadequate for an operation that can fail in several meaningful ways.

## 4. Threading

I found **no demonstrated background-thread SWT access violation in the current call paths**. Widget interactions originate in preference-page UI callbacks, and `timerExec` checks disposal.

The actual problem is blocking work on the UI thread.

Adding `asyncExec` around a parse operation would not help—it would still parse on the UI thread. Run I/O off-thread, then use `asyncExec` for the resulting UI update. Also avoid moving preference writes blindly: preference listeners may have UI assumptions.

## 5. Resource management

No obvious SWT `Image`, `Color`, or `Font` leak was found:

- The project’s `Color` is an RGB value record, not an SWT resource.
- JFace-managed fonts and system colors are borrowed correctly.
- Child widgets are disposed with their parent.
- Repository/parser file streams and `Files.list()` use try-with-resources.

The `Browser` introduces native runtime dependencies, but that is primarily a resilience/product issue rather than a confirmed resource leak.

## 6. Tests

**No automated test sources or test modules are present.** No measured coverage report exists.

| ID / Severity | Location | Problem | Recommended fix |
|---|---|---|---|
| Q16 — **High** | Reactor and source tree | Import, reset, identity, mapping, and application behavior have no regression protection. `mvn verify` cannot exercise tests that do not exist. | Add focused core/repository tests, adapter contract tests, and a small Tycho runtime integration suite. |

Minimum meaningful coverage:

1. External entities/DOCTYPE rejection and input limits.
2. Legacy identity migration, duplicate IDs/names, edited revisions.
3. Failed imports preserving old files; same-file and same-basename imports.
4. Exact mapping behavior and untouched unrelated preferences.
5. Theme A → B → disable restoring originals, including absent values.
6. Adapter failure aggregation and rollback.
7. Apply/OK/Cancel/defaults semantics.
8. Real JDT/CDT/PDE/Ant listeners and CSS interactions.

The core is testable after removing activator logging. The UI needs extracted policy/state. Actual SWT/native rendering requires runtime tests; mocks alone cannot establish it.

---

# PART 3 — BUILD, CI, AND RELEASE AUDIT

## 1. Tycho/Maven and target platform

Current configuration:

- Tycho **5.0.0**.
- Java source/target and bundle execution environment **17**.
- CI runs on JDK **21**.
- Target: Eclipse **2024-09 / 4.33**.
- Target includes Platform, RCP, JDT, and PDE.
- Declared environments cover Windows/Linux x86-64 and macOS x86-64/AArch64.

The build JVM and bundle execution environment being different is valid. The documentation is the problem: Tycho 5 requires **JDK 21 and Maven 3.9.9**, while contributing instructions say JDK 17/Maven 3.9.0. [Tycho requirements](https://tycho.eclipseprojects.io/doc/5.0.0/tycho-eclipse-plugin/plugin-info.html)

As of October 2026, the current release is Eclipse **2026-09 / 4.41**. The existing baseline is eight quarterly releases behind; it remains useful as an older compatibility target, but is insufficient as the only validation target. [Official release schedule](https://github.com/eclipse-simrel/.github/blob/main/wiki/Simultaneous_Release.md)

## 2. CI coverage

The workflow:

- Builds on pushes to `main`/`develop`.
- Builds PRs targeting `main`.
- Builds on release `created`.
- Runs `mvn clean verify`.
- Uploads the generated p2 repository.
- Deploys main/release artifacts to `gh-pages`.
- Attaches a ZIP for release-triggered runs.

It does **not** establish working SWT behavior on the declared operating systems. Those target environments are resolution/build configuration, not an OS test matrix.

### Build and release findings

| ID / Severity | Location | Problem | Recommended fix |
|---|---|---|---|
| B1 — **Medium** | [CONTRIBUTING.md](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/CONTRIBUTING.md), root POM | Documented prerequisites are below Tycho’s actual requirements; no Maven wrapper or enforced prerequisite check exists. | Add a pinned Maven wrapper and Enforcer requirements. Document build JDK separately from supported runtime Java. |
| B2 — **Medium** | Target definition | Uses release repository root and IU versions `0.0.0`; resolution is not fully pinned. CDT is absent, so C++ integration is not exercised. | Pin a release-specific repository and resolved IU versions; add CDT to an integration-test target. Add current-release compatibility validation. |
| B3 — **High** | [build.yml](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/.github/workflows/build.yml:34), reactor | Build verification has no automated behavioral tests or p2 install smoke test. | Add the Q16 suites and verify installation into a clean Eclipse runtime. |
| B4 — **High** | Workflow release trigger, lines 8–9 | `release: created` is unreliable for the documented draft-then-publish release flow. GitHub does not trigger `created` workflows for draft releases. | Use `release: published`, with an explicit stable/prerelease policy. [GitHub release-event documentation](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#release) |
| B5 — **High** | Workflow deployment condition, line 47 | Every main push can publish to the same site as releases. Unreleased builds and tagged releases share a channel; overlapping deployments lack serialization. | Separate snapshot/stable sites. Publish stable only from validated release artifacts. Serialize deployments and prevent older releases from replacing the current stable index. |
| B6 — **Medium** | POMs, manifests, feature version, local HOWTO | Version updates are manual across the reactor. HOWTO describes changing three locations but child parent versions also need consistency. No tag/version validation exists. | Use Tycho version tooling for the whole reactor and CI checks for Maven/OSGi/feature/tag consistency. |
| B7 — **Medium** | POMs and packaging | Qualifiers/tooling/timestamps are not configured for reproducible rebuilds. No artifact comparison demonstrates byte-for-byte reproducibility. | Fix build inputs and qualifier strategy, configure supported reproducible archive settings, and compare two builds. Treat signing timestamps separately. |
| B8 — **Medium** | Workflow/POMs | No configured artifact signing, signing verification, SBOM, or preserved release inventory. | Add signing if enterprise distribution warrants it; otherwise state unsigned status. Publish checksums and a dependency/artifact inventory. Preserve immutable release repositories. |
| B9 — **Medium** | Workflow permissions/action references | Global `contents: write` is broader than the build job needs. Actions are pinned to moving major tags. PRs targeting `develop` have no matching PR trigger. | Scope write permissions to publish jobs, pin action SHAs with update automation, and validate every accepted PR target. |
| B10 — **Low** | Workflow cache key | Cache key hashes POMs but omits target definitions and runtime/toolchain identity. | Include target files and relevant toolchain inputs; retain conservative restore behavior. |
| B11 — **Medium** | `build.properties`, schema, licensing metadata | Adapter schema and root license are not explicitly included in the runtime bundle. Schema documentation is largely unfinished. | Package the schema/API docs and appropriate license/notices. Verify actual artifact contents in CI. |

## 3. Release process assessment

The intended process is manual version edits → main push → GitHub release → rebuild → p2 deployment/ZIP.

Important consequences:

- A main push and a release can build the same source separately with different qualifiers.
- The stable site can contain a main build before a release is published.
- No automated assertion connects tag, commit, semantic version, and released artifact.
- No versioned/composite repository policy is configured.
- The local HOWTO is ignored by Git and is not part of a clean clone.

A better release process is:

```text
Release commit/tag
  → validate versions and clean source
  → build once
  → run tests and p2 install smoke test
  → sign/verify if configured
  → publish immutable versioned repository
  → update stable composite/index
  → attach those same artifacts and checksums
```

## 4. Reproducibility answer

**Can a clean clone build?** Plausible, but unverified here.

**Can it build identically?** Not established, and the current configuration does not provide a convincing byte-identical rebuild story.

Do not interpret this as “the build is broken.” The evidence supports **missing guarantees**, not a reproduced Tycho failure.

## 5. Missing tooling

None of the following is configured in the inspected repository:

| Tool | Priority / value |
|---|---|
| JUnit + Tycho runtime tests | Highest priority |
| Checkstyle or a formatter check | Useful for predictable contributions |
| SpotBugs | Useful for nullability, lifecycle, and suspicious code |
| JaCoCo | Useful once meaningful tests exist |
| Dependabot or Renovate | Useful for Maven plugins and Actions |
| Dependency/license inventory and scanning | Useful; must account for p2, not just Maven dependencies |
| PMD | Optional; avoid noisy rules duplicating compiler/SpotBugs checks |
| Error Prone | Optional; integration requires care because Tycho normally uses ECJ |
| OWASP Dependency-Check | Optional layer; Maven-only scanning would miss important target-platform artifacts |

Install a small set that produces actionable results. More tools will not compensate for missing transaction and ownership design.

---

# PART 4 — UX / PRODUCT AUDIT

## 1. Preference page

Positive:

- Located under Appearance.
- Search, type filtering, metadata, and import/remove are useful.
- README installation and basic usage instructions are straightforward.

### UX findings

| ID / Severity | Location | Problem | Recommended fix |
|---|---|---|---|
| U1 — **High** | [updatePreview](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/preferences/EclipseThemesPreferencePage.java:477) | “Live Preview” loads a remote page using the theme ID. It does not render the parsed XML or adapter output. Imported/edited themes can display missing or stale previews. | Render a local preview from the actual resolved theme and adapter mappings. Prefer a JFace/SWT source viewer for editor fidelity. |
| U2 — **Medium** | `createPreviewPanel()`, `updatePreview()` | Browser construction is unconditional and has no fallback. `setUrl()` result is ignored; catching exceptions does not detect asynchronous HTTP/load failures. Preview depends on browser backend and network availability. | Make preview optional, resilient, and offline. Handle construction/load failure independently of the theme list. |
| U3 — **High** | Double-click listener; `performDefaults()`; `performOk()` | Double-click applies immediately; Restore Defaults mutates immediately; Cancel does not restore those mutations. Ordinary selection is staged, so the page mixes incompatible interaction models. | Define one policy: stage changes until Apply/OK, or provide explicit temporary preview with rollback. Document import/remove as immediate operations if they remain so. |
| U4 — **Medium** | [PreferenceInitializer](C:/Users/cw/eclipse-workspace-plugins/eclipse-themes-plugin/bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/preferences/PreferenceInitializer.java:14); `initializeData()` | Default `ACTIVE_THEME_ID` is a display name, but lookup expects an `ect-id`. Initial list selection and details can disagree because details are then updated with `currentTheme=null`. | Store a real stable ID or an explicit unmanaged state. Initialize selected/current state consistently. |
| U5 — **Medium** | Import dialog and parser diagnostics | Import errors are generic; warnings are hidden; success can be reported for partially accepted XML. Existing filters can hide the newly imported item. | Show validation diagnostics before commit, an exact preview, conflict choices, and the resulting visible item. |
| U6 — **Medium** | Toolbar/details/list/status labels, fixed sizing | Symbol-only current/imported indicators, transient success text, missing mnemonics, fixed width hints, and hardcoded gray text create accessibility gaps. | Add meaningful accessible descriptions, keyboard actions/mnemonics, durable operation feedback, adaptable sizing, and high-contrast validation. |
| U7 — **Medium** | All UI code, `plugin.xml`, feature metadata | UI strings are hardcoded; no messages bundle or plugin localization files exist. | Externalize strings with Eclipse NLS and `%` metadata references. Separate model IDs from translated labels. |
| U8 — **Medium** | `websiteLink` and `Program.launch()` | Imported metadata is inserted into link markup and launched without scheme validation. Unsupported/local/custom URLs can be opened; launch failure is ignored. | Escape display text, allow intended web schemes, validate URIs, and handle launch results. |
| U9 — **Medium** | README/feature support claims; adapter list | “XML” support is PDE/Ant-specific, not a general WTP XML integration. C++ support is not represented in the build target. | Publish an exact tested editor compatibility matrix and expose installed/skipped adapter status in the UI. |

### Apply/Cancel nuance

Not overriding `performApply()` is **not itself a defect**: JFace’s default implementation calls `performOk()`. The issue is that application failure does not propagate and several other actions mutate preferences before either button. [JFace implementation](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.ui/master/bundles/org.eclipse.jface/src/org/eclipse/jface/preference/PreferencePage.java)

## 2. Accessibility and contrast

Native SWT controls provide a useful accessibility baseline, but that is not an accessibility audit pass.

Unverified areas requiring actual testing:

- Screen-reader interpretation of `●` and `*`.
- Focus order and keyboard-only import/apply/remove.
- Announcement of transient status.
- High-contrast mode and dark/light system colors.
- Large fonts, display scaling, and translated text.
- Browser preview accessibility.

Bundled themes are accepted without contrast analysis. Some light-theme muted colors are visibly aggressive—for example Jupyter’s `#aaaaaa` line numbers on white. More seriously, the hover/selection alias bug can make selection background equal to normal background.

For a customization product, accessibility should be a first-class capability: contrast diagnostics, visible selection/focus guarantees, and an option to preserve OS high-contrast behavior.

## 3. Onboarding

Basic installation is adequately explained. First-time operation needs:

- A reliable initial selection.
- Clear distinction between preview, selected theme, and applied theme.
- A truthful list of supported editors.
- An offline preview.
- A safe “stop managing my colors” operation.

The current broken default ID and misleading reset behavior undermine an otherwise simple onboarding flow.

---

# PART 5 — FEATURE REQUEST: “THEME THE ENTIRE ECLIPSE IDE”

## A. Eclipse’s theming landscape

### Three mechanisms must be coordinated

**1. Workbench CSS**

`org.eclipse.e4.ui.css.core` parses/styles; SWT integration maps supported properties to widgets; `org.eclipse.e4.ui.css.swt.theme` manages available themes. Built-in light/dark themes are registered with platform-specific stylesheets. `.css`/`.e4css` are resources, not a new universal styling format. [Theme extension point](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/extension-points/org_eclipse_e4_ui_css_swt_theme.html), [Built-in registrations](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.ui/master/bundles/org.eclipse.ui.themes/plugin.xml)

**2. Workbench color/font definitions**

`org.eclipse.ui.themes` and `IWorkbench.getThemeManager()` manage named color/font definitions and registries consumed by participating components. This is related to, but distinct from, the CSS theme engine. [Workbench themes](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/extension-points/org_eclipse_ui_themes.html)

**3. Editor-specific preferences**

JDT, CDT, PDE, WTP, TM4E, and other editors have their own highlighting machinery. CSS also has preference styling support, so CSS and your adapters can touch overlapping settings. [Built-in preference stylesheet](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.ui/master/bundles/org.eclipse.ui.themes/css/dark/e4-dark_preferencestyle.css)

The e4 application model describes windows, parts, stacks, toolbars, and contributions. Renderers turn that model into SWT widgets. Styling depends on those widgets, their IDs/classes, handlers, and renderer behavior; there is no “theme everything” field in the model.

### CSS versus Java versus limits

| Surface | Feasible approach | Limit |
|---|---|---|
| View/editor stacks, workbench backgrounds, many labels/trees/tables | CSS | Actual coverage depends on widget handlers and component behavior |
| Tabs, selected tabs, keylines, supported renderer properties | CSS and renderer integration | Renderer-specific behavior; not arbitrary browser styling |
| Toolbars/status areas | CSS where supported | Native item rendering and third-party implementations vary |
| Editor syntax | Editor adapters, TM4E/TextMate integration | Requires editor-specific contracts |
| Registered fonts/colors | Workbench registries/preferences | Components must consume them |
| Dialog contents | CSS/Java integration where styled | Not every plugin-created shell participates uniformly |
| Welcome/help/browser content | Its own HTML/CSS/provider integration | SWT CSS does not style HTML inside Browser |
| Icons owned by this plugin | Public image/model APIs | Manage lifecycle, scaling, and refresh |
| All third-party icons | Cooperation, targeted integrations, or invasive interception | No universal public replacement contract |
| Native menus, file dialogs, title bars, OS chrome | OS appearance and limited platform APIs | Cannot guarantee arbitrary theme colors |

Eclipse CSS is not browser CSS. Supported properties map to specific SWT methods and handlers; custom properties can be contributed through extension mechanisms. Do not assume modern web CSS constructs work across your baseline. [Platform CSS documentation](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.ui/master/docs/CSS.md)

### Platform limits

- **Windows:** native controls, non-client chrome, and cached system resources can behave differently from custom-drawn SWT controls.
- **GTK:** GTK theme/rendering remains relevant. SWT CSS is not a replacement GTK stylesheet.
- **GTK3 versus GTK4:** treat them as separate tested backends. SWT announced opt-in GTK4 support after your 2024-09 baseline; that does not prove equivalent styling coverage. [SWT GTK4 announcement](https://www.eclipse.org/lists/cross-project-issues-dev/msg19986.html)
- **Wayland versus X11:** test both where supported. They are display backends, not extra Eclipse CSS layers. Native popups/browser integration can expose backend-specific problems.
- **macOS:** system menu bar, native dialogs, and application appearance constrain customization. Do not promise identical colors to Windows/Linux.
- **High contrast:** preserve the user’s accessibility choice rather than forcing a theme over it.

The exact GTK4/macOS/native behavior for a proposed feature remains unverified until tested on the chosen Eclipse versions.

### Comparable solutions

| Solution | Verified approach / scope | Lesson |
|---|---|---|
| Eclipse Color Theme | Maps generic editor-theme data to editor-specific preferences | The adapter approach is established, but does not provide universal workbench styling. [Project README](https://github.com/eclipse-color-theme/eclipse-color-theme) |
| DevStyle / Darkest Dark | Vendor advertises workbench themes, editor themes, icons, previews, and sharing | It already occupies much of the proposed product scope. I did not verify its proprietary implementation details and will not invent its internal hooks. [Vendor description](https://www.genuitec.com/products/devstyle/) |
| BetterThemes | JSON editor themes, variables/CSS colors, legacy import; explicitly uses a `WeavingHook` for an editor-background problem | Some fidelity problems have motivated invasive compatibility work. Its README still lists UI/icon theming as TODO. [Project README](https://github.com/TheKodeToad/BetterThemes) |
| “Material Theme for Eclipse” | I could not identify a unique authoritative project matching that exact name | A precise repository/product reference is needed for an honest implementation comparison |
| Vogella Eclipse themes / Gruvbox Material | Current project describes coordinated workbench, syntax, and console colors, with contrast adjustments | A relevant current competitor/reference. It should not be conflated with the ambiguous Material Theme name. [Maintainer article](https://www.vogella.com/blog/eclipse-gruvbox-material-theme/) |

## B. Proposed unified architecture

### Product promise

Promise **one place to manage supported Eclipse appearance surfaces safely**, with an explicit coverage matrix.

“Every pixel of every plugin on every operating system” is not a credible public-API promise.

### Bundle design

```text
com.github.eclipsethemes.core
  immutable models, validation, legacy conversion, palette resolution

com.github.eclipsethemes.api
  versioned contributor contracts and change-plan model

com.github.eclipsethemes.runtime
  catalog, installation, persistence, ownership journal, coordinator

com.github.eclipsethemes.ui
  preference page, local preview, diagnostics, recovery

com.github.eclipsethemes.workbench
  CSS theme registration, theme-engine integration, color/font registries

com.github.eclipsethemes.editor.*
  optional JDT/CDT/PDE/Ant/WTP/TM4E integrations

com.github.eclipsethemes.themes.*
  bundled, declarative theme packages
```

Start with fewer physical bundles if needed, but enforce these dependency directions:

```text
UI → coordinator → contributors
              ↓          ↓
           persistence   public Eclipse APIs
              ↓          ↓
                 core/API
```

The core must never call the activator, SWT, or preference stores.

### Theme package model

Use a versioned declarative archive, for example `.eclipsetheme`:

```text
theme.json
editor.json
workbench/
  base.css
  win32.css
  gtk.css
  cocoa.css
icons/
licenses/
```

A package should identify:

| Field | Purpose |
|---|---|
| Schema version | Controlled format evolution |
| Stable namespaced ID | Identity independent of filename/display name |
| Package version/content digest | Distinguish revisions |
| Label, author, source URL | Presentation and provenance |
| Licenses/notices | Asset rights |
| Appearance | Light/dark/high-contrast metadata |
| Compatibility/capabilities | Supported platforms, Eclipse versions, editor integrations |
| Semantic palette | Background, foreground, accent, selection, error, warning, etc. |
| Editor tokens | Syntax and annotation roles |
| Workbench resources | CSS or supported base-theme reference |
| Optional fonts/icons | Explicit opt-in components |

**Keep Java code out of imported theme archives.** Executable integrations should be separately installed OSGi bundles through p2.

For imported packages, validate archive paths, size/count limits, resource references, and CSS resource schemes. Arbitrary remote imports or executable `bundleclass` references should not become implicit theme capabilities.

### Application contract

Adapters should produce a **change plan**, not immediately mutate preferences:

```text
ThemeApplicationPlan
  preference changes:
    node + key + value/type + contributor + ownership policy
  workbench theme selection
  supported color/font changes
  refresh actions
  warnings / skipped integrations
  restart requirements
```

Coordinator sequence:

1. Resolve and validate the package off-thread.
2. Determine supported components and detect conflicts.
3. Snapshot owned settings, including whether each key existed.
4. Persist a recovery journal.
5. Switch the workbench CSS theme on the UI thread.
6. Apply editor/font contributors in deterministic order.
7. Refresh supported surfaces and collect failures.
8. Commit active package/revision only after success.
9. Roll back on failure and retain diagnostics if rollback is incomplete.

This is a compensating transaction. Eclipse preference writes and CSS switches are not an ACID transaction, and listeners may observe intermediate states.

### Public CSS integration and switching

Use `IThemeEngine` to register/select themes and apply styles. It provides registration, active-theme lookup, selection with optional persistence, and style application. **The inspected public interface has no unregister-theme/unregister-stylesheet operation.** [IThemeEngine API](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/e4/ui/css/swt/theme/IThemeEngine.html)

That matters:

- Switching registered themes can be live.
- Imported CSS must be registered again during startup before restoring its ID.
- Updating an already registered theme cannot be designed around an imaginary public “replace” API.
- Repeated edit sessions must not accumulate unlimited registrations.
- Deleting a package can disable it immediately, while complete registry/resource cleanup may require restart.

For MVP, use built-in/p2-contributed CSS themes. Treat arbitrary imported workbench CSS and its lifecycle as a later milestone requiring a compatibility spike or upstream API work.

### When restart is unavoidable or reasonable

| Operation | Expected behavior |
|---|---|
| Editor color preference switch | Usually live; verify each editor |
| Registered workbench CSS switch | Usually live; test all relevant shells |
| Declarative editor-only import | No restart |
| New executable integration via p2 | Follow p2 restart requirements |
| Native appearance changes/cached resources | May need window recreation or restart |
| Global icon/renderer changes | Often require recreation/restart; cannot promise universally |
| Complete removal of dynamic CSS registrations | Public API limitation may make restart appropriate |

### Coexistence with Eclipse and DevStyle

Offer component ownership explicitly:

- Editor colors only.
- Workbench only.
- Coordinated theme.
- Fonts/icons separately opt-in.

Observe external theme/preference changes. If another component becomes the owner, show the conflict and offer to yield. **Do not continuously rewrite settings in a listener loop.**

On disable, restore a key only when its current value still matches the last value you wrote. If the user or another plugin changed it, preserve that newer value and report the conflict.

Do not silently disable or uninstall DevStyle.

### Extension points

Introduce small, documented contracts:

| Extension | Responsibility |
|---|---|
| `themePackages` | Contribute declarative package resources |
| `themeContributors` | Produce component change plans |
| `previewProviders` | Render accurate local previews for supported integrations |

Contributor metadata should include ID, capabilities, target bundles/version ranges, dependencies/order, owned keys, refresh behavior, and restart requirements.

A font/icon integration can initially be a contributor capability; avoid creating an extension point for every possible future feature.

### Persistence

Store:

- Package ID **and revision**.
- Enabled components.
- Previous workbench theme selection.
- Original preference values and absence.
- Last-written values for conflict-aware restoration.
- Transaction/recovery status.
- Package provenance and validation result.

Use workspace state for journals/packages initially. Keep e4 context for live services, not durable user state. Cross-workspace sharing can be a later explicit feature.

### Migration

1. Continue accepting legacy XML as editor-only packages.
2. Preserve valid `ect-id` values.
3. Store legacy `id` separately.
4. Generate stable persisted IDs where needed; do not use display names.
5. Resolve duplicate identities/names explicitly.
6. Migrate the stored active theme ID.
7. Leave existing workbench appearance unchanged until the user enables coordination.
8. Make clear that historical pre-plugin colors cannot be reconstructed reliably because no snapshot exists.

## C. Feasibility and risks

| Risk / Severity | Location | Problem | Recommended response |
|---|---|---|---|
| F1 — **High** | Whole-IDE scope | Native widgets, HTML content, custom-drawn third-party views, and OS chrome have different styling systems | Publish supported coverage and platform exceptions; avoid universal claims |
| F2 — **High** | Dynamic CSS package lifecycle | Public engine registration lacks complete removal/replacement operations | Limit MVP scope, test lifecycle early, and pursue upstream APIs before relying on internals |
| F3 — **High** | Preference/CSS coordination | Multiple mechanisms can modify the same preferences and defaults | Ownership journal, deterministic sequencing, conflict detection, and runtime tests |
| F4 — **High** | Global icons/renderers | Universal replacement would require cooperation or invasive mechanisms | Start with owned/participating icons; isolate any experimental internal integration |
| F5 — **Medium** | Version/platform matrix | CSS handlers, renderer details, native behavior, and editor keys evolve | Separate core format support from integration support; continuously test a bounded matrix |
| F6 — **Medium** | Theme assets/licensing | Existing theme files lack consistent provenance; Jupyter/Vitesse files include EPL-1.0 notices while the project declares EPL-2.0 | Inventory source/license per asset, retain notices, and obtain rights before redistribution. Do not assume repository licensing overrides upstream asset licenses. |

### Public APIs versus invasive implementation

**Realistically achievable through public mechanisms:**

- Unified catalog, package management, local previews, import/export.
- Editor preference integration.
- Registered workbench CSS themes.
- Participating color/font registries.
- Owned or cooperating icon contributions.
- Live switching across tested surfaces.

**Potentially requires internals, weaving, or platform changes:**

- Arbitrary global icon interception.
- Fixing editors that ignore supported preferences.
- Complete dynamic stylesheet/theme replacement/removal.
- Uniform restyling of non-participating third-party widgets.
- Aggressive native chrome customization.

I would keep such work outside the stable default path and upstream missing capabilities where possible.

### Supported Eclipse versions

Recommended starting policy:

- Continue editor-only compatibility testing against **2024-09 / 4.33**.
- Develop full-workbench support against **2026-09 / 4.41**, validating at least **2026-06 / 4.40** as a second target.
- Expand backward only after the CSS lifecycle spike succeeds.
- Publish separate editor/workbench compatibility statements.

This is a proposed support policy, **not proof that the current code works on those versions**.

### Licensing

The root EPL-2.0 license is a reasonable project license, but it does not automatically grant rights to copied icons, fonts, CSS, or third-party theme files. Preserve required notices and source-availability obligations for distributed EPL components. [EPL-2.0 text](https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.txt)

There is insufficient provenance here to conclude infringement. There is enough uncertainty to require an asset inventory before expanding distribution.

## D. Roadmap

Estimates assume one experienced Eclipse maintainer, bounded editor coverage, and access to Windows/Linux/macOS testing.

| Milestone | Scope | Effort | Main risks | Definition of done |
|---|---|---|---|---|
| **Foundation** | Safe reset, secure parser, identity migration, atomic imports, mapping fixes, result reporting, regression tests | **M — 2–4 weeks** | Existing state lacks originals; migration ambiguity | Invalid imports preserve old files; unrelated preferences survive apply/reset; failures cannot report success |
| **MVP** | Unified selector for existing workbench themes plus editor packages; offline preview; component opt-in; ownership journal | **M — 3–5 additional weeks** | CSS/editor interaction, rollback | Switch editor + workbench live on the tested matrix; disable restores owned settings; external changes are preserved |
| **v1** | Versioned package format, curated coordinated CSS themes, contributor API, selected WTP/TM4E integrations, accessibility and release hardening | **L — 6–10 additional weeks** | Compatibility surface, community API stability | Accurate coverage/diagnostics, restart-safe restoration, documented authoring/API, reliable releases |
| **v2** | Workbench CSS authoring/import, participating icon packs, broader integrations, export/share, OS-following profiles | **L — 3–6+ additional months** | Dynamic lifecycle APIs, icon/native limits, maintenance | Bounded resource lifecycle, published compatibility guarantees, recovery tests, sustainable ownership |

A universal icon/native-widget layer is a separate research project, not a routine v2 checkbox.

---

# PART 6 — STRATEGIC POSITIONING

## 1. Realistic blockers

| Severity | Location | Blocker | Recommended response |
|---|---|---|---|
| **High** | Product trust | A customization plugin that can erase unrelated settings cannot become canonical | Make preservation, recovery, and predictable behavior the first differentiator |
| **High** | Market positioning | DevStyle already offers much of the proposed feature set; built-in and community themes continue improving | Compete on safety, openness, portability, fidelity, and coexistence |
| **High** | Maintenance model | Broad editor/OS/version support creates a continuing integration workload | Publish a bounded matrix, recruit integration owners, and fund release testing |
| **Medium** | Distribution/discovery | Marketplace presence alone does not establish adoption or retention | Improve exact naming, screenshots, compatibility descriptions, release notes, and install reliability |
| **Medium** | Upstream adoption | “Canonical” requires other Eclipse projects/products to participate | Provide useful APIs and upstream fixes; seek collaboration rather than unilateral platform replacement |

I found no evidence of an Eclipse Foundation policy blocking this project. “Politics” should not be used as an explanation without evidence.

Foundation-level adoption would require maintainers to trust the engineering, licensing, governance, compatibility policy, and ongoing support. That is a substantially higher bar than a Marketplace listing.

Community size, active installations, retention, and available funding remain unknown. README badges are not enough to assess them.

## 2. Differentiated features that would move the needle

In priority order:

1. **Safe ownership and one-click recovery.**
2. **Offline previews generated from actual local data.**
3. **Portable coordinated theme packages with provenance.**
4. **An honest compatibility/coverage inspector.**
5. **Accessibility diagnostics and high-contrast preservation.**
6. **Independent mixing of syntax, workbench, fonts, and participating icons.**
7. **Modern editor support, especially TM4E/LSP-based workflows.**
8. **An authoring tool that validates mappings and previews real Eclipse output.**

More bundled palettes are easy to imitate. Reliable coordination across Eclipse’s separate systems is more valuable.

## 3. What must be true for success

**Technically:** theme changes are reversible, package identities are stable, previews are faithful, integrations are tested, and unsupported surfaces are clearly identified.

**Socially:** contributors can implement an adapter without reading the whole application; releases are predictable; multiple maintainers own important integrations; upstream relationships exist.

**Legally:** redistributed assets have documented provenance, licenses, and notices; imported themes do not execute arbitrary code; branding does not imply unearned Eclipse endorsement.

**Operationally:** the project can maintain its promised matrix over multiple quarterly Eclipse releases.

Becoming widely trusted is achievable. Becoming the universal default is an adoption outcome, not an architecture feature.

## 4. What I would do in the next 90 days

| Period | Work | Exit criteria |
|---|---|---|
| **Days 1–14** | Fix reset, XML security, identity, atomic import, false success, and mapping defects | Regression tests reproduce the findings and pass; patch release prepared |
| **Days 15–30** | Ownership journal, immutable validated models, offline preview, correct dialog semantics, release workflow repair | Reliable editor-only release with safe disable/reset |
| **Days 31–60** | Public-API workbench integration spike; bounded support matrix; CSS lifecycle investigation | Evidence for live switch/revert on Windows, Linux, macOS; explicit unsupported surfaces |
| **Days 61–90** | Ship coordinated-theme beta with curated packages, diagnostics, documentation, contributor example | MVP works with clean install, restart, external theme changes, and recovery scenarios |

In parallel, I would discuss the design with Eclipse Platform/theme maintainers and seek owners for editor integrations. These are recommended actions, not messages sent on your behalf.

---

# Top 10 things to fix/do next

1. **Critical — replace whole-node preference clearing** with theme-owned restoration. Q1.
2. **High — secure XML parsing** and reject external entities/DOCTYPE. Q2.
3. **High — establish stable theme identity and revision tracking**, including legacy migration. Q3.
4. **High — make imports atomic and conflict-consistent** across import/reload. Q4–Q5.
5. **High — correct mapping corruption:** debug indication keys, hover/selection aliases, and style collisions. Q6–Q8.
6. **High — make application outcomes transactional and truthful**, including OK failure handling. Q9.
7. **High — add behavioral and runtime integration tests** before expanding scope. Q16/B3.
8. **High — repair release publication:** `published` event, separate channels, immutable artifacts, serialized deployment. B4–B7.
9. **Medium — deliver an offline, accurate preview and coherent Apply/Cancel/reset behavior.** U1–U5.
10. **Medium — build the full-IDE MVP around public APIs, explicit ownership, and a bounded coverage promise**, then expand only where compatibility evidence supports it.