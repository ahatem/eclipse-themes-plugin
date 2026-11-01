# Proposal — Bundled themes as theme packages

## Why

The 33 themes shipped in `bundles/com.github.eclipsethemes/themes/` are still legacy `<colorTheme>` XML, while Theme Package v1
(`.ectheme`, shipped in 1.2.0) is the documented format for new themes. Shipping the product's own catalog in the legacy format
means the package format has no first-party exercise, bundled themes cannot carry provenance, and the loader keeps a legacy-only
bundled path. Moving the bundled corpus to packages makes the first-party catalog the reference corpus for the format.

The move must be invisible to users: same names, same colors and styles, same appearance classification, same logical identities
(`bundled:<legacy ect-id>`), same defaults, same persisted selections.

Baseline: `main` @ `2f26c45` (contains `develop` @ `d40cb61`, Theme Package v1). Evidence is in [`evidence.md`](evidence.md).

## What Changes

- Each `themes/*.xml` is replaced by a same-named `themes/*.ectheme` package:
  - `<editor>` carries the same tokens (written by the existing `ThemePackageWriter`, then reviewed by hand);
  - `appearance` comes from the legacy `type`; `name`, `author` and `website` are carried over;
  - package `id` is `com.github.eclipsethemes.<slug>`, the slug being the legacy file name lower-cased with `_` replaced by `-`;
  - no `<workbench>` and no `<fonts>` (workbench colors stay derived exactly as today; no font recommendation is invented);
  - no `<provenance>`: no verifiable upstream license or source exists in the repository for any bundled theme (see evidence E4),
    and the format says unstated is shown as "Not stated" rather than guessed.
- A curated, in-bundle identity map `themes/bundled-identities.properties` (package id → legacy ect-id) is read only by the bundled
  loader. Logical identity stays `bundled:<ect-id>`. The package format gains no attribute and no migration is performed.
- `ThemeRepository.loadPackedThemes()` loads `themes/*.ectheme` and takes identity from the map. A bundled package with no map entry
  is not admitted and is logged as a product defect, as a bundled resource without a curated identity is today.
- Bundled records have no `Theme.getId()` (packages carry no source id). Two behaviors that read that field for bundled themes are
  kept by deriving the legacy source id from the bundled logical identity: the pre-1.1 `ACTIVE_THEME_ID` selection fallback and the
  online preview URL (evidence E3). Installed themes are unaffected.
- Installed and imported legacy `.xml` themes keep working unchanged.
- The legacy bundled XML files are kept as a test-only snapshot so parity is mechanically provable, not as shipped content.
- **BREAKING:** none. Identities, defaults, stored selections, preferences and applied output are unchanged.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `theme-catalog`: bundled themes are packages whose identity comes from a curated map.

## Impact

- Code: `ThemeRepository.loadPackedThemes()`; `EclipseThemesPreferencePage.previewUrl` and `ThemeSelection.resolve` (legacy source id
  for bundled records); a small helper on `ThemeRecord`. No new bundle, dependency, extension point, exported package or preference key.
- Resources: 33 `.xml` → 33 `.ectheme`, one new `.properties`; `build.properties` already includes `themes/`.
- Tests: parity and identity tests added; tests that listed `themes/*.xml` repointed (see tasks).
- Docs: `docs/CURRENT_ARCHITECTURE.md` bundled-source line.
- Rollback: revert the change; no persisted state is rewritten, so a downgrade resolves the same identities.

## Decision Gate

**Product / Maintainer decisions: none outstanding.**

| # | Question | Class | Resolution |
|---|---|---|---|
| D1 | How does a bundled package keep its `bundled:<legacy ect-id>` identity without adding an ect-id attribute to the format or migrating users? | Product / Maintainer | **Resolved 2026-11-01 by the maintainer:** a curated in-bundle map package-id → ect-id (`themes/bundled-identities.properties`), read only by the bundled loader. No format change, no user migration. |
| D2 | Do `Constants.DEFAULT_LIGHT_THEME_ID` / `DEFAULT_DARK_THEME_ID` keep resolving? | Evidence-dependent | Yes by construction (identity unchanged); asserted by the identity test. |
| D3 | Do bundled records still work where code reads `Theme.getId()`? | Evidence-dependent | Investigated (E3): two consumers (legacy selection fallback, preview URL). Fixed by deriving the source id from the bundled logical id, preserving behavior. |
| D4 | Provenance for bundled packages | Engineering | Omit; nothing verifiable (E4). The three themes whose XML comments say "Copyright (c) 2024 Eclipse Themes / EPL v1.0" are not treated as verified upstream provenance and conflict with the project's EPL-2.0. |
| D5 | Which legacy attributes are dropped | Engineering | `modified` (no package field; not displayed, E5) and XML comments. Everything the product reads is kept. |
| D6 | Fixing the duplicated legacy numeric `id` values / licensing headers | Deferred | Out of scope; license clean-up belongs to a maintainer-led provenance review. |

No BREAKING behavior is proposed.
