# Regression testing evidence

This is the historical planning inventory. PM1–PM3 were subsequently approved and implementation/acceptance results are recorded in [verification.md](verification.md).

## Baseline and authority

Inspected on 2026-10-17, starting with a clean `test/formalize-regression-testing` branch at `28401ae13fc0b2682d960dc1145697dabb4b6692`. Its parent is the requested merged PR #22 baseline, `e1736e1559d7a280c499e8045b358b3a41a42aed`. `git diff e1736e1 HEAD -- bundles pom.xml features releng .github` is empty: the intervening commit archives mapping artifacts and promotes their deltas to canonical specs. No remote fetch or assertion about a newer live remote is made.

Read root, runtime-bundle and releng `AGENTS.md`; no ancestor guide was found above this repository. Read all eight current canonical capabilities, `openspec/config.yaml`, `docs/ROADMAP.md`, `docs/CHANGE_QUEUE.md`, `docs/TEST_STRATEGY.md`, R1/R5 target specs and target-program guidance. The reset, XML-security and mapping archives supply the preceding verification records. Current code and fresh test execution take precedence over historical statements in those records.

The original [audit section 6/Q16 and build finding B3](../../../docs/audits/2026-10-complete-project-audit.md) said no tests existed. That was true of its `ebec341` baseline, not this checkout. Q16's R1 portion now concerns durable organization/reproduction; identity/import/rollback/UI contracts remain later work. B3's absence-of-behavior-tests premise is superseded, but p2 installation acceptance and CI policy remain R5 work.

## Physical execution model

- [Root POM](../../../pom.xml): four child modules (five reactor projects including the parent), Tycho 5.0.0, Java source/target 17. No test module or runtime-test profile exists.
- [Bundle POM](../../../bundles/com.github.eclipsethemes/pom.xml): `eclipse-plugin`, `testSourceDirectory=test`, JUnit 4.13.2 with test scope, Maven Surefire 3.5.3 explicitly bound to `test` as `reset-regression-tests`, `failIfNoTests=true`.
- Fresh logs show Tycho `testCompile`, then ordinary Surefire `JUnit4Provider`. This is **not** a Tycho Surefire/PDE workbench test launch. Eclipse classes on the test classpath do not make these OSGi integration tests.
- [build.properties](../../../bundles/com.github.eclipsethemes/build.properties) has only `src/` as product source. [Manifest](../../../bundles/com.github.eclipsethemes/META-INF/MANIFEST.MF) requires JavaSE-17, core runtime/UI, and exports no packages. Test code is not a product API. Prior full-reactor records inspected the jar and excluded tests/JUnit/probes.
- Only `bundles/com.github.eclipsethemes/test/` contains registered test sources. There is no `test/resources` fixture tree or separate model test class. The 33 committed bundled theme XML files supply corpus fixtures; other XML, themes, files, and stores are constructed inside tests.
- `.classpath` is an ignored IDE file containing only `src/`; it is not clean-clone/Maven configuration. Do not claim PDE IDE discovery of `test/` is already configured.

## Current inventory: nine classes, 40 JUnit methods

All paths below are relative to [the bundle test root](../../../bundles/com.github.eclipsethemes/test/). Counts are an inspected baseline, not a limit on future coverage; loops exercise many cases per JUnit method.

| Package / class | Count | Behavior and evidence limit |
| --- | ---: | --- |
| `core/parser/SecureXmlBuilderTest` | 6 | Every factory control refusal/readback/mismatch; processor/guard setup failures; zero reads/retries; cause identity; resolver/error callbacks; actual JDK provider discovery. No OSGi launch. |
| `core/parser/XmlThemeParserSecurityTest` | 4 | Twelve DTD variants across three entry routes and injected/discovered builders; UTF-16; temporary local canaries; zero resolution callbacks/loopback connections; XInclude/schema non-retrieval; positive monitor calibration. |
| `core/parser/XmlThemeParserCompatibilityTest` | 3 | Metadata, references, aliases/styles, sparse defaults, file routes, and a secondary 33-theme resolved-corpus hash. |
| `core/parser/XmlThemeMappingTest` | 5 | Sixteen true aliases, canonical/last-valid precedence, whole-token winner, warning contents, three distinct role pairs/both orders/one-sided fallbacks, named bundled regressions and exact source color/style checks across all 33 themes. |
| `core/parser/RepositoryMappingWarningTest` | 1 | Accepted conflict warning through import, bundled and installed loading with a fake bundle/logger and temporary directory. This is a controlled repository contract, not an OSGi repository integration test or atomic-import test. |
| `eclipse/preferences/PreferenceResetTest` | 9 | Exact ownership union across six adapters; unrelated values/absence; repeated removal; recorded absent/empty/present values; node/removal/flush failures; contextual result and reset success callback gating; corrected debug/hover/optional keys. No journal or pre-theme reconstruction. |
| `eclipse/preferences/EditorMappingTest` | 4 | Exact debug RGB destinations and historical indications; separated JDT/CDT/PDE/Ant roles/styles/fallbacks; independent hover transitions; optional rich/sparse/repeat/rich roles against true/false defaults. No CDT runtime. |
| `eclipse/preferences/PdeAntListenerTest` | 6 | Real JFace `PreferenceStore` injected as a controlled store, typed Boolean dispatch/repeats/equal defaults, unsupported suffix preservation/exclusion, unavailable/incompatible/null store and dispatch/flush failure. Despite its name it does not instantiate actual PDE/Ant consumers. |
| `eclipse/preferences/AdapterBaselineTest` | 2 | Eighteen ordered mutation digests (six adapters × full dark/sparse dark/sparse light), flush counts; stable token-name assignment and explicit simulated source swap detect fixture weakness. Secondary characterization, not proof of live consumers. |
| **Total** | **40** | 19 methods in parser-package classes (including the repository contract), 21 in preference-package classes. |

### Helpers and fixture ownership

- `core/parser/XmlProviderSpy.java`: delegates to a real JAXP provider; injects control/setup/readback/callback faults and records builds/parses/resolutions without mutating JVM provider properties.
- `core/parser/LoopbackCanary.java`: ephemeral loopback server, bounded response/join, drained connections, explicit calibration, `AutoCloseable` cleanup.
- `eclipse/preferences/MemoryPreferences.java`: ordered string-value/mutation store, absent-key distinction, injected removal/flush faults; unexpected operations and wholesale `clear` fail. It does not model actual Eclipse scope resolution or listeners.
- `eclipse/adapters/editor/PdeAdapterFixture.java`: test-source access to the package-private supplier constructor. The public test helper does not imply an exported production API.
- Shared utility methods currently live on test classes: parser bytes/logger/corpus lookup on `XmlThemeParserCompatibilityTest`/`XmlThemeMappingTest`; adapter lists and generated themes on `AdapterBaselineTest`; sparse themes/token helpers on `EditorMappingTest`. This creates test-to-test ownership coupling, not a reason for a new product abstraction.
- Compatibility/mapping/repository warning tests install and restore the activator singleton/logger reflectively. Some setup/cleanup duplication and cross-class calls deserve test-only consolidation. Keep execution serial while those process-global fixtures exist; no parallel configuration is currently enabled.

## Fresh planning verification

Tools: Maven 3.9.11, Eclipse Adoptium JDK 21.0.12.1, Windows x86_64. The shell has no `mvn` on PATH; this session used `C:/Users/cw/.vscode/extensions/oracle.oracle-java-25.1.0/nbcode/java/maven/bin/mvn.cmd`. That is an execution detail, not a contributor prerequisite. `JAVA_HOME` selected the installed Temurin 21 JDK. The first sandboxed build failed to acquire the external Maven-cache Tycho lock before compiling; the authorized run with cache access succeeded. No lock was deleted and no build configuration changed.

From the repository root:

```text
mvn clean test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am
```

| Experiment | Fresh observation | Local raw output (ignored) |
| --- | --- | --- |
| Existing checkout, clean complete automated suite | 40 tests, 0 failures/errors/skips; three selected reactor projects succeed | `tmp/formalize-baseline-tests.log` |
| Tracked-files-only source export of HEAD, same clean command | 40 tests, 0 failures/errors/skips; three selected projects succeed | `tmp/formalize-clean-source-tests.log` |
| Source export, same module selection with `test` and quoted `-Dtest=SecureXmlBuilderTest` | Exactly six tests; success without `surefire.failIfNoSpecifiedTests=false` | `tmp/formalize-focused-tests.log` |
| Source export, same command with `-Dtest=NoSuchR1Test` | Expected exit 1: Surefire reports no tests matching the selector | `tmp/formalize-no-match-tests.log` |
| `javap -verbose` on freshly compiled `SecureXmlBuilder.class` | Major version 61 (Java 17) | Console inspection |

The export was created with `git archive HEAD` under ignored `tmp/formalize-clean-source/`, excluding old targets, `.classpath`, local helpers, probes, installations and logs. It reused the existing Maven cache/network and installed tools: **not** an empty-cache or network-independent reproduction. It establishes that ordinary tests need no ignored source or existing workspace state. The no-match experiment intentionally fails and does not invalidate the preceding passing full suite.

No new full reactor, Java-17 test fork, p2 install acceptance, or SWT/OSGi smoke was run for this planning-only change. Prior accepted implementation verification records are [reset](../archive/2026-10-13-eliminate-destructive-preference-reset/verification.md), [XML](../archive/2026-10-13-secure-xml-theme-parsing/verification.md), and [mappings](../archive/2026-10-17-correct-editor-preference-mappings/verification.md). They report complete reactors and narrow runtime checks; XML records 13 focused parser tests on both Java 17 and the JDK-21 build JVM. Mapping's final record reports all 40 tests and five reactor projects, helper major version 61, and jar-content checks. Earlier intermediate failures in that record must not be presented as the final result.

## Real-runtime evidence and asset ownership

| Asset | Tracked status and purpose | Missing maintenance/reproduction work |
| --- | --- | --- |
| [PM6Smoke.java](../archive/2026-10-17-correct-editor-preference-mappings/PM6Smoke.java) | Committed archive source. Actual default product PDE accessor before test acquisition; debug painters, JDT semantic manager/new hover, actual PDE/Ant comment attributes/listeners, service isolation, annotation recovery page. | Test application manifest, `plugin.xml`, compilation/registration/provisioning/launch scripts are only in ignored `tmp/mapping-smoke-bundle`; existing instructions require manual assembly and `bundles.info` edits. Source cleanup occurs mainly after success; maintained version needs failure-safe detach/disposal and timeout. |
| [DiscoverySmoke.java](../archive/2026-10-17-correct-editor-preference-mappings/DiscoverySmoke.java) | Committed archive source. Proves absent JDT/PDE preconditions, registry/service filtering, untouched nodes and direct absent accessor failure. | Metadata exists only in ignored `tmp/discovery-smoke-bundle`; the final manually isolated installation was made by removing bundles from a full runtime. Provision a platform-only absence fixture without relying on orphaned bundles or inherited configuration. |
| [PM5RootCause.java](../archive/2026-10-17-correct-editor-preference-mappings/PM5RootCause.java) and root-cause evidence | Committed historical diagnostic experiment; demonstrates String/Boolean store distinction and unsupported suffix color dispatch. | Keep historical. It deliberately exercises defective controls and logged a shutdown race; it is not the normal passing regression suite. |
| [Final mapping values/assertions](../archive/2026-10-17-correct-editor-preference-mappings/runtime-smoke-final-evidence.txt), earlier smoke and root-cause excerpts | Committed durable evidence; final record distinguishes passing corrected checks from earlier contradiction. | Complete raw logs, generated bundles, installation/workspace state and before/after projections remain ignored. Excerpts are evidence, not an executable launcher or complete logs. |
| `tmp/RuntimeResetSmoke.java` | **Ignored**, inspected locally. External Equinox main program; real six-node reset, unrelated values/child/default scope survival. | No tracked source/runner. Seeds the managed-key set dynamically; historical 537-key count predates mapping corrections and must not become a current assertion. |
| `tmp/RuntimeXmlSmoke.java` | **Ignored**, inspected locally. Packaged activator/catalog/parser/JAXP provider; harmless DTDs and resolver/network monitoring. | No tracked source/runner. Depends on a semicolon-delimited Surefire classpath file and `target/test-classes` spy; hardcodes Windows. Maintained version must use explicit resolved runtime inputs and selected test-only helpers, with a responding/calibrated bounded canary. |
| `tmp/eclipse-2024-09`, `tmp/pm6-absent-eclipse`, `tmp/p2`, framework/user/workspace directories | **Ignored generated runtime state**, not clean-clone assets. | Provision fresh outputs; never reuse normal contributor installations/workspaces or claim these paths are required. |

### Historical bounded runtime result

Final mapping evidence: Eclipse Platform `4.33.0.v20240903-0240`, Equinox `3.21.0.v20240717-2103`, JDT UI `3.33.0.v20240822-0459`, PDE UI `3.15.300.v20240815-2201`, Ant UI `3.9.500.v20240721-1149`, Windows x86_64, JDK 21.0.12.1. Programmatic unopened viewers ran on an SWT workbench UI event loop. It is consumer/listener evidence, not interactive visual proof. Present-integration checks observed zero listener exceptions; the absent-install run explicitly retained startup diagnostics from orphaned optional dependencies. The first inherited-configuration absence attempt was rejected as invalid evidence.

Provisioning log `tmp/mapping-runtime-install.log` records p2 director inputs: `https://download.eclipse.org/releases/2024-09`, `org.eclipse.platform.ide/4.33.0.I20240903-0240`, `org.eclipse.jdt.feature.group/3.19.600.v20240903-0240`, `org.eclipse.pde.feature.group/3.16.0.v20240903-0240`, with `win32/win32/x86_64` destination/profile/bundle pool under `tmp/`. The bootstrap itself was a separately installed Eclipse, not supplied by the repository. These observed IDs/versions support a bounded preparation script; they do not prove a new cold provision or pin all transitive target inputs.

### Which contracts need which evidence

| R1 boundary | Primary deterministic proof | Additional actual Eclipse proof |
| --- | --- | --- |
| Security controls, zero reads/retries, DTD rejection, references and alias/model semantics | Parser tests and calibrated local canaries | Packaged classloading/TCCL/JAXP provider establishment and catalog route when that boundary changes; not every alias edit |
| Exact node/key/value/absence and reset failure result | Controlled preferences, explicit rich/sparse/reset assertions | Real instance/default scope separation, persistence and unrelated child nodes when scope/persistence behavior changes |
| Adapter role/destination correctness | Exact controlled-store assertions, including CDT | Debug painter RGB, JDT manager/new-hover use, PDE/Ant actual attributes/events when delivery assumptions change |
| Optional integration handling | Controlled missing/incompatible supplier paths | Actual extension registry and genuinely absent installations; packaged default PDE accessor |
| UI/visual appearance, general application transactions, CDT runtime, other OS/releases | Not established by current R1 suite | Deferred to R3/R4/R5 or later roadmap owners; do not invent their contracts here |

## Commands, toolchain, and outstanding reproducibility gaps

- Focused and complete suites select both the target-definition and runtime-bundle modules with `-am`; a bare `-pl` bundle invocation is not the documented replacement for reactor target resolution. Focused filters still incur Tycho resolution/compilation overhead; there is no independent lightweight pure-core Maven module.
- `mvn clean verify -B` runs the same tests and adds feature/p2 repository construction. It does not launch a workbench or install the generated repository into a clean profile.
- Existing historical focused examples use `surefire.failIfNoSpecifiedTests=false`. Fresh evidence above shows that suppression is unnecessary for these selected projects; retain failure on typos in the proposed runbook.
- A first clean test compile is preferred because both XML and mapping work recorded stale incremental helper output. The documented focused `test` route works after a successful clean compile, as freshly verified.
- `CONTRIBUTING.md` still says JDK 17/Maven 3.9.0 for the build. [Tycho 5.0.0 requirements](https://tycho.eclipseprojects.io/doc/5.0.0/tycho-eclipse-plugin/plugin-info.html) specify JDK 21/Maven 3.9.9. Correct the testing entry-point documentation here; wrappers/Enforcer/toolchain redesign remain R5. [Surefire selector documentation](https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html) supports class/method/glob selection; actual commands above were checked against this repository's pinned 3.5.3.
- `.github/workflows/build.yml` already invokes `mvn clean verify -B` on JDK 21; triggered builds now include deterministic tests through the POM. Its PR trigger remains `main` only, without a runtime lane or p2 install smoke. Target OS entries are resolution inputs, not executed OS tests. No current hosted CI run was independently observed here.
- Digest expectations were historically updated after manual readable comparisons, but the readable corpus/adapter projections and their generator (`tmp/MappingInventory.java`) remain ignored. Preserve the approved digests and explicit regressions; make projections available from maintained test code, and require explained before/after review for expectation changes. Hash replacement without behavioral review is insufficient.
- Existing archived documentation contains links to pre-archive mapping paths. In the implementation, repair relevant testing/support-document links to durable archived evidence; do not rewrite the historical audit or resequence the roadmap.

## Architecture conclusion

**No new physical Maven test module is needed now.** The existing bundle-level structure already separates test output, resolves Eclipse compile dependencies, runs the full suite deterministically and supports focused failure-aware selection from tracked sources. Logical parser/model and controlled preference/repository/adapter layers fit that structure. The missing physical assets are a small maintained, separately invoked runtime test application and complete reproduction metadata/scripts, justified by real classloader/listener boundaries rather than by the obsolete no-tests premise. Their inclusion and contributor obligations remain pending PM1–PM3.

## Planning handoff checks (before maintainer approval)

`openspec validate formalize-regression-testing --strict` passed; `openspec validate --all --strict` passed all nine items (eight canonical capabilities and this change), zero failures. OpenSpec reports all four planning artifact categories present. Markdown links in the new change resolve, new Markdown has no trailing whitespace, and `git diff --check` passes. All implementation checkboxes remain pending; PM1–PM3 are explicitly unresolved.

The reviewed scope is this new change directory plus exactly one naming line in each of `docs/ROADMAP.md` and `docs/CHANGE_QUEUE.md` (the queue line also removes its physical-module presumption). Production/test sources, canonical specs, archived evidence, build descriptors, target and CI configuration are unchanged. Generated source-export/build outputs and raw planning-test logs remain ignored. No implementation, archive, commit, push, PR or additional change was started.
