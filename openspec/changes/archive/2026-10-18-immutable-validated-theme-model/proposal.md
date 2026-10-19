# Proposal: immutable validated theme model

## Why

`develop` at `cefd10d` (plus R2.3 archive `44055f6`) validates honestly but still admits a mutable hybrid: one `Theme` class carries raw source metadata, nullable catalog identity, an absolute managed `File`, and a live-mutable token map, with identity equality bolted on. Every catalog, selection, UI, and adapter consumer must therefore ask visual-definition questions to infer repository state (`getLogicalId().startsWith("local:")`, `getFile().isPresent()`), and any holder of a `Theme` reference can mutate a validated definition via `addToken` after admission.

R2.4 draws one clean boundary: parse-plus-validate produces an immutable visual definition; repository admission wraps it in a small catalog record. This is the last R2 structural change before R3 application truth. Full evidence in `evidence.md`.

## What Changes

- `Theme` becomes one immutable validated visual definition: raw/source `ect-id` where still needed, name, author, website, description, `ThemeType`, immutable semantic tokens with existing fallback resolution. It no longer owns logical identity, lifecycle identity, managed file/path, repository binding, or catalog equality.
- New `ThemeRecord` owns one catalog entry: `logicalId`, `ThemeOrigin`, optional managed locator, and its `Theme`. New `ThemeOrigin { BUNDLED, INSTALLED }` replaces `logicalId.startsWith("local:")` product logic.
- Parser constructs `Theme` once from a local token map and never mutates it after; `Theme.addToken` and `withLogicalId` are removed from production.
- Repository catalog, outcomes, selection, manager, and page operate on `ThemeRecord`; adapters keep receiving `Theme` with the existing `apply(Theme, prefs)` signature unchanged.
- `TokenKey` closes for now: registration helper becomes private/internal, duplicate declarations fail immediately, `byId()` stays for parsing; no enum conversion, no contributor registry.
- PM-V7 sparse fallback roots stay byte/semantic-identical at the existing parser ownership point; no bundled XML edits.

## Decision Gate

Maintainer architecture decisions PM-M1…PM-M8 are approved and recorded below. No remaining product/maintainer decision is required unless implementation evidence contradicts the approved contract, in which case work stops and returns here.

### PM-M1 — APPROVED: Option D (Theme + ThemeRecord)

- Recommended discovery Option C (separate ThemeDefinition/Metadata/Identity hierarchy) is NOT approved.
- `Theme` remains the adapter-facing name and becomes one immutable validated visual definition.
- `ThemeRecord` is the one small catalog/repository object: logicalId, origin, optional managed locator, Theme.
- Forbidden: ThemeDefinition, ThemeMetadata, ThemeIdentity, ThemeDescriptor, ThemeHandle, ThemeEntity, catalog-type hierarchies.

### PM-M2 — APPROVED: semantic roles closed for now

- `TokenKey` is a closed core vocabulary. Registrations occur only during static class initialization; no production dynamic registration; extension schema contributes no roles; all built-in adapters consume the fixed vocabulary.
- Direction: registration helper becomes private/internal; duplicate key declaration fails immediately; `byId()` remains for parsing; no enum conversion; no contributor token registry. Future extensibility is R9.

### PM-M3 — APPROVED: preserve adapter contract

- `EclipseThemeAdapter.apply(Theme, IEclipsePreferences)` is unchanged. No `ThemeDefinition` rename, no schema conceptual change. Adapters keep receiving `Theme`, minus catalog identity/storage state.

### PM-M4 — APPROVED: storage belongs with catalog state

- Managed `File` leaves `Theme`. `ThemeRecord` carries the minimum managed locator (preferred: relative managed filename, not `File`/absolute `Path`/URI). Repository owns the installed root and resolves root + locator to a managed path. Bundled records carry no locator. Identity-binding persistence format unchanged.

### PM-M5 — APPROVED: explicit tiny origin

- `ThemeOrigin { BUNDLED, INSTALLED }` on `ThemeRecord`. Product logic based on `logicalId.startsWith("local:")` is removed; ID prefixes may persist as format only. No sealed hierarchy.

### PM-M6 — APPROVED: preserve PM-V7 rendering

- Sparse-theme fallback colors unchanged; existing parser fallback policy keeps one clear ownership point, documented as accepted compatibility behavior; not folded into the immutable model. Future visual-policy change needs an explicit compatibility decision. No bundled XML edits.

### PM-M7 — APPROVED: equality semantics

- `Theme` loses catalog-identity equality; no custom equality unless an actual caller needs value equality (default: none). `ThemeRecord` identity equality, if needed, is by `logicalId`. No structural digest/content identity.

### PM-M8 — APPROVED scope

- R2.4 fits the above only. Excluded: R3 plans/results/journal/rollback, R4 preview, R6 API/architecture cleanup, R7 workbench engine, R8 coordinated theme, R9 packages/revisions/provenance. R2.4 establishes the immutable model boundary only.

### Evidence-dependent (resolved by evidence.md)

- Adapter reads of identity/File/origin: audited — none exist; adapters require only token resolution, theme type, and visual metadata.
- TokenKey dynamic use: none in production; static initialization only.
- PM-V7 corpus impact: all 33 bundled themes supply background/foreground; historical themes often omit `type`; changing roots alters accepted imports — hence preserved.

### Engineering (decided during implementation planning, recorded in design.md)

- Exact `Theme` constructor shape and whether the parser uses a local `LinkedHashMap` plus `Map.copyOf` or a tiny package-private construction seam.
- Exact `ThemeRecord`/locator Java shape (record vs final class; `String` filename vs tiny locator) under the "relative managed filename" preference.
- Package placement of `ThemeOrigin`/`ThemeRecord` (core vs eclipse/repository package) under the smallest-understandable-placement rule.
- Whether `Theme` retains any custom equality or uses identity equality.

### Deferred

- R3 applied-state truth, plans, journal, rollback; R4 preview/diagnostics UX; R6 adapter/logging/API cleanup; R7 workbench engine; R8 coordinated theme; R9 extensibility/packages. None are decided here.

## Compatibility and recovery

- No persistence migration: `theme-identities.properties` keys/values unchanged; `SELECTED_THEME_LOGICAL_ID`/legacy `ACTIVE_THEME_ID` keys unchanged.
- No XML changes: no bundled asset edits; parser accepts exactly what R2.3 accepts.
- No adapter signature/schema changes; extension point unchanged.
- Import/update/removal orderings and failure guarantees unchanged; only the in-memory carrier changes.
- Rollback: revert restores the mutable-Theme behavior; no migration to undo.

## Impact

- Production: `core/models/Theme`, `core/parser/XmlThemeParser`, new `ThemeRecord`/`ThemeOrigin`, `eclipse/ThemeRepository`, `ThemeManager`, `ThemeService`, `ThemeOperationOutcome`, `preferences/ThemeSelection`, `preferences/EclipseThemesPreferencePage`, `core/models/Token`, `core/models/TokenKey` tightening. No new bundles, interfaces, dependencies, persistence migrations, or XML edits.
- Tests: existing bundle test tree only; updated fixtures plus focused record/immutability coverage. No new harness.
- Verification: focused unit/contract tests, then strict OpenSpec validation; no Maven reactor for planning (implementation runs it later).

