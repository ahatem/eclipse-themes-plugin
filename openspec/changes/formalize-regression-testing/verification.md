# Implementation verification

The sections through the implementation handoff record the completed pre-cleanup implementation. The final proportionality review below identifies the smaller maintained implementation and the checks rerun after cleanup; earlier runtime results remain historical evidence where those optional checks or metadata were removed.

## Authorization and baseline

On 2026-10-17 the maintainer explicitly approved Option A for PM1–PM3 and requested implementation after synchronization. Proposal, delta, design and tasks were synchronized before implementation. No additional module, product API/dependency/metadata/target, CI/release work or expanded support claim is authorized. ED2's stated stop conditions remain binding.

Work continues on `test/formalize-regression-testing` at `28401ae` (parent `e1736e1` merged PR #22). Initial local changes were the planning directory and two roadmap/queue naming lines. Production/build baseline remains identical to PR #22. No remote update is inferred.

Before test edits, the clean complete deterministic command passed **40 tests, zero failures/errors/skips** on Maven 3.9.11, Temurin JDK 21.0.12.1, Windows x86_64 (`tmp/formalize-apply-baseline.log`). Caller tools used the same explicitly supplied Maven/JDK paths as the planning inventory; no machine paths were made contributor defaults.

## Deterministic fixtures and diagnostics

Shared parser bytes/corpus lookup and reversible logger lifecycle moved to test-only `ParserTestSupport`; stable adapter/theme factories moved to `AdapterTestSupport`. Existing methods/assertions and all corpus/adapter fingerprint expectations were preserved. `ParserFixtureTest` verifies rejected setup and failed/successful fixture bodies restore the prior activator. Corpus and adapter mismatches now include inspectable projections.

The post-extraction clean complete suite passed **41 tests** (the original 40 plus the lifecycle check), zero failures/errors/skips, with unchanged corpus hash (`tmp/formalize-fixtures.log`). The full reactor then passed all five projects and the same suite (`tmp/formalize-reactor.log`). Subsequent focused/negative-control results and final packaging/runtime/reproduction evidence are recorded below as completed, rather than inferred from those runs.

## Runtime preparation evidence

The first attempted preparation path under this checkout was rejected **before creating output**, because the repository is within an existing Eclipse workspace. All actual runtime preparation uses new directories outside that workspace.

The first fresh present-profile director run completed resolution/download successfully, but the original PowerShell 5.1 `Start-Process` wrapper observed an empty exit code. The wrapper correctly returned non-success and left the output marked failed; this is not counted as passing preparation. The process runner was corrected to own the .NET process handle and drain both streams concurrently. A second, newly owned output is used for the repeat experiment; no failed/historical runtime is relabeled or reused as passing evidence.

The next fresh preparation successfully provisioned both profiles, but its overbroad absence check rejected Platform's required JDT batch compiler. Direct inventory showed `org.eclipse.jdt.core.compiler.batch`, not the Java/PDE UI integrations. The check now follows the product's exact `org.eclipse.jdt.ui`/`org.eclipse.pde.ui` availability contract. That output remains failed; a third new directory successfully provisioned and verified present and genuine platform-only absent profiles (`tmp/formalize-prepare-3.log`). No bundles were removed from a full installation to simulate absence.

`Test-R1RuntimeScripts.ps1` passed its source/bootstrap/JDK/workspace/existing-directory/junction rejection, missing-tool, timeout and deliberately unreachable-repository checks. A second run verifies the specific rejection reasons as well as no mutation and retained diagnostics (`tmp/formalize-script-controls-2.log`). The failure-only repository override occurs in an owned copy of the scripts, never the real tracked baseline. Both runs used Windows PowerShell **5.1.26100.9711** without external modules. The normal bootstrap's volatile lock files prevented an early broad read-only hash inventory; no result is claimed from that incomplete inventory. Final preparation records hashes of the stable launch/p2 registry inputs instead.

## Focused, negative and Java-17 deterministic checks

All documented selectors passed: security **10**, reset **9**, parser/editor mappings **9**, PDE/Ant **6**, one parser method **1**, full compatibility class **3**, repository warning class **1** (`tmp/formalize-focused-1.log` through `-7.log`). A deliberately unmatched selector failed rather than skipping successfully (`tmp/formalize-unmatched.log`). Surefire retains exactly its existing single execution; no build descriptor was changed.

Deliberately incorrect corpus and adapter expectations each failed with complete named token/ordered mutation projections. Exact original source bytes were restored in `finally`; the correct expectations then passed **5 tests** (`tmp/formalize-diagnostics-red.log`, `tmp/formalize-diagnostics-green.log`). An initial incremental test compile hit the already documented Tycho helper-resolution issue before running assertions; clean compilation resolved it without a configuration/module change. The diagnostics experiment explicitly uses clean compilation and does not count that initial compile error as a successful negative control.

The full reactor after final test-source cleanup passed all five projects and **41 tests** (`tmp/formalize-reactor-final.log`). All **48 product class files** have major version **61**; the jar excludes test support, fixtures, spies, canaries, probes and JUnit. Tycho/bnd converts the source JavaSE-17 BREE into the equivalent `osgi.ee` JavaSE/version-17 requirement in the built manifest. The maintained verifier checks either equivalent representation; product metadata itself is unchanged.

An additional parser/repository/fixture fork passed **20 tests** on Java **17.0.20.1+1**, with Maven/Tycho still on JDK 21 (`tmp/formalize-parser-java17.log`). This is parser/test-fixture runtime evidence, not a Java-17 workbench claim.

## Maintained runtime execution

The new maintained verifier passed **core, persistence, consumers and absent**, independently reported in `result.json` under the third fresh preparation's `run-d7272566108f45c0a25c994e984542f9`. The input product SHA-256 was `9EB2B4414BABBF9DCE20920F8AC1917BB711B8FD72B848546D87317C6D448F74`; the source label explicitly identifies `28401ae` plus the working diff. Product sources remain identical to that commit; the contemporaneous full build/diff review supplies the source association rather than inferring it from the jar manifest.

- Packaged activator/catalog loaded **33** bundled themes. The real JDK Xerces provider and builder established controls through the packaged helper. Six harmless DTD attempts failed; resolver callbacks/connections were zero, and a separate runtime loopback calibration detected an actual connection.
- Repeated reset removed the dynamically discovered **503** managed keys across six real instance nodes, preserving unrelated text/numbers/absence, child contents and lower-scope defaults. A separate process verified persisted removals and unrelated state in the same disposable workspace.
- Actual debug/JDT/PDE/Ant consumers passed rich/sparse/fallback/repeat/default-valued flag assertions, actual default PDE access, Boolean delivery, unsupported-suffix exclusion, service isolation and the existing annotation support-page check. Observed listener exceptions: **zero**, with no Eclipse log suppression. Probe-created resources/listeners and display were disposed.
- The platform-only process proved absent JDT/PDE UI, real registry/service filtering, no unavailable-integration mutation and continuing text behavior. The batch compiler does not satisfy either UI availability predicate.
- Every process checked and printed actual configuration, instance/workspace, user and `user.home` paths beneath its new run directory. Nothing was borrowed from a historical workspace/profile. Platform versions match `baseline.properties`; Java **21.0.12.1**, Windows x86_64. E4's existing `javax` annotation migration warning and SPI provider INFO remain in unfiltered logs; they are not listener exceptions and were not suppressed.

Raw runtime files are outside the repository at the explicitly supplied generated root `C:/Users/cw/.codex/visualizations/2026/10/17/01a14b19-f9cd-7f63-b719-d5ab23e66188/r1-implementation-3/`; `tmp/formalize-runtime-3.log` records the result path. These are generated evidence outputs, not source prerequisites. The maintained sources/metadata/scripts provide reproduction. Archived PM5/PM6 sources and logs remain unchanged.

## Source-only reproduction and final runtime controls

A manifest-driven snapshot contains **196** tracked/proposed nonignored source files, excluding `.git`, `target`, `tmp`, local helpers, IDE metadata and runtime installations. Since this change is deliberately uncommitted, new proposed source files are explicitly included rather than misrepresented as already committed. The snapshot's full reactor passed **41 tests and all five projects**, reusing Maven's external dependency cache (`formalize-source-reactor.log` beside the snapshot). Maintained runtime source updates are synchronized before final runtime execution; each probe is recompiled into a new run directory.

A separate source-only preparation successfully created another independent present/platform-only pair at `r1-source-runtime`, without copying any existing runtime/profile. All five recorded stable bootstrap inputs (launcher, launch configuration, bundle inventory, p2 pool/profile registry metadata) had identical hashes before/after preparation: `bootstrapLaunchInputsUnchanged=true` (`tmp/formalize-source-prepare.log`, generated `runtime.json`). Actual runtime-state checks separately verified new user/configuration/workspace locations.

The final maintained sources were recompiled by the source-only verifier and **all four checks passed** using the source-only build's product (`tmp/formalize-source-runtime-final.log`). After final input/report review, the reviewed verifier repeated all four checks successfully (`tmp/formalize-source-runtime-reviewed.log`), result `r1-source-runtime/run-3acc1e0630d4406b8a1c776b97a4ec5a/result.json`; product SHA-256 `25654A64C6C569871236B4AEBEB334DB4AA17542E12AEF6629F01B9F05CB44A1`. The final consumer error guard includes default accessor, service-isolation and cleanup stages, uses a thread-safe error collection, and does not suppress Eclipse logs.

Negative consumer controls independently produced the intended failures, never false passing/skipped checks:

| Control | Observed result | Generated run under `r1-implementation-3` |
| --- | --- | --- |
| Assertion after viewer/listener allocation | Consumer process exits 1; owned cleanup and display disposal complete | `run-d294c4c79d09460aaa531f2f50ad069c` |
| Logged listener/error status | Consumer process exits 1 after error detection; cleanup and display disposal complete | `run-93c5ce8d5a1f4a1ba0abf59361e60d17` |
| Missing completion | Process exits 0 but verifier returns failure for missing assertion completion | `run-202db406ed904052aa33cd62e8fea0e9` |
| Timeout | The owned consumer process is terminated at the 30-second bound; result is failed with retained stdout/stderr | `run-a9e51f4990094af596c01bc63262f0bd` |

Assertion/listener controls were repeated after the final through-cleanup error guard; other failure protocols were unchanged. An initial negative-control driver treated native stderr as a PowerShell terminating error before inspecting the already-correct failed result; the driver was corrected to capture native exits. This was not a false passing verifier result.

The final source-only script-control run also passed explicit rejection of **source-tree `tmp/` children** and incompatible PDE baseline metadata before launch (`tmp/formalize-source-script-controls.log`, generated `r1-source-script-controls`). New output is always outside source trees/workspaces. An invalid consumer-failure-control/core-only selection is rejected before any input/runtime mutation (`tmp/formalize-invalid-control-selection.stderr.log`), rather than falsely reporting a passing core run for a control that never executed. Its first sandboxed child-script attempt was blocked by local script authorization before reaching the check; normal authorized local execution verified the intended rejection.

## Requirement coverage and final review

| Delta requirement | Implemented evidence |
| --- | --- |
| Documented verification levels | Contributor link/prerequisites, current test-strategy commands/boundary table, runtime README; exact selectors and preparation/build/verification executed |
| Explicit truthful automated selection | Original single Surefire execution, 41-test complete runs, named focused counts, unmatched selector and intentional fingerprint failures |
| Readable R1 regression contracts | All 40 original methods retained; unchanged approved expectations; exact existing assertions plus tested readable corpus/mutation failure projections |
| Isolated test-owned fixtures | Shared package-local helpers, lifecycle regression on both test JVMs, existing canaries/stores, product jar exclusion |
| Complete runtime assets | Maintained Java/metadata/baseline/scripts and a source-only build plus fresh preparation/verification; no manual registration or hidden classpath |
| Actual consumer boundaries | Packaged provider/catalog, dynamic real-scope reset/persistence, actual consumer attributes/notifications, real present/absent discovery and incompatible access |
| Protected state/resources | Pre-mutation path/junction/workspace/input checks, stable bootstrap input hashes, actual location assertions, UI-thread probes, cleanup/error/timeout controls and retained logs |
| Explicit evidence boundary | Per-check result JSON and unfiltered outputs, exact versions, separate Java-17/bytecode evidence and stated R5/compatibility limitations |

Full tracked diff and every new test/runtime asset were reviewed. Production Java, bundle/build/dependency metadata, target, feature, CI, canonical specs and archives have no diff. Roadmap/queue changes remain exactly their two approved naming lines. No production/test assertion or fingerprint expectation was silently rewritten to obtain a pass. Runtime output/profile state remains generated outside the source/workspace boundary.

### Limits distinguished from failures

- The source-only snapshot reuses Maven's external dependency cache and explicitly supplied JDK/bootstrap binaries. Runtime installations, profiles, workspaces, launch configurations and probe outputs were freshly generated; no ignored historical state supplied passing evidence. This is source independence and bounded runtime reproduction, not an empty-Maven-cache/offline guarantee.
- Only Eclipse 2024-09, Windows x86_64, JDK 21 runtime consumers were executed. Java-17 evidence is the 20-test parser/fixture fork; bytecode review is separate. CDT runtime, visual appearance, other platforms/releases, product p2-install acceptance and hosted CI remain deferred.
- No bit-for-bit artifact reproducibility is claimed. In addition to qualifier/archive differences, comparing the workspace and source-only product jars found differences in `XmlThemeParser.class` and `EclipseThemesPreferencePage.class`. `javap` shows unused-local/exception storage and corresponding debug/offset code-generation differences (for example `astore` versus `pop` for an unused catch variable). Production source bytes are unchanged, all classes retain major version 61, and both built products passed bounded runtime verification. Exact compiler-input/artifact reproducibility remains R5 work; no toolchain/target/production change was made to force equality.
- Normal Eclipse SPI provider INFO and the E4 `javax` annotation migration warning remain in unfiltered output. They were not filtered or treated as listener exceptions. Deliberate negative controls are recorded separately from passing consumer evidence.

The historical planning inventory remains in `evidence.md` and is not substituted for these current checks.

## Implementation handoff before proportionality review

`openspec validate formalize-regression-testing --strict` and `openspec validate --all --strict` pass; the latter reports **9/9** (eight canonical capabilities plus this change). OpenSpec apply reports **all_done, 20/20 tasks**. `git diff --check`, new-source whitespace checks and changed/new Markdown link checks pass. Both workspace and source-only product jars contain 48 major-61 classes and exclude JUnit/test/probe code. The source-only input manifest is refreshed to the final proposed source files; generated build/runtime state remains outside that manifest.

HEAD remains `28401ae` on `test/formalize-regression-testing`. Only approved testing assets, test support/diagnostics, contributor/testing documentation, the two naming lines and this active change are modified. No production code, build/target/dependency/export metadata, canonical spec, historical archive, CI policy, release artifact publication, archive, commit, push, PR or subsequent roadmap change was performed. No blocking maintainer decision or required verification remains within the approved scope; the evidence limits above remain explicit.

## Proportionality review and cleanup

The maintainer requested this pass after implementation, before any commit/archive/remote action. Review covered the complete tracked diff and new helpers/runtime assets against approved PM1–PM3, all delta requirements and R1 evidence. A pre-cleanup copy of the nine edited files is in ignored `tmp/proportionality-before/`; it is comparison evidence, not a workflow prerequisite.

### Classification and disposition

| Substantial piece | Classification | Contract/evidence and simplest sufficient disposition |
| --- | --- | --- |
| Existing deterministic structure, helper extraction and fixture lifecycle regression | Required — keep | Approved PM1 and fixture isolation: shared factories no longer belong to other test classes; the logger restores global state after failure. No new module, framework or production seam. Original 40 tests and the added lifecycle test remain unchanged by cleanup. |
| Corpus/adapter fingerprints and readable failure projections | Required — keep | Explicit readable-regression requirement and task 2.3. Existing hashes are secondary detectors; projections explain mismatches without a parallel golden-file system or updater. |
| Mandatory previous-revision source-export recipe for digest review | Required but unnecessarily complex — simplify | Explained behavioral review is required; exporting another checkout for every review is not. Inspect the projection and source diff first, comparing an older projection only if needed. |
| Separate present/platform-only fixtures, exact baseline and source-built application metadata | Required — keep | Approved PM2 and genuine absence/classloading requirements. A deleted-bundle simulation previously inherited invalid configuration; two p2 profiles prove actual preconditions. Local assets remain outside the reactor/product. |
| Fresh-output, source/ancestor/workspace/input overlap and reparse-path rejection | Required — keep | Explicit unsafe-output scenario and protection of contributor state. Absolute containment and ancestor checks prevent redirected writes to source, normal workspaces or supplied tools. No recursive deletion or new adversarial cases added. |
| Second source-tree rejection control using a `tmp/` child | Redundant — remove | The same ancestor/containment branch rejects both source children; the first source-child control remains. No path validation was removed. Also removed an unused script-results variable. |
| Shared process helper, timeout/tree termination and full stream capture | Required — keep | Explicit truthful failure/resource contract and the demonstrated PowerShell exit-code issue. Preparation, compilation, packaging and launches reuse it. It is bounded Windows tooling, not generic provisioning infrastructure. |
| Runtime ownership/status/baseline record and location assertions | Required — keep | The verifier must reject unprepared/arbitrary installations; actual Eclipse locations must be disposable. These are separate input identity and in-process location boundaries, not interchangeable proofs. |
| Stable bootstrap-input hash comparison | Required — keep | Explicit unchanged-bootstrap contract. The small check detects bootstrap launch/p2-registry mutation that output path checks alone cannot observe. Retain the comparison; publishing its internal hash map is unnecessary. |
| Extra preparation/report metadata | Required but unnecessarily complex — simplify | Keep only consumed preparation identity/status/baseline/error fields and useful result identity/digest/tool/baseline/selection/status/log information. Removed nine fields: preparation `jdk`, `bootstrap`, `createdUtc`, `bootstrapInputHashes`, `bootstrapLaunchInputsUnchanged`; result `schema`, `productIdentityChecked`, `sourceCorrespondence`, `requestedChecks`. The latter repeated the selected check/status or a fixed explanation already in the README. No repository workflow consumed the removed fields. |
| Packaged parser/catalog/canary, real scope reset/persistence and genuine absent/incompatible checks | Required — keep | Explicit runtime requirements: deterministic stores cannot prove OSGi provider discovery, actual scope persistence or registry availability. Positive canary calibration prevents a broken monitor from claiming zero external access. |
| Actual debug/JDT/PDE/Ant transitions, typed events/default PDE accessor and error guard | Required — keep | The demonstrated R1 contradiction was live PDE listener failure despite passing stored-value checks. These assertions remain intact, including error capture through service checks and cleanup. |
| Consumer resource cleanup helper | Required — keep | Multiple acquired viewers/listeners/managers require failure-safe reverse cleanup. A small action stack keeps that ownership readable and attempts all cleanup even if one action fails; no speculative API added. |
| Assertion/listener/missing-completion/timeout controls and script failure controls | Required — keep | Distinct boundaries: assertion exit/cleanup, logged errors otherwise swallowed by Eclipse, successful exit without completed assertions, hung process termination, unsafe inputs and provisioning failure. Combining/removing these would leave different false-pass or state-damage risks unproven. Only the duplicate source-child case was removed. |
| Annotation recovery-page tree/label traversal | Redundant — remove | Design explicitly made this optional. It tests Eclipse's own recovery page hierarchy, not theme delivery; live debug painters already prove the R1 consumer contract. Archived support evidence remains. Removed its invocation and two recursive helpers. |
| Contributor entry points and runtime boundary table | Required — keep | Approved PM3 requires applicable runtime evidence while keeping ordinary logical/docs contributions independent of Windows setup. Removed the routine source-export and extra input-hash recordkeeping obligations; retain explained digest changes, diff/build association and truthful missing-evidence reporting. |
| R2+ lifecycle/UI work, broader environments, generic provisioning, CI/p2/release and exact artifact reproducibility | Useful future work — defer | Existing roadmap owners remain R2–R7 as recorded above. No new deferred project or implementation was generated by this pass. |

### Comparison and permanent rule

Compared with the completed pre-cleanup implementation, executable maintenance surface fell by **37 lines** across preparation (69 → 68), verifier (124 → 117), script controls (109 → 105) and consumers (236 → 211). Nine unconsumed/redundant metadata fields, one duplicate negative control and two recursive UI helpers were removed. More significantly, contributors no longer routinely export a previous checkout or record extra input hashes for a digest/build review. No required behavioral assertion, safety guard, original test method or digest expectation was weakened.

Root [AGENTS.md](../../../AGENTS.md) now contains the exact permanent `Proportional Engineering` / `Proportionality review` rule. It requires concrete justification for complexity, cheapest sufficient evidence, no speculative generalization/future work, a stopping condition, both omission/excess review, and return to the existing Decision Gate for material additional scope. It is repository-wide and adds no product contract or approval gate.

### Verification after cleanup and sufficiency

- All four maintained PowerShell files parse successfully. Preparation changed only serialized metadata; path validation, director arguments, profile validation, subprocess handling and before/after bootstrap hash comparison are unchanged. The removed script case used the same retained source-containment mechanism. Fresh provisioning and all script failure experiments were therefore not repeated.
- The maintained verifier recompiled the current probe sources and passed `-Check Consumers` on the already prepared `r1-source-runtime` baseline, creating a **new** disposable run/configuration/workspace. This is a fresh execution using existing prepared profiles, not a fresh provisioning/source-only claim. Actual debug/JDT/PDE/Ant, typed Boolean/default-valued flags, packaged accessor, incompatible service isolation, zero runtime errors/listener exceptions and cleanup/display disposal passed. Report selection/status and product SHA-256 remain explicit after metadata reduction.
- Result: `C:/Users/cw/.codex/visualizations/2026/10/17/01a14b19-f9cd-7f63-b719-d5ab23e66188/r1-source-runtime/run-bdbb757e301f41d29e1ecdcf350522a7/result.json`; product SHA-256 `9EB2B4414BABBF9DCE20920F8AC1917BB711B8FD72B848546D87317C6D448F74`. Eclipse 4.33, JDK 21.0.12.1, Windows x86_64; full logs retained with the same disclosed INFO/annotation warning. The removed recovery-page traversal did not run.
- Deterministic tests/product/build metadata, core XML/reset/persistence/absence assertions and runner failure protocols did not change, so their already recorded passing evidence remains applicable. No reactor, Java-17 fork, core/absent runtime or negative-control reruns were necessary.
- Final integrity checks passed: strict validation of this change and all nine OpenSpec items, normal `git diff --check`, whitespace checks of all 26 new files, changed Markdown links, and protected-source diff review. New-file checks respect Windows line endings and the manifests' intentional blank section terminators; no unrelated formatting was changed.

All approved `formalize-regression-testing` requirements remain satisfied. Maintenance burden is reduced from the pre-cleanup implementation; retained infrastructure has a distinct contract, actual regression or contributor-state justification. No further unnecessary complexity was identified within this bounded pass. No new tests/functionality, compatibility scope, archive, commit, push, PR or subsequent roadmap work was added.

## Independent-review correction: prove service continuation

The independent review identified a false-pass gap in `EditorConsumers.checkServiceIsolation`: the earlier direct text-adapter call had already installed the expected selection value, so the previous assertion did not prove that the incompatible-PDE service call applied the available text integration. Earlier continuation claims are superseded by this corrected evidence; other consumer results are unchanged.

Immediately before the existing `ThemeService.applyTheme` invocation, the probe now writes and checks `org.eclipse.ui.editors/AbstractTextEditor.Color.SelectionBackground=1,2,3`. After that unchanged invocation it asserts `51,68,85` (the theme's `#334455`). There is no intervening direct adapter application. The existing assertions still require an unchanged PDE snapshot and exactly one diagnostic identifying `PluginEditorThemeAdapter`.

The maintained verifier recompiled the corrected probe with `javac --release 17` and passed only `-Check Consumers`, using the existing prepared Eclipse 2024-09 profiles and a new disposable run. Logs explicitly record the sentinel precondition, the `1,2,3` → `51,68,85` service transition, unchanged PDE state, expected incompatible-access diagnostic, zero unexpected runtime errors/listener exceptions, completed consumer assertions, and cleanup/display disposal. Other existing consumer assertions passed. The disclosed SPI INFO and E4 annotation warning remain unfiltered.

Result and full logs: `C:/Users/cw/.codex/visualizations/2026/10/17/01a14b19-f9cd-7f63-b719-d5ab23e66188/r1-source-runtime/run-0a6ad85663314f91bf83d1a4498be09c/result.json`. Baseline: Eclipse 4.33, JDK 21.0.12.1, Windows x86_64; unchanged built-product SHA-256 `9EB2B4414BABBF9DCE20920F8AC1917BB711B8FD72B848546D87317C6D448F74`.

This correction changes only the existing consumer scenario and this evidence record. No helpers, controls, cleanup, production code, manifests, modules, packaging, target, CI or roadmap scope changed. No deterministic suite, reactor, provisioning, Java-17 fork, core/persistence/absence runtime or negative controls were rerun. No archive, commit, push, PR or further roadmap work was performed.
