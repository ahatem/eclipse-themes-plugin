# Verification

**Current result — 2026-10-17:** approved implementation and bounded runtime verification are complete. PM1–PM6 remain unchanged. See the final PM6 section below; earlier pauses are preserved as historical evidence. No archive or remote action is authorized.

Implementation authorized by the maintainer's explicit apply request on 2026-10-13. PM1–PM4 Option A remain unchanged. The planning-only boundary in the original artifacts records the preceding request; it is superseded for implementation by this apply request. No archive or remote action is authorized.

## Baseline

`HEAD=c438a29`, branch `fix/editor-preference-mappings`; local `develop`, `origin/develop`, and live `git ls-remote origin refs/heads/develop` all report `c67ece1a15b6d752f4c4dfbeccbaba6d4a1392d3`. `git diff develop..HEAD -- bundles pom.xml features releng` is empty. Initial status contains only the supplied untracked change directory. No superseding production changes were found. Root, bundle, releng guides, roadmap, config, proposal, design, all four deltas and tasks were read. OpenSpec 1.14.1 reports ready, 1/23 tasks complete before implementation.

Installed tools: Maven 3.9.11 at `C:/Users/cw/.vscode/extensions/oracle.oracle-java-25.1.0/nbcode/java/maven/bin/mvn.cmd`; build JDK Temurin 21 at `C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot`. OpenSpec uses the installed Node entry point recorded in design; schema-aware commands require sandbox escalation. PowerShell Maven `-D` arguments are quoted. The first incremental test compile encountered stale Tycho output; regressions use clean compilation.

## Debug regressions (2.1–2.4)

Baseline clean run: 1 test, 1 expected assertion failure (currentIPColor expected 10,20,30; actual null), tmp/mapping-debug-red.log. Corrected focused run is recorded in tmp/mapping-roles-red-debug-green.log. Tests enumerate 16 indication state combinations and repeat apply; reset enumerates indication states, both corrected-key removal failures and flush failure, repeats reset, and asserts success commit gating and zero clears. Provenance is beside the descriptors. Historical booleans, including malformed RGB-shaped values, are deliberately preserved; users must choose desired annotation indication values manually. Originals are unknown; the exact recovery UI path awaits ED2.

## Parser regressions (3.1–3.5)

Baseline role failures: selection #123456 became #abcdef, bundled GitHub selection #3d4149 became #2d333b (tmp/mapping-roles-red-debug-green.log). Before ranking, canonical occurrence #123456 became alias #abcdef (tmp/mapping-roles-green.log). New tests cover all three role pairs in both orders and all four presence combinations, named GitHub/Catppuccin Frappé/Jupyter values and four flags, all 16 true aliases, repeats, invalid/missing canonical colors, equivalent effective flags, no style merging and warning sources/occurrences/winner. Repository warning test covers import, packed and installed routes. Parser comparison used saved pre-role compiled classes and tmp/MappingInventory.java: all 33 metadata projections and unrelated named tokens unchanged. Approved six-role differences additionally affect findScope in seven themes via its unchanged selection-background parent. No inheritance links outside the three additions changed. Corpus digest updated only after that review to 251c86d30bd40784acd92b7e92a61ab96a4635536161d29a106678bb3beeea02. XML security/control failure and character-reference tests pass in tmp/mapping-parser-green.log; that intermediate run fails only the then-unupdated corpus digest.

## Adapter and semantic verification (4–6.2)

Before corrections, tmp/mapping-adapter-red.log reports three intended failures: hover color absent, inheritedMethodInvocation effective default true instead of explicit false, and JDT static-final color from foreground instead of the supplied specific role. After bounded consumer corrections, role/hover/debug/reset checks passed (11 tests, tmp/mapping-adapter-green.log). After optional-enable cleanup separation, combined parser/adapter/reset/security checks passed 32 tests (tmp/mapping-focused-green.log). All focused tests including reviewed fingerprints then passed 34/34, no failures/errors/skips (tmp/mapping-focused-all.log). Tests cover effective defaults true/false, initially sparse, rich/sparse/repeated-sparse/rich, ancestor presence, all four flags cleared/replaced, unrelated semantic values, managed hover and optional-enable reset, every targeted removal/flush failure and success-commit gating.

Readable six-adapter before/after projections in tmp/mapping-inventory-diff.txt preserve original fixture RGB/style assignments for existing named roles; three new roles use 200/201/202. Only JDT static-final/hover/optional-enable removals and PDE/Ant XML-comment source values differ. The pre-role snapshot already included the separately verified debug destination fix; no additional text mapping changed. CDT and debug-console fingerprints are identical, as are unaffected sparse PDE/Ant fingerprints. New broad digest expectations derive from reviewed ordered mutation projections, not shifted reflection indexes.

Full reactor: mvn clean verify -B on Temurin 21.0.12.1 passed all five projects and 34 tests in tmp/mapping-reactor.log. No manifests, dependencies, adapter additions, target, plugin.xml, feature or build descriptor edits. Runtime output compatibility and jar content checks are recorded at final review.

## ED2 runtime contradiction — implementation stopped

Provisioned an isolated Eclipse installation under `tmp/eclipse-2024-09` using the existing 2024-09 repository and Platform/JDT/PDE IUs. Platform `4.33.0.v20240903-0240`, Equinox `3.21.0.v20240717-2103`, JDT UI `3.33.0.v20240822-0459`, Windows x86_64, Temurin `21.0.12.1`. Installation log: `tmp/mapping-runtime-install.log`. This did not modify the repository target, user workspace or installed themes.

Probe source `tmp/MappingSmoke.java` was compiled with `--release 17` into an external test-only application bundle in the disposable installation. The built product jar was copied into that installation; no product exports/dependencies were added. The probe ran a real workbench event loop on its SWT UI thread without opening windows. It used real `ScopedPreferenceStore` instances, `SourceViewerDecorationSupport` annotation painters, `SemanticHighlightingManager` and Java source viewer, newly constructed `SourceViewerInformationControl`, and PDE `XMLConfiguration` / Ant `AntSourceViewerConfiguration` with their real preference-change handlers attached to the editor stores. Internal-class reflection exists only in this ignored external probe. This is executed consumer/listener evidence, not an interactive visual or cross-platform claim.

The definitive run used fresh disposable workspace `tmp/mapping-smoke-workspace3`. Full log: `tmp/mapping-runtime-smoke.log`; durable concise observations and persisted preference values: [runtime-smoke-evidence.txt](runtime-smoke-evidence.txt).

- Debug: both actual annotation-painter color maps updated to current `18,52,86` and secondary `101,67,33` through the editor store's listener; `currentIPIndication=true` and `secondaryIPIndication=false` remained stored unchanged.
- JDT: all four real semantic highlighting states tracked rich → sparse → repeated sparse → rich, with exact instance enabled values and absent sparse color/four style overrides. Deprecated-member default was confirmed true. Mandatory semantics remained available.
- Hover: newly created real source-hover widgets used `35,69,103` for rich and editor/default background `16,17,18` for omission, then returned to `35,69,103`. The consumer reads the pair when a hover is constructed; this does not claim an already-open hover recolors itself live.
- **PDE contradiction:** after applying the rich specific XML-comment token, stored `editor.color.xml_comment=69,103,137` and `_bold=true`, `_italic=false`, `_underline=false`, `_strikethrough=false` were correct, but the real configuration's final comment `TextAttribute` had foreground `0,0,0` and style `0`, rather than the specified color/bold. The smoke failed its PDE consumer assertion.
- **Ant limitation:** the final rich comment attribute had `69,103,137`, style `1`, but the real listener emitted `DataFormatException` stacks from `AbstractAntEditorScanner.adaptToColorChange` during existing style notifications. No exception-free transition or complete Ant smoke is claimed.

Inspection of the **loaded 2024-09 binaries** (`tmp/pde-listener-bytecode.txt`, `tmp/ant-listener-bytecode.txt`, `javap -constants`) confirms that these PDE/Ant consumers branch on `_bold` / `_italic`; other property notifications fall into color-handling paths. PDE style-change handling checks `instanceof Boolean`. The PDE R4_33 source (`XMLConfiguration.adaptTextAttribute`, `adaptToStyleChange`, `BasePDEScanner`) agrees with the loaded implementation. Direct node mutation and unsupported existing style notifications therefore need application/notification investigation beyond simply routing a different token. No speculative repair or style-key cleanup was made.

**Disposition:** followed the maintainer's explicit ED2 stop rule. PM1–PM4 remain approved; proposal PM5 presents a new scope decision, with bounded listener-safe PDE/Ant correction recommended over silently weakening the contract. Tasks 4.3 and 6.3 remain unchecked: absent/available JDT discovery, complete PDE/Ant transitions and manual recovery UI-path verification were not completed before stopping. The smoke aborts at the contradictory PDE assertion, so the later UI-path probe did not run. No approved product decision was changed and no compatibility exception was accepted.

## Final review at pause

`javap -verbose` confirms parser class major version **61 (Java 17)**. Manifest remains JavaSE-17. Product packaging excludes the smoke bundle, JUnit and test classes. Tracked and new-file review found only the approved parser/model/mapping changes, regression tests, author note and active change evidence/status. Accepted specs, audit, roadmap, queue, reset implementation, secure builder, discovery/service lifecycle, CDT/debug-console adapter sources, extension registration, manifests, dependencies and target definition are unchanged. Preference-store writes remain non-atomic under the existing lifecycle; no rollback/recovery claim is added.

The full reactor and isolated regressions pass, but the change is **not complete** because ED2 contradicted its observable contract. Strict validation and task progress at this pause are recorded below. No archive, commit, push, PR, release operation or other OpenSpec change was performed.

Strict validation at pause: change validation passed; all-spec strict validation passed all nine items, 0 failures. git diff --check passed. Product jar inspection found the new parser candidate class and no smoke, Test or JUnit entries. OpenSpec task progress is 21/23; 4.3 and 6.3 remain unchecked. This measures completed task evidence, not complete implementation of the runtime contract.

| Contract area | Concrete evidence | Remaining limitation |
| --- | --- | --- |
| Debug colors and historical state | EditorMappingTest.debugColorsPreserveEveryHistoricalIndicationState; PreferenceResetTest.correctedKeysResetWithoutGuessingHistoricalIndications; actual annotation-painter maps | Exact manual recovery UI path pending |
| Role separation and one-way fallbacks | XmlThemeMappingTest distinct pairs, named bundled regressions and all 33 source projections; EditorMappingTest separated destinations | PDE live-consumer contradiction |
| Alias whole-token ranking and diagnostics | XmlThemeMappingTest all 16 aliases and diagnostics; RepositoryMappingWarningTest all three loading routes | External legacy corpus unavailable, acknowledged |
| Hover explicit/default policy | EditorMappingTest hover transitions; targeted reset failures; actual new hover widget colors | JDT discovery available/absent test incomplete |
| Four optional semantic roles | EditorMappingTest initially sparse and rich/sparse/rich/defaults/flags; real JDT highlighting manager states; reset enable failure coverage | No support matrix expansion |
| XML safety and unchanged mappings | SecureXmlBuilderTest; XmlThemeParserSecurityTest; compatibility references; six-adapter ordered projections; full reactor | Existing import/lifecycle/ownership debt unchanged |


## PM5 resumption and concrete root causes — 2026-10-17

PM5 Option A is explicitly approved, with PM1–PM4 unchanged. Re-read applicable root/runtime guides, roadmap/config and all change artifacts; status/history remain the existing `fix/editor-preference-mappings` at `c438a29` with all prior implementation edits preserved. No production or existing unit-test implementation was modified during this resumption. The OpenSpec update skill synchronized the approved runtime scope; the apply skill was then used for task 4.5 and the root-cause experiment. The new evidence triggered the maintainer's explicit internal-API stop condition before correction.

### Isolated 2024-09 controls

Executed `smoke.PM5RootCause` in fresh disposable `tmp/pm5-root-cause-workspace` using the same existing 2024-09 installation, Temurin 21.0.12.1, unopened SWT viewers and the actual PDE/Ant preference-store listeners on the UI thread. The experiment uses optional-plugin internals only in an external test application; it does not introduce those accesses into the product. `Platform.addLogListener` records the Ant error without swallowing it; normal Eclipse logging remains enabled.

| Control | Exact observation | Root cause / implication |
| --- | --- | --- |
| PDE raw color, before other suffixes | RGB `69,103,137` | The RGB destination itself is correct. |
| Raw node `_bold=true` | Existing PDE store listener gets `java.lang.String`; PDE attribute remains non-bold | PDE's handler requires Boolean, so valid stored state alone does not update its live style. |
| Actual PDE store typed false → true | Existing listener gets `java.lang.Boolean`; PDE attribute becomes bold | Positive control establishes the necessary event type and that the real consumer can render it. |
| Separate public scoped store false → true | Its own listener gets Boolean; existing PDE store listener still gets String; stored bold is true but live bold false | Constructing a separate public store does not deliver the typed event to the store used by open consumers. |
| PDE `_underline=false` | Comment becomes RGB `0,0,0` | Its suffix dispatch treats the unsupported style key as a color property. |
| Ant `_bold=true` | Comment becomes bold | Ant already accepts String booleans for supported bold/italic. |
| Ant `_underline=false` | Real `AntSourceViewerConfiguration.adaptToColorChange` → `StringConverter.asRGB` logs `DataFormatException` | Boolean-as-RGB parsing, not a theme parse or flush failure. No exception suppression was used. |
| Public store retrieval checks | `Platform.getPlugin` returns null; bundle adapter returns null; no registered `IPreferenceStore` service | Those investigated public routes cannot obtain PDE's existing store. |

Loaded 2024-09 bytecode corroborates the controls: `ScopedPreferenceStore` silences its own underlying callback and emits typed values on its own listener list; its direct-node callback forwards the String value. PDE only branches on `_bold`/`_italic` and requires Boolean in `adaptToStyleChange`; unsupported suffixes run its color branch. Ant's supported style handler additionally accepts String `true`, while unsupported suffixes enter RGB parsing. Both `_underline` and `_strikethrough` emitted by the current shared legacy helper therefore take unsupported color paths; the isolated run demonstrates `_underline` independently of other adapter writes. It does not claim a corrected complete smoke or absence of errors.

Full unfiltered runtime output is preserved in [pm5-root-cause-evidence.txt](pm5-root-cause-evidence.txt). The final late `Device is disposed` log is a test-application shutdown race after all root-cause assertions and completion output, not an attributed product mapping defect; the log is retained rather than filtered. There is deliberately no claim of an exception-free runtime pass. Sources/binary traces remain in `tmp/pm5-*-bytecode.txt`. A standalone copy of the focused experiment is [PM5RootCause.java](PM5RootCause.java), independent of the earlier MappingSmoke helper.

### Reproduction

Compile the standalone experiment with the installed JDK 21 `javac --release 17 -cp "tmp/eclipse-2024-09/plugins/*" -d <probe-bundle-directory> PM5RootCause.java`. The probe is a separate bundle named `smoke.mapping`, version 1.0.0, requiring the already installed core runtime/equinox app/ui/ui.ide/ui.editors/jface.text/pde.ui/ant.ui bundles, with the `org.eclipse.core.runtime.applications` extension `rootCause` running `smoke.PM5RootCause`. Register that test-only bundle in the disposable installation's `bundles.info`, without editing the product manifest. Launch JDK 21 `java -jar <2024-09-equinox-launcher.jar> -clean -nosplash -application smoke.mapping.rootCause -data <fresh-disposable-workspace> -consoleLog`. The executed temporary source/bundle/log are `tmp/PM5RootCause.java`, `tmp/mapping-smoke-bundle`, and `tmp/pm5-root-cause.log`.

### Return to the Decision Gate

The actual-store positive control obtains the store through `org.eclipse.pde.internal.ui.PDEPlugin.getDefault().getPreferenceStore()` in test code. Adding that access to the product is a new internal entry-point commitment beyond current key-based integration. It falls within the maintainer's explicit stop condition, even though `IPreferenceStore` setters themselves are public and no mandatory dependency would necessarily be added. Proposal PM6 presents the narrow authorization decision and alternatives. No reflection/internal-store access, listener bridge, refresh/reopen, unsupported-key ownership policy or exception suppression was added to production.

Task 4.5 is complete. Correction regressions (4.6), implementation (4.7), repeat PDE/Ant smoke (4.8), pending discovery/manual UI checks, and post-correction full verification remain unchecked. Earlier 34-test/reactor results remain historical; they are not reported as new PM5 completion evidence. Strict artifact validation and diff checks at this return to gate follow below.

Final checks at the 2026-10-17 PM6 pause: standalone root-cause source compiles with JavaSE-17 bytecode targeting the installed 2024-09 bundles (the deliberately tested deprecated Platform.getPlugin route produces its expected compiler note). Change strict validation passes; all-spec strict validation passes 9/9, 0 failures. git diff --check passes. OpenSpec apply reports ready with 19/27 tasks complete, task 4.5 done and eight tasks pending. The ready CLI state does not override the unresolved maintainer gate. No post-correction reactor or successful ED2 smoke is claimed because no correction was authorized beyond the discovered boundary. No archive, commit, push, PR, target/manifest/dependency change or new OpenSpec change occurred.

## PM6 final implementation and verification — 2026-10-17

The maintainer explicitly approved the single optional, version-fragile PDE accessor. Its only production internal class reference is isolated in package-private `PdePreferenceAccess`, loaded through the optional bundle. There is no new manifest/dependency/export/target entry or parser/model/service dependency on PDE. Absent/incompatible resolution is checked before mutation and reported as `BackingStoreException`, which the existing service handles while continuing other adapters. A live-dispatch incompatibility is also contextual and checked. No separate-store fallback, persistent listener/refresh mechanism, editor reopening or exception suppression is used. Public/upstream replacement remains deferred R6/upstream work.

The local PDE mapping writes explicit node values and sends Boolean notifications to the actual store for supported bold/italic only. Unknown old event payload is null; it is not an invented previous preference value. This repairs repeated/equal-value application and preserves explicit overrides even when defaults are true. Only PDE/Ant unsupported underline/strikethrough writes and managed-key declarations were removed: historical stored values or absence remain untouched, including by bounded reset. All other source roles, keys and supported values remain as approved.

### Red/green regressions and build

`tmp/pm6-listeners-red.log`: the unchanged implementation failed both new regressions, emitting/managing unsupported suffixes. Existing 2024-09 root-cause controls additionally prove the String-versus-Boolean PDE failure and Ant boolean-as-RGB exception. After correction, `PdeAntListenerTest` covers typed true/false events, complete transitions, repeated/default-valued flags, unrelated values/absence, historical unsupported values through repeated reset, null/absent/incompatible store failures before mutation, dispatch incompatibility and flush failure. Unit store injection is package-private and test-only; it is not a production fallback.

Commands used with the same Maven 3.9.11 and Temurin 21.0.12.1 paths recorded above:

```text
mvn clean test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am -Dtest=PdeAntListenerTest -Dsurefire.failIfNoSpecifiedTests=false
mvn clean test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am -Dtest=PdeAntListenerTest,EditorMappingTest,PreferenceResetTest,RepositoryMappingWarningTest,XmlThemeMappingTest,XmlThemeParser*Test,SecureXmlBuilderTest -Dsurefire.failIfNoSpecifiedTests=false
mvn clean test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am
mvn clean verify -B
```

The targeted suite passed **38 tests** (`tmp/pm6-focused.log`). The combined suite including reviewed broad fingerprints passed **40 tests** (`tmp/pm6-all-focused.log`). Full reactor then passed **all five projects and 40 tests**, no failures/errors/skips (`tmp/pm6-reactor.log`, 45.244 seconds). Source and tests were unchanged between this build and the passing runtime probes; subsequent edits are documentation/evidence only.

`tmp/pm6-mapping-inventory-diff.txt` compares readable ordered projections with the prior approved implementation: all 33 parser projections and the other four adapters are unchanged; the only mutation-list differences are removal of unsupported PDE/Ant suffix writes. All retained color/bold/italic values, order and token source assignments agree. Fingerprints were revised only after this comparison; original role fixture assignments remain stable.

### ED2 runtime resolution

Executed rebuilt packaged product in disposable Eclipse Platform `4.33.0.v20240903-0240`, Equinox `3.21.0.v20240717-2103`, JDT UI `3.33.0.v20240822-0459`, PDE UI `3.15.300.v20240815-2201`, Ant UI `3.9.500.v20240721-1149`, Windows x86_64, Java 21.0.12.1. Source: [PM6Smoke.java](PM6Smoke.java); complete unfiltered output: `tmp/pm6-runtime.log`; concise durable values/assertions: [runtime-smoke-final-evidence.txt](runtime-smoke-final-evidence.txt).

The programmatic SWT workbench probe uses unopened real viewers and actual preference-store handlers on the UI thread. It validates concrete consumer attributes and notifications; it is not a screenshot/interactive visual or multi-OS claim.

- The **default product PDE accessor succeeds before the probe itself obtains the PDE plugin store**, proving the real packaged access path, not just injected unit behavior.
- Debug annotation painters consume current RGB `18,52,86` / secondary `101,67,33` through listeners, preserving `currentIPIndication=true` / `secondaryIPIndication=false`.
- JDT's four real semantic states follow rich → sparse → repeated sparse → rich. Sparse color/four style overrides are absent and explicit disabled state survives the default-enabled deprecated-member role.
- Actual new source-hover controls use rich RGB `35,69,103`, omitted/default RGB `16,17,18`, then rich RGB again. No already-open-hover refresh claim is made.
- **PDE and Ant live comments** follow explicit rich RGB `69,103,137` / bold, generic-only fallback RGB `120,154,188` / italic, root fallback RGB `222,223,224` / normal, new rich RGB `171,196,86` / italic, repeated new rich, then original rich. All supported flags are exact and no generic/XML role overwrites occur.
- PDE's actual store observes Boolean true and false events; explicit stored flags remain present even when equal to true defaults. Unsupported suffix notifications are absent. The full present-integration probe observes **zero listener exceptions**, without suppressing logs or handlers.
- Existing registry discovers available JDT. A deliberately incompatible injected PDE accessor produces exactly one existing-service diagnostic, causes no PDE mutation, and other adapters still apply. Injection/reflection into registry internals exists only in the external probe.
- Real preference hierarchy and instantiated page verify **Window → Preferences → General → Editors → Text Editors → Annotations**, both **Debug Current Instruction Pointer** and **Debug Call Stack**, and the **Text as** control. Author documentation now states that exact verified path for manual choice of desired indication values, without claiming original-value recovery.

For task 4.3/optional behavior, [DiscoverySmoke.java](DiscoverySmoke.java) ran in a separate copy of the disposable installation with JDT UI and PDE UI absent. Existing registry contributions remain intact, existing service filtering skips unavailable integrations, both nodes retain their unrelated sentinel/absence, and text selection still applies independently. Direct invocation of the default absent-PDE accessor returns a checked integration failure before mutation. Output: `tmp/pm6-absent-final.log`.

The first attempted external configuration inherited its parent installation and therefore did not satisfy the absence precondition; it is not counted as passing evidence. The final test uses fully isolated `tmp/pm6-absent-eclipse` and fresh workspace `tmp/pm6-absent-workspace2`. Its unfiltered startup log includes expected resolution diagnostics from other deliberately orphaned optional-dependent bundles; the application assertions all pass. This is distinguished from the full present-runtime zero-listener-error result, and does not change product target support.

### Runtime reproduction and final boundary review

Compile PM6Smoke with JDK 21 `javac --release 17 -cp "tmp/eclipse-2024-09/plugins/*"` into an external `smoke.mapping` application bundle, requiring the already installed core/runtime/ui/ui.ide/ui.editors/jface.text/jdt.ui/pde.ui/ant.ui bundles. Application extension `pm6` runs `smoke.PM6Smoke`. Register that external bundle and the rebuilt product jar in the disposable installation's `bundles.info`, using the built product's actual qualifier. Launch its Equinox launcher with `-clean -nosplash -application smoke.mapping.pm6 -data <fresh-workspace> -consoleLog`. For absence, build the separate `smoke.discovery` bundle requiring core runtime/equinox app/ui, application `run` → `smoke.DiscoverySmoke`, and use the isolated install/configuration without JDT/PDE UI. These probe manifests and build outputs are in ignored `tmp/`; product metadata is unchanged.

`javap -verbose` confirms new helper major version **61**, and runtime manifest remains JavaSE-17. Product jar contains the helper but excludes JUnit, fixtures, tests and smoke classes. No accepted specs, audit/roadmap/queue, XML security builder, reset implementation, discovery/service lifecycle, target, manifests, dependencies or packaging descriptors were changed. No speculative historical cleanup or ownership/rollback mechanism was introduced. The unchanged non-atomic application and broader lifecycle limitations remain R3/R4 work; CDT runtime and external legacy corpus gaps remain acknowledged.

### Final strict validation, review and task completion

`openspec validate correct-editor-preference-mappings --strict` passed. `openspec validate --all --strict` passed **9/9 items**, 0 failures. `git diff --check` passed; Git's existing LF/CRLF notice on the baseline test is informational. Reviewed all tracked diffs plus new production helper, regression fixtures/tests, active change artifacts and reproducible probe sources. Only the approved mapping/parser/diagnostic corrections, narrow PDE/Ant runtime work and their test/documentation/evidence changes are present. Accepted specs and unrelated user work are preserved.

Requirement/evidence closure: debug destinations/history → exact apply/reset tests + real painters; role separation/aliases → all 33 source projections and parser diagnostics; hover/semantics → focused transitions + real JDT consumers; PDE/Ant → typed-event/unsupported-suffix unit regressions + actual attributes/errors; optional integration safety → available registry, real absent configuration, checked incompatible service probe; security/reset failures → unchanged focused regressions and full reactor. The manual recovery hierarchy and controls are now verified. ED2 is resolved within the bounded target; ED3 external corpus and CDT runtime limits remain explicitly acknowledged.

All **28/28 tasks are complete** after the final apply-status refresh. The active change is ready for maintainer review. No archive, commit, push, PR, release action, accepted-spec promotion, or new change was performed. There is no further implementation work authorized in this request.
