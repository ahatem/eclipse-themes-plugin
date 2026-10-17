# Tasks

**Approved implementation checklist.** On 2026-10-18 the maintainer approved PM1–PM4 Option A and PM5 Option D and authorized recording/committing that contract, then continuing directly to implementation. Generated identity is durable only after binding persistence; selected intent is not truthful applied-state tracking. No archive, push, PR or next roadmap change is authorized.

## 1. Resolve the contract before implementation

- [x] 1.1 Record explicit PM1–PM5 resolutions, reconcile proposal/design/deltas/tasks and verify every changed compatibility/fallback rule has approval; retain the decision options and evidence limitations in the record.
- [ ] 1.2 Recheck the recorded production baseline and add narrow red regressions for missing-ID equality, same-name independent records, duplicate source IDs, the default-label selection mismatch and same-logical-ID changed-token application dispatch; verify failures express Q3/U4 rather than an incidental fixture error using the existing test setup.

## 2. Establish logical identity without expanding the theme model

- [ ] 2.1 Separate established logical ID from raw legacy source ID and metadata; reuse curated bundled IDs in their domain, keep candidates without established identity distinct, and verify equality/hash and bundled-content/name-change cases with focused tests. Document the identity/source distinction without adding a public API, grammar or revision field.
- [ ] 2.2 Add the minimal plugin-state binding for installed records and lazy admission of existing supported files. Verify ID stability through a new repository instance, unchanged XML bytes/locations, distinct missing/blank/duplicate source claims, and ED3 read/write-failure preservation before advertising an ID. Document metadata ownership and its limited recovery boundary; return to the gate if more infrastructure is needed.

## 3. Connect catalog and reference consumers

- [ ] 3.1 Key admitted catalog records by established identity and sort by label then logical ID; verify independent same-name records survive repeated reloads and source/label edits keep their bindings. Adapt successful admission/selection to the canonical loaded object without choosing a new import collision/update policy; document that copy-before-parse/basename hazards remain deferred.
- [ ] 3.2 Introduce distinguishable logical selected references and the PM4 compatibility reader/default behavior. Test new-reference precedence, unique legacy source match, ambiguous/blank/missing/deleted references, fresh no-selection state and no implicit fallback apply. Verify unrelated preferences/absence are unchanged and stop-management cannot recover a stale new reference after its existing success gate.
- [ ] 3.3 Update identity consumers together, including current markers, list membership, removal equality, curated default lookup and remote-preview source-ID use. Verify removal of one record cannot be classified as removal of another merely by duplicate source ID, defaults target the curated bundled IDs, and generated local IDs do not become website lookups. Document new-reference/downgrade reselection limits; do not fix deletion transactions, preview rendering or Cancel policy here.

## 4. Apply the current definition explicitly

- [ ] 4.1 Remove ID-only suppression from explicit Apply/OK dispatch per PM5. Verify a newly loaded same-ID definition with changed tokens actually reaches the application path, unchanged explicit requests dispatch again, and opening/reload or unresolved confirmation does not dispatch. Use a narrow observable dispatch assertion without adding a general coordinator, digest, revision history or immutable-model refactor; document repeat-apply behavior and retained R3 limitations.

## 5. Verify the approved implementation and stop

- [ ] 5.1 Run affected focused regressions, then the complete deterministic suite and `mvn clean verify -B` after production changes. Record test counts and any necessary corpus-projection explanation, preserve XML/mapping/reset assertions and confirm JavaSE-17/product metadata/dependencies remain unchanged. These are future apply-phase checks, not planning commands.
- [ ] 5.2 Assess whether deterministic isolated binding/reconstruction and controlled preference/dispatch tests prove the implemented boundary. Document sufficiency and stop there if so; use an existing bounded R1 runtime facility only for a concrete Eclipse state-area boundary they cannot prove. Do not provision a runtime or create new harness infrastructure for completeness.
- [ ] 5.3 Run strict change/all OpenSpec validation and `git diff --check`, review implementation against every approved delta scenario, and perform both omission/excess proportionality checks. Record exact results/limitations, require no unapproved import policy or adjacent R2/R3/R4 behavior, and stop without starting the next change.
