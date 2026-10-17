# Proposal

## Why

R1 has already established 40 useful bundle-level tests and demonstrated why real Eclipse consumers must also be checked: stored PDE preferences passed unit checks while live listeners still failed. Contributors need a maintained, reproducible explanation of these layers and their execution, rather than a new framework based on the historical absence of tests in [Q16/B3](../../../docs/audits/2026-10-complete-project-audit.md).

## What Changes

- Formalize the existing Maven/Tycho/JUnit structure, preserving its behavioral regressions and keeping logical layers inside the current runtime bundle's test tree. **Recommendation: no new Maven test module or product bundle split.**
- Document focused tests, the complete automated suite, full reactor verification, and separately invoked real Eclipse verification, including what each proves and its prerequisites.
- Extract only shared test fixtures currently owned by other test classes; retain package-private production seams, controlled stores, temporary inputs, and serial execution. Make broad fingerprint failures explainable without making hashes the primary behavior contract.
- Subject to the Decision Gate, promote reusable R1 probes into maintained, test-only runtime assets with complete launch metadata and a disposable-runtime workflow for Eclipse 2024-09 on Windows x86_64. Preserve archived experiments as historical evidence.
- Synchronize only the renamed R1 item in `docs/ROADMAP.md` and `docs/CHANGE_QUEUE.md`; remove the queue item's presumption of physical test modules. Do not resequence other work.
- Keep later R5 CI, p2 installation acceptance, release hardening, and OS/version matrices deferred. Current Maven invocation remains consumable by people, CI, and other automation without an agent-specific path.

## Capabilities

### New Capabilities

- `regression-verification`: repository testing entry points, layer boundaries, fixture isolation, reviewable regression expectations, and bounded real-runtime evidence. This is an observable repository verification contract, not a new product feature or public API.

### Modified Capabilities

None. Existing parsing, definition, integration, and preference requirements determine the assertions; this change does not redefine their product semantics.

## Impact

Proposed implementation areas: bundle test sources and the existing Surefire configuration if its historical execution name is clarified; `docs/TEST_STRATEGY.md`, contributor testing instructions, and test-only runtime assets under `tools/runtime-tests/`. No production Java, product manifest, exports, target definition, feature, published p2 contents, or GitHub Actions redesign is proposed. JUnit 4.13.2, Surefire 3.5.3, Tycho 5.0.0, JavaSE-17 output, and JDK-21 build separation remain in place. Runtime fixtures mutate only generated disposable storage; there is no product migration, state recovery policy, or rollback change.

The [evidence inventory](evidence.md) compares merged production baseline `e1736e1559d7a280c499e8045b358b3a41a42aed` with checkout `28401ae13fc0b2682d960dc1145697dabb4b6692` after archive. It distinguishes fresh test execution from historical runtime evidence and identifies remaining reproducibility limits.

## Decision Gate

**PM1–PM3 Option A approved; implementation authorized on 2026-10-17.** The maintainer explicitly approved the recommended structure, bounded runtime assets/tooling, and proportionate contributor verification, and requested implementation after artifact synchronization. The alternatives below preserve the decision rationale; they are no longer unresolved choices. No blocking Product / Maintainer decision remains within this scope.

Approval requires caller-supplied p2-enabled Eclipse bootstrap and JDK/tool paths, freshly owned disposable output, and Windows PowerShell 5.1-compatible entry points under `tools/runtime-tests/eclipse-2024-09/`. It does not authorize a new reactor module, product bundle/API, production dependency/export/manifest or target change, generic framework, runtime downloads from Maven tests, CI/release/p2-acceptance work, matrices, or expanded support claims. ED2 must prove fresh present/platform-only absent profiles, path safety, bounded processes, diagnostics and state isolation. If that needs another maintainer decision, return to this gate.

### Evidence-dependent

- **ED1, resolved for structure:** nine test classes/40 tests already execute through ordinary Maven Surefire after Tycho compilation. The full suite also passes from a tracked-files-only source export. There is no demonstrated isolation/lifecycle reason to migrate these tests to a new reactor module.
- **ED2, resolved within the approved boundary:** maintained preparation provisioned fresh present and genuine platform-only absent profiles, including from source-only assets into separate new output. Runtime scope/persistence/consumer/discovery checks, unsafe-path/repository/timeout controls and failure cleanup passed their acceptance checks. Stable bootstrap launch/p2 registry inputs remained unchanged. See [verification.md](verification.md); the older absence heuristic's batch-compiler contradiction and process-wrapper correction are retained as failed intermediate experiments.
- **ED3, known limits:** source-only verification reused the Maven dependency cache and supplied bootstrap/JDK; actual R4_33 Windows/JDK-21 runtime checks and a fresh Java-17 parser fork are recorded separately. Artifact byte-for-byte reproducibility is not established (unused-local/debug code-generation differences were observed). No other OS, Eclipse release, CDT runtime, or Java-17 workbench claim is inferred.

### Product / Maintainer decisions — resolved, Option A for each

| ID / decision | Realistic options and consequences | Recommendation and rationale |
| --- | --- | --- |
| **PM1 — physical structure** | **A:** keep deterministic tests in the existing bundle and add a small non-reactor test-application source/metadata directory only for real runtime checks; minimizes build changes. **B:** migrate to a dedicated Tycho test module; introduces reactor/lifecycle and test-access wiring, with no demonstrated benefit for the current 40 tests. | **A.** Preserve working tests and private production boundaries. A test-only application bundle generated for a runtime launch is distinct from a new Maven module or shipped bundle. |
| **PM2 — committed runtime infrastructure and tooling boundary** | **A:** maintain reusable reset/XML/present/absent probes, metadata, and Windows PowerShell-compatible preparation/verification scripts for a single Windows x86_64 Eclipse 2024-09/JDK-21 baseline. Requires a caller-supplied p2-enabled Eclipse bootstrap and network/cache for preparation, separate from normal Maven tests. **B:** maintain sources plus a manual runbook only; fewer executable assets, but continued manual assembly and weaker repeatability. | **A.** The prior real-listener contradiction justifies complete maintained reproduction. Use Windows PowerShell 5.1-compatible scripts and existing JDK/Maven tools; no mandatory PowerShell 7, container, downloaded runtime in ordinary tests, or test-framework migration. This testing boundary does not redefine supported product platforms. |
| **PM3 — required contributor verification and CI expectations** | **A:** keep ordinary Maven tests as the normal automated suite; require bounded runtime evidence when parser/provider discovery, preference-scope persistence, extension discovery/optional availability, or live editor delivery changes. A contributor may obtain that evidence from a maintainer with the baseline. Docs-only and isolated logical changes need proportionate checks. **B:** make the runtime baseline mandatory for every change/local suite, increasing setup and Windows dependence. **C:** keep all runtime checks advisory, allowing listener regressions to pass solely on doubles. | **A.** Protect the contracts mocks cannot prove without imposing Windows/SWT setup on every contributor. Preserve current CI invocation/triggers; do not claim CI runs the runtime lane or fixes missing `develop` PR coverage. R5 owns that automation policy. |

### Engineering

Subject to PM1–PM3, retain the existing tool versions and serial test lifecycle; extract test-only fixture owners without changing assertions; use repository-relative fixture paths and caller-supplied tool paths; report readable projections on fingerprint failure. Exact helper names and script function decomposition do not create public contracts. Rationale and alternatives are in [design.md](design.md).

### Deferred

- R2: identity/revisions, atomic import, structural validation, immutable model, and their future tests.
- R3/R4: ownership journal, conflict recovery/rollback, aggregate application truthfulness, and preference-page lifecycle/UX tests.
- R5: wrapper/Enforcer/toolchain enforcement, target modernization, CDT runtime, CI triggers/hardening, p2 installation smoke acceptance, releases, and OS/version matrices. Only accurate testing prerequisites are documented here.
- R6/R7+: public adapter API, parser/activator architecture, replacement for the approved PDE internal accessor, and workbench theming. No production API is added to make testing convenient.
