# Design — Bundled themes as theme packages

## Decisions

**Conversion.** A one-off converter, kept in the session scratchpad and never committed, parses each legacy file with `XmlThemeParser`,
re-identifies it (`com.github.eclipsethemes.<slug>`, appearance from `type`) and serializes it with the existing `ThemePackageWriter`.
Output is reviewed by hand and checked by the committed parity test. The writer emits canonical token names in a deterministic order;
that is why parity is asserted on the parsed token map (color + style flags), type and name, not on text.

**Identity map.** `themes/bundled-identities.properties`, lines `<package id>=<legacy ect-id>`, UTF-8/ISO-8859-1 safe (ASCII only).
`ThemeRepository.loadPackedThemes()` reads it once per load through `Bundle.getEntry`, then for each `themes/*.ectheme`:
parse → look up `packageMetadata().packageId()` → `ThemeRecord.bundled("bundled:" + ectId, theme)`. Missing map file, missing entry,
or blank value logs the existing "Bundled theme without curated identity" product-defect warning and skips that resource. Duplicate
identities keep the existing duplicate warning. The map is data owned by the bundle; the parser, the format and `Constants` are untouched.
A package-owned claim is never used as identity (consistent with `theme-package`).

**Legacy source id for bundled records.** `Theme.getId()` is null for packages. Its only two consumers that matter for bundled themes are
`ThemeSelection.resolve` (pre-1.1 `ACTIVE_THEME_ID` fallback compares the raw ect-id) and `EclipseThemesPreferencePage.previewUrl`.
`ThemeRecord` gains one small method returning the source id: for `BUNDLED` origin the logical id without the `bundled:` prefix, otherwise
`theme().getId()`. Both consumers use it. Origin is read from `origin()`, never from prefix sniffing of other records, matching the
catalog-entry requirement; the prefix is stripped only because bundled identities are defined as `bundled:<source id>`.

**Test-only legacy snapshot.** The 33 legacy files move (git mv) to `bundles/com.github.eclipsethemes/test-data/legacy-bundled/`. They
are not in `build.properties` and are not shipped. They back the parity test, the legacy-mapping tests and the workbench palette
fingerprint, which keep their previous inputs and hash.

## Alternatives rejected
- ect-id attribute on `<themePackage>`: changes the public format; rejected by the maintainer (D1).
- Deriving identity from package id by naming convention: loses the arbitrary legacy UUIDs; would change identities.
- Keeping both `.xml` and `.ectheme` shipped: duplicate themes, dead weight.

## Risks / rollback
- A wrong map entry silently changes an identity → identity test pins all 33 ids against the snapshot's `ect-id` values.
- Typo in a converted color → parity test over every token.
- State: nothing persisted is rewritten; revert restores the old resources with identical identities.
- Threading/resources: load stays on the same path and thread as today; `InputStream`s opened with try-with-resources.
- No new internal Eclipse API; `Bundle.getEntry` is public OSGi API.
