# Evidence

Baseline: `develop` at `cefd10df4ebf4dd23323cdce0afc521bc239e44e` (PR #27 squash-merge, post-merge CI green), plus archive commit `44055f6` for R2.3. Branch: `feat/immutable-validated-theme-model` forked cleanly from `develop`; no merge of the old R2.3 feature branch. No production code modified by this planning change.

## E1 — Current Theme owns visual and catalog concerns together

`bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/models/Theme.java:9-39`: single class holds `id` (raw `ect-id`), `logicalId` (nullable, assigned by `withLogicalId`), `name/author/website/description`, `Optional<File> file`, `ThemeType type`, and mutable `HashMap<TokenKey,Token> tokens`.
`withLogicalId` (lines 51-56) copies tokens into a new `Theme`; `addToken` (86-88) mutates post-construction; `getTokens` (82-84) returns an unmodifiable live view over mutable backing state.
`equals/hashCode` (116-125) compare by `logicalId`; two null-identity candidates never compare equal.

## E2 — Parser builds Theme incrementally then repository assigns identity

`XmlThemeParser.java:96-101`: constructs `new Theme(id, name, author, website, description, sourceFile, ThemeType.from(type))`, pre-populates dark BACKGROUND/FOREGROUND roots via `addToken`, then adds winners per role (line 170).
`ThemeRepository.java:100-103` (import), `:154-156` (update), `:381-384` (bundled `withLogicalId("bundled:"+id)`), `:422-424` (installed `identityFor(fileName)`): admission assigns identity after validation. No frozen boundary exists between "candidate complete" and "admitted".

## E3 — TokenKey registry is globally mutable

`TokenKey.java:12-25`: static `HashMap<String,TokenKey> TOKENS`; public `register(TokenKey)` and `register(String,TokenKey)` silently `put` (overwrite); `byId` reads. All 60+ core roles registered during static initialization (lines 28-91). No production dynamic registration found; `AdapterRegistry`/`plugin.xml` extension schema contributes adapter classes only, not token roles. `Theme.get` follows `inheritsFrom` chains and throws when roots are missing (Theme.java:94-108).

## E4 — Adapters consume only visual definition

`EclipseThemeAdapter.apply(Theme, IEclipsePreferences)` (EclipseThemeAdapter.java:28) is the preserved signature; extension schema `schema/com.github.eclipsethemes.theme.adapters.exsd` binds `class` (an `EclipseThemeAdapter`) plus optional `plugin` only. All built-in adapters (`TextEditor`, `Java`, `Cpp`, `Pde/Ant`, `DebugConsole`) read `theme.get/has/type` and never read `getLogicalId/getFile/getId`. One exception-adjacent read: `TextEditorThemeAdapter:68` fallback lambda receives `theme` only to branch on `theme.getType()` for removed-line color. No adapter reads identity, File, or origin.

## E5 — Repository-derived origin and locator checks are string/file sniffs

`ThemeRepository.managedTarget` (lines 233-258): refuses unless `selected.getLogicalId().startsWith("local:")`, then requires `selected.getFile()` present, normalizes to `installedThemesDir`, checks parent/extension, then verifies `ThemeIdentityBindings.establishedIdentityFor(locator)` equals `selected.getLogicalId()`.
`EclipseThemesPreferencePage.isInstalledTheme` (lines 537-539): `theme.getLogicalId().startsWith("local:")`; label provider (293-295) uses `theme.getFile().isPresent()` for the installed marker. Both conflate visual Theme with catalog state; both are the smells PM-M4/PM-M5 remove.

## E6 — Identity persistence format is locator-keyed and unchanged by R2.4

`ThemeIdentityBindings.java`: `theme-identities.properties` maps relative managed filename to `local:<UUID>`; `identityFor`/`newIdentityFor`/`establishedIdentityFor`/`remove`/`restore` semantics verified by `ThemeIdentityBindingsTest`, `ThemeRepositoryImport/Update/Removal/ValidationTest`. R2.4 keeps this file, its keys, and its values byte-compatible; only the in-memory carrier changes from `Theme` to `ThemeRecord`.

## E7 — PM-V7 fallback roots are load-bearing compatibility behavior

`XmlThemeParser.java:98-101` unconditionally seeds BACKGROUND `rgb(30,30,30)` / FOREGROUND `rgb(212,212,212)` before reading elements; all 33 bundled themes explicitly supply both colors, so bundled output is unaffected, but sampled historical themes often omit `type` and sparse themes rely on these roots via `Theme.get` inheritance. `XmlThemeParserCompatibilityTest.sparseDefaultsAndSourceEntryPoints` and the bundled corpus fingerprint pin current resolved values. Changing roots visibly alters historically accepted imports; R2.4 retains them exactly at the parser ownership point.

## E8 — Token/Color/Options immutability status

`Color` is a record with range checks (Color.java). `TokenOptions` is a record (TokenOptions.java). `Token` is final with final fields but its constructor accepts null key/color (Token.java:11-15); `TokenBuilder.build` already refuses null key/color (TokenBuilder.java:59-65). `Theme` does not snapshot: `withLogicalId` shares no map but both source and copy remain mutable via `addToken`.

## E9 — Selection and outcome touchpoints

`ThemeSelection.java:16-44`: `resolve(store, List<Theme>)`, `remember(store, Theme)`, `clear`, `defaultTheme(List<Theme>,boolean)`; persistence keys `SELECTED_THEME_LOGICAL_ID` / legacy `ACTIVE_THEME_ID` unchanged. `ThemeOperationOutcome` is `record(Theme theme, List<ThemeDiagnostic>)` (ThemeOperationOutcome.java:14); import/update return `committed(theme,findings)` / `refused(findings)`. Page flows (`importTheme`, `updateSelectedTheme`, `removeSelectedTheme`, `selectThemeByIdentity`, `restoreSelectedIntent`, apply/OK) all thread `Theme` today and are the conversion surface to `ThemeRecord`.
