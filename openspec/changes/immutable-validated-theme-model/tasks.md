# Tasks

**Status: PROPOSED. Do not execute yet.** Implementation may be marked ready after the approved architecture above is recorded; no remaining product decision should be invented unless new evidence contradicts the contract.

## 1. Make Theme immutable and change parser construction

- [ ] 1.1 Remove `logicalId`, `file`, `withLogicalId`, `addToken`, catalog-identity `equals/hashCode` from `Theme`; add snapshot constructor taking `Map<TokenKey,Token>` with `Map.copyOf`; keep `get/has` fallback behavior.
- [ ] 1.2 Change `XmlThemeParser` to build a local `LinkedHashMap`, seed PM-V7 roots at the existing point, resolve winners, and construct `Theme` once. Update parser tests to the construction path.
- [ ] 1.3 Enforce non-null key/color in `Token`; close `TokenKey.register` to private/internal with immediate duplicate failure. Expected breakage: every `new Theme(...)` / `addToken` / `withLogicalId` call site fails to compile — intended, fixed once in step 3-4.

## 2. Introduce ThemeRecord and ThemeOrigin

- [ ] 2.1 Add `ThemeOrigin { BUNDLED, INSTALLED }` plus `ThemeRecord(logicalId, origin, locator, theme)` in the smallest understandable placement (preferred: existing Eclipse/repository package).
- [ ] 2.2 Add focused record tests: bundled origin/identity, installed origin/locator, equality by logicalId only.

## 3. Convert repository catalog to records

- [ ] 3.1 Change `ThemeRepository` catalog to `ThemeRecord`s; map bundled/installed-reload/import/update/removal per design.md; resolve managed targets from record locator + binding, never from `Theme.getFile`.
- [ ] 3.2 Change `ThemeOperationOutcome` success object to `ThemeRecord`; preserve diagnostics semantics; update outcome tests.
- [ ] 3.3 Update `ThemeManager`/`ThemeService` signatures to thread records and unwrap `record.theme()` only at `adapter.apply`.

## 4. Adapt selection, manager, and page

- [ ] 4.1 Retarget `ThemeSelection` to `List<ThemeRecord>`; keep persistence keys and legacy fallback; update selection tests.
- [ ] 4.2 Retarget `EclipseThemesPreferencePage` to records (`origin()`, `logicalId()`, `theme().getName()`); remove `startsWith("local:")` and `getFile` sniffs; keep visible UX identical.
- [ ] 4.3 Remove duplicate old/new models as soon as compilation is green; no parallel-model period longer than needed.

## 5. Preserve adapters and PM-V7 output

- [ ] 5.1 Re-audit every adapter and the extension schema: confirm `apply(Theme, prefs)` unchanged and no identity/File/origin reads; flag any contradiction immediately.
- [ ] 5.2 Confirm PM-V7 fallback output is byte/semantic-equivalent: bundled corpus fingerprint and sparse-default tests pass unchanged; no bundled XML edits.

## 6. Update focused tests

- [ ] 6.1 Theme snapshot: source-map mutation cannot alter Theme; no public post-validation mutation exists; required-field enforcement; duplicate TokenKey refusal.
- [ ] 6.2 Lifecycle: import identity/locator/order unchanged; update preserves logicalId + locator while replacing Theme; removal exact; selection logical-ID based; duplicate display/source IDs independent.
- [ ] 6.3 Adapters receive only Theme and behave unchanged; R2.3 warnings/errors/outcomes unchanged.

## 7. Verify

- [ ] 7.1 Focused suites, then `openspec validate immutable-validated-theme-model --strict`, `openspec validate --all --strict`, `git diff --check`, `git status --short`. Implementation (not this planning) then runs `mvn clean verify -B` for R2.1-R2.3 lifecycle regressions. No push, no PR from planning.
