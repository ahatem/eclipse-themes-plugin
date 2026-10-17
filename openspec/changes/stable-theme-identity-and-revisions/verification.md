# Planning verification inventory

## Approval and implementation authority

On 2026-10-18 the maintainer explicitly approved PM1 A, PM2 A, PM3 A, PM4 A and PM5 D and requested a separate `docs: define stable theme identity contract` commit followed directly by implementation. The approval includes durable binding before ID exposure and preservation on failure, and limits this change to selected intent rather than truthful successful-applied-state tracking. It permits deterministic isolated persistence evidence when sufficient, without mandatory runtime provisioning. All alternatives and discovery below remain historical context. The archive commit `aee20ba` must remain unchanged.

## Historical discovery phase

The initial 2026-10-18 handoff was discovery/planning only, with PM1–PM5 unresolved and no implementation. This historical status is superseded by the approval above. Planning-file existence reported by OpenSpec is not itself maintainer approval.

## Evidence inventory

| Planning acceptance | Evidence |
| --- | --- |
| Exact baseline and archive hygiene | `evidence.md` baseline; fetch confirmed PR #24 `9f708fa`, archive-only local commit preserved/rebased as `aee20ba`; no remote writes |
| Current identity roles and persistence | `evidence.md` E1–E10, role map and concept matrix |
| All twelve lifecycle scenarios | `evidence.md` code-trace table, with uncertainty/conditional behavior identified |
| Bundled IDs, duplicate names/IDs, legacy acceptance | 33 source root tags inventoried: no missing/blank IDs or duplicate literal IDs/names; existing missing-ID parser/import fixtures read, not executed |
| Demonstrated problems | Current parser/equality, catalog/storage and page guards directly support Q3/U4; Q4/Q5/removal hazards remain scoped to later work |
| Revision alternatives challenged | `design.md` options A–D; no history/version/digest requirement demonstrated; proposed explicit dispatch is enough for the bounded bug |
| Migration and product choices | Proposal PM1–PM5; lazy metadata/selection compatibility are recommendations, not approved behavior; external import-corpus reliability is unknown |
| Future sanity and scope | `design.md` four future scenarios and required-now/deferred table; no R9 package assumption promoted as a present dependency |
| Future verification | Unstarted tasks contain focused tests/ED3 and bounded runtime/reactor checks only for a later authorized implementation |

## Planning commands

Executed successfully:

- `openspec validate stable-theme-identity-and-revisions --strict` — valid.
- `openspec validate --all --strict` — 10 items passed, zero failed (9 main specs and this draft change).
- `git diff --check` — no whitespace errors in tracked differences; equivalent per-file no-index whitespace inspection also passed for all 9 new untracked planning files.

Read-only inspection, OpenSpec context/status/instructions and the requested baseline Git operations were also used. No Maven/reactor, Eclipse runtime, provisioning, broad suite, CI or production build ran. The pre-existing R1 verification results were reviewed as historical evidence only.

## Manual review and stop

At discovery handoff, manual review covered every new planning file and the archive-only commit diff. Production/build files under `bundles`, `features`, `releng` and the root POM had no difference from `develop`. The three listed capabilities had corresponding deltas and the modified selection requirement preserved its existing scenario. At that time draft SHALL language was conditional on PM1–PM5; approval above now resolves that condition. The twelve lifecycle traces remain historical current-state evidence. No digest/history or import conflict engine was added to the recommendation.

Discovery handoff branch: `feat/stable-theme-identity-and-revisions`; HEAD `aee20ba`, one archive-only commit above `9f708fa`, message `chore: archive regression testing OpenSpec change`. R2 planning files were uncommitted/untracked at that handoff. The existing dated R1 archive was preserved, not duplicated or redated. No production implementation, push, PR or next R2 change had started.

## Approval recording verification

Proposal, design, three deltas and tasks record the approved options and preserve their alternatives/rationale. No schema metadata change is needed. Strict change validation passed and strict all-validation passed all 10 items; `git diff --check` passed. The staged planning files receive whitespace/full-scope review before the separate planning commit; implementation results will be recorded below after execution.

## Historical implementation results and blocking review finding (before PM6 approval)

This section preserves the earlier stopped implementation and its results. The PM6-A approval and completion record below supersede its incomplete/blocked status; the line references describe that earlier working tree.

Approval contract committed separately as `df6a49b` (`docs: define stable theme identity contract`), directly above the unchanged `aee20ba` archive commit. Production/test/doc implementation remains uncommitted on `feat/stable-theme-identity-and-revisions`.

Implemented working-tree model: raw `Theme.getId()` remains XML source metadata; separate nullable logical ID is assigned on admission, used for equality/hash and catalog membership. Bundled IDs are `bundled:<curated ect-id>`; installed records use `local:<UUID.randomUUID()>` stored in a flat `theme-identities.properties` file. Temporary-file atomic metadata replacement completes before an ID is returned. The source/token model, parser security, adapter mappings, runtime JavaSE-17 metadata and build inputs are unchanged. A small package-private selection helper handles modern intent, conservative legacy lookup and curated defaults; page Apply/OK no longer uses the ID-only skip. Raw source metadata continues to drive the preview URL. There is no revision/digest/history or applied-state journal.

### Executed checks

- Before production edits, `ThemeIdentityTest` ran **2 tests, 2 expected assertion failures, no errors/skips**: missing-ID candidates compared equal, and four independent same-name stored files collapsed to one. `tmp/r2-identity-red.log` records the failures. The old default-label lookup and same-ID OK guard were established by direct source inspection; they were not claimed as separate red executions.
- First clean focused run passed **14 tests**, zero failures/errors/skips (`tmp/r2-identity-focused.log`). After adding the actual 33-theme bundled/admission assertion, an incremental compile hit the documented Tycho `ParserTestSupport` resolution problem before tests ran (`tmp/r2-identity-focused-final.log`). No configuration was changed; clean recompilation resolved it.
- Final clean focused run passed **15 tests**, zero failures/errors/skips (`tmp/r2-identity-focused-clean.log`): 6 theme identity/catalog tests, 3 binding persistence/failure tests, 5 preference/pre-widget dispatch tests, plus the existing repository mapping-warning test.
- The normal **`mvn clean verify -B` reactor ran once and passed all five projects and 55 tests**, zero failures/errors/skips (`tmp/r2-identity-reactor.log`). The existing corpus fingerprint remains `e18ab516757839e4a62cada37cdcbfdbfb94b239fa45908fdecfbb55e534a6af`; no expectation was regenerated. The already-running reactor was allowed to finish after PM6 was discovered; success does not resolve that product conflict.
- No Eclipse runtime, provisioning, CI or new harness ran. Existing state-area-to-File and preference-store boundaries did not change; isolated real files/reconstruction and controlled preferences/page guards are sufficient for the new binding/intent logic, with no native rendering or successful-applied-state claim.

### Underengineering / gate return

Review found a missing semantic boundary not caught by the passing suite: `ThemeRepository.importTheme()` lines 55/70 chooses the old basename destination and calls `installedIdentity`; lines 150–151 pass only that filename to `ThemeIdentityBindings.identityFor`, which returns its existing binding at lines 28–34. Therefore importing an unrelated `other-directory/existing.xml` can inherit `installed_themes/existing.xml`'s established logical ID merely from its filename. PM1 explicitly forbids that inference. Catalog reload reuse itself is required and correct; using it for an incoming file needs a product choice. Proposal PM6 records exact options, consequences and the recommended small interim collision refusal, **not implemented or approved**. Production work stopped immediately at this finding. Task 3.1 and final acceptance remain unchecked; the change is not complete or ready to land.

### Overengineering / scope review

No new module, exported identity API, runtime dependency, storage framework, migration engine, source-author namespace, conflict UI, immutable-model refactor, revision/digest/history, R3 journal or next roadmap change entered the diff. Metadata atomic replacement is limited to protecting the newly required binding file. Candidate parse/binding before direct copy is necessary to prevent XML damage on binding failure; it does not provide atomic XML replacement, rollback or a chosen incoming continuity policy. Resolving PM6 must not be disguised as an engineering detail. No further implementation is authorized by this record.

Strict change validation and strict all-validation passed after recording the blocker (**10 items, zero failed**). Tracked diff whitespace checks passed, with new-file whitespace checked separately. Production target/manifest/plugin registration/POM/feature inputs have no changes. Manual review covered changed source, new helpers/tests, docs and planning updates; it exposed PM6, so acceptance is deliberately incomplete despite passing commands. The working diff contains 8 production source files (6 changed, 2 new), 3 new regression classes, 2 documentation files and 4 OpenSpec artifact updates. No implementation commit, archive, push, PR or next roadmap work occurred.

## PM6-A approval and completed implementation

The maintainer approved PM6 **Option A** with the explicit rule that an occupied destination **or any existing binding reservation** must cause refusal before XML, binding, catalog or selection mutation. A missing-file reservation is preserved and diagnosed as existing managed state, not recycled. The approval authorizes finishing PM1–PM5 plus this temporary conservative guard; it does not authorize the next import change. Proposal/design/tasks and the new `theme-import` delta now record this resolution; historical alternatives remain intact.

### Guard and mutation ordering

`ThemeRepository.importTheme` first checks destination absence without following links; an occupied or indeterminate destination is refused. It then reads binding metadata through `requireUnused`, which refuses any existing locator entry without writing or allocating an ID. Only an unused locator proceeds to existing source parsing. The parsed destination is then exclusively created/copied with `CREATE_NEW`, so it cannot truncate an occupied file; only after that copy succeeds does `newIdentityFor` allocate and persist the new binding, and catalog admission follows. If exclusive creation or copying fails, only a destination created by that attempt is removed, best effort, and no binding or catalog entry exists. If binding persistence fails after the copy, that newly created destination is removed best effort; prior bindings, catalog and selected intent remain unchanged. The page changes selection only on successful returned admission. Same-destination import is refused by the first check. No transaction, replacement, renaming, staging framework, keep-both or conflict UI was added.

### Acceptance and regression coverage

| Approved boundary | Evidence |
| --- | --- |
| Existing managed filename refused before mutation | `ThemeIdentityTest.occupiedOrReservedLocatorRefusesImportWithoutChangingEstablishedState`, existing-file case; returns empty, retains exact XML/binding/intent bytes and the same canonical catalog object |
| Existing XML without a binding is also protected | `occupiedUnboundLocatorIsRefusedWithoutAllocatingIdentity`; exact bytes retained and no binding created |
| Incoming source cannot inherit or replace the old ID | Empty import result plus byte-identical bindings and unchanged established catalog identity in refusal cases; import uses creation-only binding entry point, never the reload reuse path |
| Catalog and selected intent are preserved | Same-object catalog assertions, unchanged logical reference and byte snapshots of persisted JFace selected intent; UI failure path returns before its success-only selection/reload branch |
| Reservation whose file is missing is not recycled | Missing-file case of `occupiedOrReservedLocatorRefusesImportWithoutChangingEstablishedState`; no file created, binding/intent bytes unchanged, reserved-locator diagnostic retained |
| Same-destination truncation is prevented | Importing the managed destination itself returns empty and retains its XML and binding bytes |
| Unused locator still succeeds | `bundledCorpusHasCuratedIdentitiesAndImportReturnsAnAdmittedRecord`; new local ID, canonical admission, 33 bundled identities retained, duplicate label/source claim distinct after reload |
| Failed exclusive copy leaves no reservation or partial state | `ThemeRepositoryImportTest.failedExclusiveCopyLeavesNoBindingOrDestinationAndAllowsRetry`; empty result, no binding file, no destination, empty canonical catalog, then normal admission after the obstruction is removed |
| Binding persistence failure after copy removes only the new destination | `ThemeRepositoryImportTest.failedBindingAfterCopyRemovesOnlyNewDestinationAndPreservesPriorState`; new destination absent, pre-existing XML and binding bytes unchanged, prior ID still resolvable, empty catalog, no staged/live residue |
| PM1–PM5 remain satisfied | Existing parser-candidate, equality/hash, current-content, reconstruction, binding-failure, legacy/modern intent, preview/default and pre-widget Apply/OK assertions all pass |

These are isolated filesystem/controlled preference tests and pre-widget page-dispatch checks, not a graphical SWT or native persistence claim. No new runtime setup was needed: the existing state-area-to-File abstraction is unchanged.

### Executed final verification

- PM6 red run before guard changes: **2 expected assertion failures, zero errors/skips** (`tmp/r2-pm6-red.log`), proving occupied imports were admitted by the prior working tree.
- Pre-correction final clean focused run: **17 tests passed**, zero failures/errors/skips (`tmp/r2-pm6-focused.log`): 8 identity/catalog/admission tests, 3 binding tests, 5 selection/dispatch tests and the existing repository warning test.
- Focused admission-ordering run after reordering and adding the regression: **13 tests passed**, zero failures/errors/skips (`tmp/import-ordering-focused.log`): 8 identity/catalog/admission tests, 3 binding tests and 2 new import-failure tests.
- The normal **`mvn clean verify -B` reactor ran again after the admission-ordering correction**: **all five projects and 59 tests passed**, zero failures/errors/skips (`tmp/import-ordering-reactor.log`). Maven 3.9.16 (IntelliJ-bundled) / existing JDK 21 toolchain; no build configuration changes. The corpus fingerprint remains `e18ab516757839e4a62cada37cdcbfdbfb94b239fa45908fdecfbb55e534a6af`.
- All **50 production class files** have major version **61** (JavaSE-17), and the built product jar contains no test/JUnit/Hamcrest entries. Target/manifest/extension/POM/feature inputs are unchanged.
- `openspec validate stable-theme-identity-and-revisions --strict` passed; `openspec validate --all --strict` passed **10 items, zero failed**. `git diff --check` passed; all new files receive equivalent no-index whitespace checks before handoff.

### Final proportionality and remaining boundaries

Underengineering review: PM1–PM6-A have implementation/evidence coverage; occupied and orphan-reserved locators now refuse without state transfer, closing the blocker. Generated identity is exposed only after the exclusive copy succeeds and binding persistence completes; failed copy or late binding save leaves no new binding/catalog record and best-effort removes only this attempt's new destination. Candidates remain distinct; same-name records survive; legacy lookup never guesses; explicit same-ID Apply/OK dispatches current content. Selected intent remains distinct from successful-applied-state truth.

Overengineering review: only two small internal helpers are added for bindings and selected-intent lookup. PM6 adds read-only preflight, creation-only identity admission, exclusive file creation and bounded best-effort removal of only the current attempt's destination to them; no general import/storage/migration framework, new module/dependency, public identity export, content comparison/digest/history, conflict UI, R3 journal or runtime harness is introduced. No unrelated source, bundled XML, adapter or build churn remains in the reviewed diff.

Deferred risks remain explicit: imports are not transactions; cleanup after a failed unused-locator copy or late binding save is best-effort, so a destination the filesystem refuses to remove can remain unbound and be refused as occupied on retry, and source changes during copying are not reconciled. PM6 intentionally refuses reserved locators rather than recycling them. Removal results/paired binding cleanup, real update/install-new targeting, keep-both/rename policy, atomic XML replacement and deduplication remain `atomic-deterministic-theme-import`. Broad validation, immutable definitions and R3 applied-state truth remain their later owners.

### Complete changed-file inventory relative to the planning commit

| Area | Files / reason |
| --- | --- |
| Core (2 changed) | `bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/Constants.java` — curated default IDs; `core/models/Theme.java` under the same source root — logical/source identity distinction and equality/hash |
| Eclipse catalog (1 changed, 1 new) | `bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/ThemeRepository.java` — identity catalog, exclusive-copy-before-binding admission guard and best-effort removal of the attempt's new destination; `eclipse/ThemeIdentityBindings.java` under the same source root — minimal durable bindings and creation-only import admission |
| Preferences (3 changed, 1 new) | `bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/preferences/EclipseThemesPreferencePage.java`, `PreferenceInitializer.java`, `PreferenceKeys.java`, new `ThemeSelection.java` — logical intent, conservative lookup, curated defaults, source preview and explicit dispatch |
| Tests (4 new) | `bundles/com.github.eclipsethemes/test/com/github/eclipsethemes/core/parser/ThemeIdentityTest.java`; `eclipse/ThemeIdentityBindingsTest.java`, `eclipse/ThemeRepositoryImportTest.java` and `eclipse/preferences/ThemeSelectionTest.java` under that test root — 18 new contract tests in total |
| Documentation (2 changed) | `docs/CURRENT_ARCHITECTURE.md`, `docs/TEST_STRATEGY.md` — shipped behavior, failure limits and verification entry point |
| OpenSpec (4 changed, 1 new) | `openspec/changes/stable-theme-identity-and-revisions/{proposal,design,tasks,verification}.md`; new `specs/theme-import/spec.md` — PM6-A approval, coherent admission contract and completed evidence |

Final state: `feat/stable-theme-identity-and-revisions`, HEAD `df6a49b` (`docs: define stable theme identity contract`) above unchanged `aee20ba` (`chore: archive regression testing OpenSpec change`). The **19-file implementation/docs/spec diff remains uncommitted** for senior review. All tasks are complete; the R2 change is not archived. No push, PR or next roadmap change was started.
