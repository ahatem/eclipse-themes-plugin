# Test Strategy

## Current R1 regression structure

Deterministic tests remain in `bundles/com.github.eclipsethemes/test/`. Tycho compiles them; ordinary Maven Surefire 3.5.3 runs JUnit 4.13.2. There is no new test module. Logical layers need not be physical modules:

- Core/parser/model: security configuration, DTD/external resolution rejection, references, metadata, legacy aliases, complete style winners, role separation and fallbacks.
- Controlled preference/repository/adapter contracts: exact values and absence, rich/sparse/repeat transitions, managed-key reset, unrelated state, failure diagnostics, success gating and typed notification intent.
- Actual Eclipse/OSGi: separate, explicitly invoked [R1 runtime assets](../tools/runtime-tests/eclipse-2024-09/README.md), for classloading, scopes, availability and real consumers that controlled stores cannot establish.

The existing behavioral suite remains and has expanded through identity and import-lifecycle regressions. Shared parser/logging/theme fixtures belong to test helpers, not other test classes or production APIs. Tests using the activator stand-in run serially. `PdeAntListenerTest` uses a controlled JFace store; only the runtime checks instantiate the actual PDE/Ant consumers. CDT has controlled adapter coverage, not runtime coverage. Temporary canaries/stores never use personal workspace data.

R2 identity coverage uses the same JUnit/Surefire setup: `ThemeIdentityTest` exercises parser candidates, curated bundled loading, independent installed records, extension/locale acceptance and restart-equivalent repository reconstruction; `ThemeIdentityBindingsTest` exercises real isolated binding files, established-only reads, removal/restoration and failed metadata replacement; `ThemeSelectionTest` exercises controlled preference intent and the preference page's pre-widget restore and Apply/OK dispatch boundaries. `ThemeRepositoryImportTest`, `ThemeRepositoryUpdateTest` and `ThemeRepositoryRemovalTest` exercise staged-byte commits, repository-allocated locators, atomic update failure, removal ordering and preservation of unrelated files, bindings and catalog records using real temporary managed storage. These do not launch SWT widgets or claim visual/workbench persistence evidence. The state-area-to-`File` lookup itself is unchanged, so deterministic files/reconstruction and controlled preference tests cover the added boundary without a new runtime harness. R3 applied-state truth remains deferred.

Import/update/removal regressions retain byte snapshots of XML and bindings, compare canonical catalog objects and assert no staged residue after success or failure. They cover same-basename and identical-bytes coexistence, staged-byte preservation, binding failure cleanup, missing/bundled/foreign/unbound/inconsistent targets, missing update targets versus already-absent removal, fail-closed unexpected path states, atomic-move refusal without fallback, and restoration of an exact binding after file-deletion failure. Narrow package-private seams inject binding replacement, managed replacement and deletion failures; no SWT harness or production test-only abstraction is added.

## Commands from a clean checkout

Use Maven 3.9.9+ and JDK 21 for Tycho 5.0.0. Set `JAVA_HOME` and put Maven on PATH, or invoke your supplied Maven executable explicitly. These are build prerequisites, separate from JavaSE-17 product bytecode/runtime requirements. No local Eclipse installation is needed for ordinary tests. Initial dependency/target resolution needs network access or a populated Maven/p2 cache.

Run from repository root. First perform clean compilation:

```text
mvn clean test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am
```

This is the complete deterministic suite: all registered classes, with XML/text results under `bundles/com.github.eclipsethemes/target/surefire-reports/`. It does not package the feature/update site or launch Eclipse. A failed test, empty suite or explicit unmatched selector fails. Do not add `surefire.failIfNoSpecifiedTests=false` to hide a typo.

After clean compilation, select focused classes or methods (quote Maven `-D` arguments in PowerShell):

```text
mvn test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am "-Dtest=SecureXmlBuilderTest,XmlThemeParserSecurityTest"
mvn test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am "-Dtest=PreferenceResetTest"
mvn test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am "-Dtest=XmlThemeMappingTest,EditorMappingTest"
mvn test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am "-Dtest=PdeAntListenerTest"
mvn test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am "-Dtest=XmlThemeParserCompatibilityTest#metadataStylesAliasesAndReferences"
mvn test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am "-Dtest=ThemeIdentityTest,ThemeIdentityBindingsTest,ThemeSelectionTest"
```

Class globs such as `"-Dtest=XmlThemeParser*Test"` work too. Keep both the target-definition and bundle module selection with `-am`; target resolution is part of this reactor. Focused tests still incur Tycho startup/resolution. If helper/source changes produce stale incremental output, repeat with `clean test`.

For production or test/build-infrastructure changes, run the full reactor after focused checks:

```text
mvn clean verify -B
```

It runs the deterministic suite and builds bundle/feature/p2 repository outputs under their `target/` directories. It does not establish generated-p2 installation acceptance, a running workbench, or an OS test matrix. Verify test helpers/JUnit/probes are absent from the product jar when packaging/test infrastructure changes. Bytecode major version 61 and JavaSE-17 must remain separate from the JDK-21 build.

For provider/runtime compatibility work, an additional focused Java-17 fork can be run with `"-Djvm=<absolute-java17-executable>"` while Maven remains on JDK 21. A second JDK is not required for every contribution; JavaSE-17 bytecode or a parser fork does not prove a Java-17 workbench run.

## Fingerprints and reviewable expectations

The corpus hash and eighteen adapter-mutation hashes are secondary broad-change detectors. Exact readable parser/mapping/reset assertions remain primary. A hash failure includes the corpus token/metadata or ordered adapter/key/value projection. Inspect the named roles, explicit presence, colors/flags and unrelated mutations before changing an expectation. Preserve fixture color assignments. Add a focused behavioral regression for an intentional new contract; do not rewrite bundled XML or regenerate hashes merely to make the test green.

Review the emitted projection against the changed behavior and explain intended differences before updating a digest. Compare the previous revision's projection only when the source diff and readable assertions leave the difference unclear; a separate source export is not a routine contributor requirement.

## When real runtime evidence is required

| Changed boundary | Deterministic checks | Required additional runtime evidence |
| --- | --- | --- |
| Alias/model/fallback rules only | Exact parser/model assertions and corpus checks | Only when the change also alters a consumer or OSGi assumption |
| OSGi parser/provider discovery | Security controls, fault injection, external access canaries | Packaged provider/TCCL, catalog and harmless prohibited input |
| Actual preference scopes/persistence | Exact managed-key/value/absence and reset failure assertions | Real instance/default scopes, unrelated child nodes and persistence through a fresh lifecycle |
| Extension discovery/optional availability | Missing/incompatible controlled paths | Registry discovery, genuinely absent profile and continuing available integration |
| Live editor consumer/listener delivery | Exact destinations, rich/sparse/repeat and typed-event intent | Debug/JDT/PDE/Ant actual attributes/listeners and packaged PDE accessor |

Use the [runtime preparation/verification instructions](../tools/runtime-tests/eclipse-2024-09/README.md). The maintained fixture is Eclipse 2024-09, Windows x86_64, JDK 21; callers supply p2 bootstrap/JDK/tool paths and newly owned disposable output. Normal Maven tests never download/launch it. Contributors without this baseline may obtain evidence from a maintainer. Report missing runtime evidence explicitly. A docs-only or isolated logical change does not require a Windows environment.

Reports distinguish passing automated tests, reactor packaging, actual runtime results, historical evidence, expected negative controls and unexecuted checks. Unopened SWT viewers establish listener/attribute behavior, not visual appearance. No CDT runtime, other OS/releases, already-open-hover refresh, or expanded product support is implied. Current CI policy is unchanged: triggered `mvn clean verify -B` builds run deterministic tests; hosted runtime/p2 CI and `develop` PR trigger coverage remain R5 work.

## Future roadmap coverage

The layers/cases below remain planning guidance for later product changes. They are not claims that ownership recovery, Apply/Cancel behavior, workbench CSS, p2 installation acceptance or compatibility matrices are already implemented. Promote only the next bounded contract through OpenSpec.

## Layer 1 — pure/core tests

Cover:
- XML security and structural validation;
- color/style parsing and fallback rules;
- legacy property mapping and collision precedence;
- identity/revision equality rules;
- immutable model behavior;
- import candidate validation where filesystem/Eclipse dependencies can be isolated.

## Layer 2 — repository/application contract tests

Cover:
- application result aggregation;
- ownership snapshot including absence;
- A → B → disable restoration;
- external modification conflict preservation.

## Layer 3 — adapter tests

For every supported editor integration, assert exact node/key/value transitions and exact keys that remain untouched. Include rich → sparse theme transitions and semantic-highlight enable/disable behavior.

## Layer 4 — Eclipse runtime/Tycho integration

Use a small runtime suite to establish:
- extension discovery;
- adapter availability/version behavior;
- real preference listener effects where relevant;
- Apply/OK/Cancel/defaults semantics;
- p2 installability;
- workbench CSS switching when that roadmap stage begins.

## Layer 5 — OS/version matrix

Run only for behavior that can vary by SWT/native/backend/Eclipse release. Initial full-workbench matrix should include the declared supported Windows, Linux/GTK, and macOS environments and at least the selected current + previous Eclipse releases.

## Required regression cases from the audit

- external XML entity/DOCTYPE rejection;
- invalid replacement import does not delete previous valid theme;
- distinct legacy themes do not collapse to equal identity;
- arbitrary XML root is rejected or handled by explicit legacy mode;
- `.XML`/extension policy is deterministic;
- optional semantic token transitions retain intended enabled/disabled state;
- GitHub Dark Dimmed selection mapping regression;
- static-final style collision regression;
- unrelated foreign preferences survive apply/reset;
- debug instruction/color keys are mapped correctly;
- application failure cannot persist/report success.
