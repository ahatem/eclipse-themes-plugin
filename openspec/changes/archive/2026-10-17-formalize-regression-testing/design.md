# Design

## Context

See [proposal.md](proposal.md) for motivation and approved PM1–PM3, [evidence.md](evidence.md) for the inspected baseline and fresh runs, and the [verification delta](specs/regression-verification/spec.md) for the repository contract. The maintainer approved Option A for all three decisions and explicitly authorized implementation on 2026-10-17. All stated exclusions and ED2 stop conditions remain binding.

The existing runtime bundle already compiles JavaSE-17 tests through Tycho and runs 40 methods through Maven Surefire/JUnit 4. A clean tracked-source export passes. Physical separation is therefore unnecessary for ordinary regression tests. Real PDE listeners supplied the counterexample to relying entirely on controlled preferences: correct stored Strings did not establish typed live style delivery.

## Goals / Non-Goals

**Goals:** preserve working regression assertions; clarify test/helper ownership and execution; make diagnostic expectations reviewable; maintain complete reproduction of the existing bounded R1 runtime checks using disposable state.

**Non-Goals:** a new framework, Maven test module, product bundle split, test-only public production API, mandatory graphical environment for ordinary tests, or expanded product compatibility. The proposal lists the deferred roadmap owners. In particular, runtime fixture provisioning is not acceptance testing of the generated product p2 repository and does not add a hosted CI lane.

## Decisions

### 1. Logical layers within the current physical structure — approved PM1 Option A

| Logical layer | Physical owner | What it proves |
| --- | --- | --- |
| Fast deterministic core/parser/model behavior | Existing `bundles/com.github.eclipsethemes/test/com/github/eclipsethemes/core/parser/` and any narrowly needed model tests | Parsing, security setup, mapping/alias/fallback rules, model-derived colors/styles. Runs without an Eclipse process, despite resolving Eclipse compile dependencies. |
| Controlled preference/repository/adapter contracts | Existing preference tests, repository warning contract, and package-local fixture bridge | Exact node/key/value/absence, unsupported-key preservation, rich/sparse transitions, reset failure results, typed notification intent. Does not prove real scope/listener consumption. |
| Real OSGi/Eclipse runtime verification | Proposed `tools/runtime-tests/eclipse-2024-09/`, outside the Maven reactor and product distribution | Packaged provider/catalog, actual preference scopes, registry/availability, packaged PDE access, actual debug/JDT/PDE/Ant consumers. |

Keep JUnit 4.13.2/Surefire 3.5.3 and `test/` source roots. The existing `reset-regression-tests` execution name is historical; renaming it to `regression-tests` is acceptable bookkeeping if done without adding a second execution or changing phase/discovery. It is not necessary for test correctness. No categories, JUnit migration, dependency injection framework, or parallel test execution is needed. Class and method selection already works.

Alternative: a dedicated `eclipse-test-plugin` module with Tycho Surefire. It could later own a larger OSGi suite but currently adds reactor resolution, fragment/export/access decisions and test migration without improving the passing deterministic suite. A future demonstrated lifecycle/dependency boundary may justify it; the old Q16 wording does not.

### 2. Consolidate test ownership without rewriting contracts

Extract reusable parser bytes/corpus lookup and activator-log fixture from test-class static methods into test-source helpers. Acquire/restore the singleton and logger in one fixture lifecycle; ensure partially failed setup can restore the original value. Keep warning capture local to each test. Preserve serial execution while any fixture touches global state.

Extract stable adapter lists/theme builders and explicit sparse/token factories from `AdapterBaselineTest`/`EditorMappingTest` into test-source support in the packages that need them. Preserve their RGB/flag assignment and each assertion. Keep `MemoryPreferences`, `XmlProviderSpy`, `LoopbackCanary`, and the same-package `PdeAdapterFixture` as the existing controlled seams. Do not replace them with a generalized testing service or production API.

Use only committed corpus input and generated temporary files. The two current working-directory fallbacks (`themes/` or the root-relative bundle path) can become a single documented helper supporting the root/module entry points; do not depend on an ignored IDE `.classpath`. Standard Maven execution is authoritative. An IDE testing recipe may describe explicit setup, without making editor-specific metadata part of this change.

Alternative: leave shared fixtures on test classes. That works today but makes reusable setup implicitly depend on a particular test's lifecycle. A small extraction addresses the demonstrated coupling while preserving existing tests and packages.

### 3. Keep fingerprints secondary and make them inspectable

Retain the corpus hash and the 18 adapter mutation hashes with their current expected values. Keep exact source/named-color/style and reset/transition assertions as the primary contracts. Reuse the projection construction already present in those tests to emit deterministic readable content on mismatch: corpus file, metadata, named role, explicit presence, RGB and effective flags; adapter ID, fixture variant, ordered operation, key and value, plus the compared digest. No opaque reflection-index renumbering or automatic expectation-update command.

Document the review process: inspect the emitted projection and source diff and explain intended changes alongside the focused behavioral assertions before editing a digest. Compare the previous revision's projection only when needed to explain a difference; exporting a second checkout is not a routine obligation. Do not rely on ignored `tmp/MappingInventory.java` or saved compiled baseline classes. Add or strengthen explicit assertions only for an identified behavioral gap, without duplicating functioning tests for organizational purity.

Alternative: replace all hashes with full golden snapshots. That adds a large parallel expectation surface and churn for mappings already covered explicitly. Readable diagnostics and an expectation-review rule are sufficient for this bounded change; wholesale fixture migration is not justified.

### 4. Maintain bounded runtime assets — approved PM2 Option A

Proposed layout (source/build outputs remain separate):

```text
tools/runtime-tests/eclipse-2024-09/
  README.md
  Prepare-R1Runtime.ps1
  Verify-R1Runtime.ps1
  baseline.properties
  present/                 # test-only application sources, MANIFEST.MF, plugin.xml
  absent/                  # separate application without JDT/PDE requirements
```

These are maintained test assets, not new Maven reactor modules or shipped product bundles. Generate probe jars/classes only under a newly created output directory. Compile with JDK 21 `javac --release 17` and `jar`; load private product implementation types through the installed product bundle, as the existing probes do. Compile selected test-only provider/canary helpers from their source locations if needed; never recover dependencies by scraping a Surefire XML classpath or shipping all test classes with the product.

`baseline.properties` records the observed 2024-09 repository, present/absent root IUs and expected key consumer bundle versions from the evidence inventory. Preparation uses p2 to create two independent profiles: Platform/JDT/PDE (including the observed Ant consumer) and platform-only for genuine JDT/PDE absence. It must verify both preconditions, rather than deleting bundles from the full profile and accepting arbitrary orphan-resolution errors. Pinning these small runtime-fixture inputs is distinct from modernizing the repository target definition; the report still records all resolved versions and makes no full transitive reproducibility claim.

Preparation accepts an explicit, p2-enabled Eclipse bootstrap, JDK path, and fresh output directory. The bootstrap is read-only input; put its writable launch configuration/user/data state under generated storage. It may use the existing installed Eclipse to run the director, but must not modify that install or reuse its user workspace/profile. Network and download/cache needs are explicit preparation prerequisites. Do not silently download a runtime during `mvn test` or assume a developer's already themed Eclipse is a fixture. ED2 requires proving this clean bootstrap/configuration isolation before declaring the scripts reproducible.

Verification accepts the prepared output and an explicit built product jar, compiles the maintained probes, and creates the probe/product registrations programmatically inside generated profiles. No manual `bundles.info` edit or fixed product qualifier is part of the contributor workflow. Do not install the generated product update site: that separate p2 acceptance gate belongs to R5. Validate the supplied jar's identity/version, record its digest and source revision, and report if correspondence to the current source cannot be established.

Promote reusable behavior from the archived `PM6Smoke` and `DiscoverySmoke` and locally inspected reset/XML probes. Keep archives unchanged. The committed PM5 root-cause experiment remains diagnostic history, not a passing smoke test. The maintained files should have behavioral names, not approval-session numbers.

### 5. Runtime assertions and lifecycle

| Check | Maintained assertion scope |
| --- | --- |
| Parser/catalog | Start packaged product in Equinox; discover JAXP under its classloader/TCCL; load bundled catalog; verify required controls and harmless DTD rejection with zero monitored external resolution/connection. Use a responding, calibrated bounded loopback canary; a network regression must not hang the test. Restore TCCL and stop the framework. |
| Reset | Seed current managed-key sets dynamically in all six real instance nodes, then verify repeated bounded reset, unrelated explicit and absent values, child nodes, lower-scope defaults and persistence using a new preference-service lifecycle. Do not assert the obsolete 537-key count or recover guessed historical originals. |
| Debug/JDT | Actual annotation-painter RGB with untouched indication switches; four real optional semantic states through rich/sparse/repeated-sparse/rich; new hover control uses explicit hover and follows omitted/default policy. No already-open hover refresh or visual claim. |
| PDE/Ant | Actual existing store and consumer attributes, full explicit/generic/root/new-rich/repeat transitions, supported bold/italic only, typed PDE true/false events, default-valued explicit overrides, no unsupported suffix notifications. Exercise the default product accessor before the probe obtains the store. Fail on unexpected listener exceptions. |
| Discovery/availability | Real extension registry in present profile; isolated absent profile proves missing JDT/PDE before asserting no mutation and continuing text mappings; incompatible PDE seam remains checked/diagnostic beside available adapters. No new discovery lifecycle contract. |

The archived annotation recovery-page hierarchy experiment remains historical support evidence. The maintained probe omits that optional Eclipse UI traversal: actual debug painter delivery is checked directly, and Eclipse Themes Apply/OK/Cancel/defaults semantics remain R4.

All SWT creation, preference application that drives UI listeners, assertions, listener removal and widget disposal run on the same SWT UI thread. Use `try/finally` cleanup from first allocation, including failed assertions. Remove log/property listeners, uninstall managers, dispose created viewers/color managers/shells, and stop/await frameworks. Borrowed JFace/system resources remain owned by Eclipse. The outer runner bounds each launched process, returns nonzero for timeout or missing completion, and retains stdout/stderr plus workspace logs; success requires completed assertions and successful process termination, not a log marker alone.

Test code intentionally uses fragile internal consumer types/reflection: `EditorsPlugin`, `JavaPlugin`, `JavaSourceViewer`, `SemanticHighlightingManager`, `SourceViewerInformationControl`, `PDEPlugin`, `XMLConfiguration`/PDE color manager, `AntUIPlugin`, `AntSourceViewerConfiguration`, and private attribute/registry fields. Isolate these to the R4_33 test assets, check versions/preconditions before interpreting attributes, and fail explicitly if unavailable. These accesses create no supported product API. The production `PdePreferenceAccess` commitment remains exactly the approved optional accessor; no new export, import, reflection entry point, or mandatory PDE dependency is added to production.

### 6. Contributor commands and obligations — approved PM3 Option A

Document from repository root, with Maven 3.9.9+ and build JDK 21. Show PATH-based commands and optional explicit tool-path configuration instead of copying the evidence machine's VS Code/JDK paths. Quote Maven `-D` arguments in PowerShell. `clean` is the reliable first run and recovery path for changed/new helpers; subsequent focused `test` runs are supported after clean compilation. A focused test still pays Tycho resolution cost.

| Level | Command / planned entry point | Use and limit |
| --- | --- | --- |
| Focused development | `mvn test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am "-Dtest=SecureXmlBuilderTest"` | One class after initial clean compile. Document class/method/comma/glob selectors and examples for parser, reset, mapping and PDE/Ant. An unmatched selector fails; do not copy `surefire.failIfNoSpecifiedTests=false`. |
| Complete automated suite | `mvn clean test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am` | All deterministic tests, Surefire XML/text under the bundle's `target/surefire-reports`; no feature/repository packaging or workbench. |
| Full reactor | `mvn clean verify -B` | Production/build/test-infrastructure changes that affect compilation or packaging. Same suite plus bundle/feature/p2 construction. Does not prove p2 installability or actual consumers. |
| Prepare runtime (proposed) | `powershell -NoProfile -File tools/runtime-tests/eclipse-2024-09/Prepare-R1Runtime.ps1 -BootstrapEclipse <p2-enabled-eclipse> -OutputDirectory <new-generated-directory>` | Windows x86_64, JDK 21, network/cache, existing p2 bootstrap; constructs disposable baseline. No PowerShell 7 requirement. |
| Real runtime (proposed) | `powershell -NoProfile -File tools/runtime-tests/eclipse-2024-09/Verify-R1Runtime.ps1 -RuntimeDirectory <prepared-directory> -ProductJar <built-product-jar>` | Reports the applicable R1 checks, versions, outputs and failures separately from Surefire. Exact script interfaces remain engineering details within the approved inputs/outputs. |

Security/provider or runtime-compatibility work may additionally fork focused parser tests on an explicitly supplied Java-17 executable while Maven stays on JDK 21, using `"-Djvm=<java17-executable>"`. Do not require a second JDK for every contribution, claim Java-17 workbench execution from a JDK-21 launch, or raise the product execution environment to fix build setup. Fresh bytecode checks and the existing Java-17 security evidence have distinct meanings.

Runtime evidence is required for changes to OSGi parser/provider discovery, real preference-scope persistence, registry/availability assumptions or actual consumer delivery. The [evidence layer table](evidence.md) identifies those boundaries; pure alias/model edits remain deterministic unless they reach a consumer assumption. Contributors on other hosts may request/obtain a bounded run from a maintainer; they must report missing runtime evidence truthfully. Existing CI keeps its current Maven command; the `main`-only PR trigger and lack of runtime/p2 CI are disclosed and left to R5.

Update `docs/TEST_STRATEGY.md` to distinguish current R1 coverage and commands from future roadmap acceptance criteria. Link it from `CONTRIBUTING.md` and correct build prerequisites there; avoid a global build-toolchain project. Repair support-doc links to the now archived mapping evidence when referencing it. Roadmap/queue receive only the approved name and the queue description correction already made during this planning pass; unrelated stale sequencing text is not rewritten.

## Risks / Trade-offs

- [Test-only singleton reflection prevents safe parallel classes] → Keep serial execution and centralize reversible fixture lifecycle; defer production logging architecture to R6.
- [Version-fragile runtime consumers] → Single declared R4_33 baseline, exact version reporting/preconditions, explicit incompatibility failures. No other OS/version or CDT runtime claim.
- [Bootstrap/network/cache differences] → Explicit inputs, fresh profiles, recorded IUs, bounded processes, preserved full errors. Prove ED2 from tracked sources and newly provisioned state; do not mask failures by reusing old `tmp/` installations.
- [Destructive fixture preparation] → Resolve absolute paths before any mutation; reject source/ancestor/bootstrap/user-workspace targets and existing unowned directories, including aliases/reparse paths. Generate a fresh owned directory; prefer leaving failure artifacts for inspection over recursive cleanup. All state and p2 bundle pools remain within it.
- [Tests change foreign preferences/defaults] → Only disposable runtime profiles/workspaces/user/config areas; no compensation into user state. A failed probe may leave generated preferences for diagnosis, which is not product rollback.
- [Fingerprint helper extraction masks behavioral drift] → Preserve fixture assignments and hashes, run exact regressions first, inspect readable differences. Add a small diagnostic check or controlled mismatch experiment only where it validates new failure reporting.
- [Runtime preparation grows into release/CI work] → It provisions fixtures and registers a built jar only; no update-site acceptance, CI workflow edits, cache fleet, wrapper or compatibility matrix.

## Migration Plan

After explicit PM1–PM3 approval and a separate apply request, retain the passing test baseline, consolidate test-only owners and diagnostics, add runtime source/metadata and bounded scripts, then update contributor documentation. Run focused tests before the complete suite, full reactor, and applicable runtime checks. Verify jar contents and major version 61 without altering the product manifest. A tracked-source-only run must work without preexisting ignored state; cold provisioning failures must be documented as blockers rather than silently accepted.

No user data or product state migration exists. Reverting test/docs/assets restores the previous developer workflow without changing installed themes. Generated runtimes are disposable; removal, if offered, must validate ownership and absolute containment. No product downgrade or speculative preference restoration is proposed.

## Approved decisions and acceptance evidence

PM1–PM3 Option A are resolved by explicit maintainer approval. ED2 is resolved by the maintained and source-only preparation/runtime runs and negative controls in [verification.md](verification.md). No new module, mandatory external tooling, production access, target change or CI obligation was needed. A future need for any excluded commitment must return to the Decision Gate.

Implementation details resolved within that boundary: shared R1 script/process/path helpers and a test-only runtime-state assertion are local assets; Java/JAR tools and JDK paths are explicit inputs; the source revision is a caller-supplied build label with artifact digest rather than an inferred provenance claim. Preparation copies only bootstrap launch metadata into relocated state and checks stable input hashes. Platform's batch compiler is compatible with a platform-only fixture; absence targets the actual JDT/PDE UI IDs. The verifier accepts Tycho/bnd's equivalent JavaSE-17 capability representation. Error capture covers the default accessor, consumers, service checks and resource cleanup. These choices add no product API or generic runtime-test framework.
