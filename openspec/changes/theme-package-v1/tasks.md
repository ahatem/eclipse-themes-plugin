# Tasks — Theme Package v1

> PM1–PM8 approved with the recommended options on 2026-10-19.

## 0. Gate

- [x] 0.1 Record the maintainer's PM1–PM8 resolutions in the proposal's Decision Gate.
- [x] 0.2 Create `feat/theme-package-v1` from up-to-date `develop`; commit planning artifacts separately from implementation.

## 1. Legacy safety net (before refactoring)

- [ ] 1.1 Confirm `XmlThemeParserCompatibilityTest`, `ParserFixtureTest`, `XmlThemeValidationTest`, `WorkbenchPaletteResolverTest` and the
      corpus fingerprint pass unchanged on the branch base.
- [ ] 1.2 Extract the legacy child-element token interpretation from `XmlThemeParser` into a package-private interpreter with no
      behaviour change; the tests in 1.1 must pass byte-for-byte.

## 2. Model

- [ ] 2.1 Add `core.models.PackageMetadata` and the two optional immutable `Theme` fields; keep the existing constructor signature.
- [ ] 2.2 Add `WorkbenchColor.schemaName()`; test the eight names and that unknown names are rejected at `Theme` construction.
- [ ] 2.3 `ThemeModelTest`: immutability of the authored map and metadata; legacy themes have none.

## 3. Package parser

- [ ] 3.1 `ThemePackageParser` in `core.parser`, using `SecureXmlBuilder` and the extracted interpreter; new `ThemeDiagnostic` kinds
      (`schema-version`, `unsupported-schema-version`, `package-id`, `appearance`, `missing-section`, `duplicate-section`,
      `required-editor-token`, `unknown-workbench-role`, `duplicate-workbench-role`, `license`, `version`, `file-too-large`,
      `workbench-readability`).
- [ ] 3.2 `ParserFactory` dispatch by extension (`Locale.ROOT`).
- [ ] 3.3 Fixture tests, one per `theme-package` scenario, including: security probes reused from `XmlThemeParserSecurityTest` against
      `.ectheme`; schema version absent/`abc`/`2`; id grammar; appearance; missing/duplicate sections; legacy alias parity (same
      element in legacy and package yields the same token); no PM-V7 defaults; unknown element warning; unknown attribute silence.

## 4. Palette

- [ ] 4.1 Authored-role precedence and dependent derivation in `WorkbenchPaletteResolver`; readability correction of failing text roles.
- [ ] 4.2 Tests: every bundled theme resolves to the identical palette (regression); partial authored sections; unreadable authored
      accent corrected with hue kept; fully incoherent authored palette → family fallback.

## 5. Repository

- [ ] 5.1 Shared supported-extension set; discovery, import, update, locator extension.
- [ ] 5.2 Size bound on the staged `.ectheme` before parsing.
- [ ] 5.3 Format-preserving update refusal before staging.
- [ ] 5.4 Admission-time `workbench-readability` warnings via the pure resolver.
- [ ] 5.5 Tests in `ThemeRepositoryImportTest` / `UpdateTest` / `ValidationTest`: package import → `<uuid>.ectheme` + binding + exact
      bytes; reload rediscovery; two packages with the same `id` stay independent; oversized refusal leaves state unchanged;
      cross-format update refused; package→package update keeps identity; unknown `<fonts>` bytes preserved.

## 6. UI

- [ ] 6.1 File dialog filters; provenance labels with "Not stated"; source link via `WebsiteUri`; preview guard for absent legacy id.
- [ ] 6.2 Extend `ThemeWebsitePresentationTest` for `source`; a test that a package produces no preview URL.

## 7. Docs

- [ ] 7.1 Author guide `docs/THEME_PACKAGE_FORMAT.md` with the v1 schema, an example package and conversion notes from legacy XML.
- [ ] 7.2 Update `docs/CURRENT_ARCHITECTURE.md` (flow, persistence, downgrade note) and `docs/ROADMAP.md` (R9 slice delivered).

## 8. Verification

- [ ] 8.1 `openspec validate theme-package-v1 --strict` and `openspec validate --all --strict`.
- [ ] 8.2 `mvn clean verify -B`.
- [ ] 8.3 Runtime smoke on the verified matrix (Windows, 4.40/4.41): import the example package, apply with coordination off (editor
      only) and on (authored surface/accent visible), restart, remove. Record evidence in `verification.md`.
- [ ] 8.4 Proportionality review and full-diff review for unrelated churn.
