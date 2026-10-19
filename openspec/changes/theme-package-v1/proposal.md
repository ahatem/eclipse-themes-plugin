# Proposal — Theme Package v1

## Why

A legacy `colorTheme` XML can only express editor colors; R8 then *infers* the workbench palette from them (OKLCH derivation, a fixed
blue accent, readability correction). A theme author who knows exactly what their tab bar, borders and accent should look like has no
way to say so, and nothing in the format lets the project tell whether a redistributed theme's license is known. Theme Package v1 is
the smallest durable, declarative format that lets an author state coordinated intent explicitly and carry provenance, while every
existing XML theme keeps working unchanged.

Baseline: `develop` @ `4a4d0b829a84b43826260f8a90647f5965ce58c7` (R8 complete), verified clean before this proposal. Evidence for every
claim below is in [`evidence.md`](evidence.md).

## What Changes

**Approved for bounded implementation on 2026-10-19.** The maintainer explicitly approved PM1–PM8 with the recommended options (see
the resolution record in the Decision Gate). Alternatives remain below as historical decision context.

- A new **package document**: a single, secure-parsed XML file with the extension `.ectheme` and root `<themePackage>`, holding
  metadata, provenance, a required `<editor>` section (the existing editor token vocabulary, interpreted by exactly the legacy rules)
  and an optional `<workbench>` section (the eight R8 palette roles). No archive, no resources, no CSS, no code.
- An explicit integer **`schemaVersion`**: version `1` is the only accepted value; anything else refuses the document. Unknown
  optional elements are ignored with a warning; unknown attributes are ignored silently (legacy parity).
- **Identity is unchanged.** The package `id` is an untrusted *source claim* in the same category as the legacy `ect-id`: it never
  becomes the catalog logical identity, never establishes continuity, and never drives equality. Installed packages receive a
  `local:<uuid>` identity and a repository-allocated `<uuid>.ectheme` locator through the existing import/update/remove lifecycle.
  No revision counter and no content digest are introduced (R2 PM5-D stands).
- **Appearance** is a required `dark|light` attribute; there is no `auto`, `high-contrast` or `unspecified` mode. OS high contrast
  continues to follow the R8 `HIGH_CONTRAST` gate (workbench UNAVAILABLE, editor still applied).
- **Authored workbench colors** take precedence over R8 derivation role by role; omitted roles are derived exactly as today; the R8
  readability and coherence rules still apply (PM5). Coordination stays a user opt-in: no package enables it.
- Minimal **provenance**: optional SPDX license expression, copyright holder, upstream source URL and author-facing version string.
  Missing provenance never blocks a user import and is shown as "not stated"; the product never implies the plugin's own license.
- Import/update/discovery accept `.ectheme` alongside `.xml`, with locale-independent extension matching and a bounded document size.
  Update never changes a record's format (PM6).
- The details pane shows the package's license, copyright, source and version; the remote preview is never queried with a package id.
- **BREAKING:** none for existing XML themes, identity bindings, selected intent or preferences. Downgrade note: older versions ignore
  `.ectheme` files and their bindings; a selected package becomes unresolved after a downgrade and needs reselection (PM8).

## Capabilities

### New Capabilities

- `theme-package`: the package document contract — container, schema versioning, metadata and provenance fields, sections, limits,
  validation findings and forward-compatibility rules.

### Modified Capabilities

- `theme-definition`: a validated theme may carry package metadata and authored workbench colors; legacy themes carry neither.
- `theme-catalog`: discovery admits `.ectheme` direct children under the same validation, binding and ordering rules.
- `theme-import`: import/update accept `.ectheme`; extension decides the format; update preserves format; size bound.
- `workbench-theming`: authored workbench roles take precedence over derivation; coherence and fallback rules unchanged.
- `preference-ui`: import filter and details pane show package provenance; no remote preview lookup for packages.

## Impact

- Code: `core.parser` (new package reader sharing `SecureXmlBuilder` and the legacy token interpreter), `core.models.Theme` (two
  optional immutable fields), `core.validation.ThemeDiagnostic` (new kinds), `core.workbench.WorkbenchPaletteResolver` (authored
  roles), `eclipse.ThemeRepository` (extension set, locator extension, size bound, format-preserving update), the preference page
  (filter, provenance labels, preview guard). No new bundle, dependency, extension point, exported package or preference key.
- Persistence: new installed files `<uuid>.ectheme` in the existing `installed_themes` directory; bindings unchanged in format.
- Compatibility: JavaSE-17, Eclipse target, adapters, plug-in color table and CSS are untouched. Legacy parsing is byte-for-byte
  unchanged (regression corpus must stay green).
- Rollback: reverting the change leaves `.ectheme` files and their bindings inert on disk; no foreign preference is affected.

## Decision Gate

**Resolution record (2026-10-19): PM1 A, PM2 A, PM3 A, PM4 A, PM5 A, PM6 A, PM7 A and PM8 A are explicitly maintainer-approved**
("Approve PM1–PM8 with the recommended options"). No blocking Product / Maintainer decision remains. ED5 is still an implementation
check; a material new product question found during `apply` must return to this gate. The table below is retained as context.

### Evidence-dependent (investigated during planning)

- **ED1 — Does released v1.1.0 safely ignore a package?** Resolved: v1.1.0 parses *any* root element with an unsecured factory and
  lists only `*.xml`. A package saved as `.xml` would be parsed insecurely and admitted as a bogus theme after a downgrade; a
  `.ectheme` file is never listed. This is the main reason for a distinct extension (evidence §2).
- **ED2 — Can the legacy token rules be reused verbatim?** Resolved: 17 of 53 element names used by the bundled corpus are legacy
  aliases (`singleLineComment`, `javadoc`, `occurrenceIndication`, …). Authors converting themes will copy them, so the `<editor>`
  section must use the same alias, precedence and diagnostic rules (evidence §3).
- **ED3 — Is a container archive needed in this release?** Resolved negative: icons are out of scope, the release has no binary
  asset; fonts (next change) can reference installed font families by name. An archive becomes necessary only if the fonts change
  decides to embed font files (see Deferred D1).
- **ED4 — Would the preview leak package ids?** Resolved: the page builds `PREVIEW_SERVER_URL + theme.getId()`. Packages must not
  populate that legacy id.
- **ED5 (open, implementation)** — confirm that the JAXP secure-processing limits plus a pre-parse byte bound keep a pathological
  256 KiB document under one second on the CI JDK; adjust the bound only with evidence.

### Product / Maintainer decisions

| ID / question | Options and consequences | Recommendation |
| --- | --- | --- |
| **PM1: Physical container** | **A** single XML document, extension `.ectheme`: no new parser surface, hand-authorable, diffable on GitHub, old versions ignore it; cannot carry binary assets. **B** ZIP `.ectheme` archive with `theme.xml` + `resources/`: future-proof for fonts/images but adds archive attack surface (zip-slip, bombs, duplicates, symlinks), a staging extractor and limits for an asset type this release does not have. **C** keep `.xml` with a new root: zero new extension, but v1.1.0 parses it insecurely and admits garbage on downgrade (ED1). | **A.** Smallest container that satisfies every v1 need; B is cheap to add later as a *separate* extension whose manifest is this same document, so nothing authored now is wasted. |
| **PM2: Manifest format** | **A** XML (reuses `SecureXmlBuilder`, fail-closed controls, diagnostic model, the token interpreter, and the authoring style of 33 bundled themes). **B** JSON (needs a new dependency or hand-written parser in an OSGi bundle with none today, a second diagnostic path). **C** TOML (same, less tooling in Eclipse). | **A.** No demonstrated advantage justifies a second parser and security review. |
| **PM3: Package `id` — required, and what grammar?** | **A** required, lower-case reverse-DNS-like (`[a-z0-9]+(\.[a-z0-9][a-z0-9-]*)+`, ≤ 128 chars), *source claim only*. Enables later update-suggestion UX without trusting it. **B** optional, like `ect-id`. Less author friction, but most packages would carry nothing usable later. **C** make it the logical identity. Contradicts R2 PM2-A; one author could shadow another's install. | **A.** Costs authors one line, keeps one identity system. |
| **PM4: Missing or legacy-style required content** | **A** a package refuses without `<editor>` `background` *and* `foreground`; a package never receives the PM-V7 hard-coded dark root defaults. **B** reuse legacy defaults: sparse packages accepted but a light package can silently get a dark background. | **A.** New format, no corpus to break; removes an accepted legacy defect instead of inheriting it. |
| **PM5: Authored workbench colors vs. readability** | **A** authored roles used verbatim; if the coherence check fails, only failing *text* roles (`foreground`, `mutedForeground`, `accent`) receive the existing lightness-only correction, each producing a warning at admission; if still incoherent, the family fallback applies (as today). **B** verbatim always, no correction: author fully in control, may ship unreadable chrome. **C** treat authored values as mere candidates under R8 suitability tests: safe, but an author's explicit choice can be silently dropped. | **A.** Honors intent, keeps the accessibility floor already promised by `workbench-theming`, and is truthful (warnings). |
| **PM6: Cross-format update** | **A** update requires the replacement to have the record's format; converting a legacy record to a package is "import the package, then remove the old record" (and selected intent must be reselected). **B** allow it by renaming the locator, which violates the catalog rule that locators are never renamed. | **A.** No identity-rule exception; a guided conversion belongs to the import/export UX change. |
| **PM7: Provenance policy** | **A** all provenance optional for user imports; displayed as "not stated" when absent; bundled/curated packages (none in v1) must carry license and copyright, enforced by a corpus test. **B** require license for any import: blocks personal themes for no safety gain. **C** no provenance fields: leaves the project unable to distinguish third-party terms. | **A.** |
| **PM8: Downgrade behavior** | **A** accept: older versions ignore `.ectheme` files and bindings; a selected package becomes unresolved and needs reselection; nothing is deleted. **B** add a compatibility shim writing a legacy XML twin: doubles storage and identity semantics. | **A.** Matches the R2 downgrade note already accepted. |

### Engineering (decided here; rationale in `design.md`)

- EN1 one bundle; package reader lives in `core.parser`, no activator/SWT dependency.
- EN2 `Theme` gains `Optional<PackageMetadata>` and an immutable authored-workbench map keyed by schema role name; `core.models` does
  not import `core.workbench`.
- EN3 format chosen by extension only, never by content sniffing; a `.ectheme` file whose root is `colorTheme` is refused.
- EN4 diagnostics extend the existing `ThemeDiagnostic` kinds; no numeric codes, no exported API.
- EN5 byte bound checked on the staged file before parsing; 256 KiB (largest bundled theme is 2.8 KiB).

### Deferred (must not be decided by this change)

- D1 archive container and resource references — owned by the **Fonts** change *only if* it embeds font files; otherwise later.
- D2 font fields in the package — **Fonts** change (adds an optional `<fonts>` element; v1 readers ignore it with a warning).
- D3 export, guided legacy→package conversion, update suggestions keyed by package `id`, conflict UI — **Import/export and preference
  UX polish** change.
- D4 bundled packages and their identity domain — first change that ships one.
- D5 contributor API, executable OSGi contributors, WTP/TM4E — R9 contributor slice / R6, unchanged by this format.
- D6 theme-authored plug-in colors, named palette variables, CSS, icons, per-color designer — out of this release.
- D7 content digest / revision — only if a future consumer needs one; would be product-derived from bytes, never author-declared.
