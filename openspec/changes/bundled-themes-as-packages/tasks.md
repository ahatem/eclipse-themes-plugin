# Tasks — Bundled themes as theme packages

## 0. Gate
- [x] 0.1 Decision Gate recorded (D1 resolved 2026-11-01); branch `feat/bundled-themes-as-packages` from the `main`/`develop` tip.

## 1. Snapshot and safety net
- [x] 1.1 `git mv` the 33 legacy XML files to `bundles/com.github.eclipsethemes/test-data/legacy-bundled/`.
- [x] 1.2 Repoint `ParserTestSupport.themes()` (legacy mapping tests, `XmlThemeMappingTest`) and `WorkbenchPaletteResolverTest` (fingerprint hash unchanged) at the snapshot.

## 2. Conversion
- [x] 2.1 One-off scratchpad converter (not committed) generates `themes/*.ectheme` with `ThemePackageWriter`; ids `com.github.eclipsethemes.<slug>`.
- [x] 2.2 Generate `themes/bundled-identities.properties` from the legacy `ect-id` values; review all outputs by hand.

## 3. Loader
- [x] 3.1 `ThemeRepository.loadPackedThemes()` loads `*.ectheme` + identity map; unmapped package → product-defect log, skipped.
- [x] 3.2 `ThemeRecord` source-id helper; use it in `ThemeSelection.resolve` and `EclipseThemesPreferencePage.previewUrl`.

## 4. Tests
- [x] 4.1 Parity test: for each snapshot file, the matching package yields equal name, type, and token map (colors + options).
- [x] 4.2 Identity test: 33 records, logical ids equal `bundled:`+snapshot ect-id, no duplicates, both defaults resolve; unmapped package is skipped.
- [x] 4.3 Selection/preview tests for bundled records (legacy fallback, preview URL); update `ThemeIdentityTest` bundled helper for the map entry.

## 5. Docs, verification
- [x] 5.1 Update `docs/CURRENT_ARCHITECTURE.md`; finish `evidence.md`.
- [x] 5.2 Focused tests, `openspec validate bundled-themes-as-packages --strict` and `--all --strict`, `mvn clean verify -B`.
- [ ] 5.3 Full-diff proportionality review; commit (no push).
