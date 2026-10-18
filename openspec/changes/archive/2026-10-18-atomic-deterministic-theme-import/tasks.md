# Tasks

**Plan approved 2026-10-18.** PM-I1–PM-I5 and the missing-managed-file removal clarification are recorded as approved in `proposal.md`; the Decision Gate is no longer blocking. Implement only the approved options (PM-I1 B, PM-I2 A, PM-I3 A, PM-I4 A, PM-I5 A) and do not extend scope beyond them. If implementation evidence contradicts an approved assumption, stop and return to the Decision Gate rather than silently choosing a different behavior.

## 0. Record and reconfirm the decision baseline

- [x] 0.1 Record explicit PM-I1–PM-I5 resolutions, the approved conflict contract, and the missing-file removal clarification across proposal/design/deltas/tasks. (Done 2026-10-18: proposal `Decision Gate` section, design removal/update sections, and the `theme-import` delta carry the approved contract.)
- [x] 0.2 Reconfirm current source baseline at implementation time (`git status`, branch, relevant methods) and re-run the focused red regressions for staged-byte divergence, missing update path, unchecked removal, and uppercase-extension rejection. If current evidence differs, return to the Decision Gate instead of implementing a stale assumption.

## 1. Establish locale-independent acceptance and established-binding reads

- [x] 1.1 Make `ParserFactory` select only `.xml` extensions using ASCII case-insensitive comparison independent of the default locale; make `ThemeType.from` use `Locale.ROOT`. Add focused tests for `.xml`, `.XML`, mixed case, unsupported extensions, and a Turkish default locale. Do not add content sniffing, MIME detection, a new parser abstraction, or locale-sensitive name presentation changes.
- [x] 1.2 Add established-only binding operations: read an existing identity without creating one, and remove exactly one expected key. Verify missing/inconsistent bindings refuse, unrelated keys and bytes remain semantically unchanged, atomic replacement is reused, and no lazy generation occurs. Do not add a lock, journal, schema migration, or public API.

## 2. Implement deterministic new-install admission

- [x] 2.1 Create the managed and staging directories with checked results, stage input to a unique `.part` file in one copy, and parse the staged bytes with the intended final `File`. Verify staged files are never catalogued, parse/copy failures remove only staging, and source bytes are not reopened for the commit.
- [x] 2.2 Allocate an unused `<UUID>.xml` locator by checking both filesystem absence and binding reservation, move the staged file without replacement, persist one binding, and admit one catalog record only after both succeed. Verify existing basename locators are never renamed, two imports of the same source basename coexist, same name/source ID/identical bytes do not infer continuity, occupied allocations never overwrite, and binding/move failures leave no admitted record or prior-state change.
- [x] 2.3 Update `ThemeManager`/repository callers for checked staging-directory and operation failures, preserving the existing UI-thread dispatch. Add no background executor, retry framework, or new module.

## 3. Implement explicit identity-preserving update

- [x] 3.1 Add repository update for one selected installed record after all preconditions: existing regular file directly under managed storage, established matching binding, supported source, and staged parse success. Refuse bundled, missing, outside-managed, and unbound/inconsistent targets before any managed write.
- [x] 3.2 Commit with one `ATOMIC_MOVE` + `REPLACE_EXISTING` from staging to the existing locator after re-checking that the target is still present, retain the logical identity and binding, and replace only that catalog entry. Verify success changes bytes but not ID/locator/binding, parse or move failure preserves prior bytes, a target that disappeared before the re-check fails without recreation, `AtomicMoveNotSupportedException` fails truthfully without fallback, staging is cleaned, and no other record changes.

## 4. Implement truthful bounded removal

- [x] 4.1 Add repository removal for one selected installed record only. Remove only its binding key first, delete only its managed file with an observed result, then remove its catalog entry and report success. If the exact managed file is already absent while the binding is established and consistent, confirm absence and continue (approved clarification) without recreating the file; if the managed path exists but is not the expected safe target type/state, fail closed. Refuse bundled, foreign-path, and unbound/inconsistent targets before mutation.
- [x] 4.2 On file-delete failure restore the exact prior binding and report failure; if restoration also fails, report the composite failure without claiming removal. Verify unrelated files/bindings/records and selected intent are preserved, a removed theme does not reappear after reload, and the documented crash residual is not described as transactionality.

## 5. Wire the explicit UI operations and truthful outcomes

- [x] 5.1 Add the separate update action for a selected installed theme and disable update/removal unless the selected record has a `local:` logical ID. Keep import as a new-record action. Use confirmation text that states update replaces XML content while preserving identity and removal permanently deletes the managed file.
- [x] 5.2 Reload and reselect by returned/preserved logical identity after import/update; after removal, reload without selecting the removed identity. Show success only after repository success and verified refresh presence. Report removal-committed/reset-failed separately. Keep SWT/preference work on the existing UI thread and add no widget harness or general operation framework.

## 6. Verify and stop

- [x] 6.1 Add/update focused tests in the existing test tree for parser/type locale behavior, staged new install, same-basename coexistence, staged-byte preservation, binding/move failure cleanup, explicit update success/failure/preconditions, atomic-move refusal, removal success/refusal/restoration, the already-absent-file removal success path versus the missing-update-target failure path, fail-closed unexpected managed path states, and the conflict matrix. Use only narrow package-private seams where the prior existing test pattern already injects failure; add no production test-only abstraction.
- [x] 6.2 Run the focused suite, then `mvn clean verify -B`, and one available-platform manual preference-page smoke for import, update, removal, confirmation, and failure reporting. Do not claim an OS matrix or non-atomic-filesystem verification without direct evidence. (2026-10-18: focused suite and full clean verify passed, 88 tests; post-review focused `ThemeRepositoryRemovalTest` passed, 9 tests, including the reload persistence assertions for task 4.2. The maintainer then manually performed the SWT preference-page smoke — **PASS**; the approved import, explicit update, removal, confirmation, and user-visible failure-reporting paths behaved correctly, and no defect was found. Available PDE runtime: Eclipse Platform 4.40.0.20260604-0652, JDK 25.0.1.8, Windows x86_64. No OS matrix, non-atomic-filesystem test, injected UI failure, or crash recovery is claimed.)
- [x] 6.3 Review implementation against every approved delta scenario, run `openspec validate atomic-deterministic-theme-import --strict`, `openspec validate --all --strict`, and `git diff --check`. Perform omission and excess proportionality checks, update public behavior docs if the support/operation contract changed, and stop without starting `strict-theme-validation-diagnostics`, pushing, opening a PR, or archiving.
