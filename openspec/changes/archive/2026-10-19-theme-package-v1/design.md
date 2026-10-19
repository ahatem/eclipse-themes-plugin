# Design — Theme Package v1

Status: approved 2026-10-19. The maintainer accepted the recommended option for every PM item (PM1–PM8 = A); this design implements
exactly those options.

## 1. Flow

```text
legacy  *.xml     -> XmlThemeParser (unchanged)           -> Theme (no package metadata, no authored workbench)
package *.ectheme -> ThemePackageParser                   -> Theme (+ PackageMetadata, + authored workbench map)
                       ├─ SecureXmlBuilder (shared, unchanged)
                       └─ editor children -> LegacyTokenInterpreter (extracted verbatim from XmlThemeParser)
both              -> ThemeRecord(local:<uuid> | bundled:<ect-id>) -> ThemeService
                       ├─ WorkbenchCoordinator (only if the user enabled coordination) -> WorkbenchPaletteResolver (authored first)
                       └─ editor adapters (unchanged; receive Theme)
```

`ParserFactory.getParserFor(fileName, sink)` dispatches on the final extension (`.xml` → legacy, `.ectheme` → package), using
`Locale.ROOT`. No content sniffing.

## 2. Document contract (v1)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<themePackage schemaVersion="1"
              id="io.github.example.nightfall"
              name="Nightfall"
              appearance="dark"
              author="Jane Doe"
              version="2.1"
              description="A cool blue dark theme."
              website="https://example.github.io/nightfall">
  <provenance license="MIT" copyright="© 2026 Jane Doe" source="https://github.com/example/nightfall"/>
  <editor>
    <background color="#1b1f2a"/>
    <foreground color="#c8d0e0"/>
    <keyword color="#82aaff" bold="true"/>
    <singleLineComment color="#5c6773" italic="true"/>   <!-- legacy alias accepted -->
  </editor>
  <workbench>
    <surface color="#222838"/>
    <accent color="#82aaff"/>
  </workbench>
</themePackage>
```

### Root attributes

| Attribute | Class | Rule | Finding |
|---|---|---|---|
| `schemaVersion` | REQUIRED | ASCII decimal integer; only `1` accepted | absent/non-integer → ERROR `schema-version`; integer ≠ 1 → ERROR `unsupported-schema-version` ("requires a newer Eclipse Themes") |
| `id` | REQUIRED (PM3) | `[a-z0-9]+(\.[a-z0-9][a-z0-9-]*)+`, ≤ 128 chars | ERROR `package-id` |
| `name` | REQUIRED | non-blank | ERROR `name` (existing kind) |
| `appearance` | REQUIRED | `dark` or `light`, case-insensitive | absent or other → ERROR `appearance` (no light default, unlike legacy `type`) |
| `author` | OPTIONAL | free text | – |
| `description` | OPTIONAL | free text | – |
| `website` | OPTIONAL | existing `WebsiteUri` rule | WARNING `website` (existing) |
| `version` | OPTIONAL | free display text ≤ 64 chars, never compared | over length → WARNING `version`, value dropped |
| anything else | – | ignored silently (legacy parity) | – |

Not v1: `compatibility`/min-version (schemaVersion covers the format; product version gating has no consumer), `license` on root
(lives in `<provenance>`), digest, revision, legacy `ect-id` bridging.

### Elements

| Element | Cardinality | Rule |
|---|---|---|
| `<provenance>` | 0..1 | attributes `license` (SPDX expression text, not validated against the SPDX list; blank → WARNING `license`, dropped), `copyright` (holder text; distinct from `author`), `source` (upstream URL, same `WebsiteUri` rule, launch-checked separately from `website`). Duplicate → ERROR `duplicate-section`. |
| `<editor>` | 1 | children interpreted *exactly* as legacy `colorTheme` children (aliases, canonical precedence, last-wins, all existing warnings). Must yield explicit `background` and `foreground` (PM4) → else ERROR `required-editor-token`. No PM-V7 root defaults are injected. Missing → ERROR `missing-section`; duplicate → ERROR `duplicate-section`. |
| `<workbench>` | 0..1 | children named `background, foreground, surface, surfaceRaised, border, mutedForeground, selection, accent`, each with a `color` attribute in the legacy colour grammar. Unknown role → WARNING `unknown-workbench-role`; repeated role → ERROR `duplicate-workbench-role`; missing/invalid colour → WARNING (existing `missing-color`/`invalid-color` kinds) and role treated as not authored. Style attributes are ignored. Duplicate section → ERROR. |
| any other direct child | – | WARNING `unknown-element`, ignored (forward-compat for e.g. a future `<fonts>`). |
| DOCTYPE, external entities, XInclude | – | refused by the shared `SecureXmlBuilder` exactly as legacy. |

Text content and comments are ignored. Element names are case-sensitive (XML). Document order of sections is free.

## 3. Schema evolution rules

| Situation | Behaviour |
|---|---|
| `schemaVersion` > 1 | **reject** with `unsupported-schema-version`; no partial admission. |
| unknown optional element (root child or inside `<workbench>`) | **ignore with warning**; the exact bytes stay in the managed file, so the information is **preserved** on disk and returns when a newer reader loads it. |
| unknown attribute | **accept** silently. |
| future required capability | not expressible in v1: a feature an old reader must not ignore *must* bump `schemaVersion`. No capability list is introduced. |
| a later optional field omitted | **accept**; behaviour equals v1. |

## 4. Identity matrix

| Concept | Created by | Can change | Used for | Persisted | User-visible | In equality |
|---|---|---|---|---|---|---|
| package `id` | author | yes, per file | diagnostics; future update suggestions (D3) | only inside the file bytes | details pane | no |
| theme visual identity (`Theme` object) | parser | new object per parse | adapters, resolver | no | no | identity only |
| legacy `ect-id` | legacy author | yes | legacy selected-intent lookup, remote preview | in XML | indirectly | no; *absent* for packages |
| catalog logical id | repository (`local:<uuid>`) / curator (`bundled:`) | never for a record | selection, catalog equality | `theme-identities.properties` / selected-intent pref | no | **yes** |
| package `version` | author | yes | display | in bytes | yes | no |
| content digest / revision | – | – | not v1 (D7) | – | – | – |
| origin | repository | no | lifecycle branching | implied by binding | indirectly | no |

## 5. Model changes (EN2)

- `core.models.PackageMetadata` record: `packageId`, `version`, `license`, `copyright`, `source` (nullable strings except `packageId`).
- `Theme` gains `Optional<PackageMetadata> packageMetadata()` and `Map<String, Color> authoredWorkbench()` (immutable, possibly
  empty; keys restricted to the eight schema role names at construction). Legacy constructor keeps its signature and yields empty
  values, so all existing callers and tests stay source-compatible. `Theme.getId()` returns `null`/empty for packages.
- `core.workbench.WorkbenchColor` gains `schemaName()` (`background, foreground, surface, surfaceRaised, border, mutedForeground,
  selection, accent`); `core.models` does not import `core.workbench`.

## 6. Palette resolution with authored roles (PM5-A)

In `WorkbenchPaletteResolver.resolve(theme)`:
1. For each role, if `authoredWorkbench` has it, use it verbatim, else resolve as today. Derivations that depend on another role
   (raised from surface, border from surface/raised, muted from foreground) use the *final* value of that role, authored or not.
   Authored `background`/`foreground` replace the editor-derived base for chrome only; editor preferences are unaffected.
2. Run the existing coherence check. If it fails, apply the existing `readable(...)` lightness correction to failing text roles only
   (`foreground`, `mutedForeground`, `accent`), keeping authored surfaces; re-check; on failure return the family fallback (unchanged).
3. A legacy theme (empty map) follows exactly the current code path; `WorkbenchPaletteResolverTest` must stay green unchanged.

Admission emits WARNING `workbench-readability` per corrected role (computed in the repository admission boundary by calling the pure
resolver, so `core.parser` gains no dependency on `core.workbench`). `PluginColors.derive(palette)` needs no change.

## 7. Repository and storage

- Supported extensions: `{".xml", ".ectheme"}`, `Locale.ROOT`. Discovery, import, update and file dialog share one definition.
- Import allocates `<uuid>` + *source extension*; binding format unchanged.
- Update: refuse before staging when the replacement's extension differs from the locator's (PM6-A), with a diagnostic.
- Size bound (EN5): `ThemePackageParser` reads at most 256 KiB + 1 byte of the staged copy and refuses a larger package with ERROR
  `file-too-large` before any XML parsing, so the bound holds for every caller (import, update, reload). Legacy XML is
  not bounded by this change (would be a behaviour change for the existing corpus; deferred).
- Bundled loading stays `themes/*.xml` only (D4).

## 8. UI (minimal; polish is the later UX change)

- File dialog filters: `*.xml;*.XML;*.ectheme;*.ECTHEME` plus separate "Theme package" and "Legacy theme" entries.
- Details pane: for a package, show Version, License (or "Not stated"), Copyright (or "Not stated") and Source (inert unless
  launchable, same rule as website). Never state or imply the plugin's own license.
- Preview: when `getId()` is absent, show the existing unavailable state; never request the preview server with a package id.
- Workbench section unchanged; a package with `<workbench>` does not change the checkbox.

## 9. Threading, security, failure ordering

Parsing stays on the caller's thread as today (import already runs there; files are tiny and bounded). Import/update/remove ordering is
unchanged: stage → bound → parse → move/replace → bind → admit. A refusal at any step leaves files, bindings, catalog and intent
unchanged. No foreign preference is touched by parsing or admission.

## 10. Future archive (D1) — constraints recorded, not built

If a later change introduces an archive it must: use a distinct extension; contain this exact document as `theme.xml` at the root;
reject absolute paths, `..`, backslashes, drive letters, duplicate names (case-insensitively), symlink entries, directories other than
an allow-list, nested archives and executable types; bound entry count, per-entry and total uncompressed size and compression ratio;
extract to staging only; and resolve references only to normalized in-archive paths. Nothing in v1 references a resource.

## 11. Proportionality review

Added: one parser class, one metadata record, two `Theme` fields, a role-name accessor, an extension set, a size check, a few labels.
Not added: archive, JSON, capability negotiation, digest, revision, identity domain, contributor API, export, bundled packages,
named palette variables. Each omission is tied to a Deferred item.
