# Design

## Context

**Final implementation status — 2026-10-17:** the approved bounded code is implemented. All focused tests, the JDK 21 reactor and real 2024-09 consumer/listener probes pass; available/absent/incompatible integration paths and the real annotation recovery preference page are verified. PM1–PM6 remain unchanged. Earlier pause/status sections below are historical. See verification.md for exact evidence and limits.

**PM6 resumed — 2026-10-17:** the maintainer explicitly authorized the one PDE internal-store accessor, optional and isolated to the PDE boundary. PM1–PM5 remain unchanged. The prior pauses below are historical. The correction uses no additional internal API, mandatory dependency or generic application mechanism.

**Historical PM6 gate status — 2026-10-17:** isolated root-cause controls are complete. Production correction is paused at proposal PM6 because the verified PDE fix path needs access to the existing PDE store through a new internal entry-point commitment. PM5 is approved, not reopened. No production or existing unit-test implementation was changed during this resumption; current code remains the earlier bounded implementation.

**Resumption — 2026-10-17:** PM5 Option A is approved under the narrow boundary in the proposal. Establish the demonstrated PDE/Ant causes, synchronize their runtime contract, then correct only necessary existing integration behavior. No new mechanism or internal API commitment is authorized by inference. The earlier pause and verification record remain historical evidence.

**Historical implementation status — 2026-10-13:** the later explicit apply request authorized this design. Implementation and focused/full reactor verification proceeded with PM1–PM4 unchanged. ED2 then demonstrated a PDE/Ant preference-listener contradiction, so work stopped at proposal PM5. See [verification.md](verification.md). The historical planning-only record below is retained; it does not describe the current execution authority. No listener correction, new policy or compatibility exception has been chosen.

See `proposal.md` for motivation, scope, and the dated Decision Gate resolution. The maintainer **approved PM1–PM4 Option A on 2026-10-13**, explicitly acknowledged the BREAKING compatibility consequences and ED3's unavailable non-bundled corpus, and requested planning updates only. This design records the approved policies; implementation still requires a later explicit apply request. The canonical specs remain unchanged until implementation is reviewed and archived.

### Verified baseline and instructions

On 2026-10-13, `git ls-remote origin refs/heads/develop` returned `c67ece1a15b6d752f4c4dfbeccbaba6d4a1392d3`, matching local `develop` and `origin/develop`. `HEAD=c438a29` on `fix/editor-preference-mappings` includes the archive/spec promotion of the merged secure-XML change; `git diff develop..HEAD -- bundles pom.xml features releng` is empty. No working-tree edits predated this proposal, and `openspec list --json` reported no active changes.

Read the root, runtime-bundle, and releng `AGENTS.md` guides; no additional ancestor guide was found. Read all eight canonical capabilities, OpenSpec config, roadmap, queue, R1 target requirements, and the original Q6/Q7/Q8 audit rows and probe summary. The queue/roadmap baseline is older than the merged reset/XML fixes; the active slice remains the mapping correction.

### Direct current-code evidence

| Finding | Current path and operation | Verified consequence |
| --- | --- | --- |
| Q6 | [TextEditorThemeAdapter.java](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/adapters/editor/TextEditorThemeAdapter.java), `MAPPINGS`, lines 59–60 | Color descriptors write RGB strings into `currentIPIndication` and `secondaryIPIndication` in `org.eclipse.ui.editors`. Their descriptors also advertise these as managed reset keys. |
| Q7 | [LegacyEclipsePropertyMapper.java](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/parser/mapper/LegacyEclipsePropertyMapper.java), lines 58–59, 81, 84; [XmlThemeParser.java](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/parser/XmlThemeParser.java), token insertion at line 105; [Theme.java](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/models/Theme.java), `addToken`, line 68 | Three distinct property pairs resolve to the same key. `Map.put` replaces the whole earlier token in document order, including its options. |
| Q8 | [EclipseThemeAdapter.java](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/adapters/EclipseThemeAdapter.java), `mapSemanticStyleOptional` and `clearSemanticStyle`, lines 59–66 and 109 onward | Missing explicit token writes `.enabled=false`, then removes that key with color/styles. Default scope can become effective again. |

The complete current integration inventory was inspected: text editor; JDT; CDT; PDE; Ant; debug console, their `ManagedPreference` factories, managed-key derivation, `PreferenceReset`, registry contributions, and token inheritance. Relevant consumers are JDT `semanticHighlighting.staticFinalField` and CDT `semanticHighlighting.enumerator` (both currently CONSTANT), PDE `editor.color.xml_comment` and Ant `org.eclipse.ant.ui.commentsColor` (both currently COMMENT). No existing adapter consumes a distinct hover role. Only JDT uses the optional semantic helper: inherited method, inherited field, type argument, deprecated member. Mandatory JDT/CDT semantics and debug-console mappings remain outside the correction.

### Upstream evidence and provenance

Inspected these **Eclipse 2024-09 / R4_33** primary sources, rather than assuming the audit's `master` references match the target:

- [Debug annotation declarations](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/R4_33/debug/org.eclipse.debug.ui/plugin.xml): current/secondary instruction-pointer specifications declare `currentIPColor`/`secondaryIPColor` as color keys and `currentIPIndication`/`secondaryIPIndication` as text boolean keys. Their highlight, text-style, and ruler preferences are distinct; this change does not manage those switches.
- [JDT public preference constants](https://raw.githubusercontent.com/eclipse-jdt/eclipse.jdt.ui/R4_33/org.eclipse.jdt.ui/ui/org/eclipse/jdt/ui/PreferenceConstants.java) and [source-hover consumer](https://raw.githubusercontent.com/eclipse-jdt/eclipse.jdt.ui/R4_33/org.eclipse.jdt.ui/ui/org/eclipse/jdt/internal/ui/text/java/hover/SourceViewerInformationControl.java): the source-hover background color has a separate system-default switch, whose default is true. The consumer uses the color preference only when that switch is false. This is evidence of the preference surface, not permission to import the internal UI class.
- [JDT semantic declarations](https://raw.githubusercontent.com/eclipse-jdt/eclipse.jdt.ui/R4_33/org.eclipse.jdt.ui/ui/org/eclipse/jdt/internal/ui/javaeditor/SemanticHighlightings.java): deprecated-member enablement defaults to true; inherited method invocation, inherited field, and type argument default to false. Removing `.enabled` therefore has a concrete visible default-enabled case. Preference/product/CSS customization can supply other effective defaults too.
- [PDE color constants](https://raw.githubusercontent.com/eclipse-pde/eclipse.pde/R4_33/ui/org.eclipse.pde.ui/src/org/eclipse/pde/internal/ui/editor/text/IPDEColorConstants.java): `editor.color.xml_comment` is the XML-comment destination. Existing Ant XML-editor and CDT enumerator destinations were verified in repository descriptors; no runtime equivalence or new CDT compatibility claim follows from that inspection.

All of these strings remain version-fragile integration couplings. Keep provenance beside the changed mapping descriptors, test exact node/key/value writes, and avoid internal Eclipse Java imports. No new mandatory JDT/debug/PDE dependency is necessary: retain the existing preference API and optional discovery behavior.

### Bundled corpus evidence

Inspected all 33 tracked bundled XML files and compared paired colors and style attributes. Combining this source evidence with the mapper/insertion path establishes the current overwrites without relying on old audit probe outputs.

| Pair present in all 33 themes | Differing colors | Differing style attributes |
| --- | ---: | ---: |
| selectionBackground / sourceHoverBackground | 33 | 0 |
| singleLineComment / xmlComment | 0 | 11 |
| staticFinalField / constant | 9 | 18 |

GitHub Dark Dimmed specifies selection `#3d4149` before hover `#2d333b`, and static-final `#e0823d` before constant `#6cb6ff`; the current parser retains the later member of each collision. Catppuccin Frappe specifies italic generic comments before unstyled XML comments and bold/italic static-final fields before unstyled constants; options are lost because tokens are replaced, not merged. Jupyter is another original static-final style regression fixture. No bundled file supplies both `deprecated` and `deprecatedMember`; synthetic fixtures are necessary for true-alias precedence.

This was code/declaration/corpus inspection, not a new graphical runtime test. Existing historical digest tests protect the wrong behavior as well as the correct behavior; they do not prove these mappings are semantically correct.

## Goals / Non-Goals

**Goals:** correct the bounded writes and token resolution using the current model, mapper, descriptors, and tests; preserve exact unrelated preferences and XML security controls; state one-sided/sparse behavior before implementation.

**Non-Goals:** broad alias/schema validation, parse-result architecture, token immutability/identity, storage migration, preference snapshots/journaling, aggregate application transactions, workbench CSS, new adapters, public API exports, UI restructuring, build/test-module redesign, or an expanded Eclipse/OS matrix.

## Decisions

### 1. Correct descriptors and retire historical wrong keys (EN1, approved PM1-A)

Replace the two text-editor RGB destinations with `currentIPColor` and `secondaryIPColor`. Derived `getManagedKeys()` then includes the correct keys and excludes the boolean indication keys. Preserve their values/absence during apply and reset, even when RGB-shaped: malformed content is not proof of writer ownership, and neither the selected theme nor a guessed default reconstructs an original boolean.

Keep reset's current remove-instance-override behavior for the corrected colors. Do not add a second unconditional historical cleanup set or migrate on startup/application. Conditional RGB cleanup (PM1-B) and unconditional historical-key reset cleanup (PM1-C) were considered and not selected; neither belongs to the approved plan. No heuristic cleanup or recovery claim for unknown original values is permitted.

### 2. Add only three canonical roles (EN1, approved PM3-A)

| Canonical token | Parent for existing `Theme.get` fallback lookup | Explicit integration consumer |
| --- | --- | --- |
| `sourceHoverBackground` | BACKGROUND | JDT `sourceHoverBackgroundColor` with `sourceHoverBackgroundColor.SystemDefault=false` |
| `xmlComment` | COMMENT | PDE XML-comment and Ant comment style mappings |
| `staticFinalField` | CONSTANT | JDT static-final semantic highlighting |

Existing SELECTION_BACKGROUND → BACKGROUND, COMMENT → FOREGROUND, and CONSTANT → STATIC_FIELD → FIELD → FOREGROUND remain intact. Never add reverse links: they cause semantic coupling and can create cycles. Distinct explicit properties must survive either XML order. Change only the relevant mapper entries and adapter consumers; CDT continues to use CONSTANT. Select the full token/options via existing inheritance rather than mixing flags from unrelated roles.

JDT hover is **explicit-only** despite a general background lookup parent: use `Theme.has` to distinguish omission from inherited availability. A narrowly scoped descriptor advertises exactly the color and system-default keys; presence writes color before disabling system-default selection, and omission removes those two overrides, exposing editor defaults. It then participates in existing bounded reset. Do not map the token to Javadoc, native tooltip, workbench, or every hover surface. Deferring these hover writes (PM3-C) was not selected.

Bidirectional sparse cross-filling (PM3-B) and folding roles into the old key with new ordering were considered and not selected. Ordering alone cannot retain two explicit semantic values. The approved one-way general-to-specific inheritance must not be replaced with reverse fallback or cross-filling as an engineering shortcut.

### 3. Resolve true aliases before inserting their winner (EN2, approved PM4-A)

Keep the mapping centralized. For each valid parsed candidate, retain raw source spelling, occurrence position, canonical key, and whole token. Canonical spelling has higher priority than a legacy alias; equal rank uses the last valid occurrence. Root fallback seeds are not explicit source candidates. Invalid/missing colors keep their existing skip/warning behavior and cannot displace a valid lower-ranked candidate. Do not introduce duplicate rejection, a new XML schema, or style merging.

The current non-identity true aliases to cover are:

| Legacy spelling | Canonical spelling |
| --- | --- |
| occurrenceIndication | occurrence |
| writeOccurrenceIndication | writeOccurrence |
| deletionIndication | removedLine |
| searchResultIndication | searchResult |
| filteredSearchResultIndication | filteredSearchResult |
| debugSecondaryInstructionPointer | debugCallStack |
| singleLineComment | comment |
| commentTaskTag | taskTag |
| javadoc | doc |
| javadocTag | docXmlTag |
| javadocKeyword | docTag |
| javadocLink | docLink |
| parameterVariable | argument |
| typeArgument | templateArgument |
| typeParameter | templateParameter |
| deprecatedMember | deprecated |

After role separation, static-final/constant, XML/generic comment, and hover/selection are not aliases. Compare conflicting candidates by color and effective four style flags; absent options and explicitly false flags do not create a spurious style conflict. Warnings must identify sources/occurrences, role, and the winner. Preserve the winner's own options representation; equality checks for diagnostics do not rewrite it.

Use a small Java-only collision resolver/diagnostic callback, connected at the existing Eclipse loading boundary, with a test capture sink. This permits observable warnings without adding another parser-to-activator lookup or introducing SWT dependencies. Keep it internal to this unexported bundle implementation; no generic parser diagnostics API or extension-schema commitment is needed. Existing unknown/invalid logging debt is deferred. Last-valid document order across canonical and alias spellings (PM4-B) and rejecting accepted duplicates (PM4-C) were considered and not selected: the former retains ordering dependence and the latter broadens theme-author requirements.

### 4. Separate style clearing from enable-state policy (EN1, approved PM2-A)

For the existing four optional JDT descriptors, retain the `Theme.has` presence test. Present means enabled true plus complete color/four flags; missing means explicitly enabled false plus removal of color/four flags only. Clearing style properties must not erase the disable decision. Keep any full-cleanup semantics separate so stop/reset still removes every declared key, including `.enabled`, to expose defaults.

Do not turn all semantic mappings into optional ones, use `Theme.get` as presence, add a global semantic enable switch, or introduce a new author-facing enable/disable/inherit schema. The approved policy is bounded to legacy XML and the four existing optional JDT roles; it does not establish a universal future missing-token semantic or equate missing and explicitly disabled tokens in other formats. Editor-default inheritance (PM2-B) and previous-value retention (PM2-C) were considered and not selected.

### 5. Stay within existing lifecycle, preference and OSGi boundaries

#### PM5 bounded listener investigation and correction

On the unchanged 2024-09 binaries, PDE `XMLConfiguration.adaptTextAttribute` and `BasePDEScanner.adaptToStyleChange` distinguish `_bold`/`_italic` and accept a Boolean style event; direct `IEclipsePreferences.putBoolean` reaches the editor `ScopedPreferenceStore` as a String. Ant's style handler also accepts the String `"true"`, so its supported bold/italic values do not have the same event-type failure. Both configurations route other suffixes into color handling. Our shared legacy mapper emits `_underline`/`_strikethrough` for these integrations although their color/style consumer supports only bold/italic. PDE then looks up the unknown suffix as a color key; Ant parses the boolean as RGB and throws. These findings need isolated runtime confirmation before choosing the implementation path.

The correction must deliver the selected XML token's supported color/bold/italic to existing consumers through their transitions and must stop causing the demonstrated non-color notifications to be interpreted as colors. Model/parser retention of all four flags, JDT/CDT application, sparse-role fallback policy, unrelated mappings/preferences and caller-thread/lifecycle behavior remain unchanged. Do not add a persistent event bridge, refresh/reopen editors, suppress exceptions, or introduce optional-plugin internal class access without returning to the Decision Gate.

**Executed controls:** the separate public store emits Boolean events to its own listeners but String events reach the PDE store's listeners, leaving bold disabled. Only the actual-store typed setter positive control enables bold. Loaded `ScopedPreferenceStore.setValue(String, boolean)` uses an instance-local `silentRunning` flag and fires its typed event through that store's listener list; its underlying-node callback forwards String values. This explains why instantiating a public store at the same qualifier is insufficient. Loaded PDE `XMLConfiguration` checks only Boolean style payloads; Ant also accepts the String `"true"`. The two demonstrated failures therefore have distinct, verified causes rather than a generic need to refresh the workbench.

The investigated public retrieval routes failed (`Platform.getPlugin` returns null, no PDE bundle adaptation/service for `IPreferenceStore`). The test-only successful route obtains the existing store using `PDEPlugin.getDefault().getPreferenceStore()`. Production access would add an internal class/entry-point coupling beyond the current preference-key integration. Proposal PM6 records the required maintainer decision. The implementation path, supported-suffix responsibility and preservation of instance-default/reset semantics must be reviewed after that decision; no correction mechanism is selected here.

#### Approved PM6 implementation choices

Resolve `org.eclipse.pde.internal.ui.PDEPlugin` through the optional PDE bundle, invoke only its `getDefault` accessor, then call the inherited public `AbstractUIPlugin.getPreferenceStore`. Keep this in a package-private PDE-specific helper; no internal Java import, exported API, new mandatory PDE dependency or startup hook is added. Resolve before any PDE mapping mutation. Missing class/method/plugin/store, linkage or access failure becomes a contextual `BackingStoreException`, which the existing application service already logs while continuing other integrations. Do not silently write PDE mappings through another store.

PDE and Ant descriptors declare/write only their supported color, `_bold` and `_italic` keys. Excluding unsupported `_underline`/`_strikethrough` prevents their demonstrated false color notifications; existing stored unsupported values are left untouched, including during bounded reset. There is no migration or speculative removal. Other mappings retain their token source and supported style values.

PDE writes explicit booleans to the supplied instance node and then fires a public typed Boolean property notification on the **actual** PDE store for each of the two managed flags. This is local to PDE application and uses no persistent listener/refresh framework. `oldValue=null` means no previous payload is supplied; it permits deterministic reapplication even if a previous writer stored the desired value with an ineffective String event. Using only `setValue` would remove overrides equal to defaults or skip equal values, and writing the node after it could again send a String event. Explicit writes plus typed notification preserve presence, repeatability and default-valued true flags. Flush the original node once; keep exceptions/failure ordering visible and non-atomic as before. Ant needs no internal store accessor because its existing supported style handler accepts String booleans.

The R4_33 PDE UI binary is `3.15.300.v20240815-2201`; exact class/member provenance and compatibility risk belong beside the helper and in verification. Structural incompatibility fails visibly without affecting unrelated adapters. This does not claim support for other PDE versions. Replacement with public/upstream access remains deferred R6/upstream work.

No SWT objects/resources are added; mutations stay on the caller's existing thread so listeners keep their UI-thread assumptions. Parser work and mutation ordering remain separate: alias decisions occur before adapter writes. No jobs, startup migration, manifests, extension registration, exported packages, physical bundle splits, or JavaSE-17 changes are needed.

Writes/removals and flushes remain non-atomic. A per-key failure can leave partial changes before persistence; writing the hover color first reduces the chance of activating stale color but is not rollback. Preserve current flush exception propagation at the adapter boundary and reset's contextual failure/success gating. Do not add compensating changes without trustworthy snapshots. General apply failure swallowing/early active-state persistence is existing Q9/R3 debt; this proposal does not claim to repair it or make every application transactional. Tests must ensure failures do not trigger speculative debug repair, whole-node clearing, or new success claims.

## Risks / Trade-offs

- **Changed appearance/legacy expectations** → BREAKING consequences explicitly acknowledged with PM1–PM4 approval; test both document orders, one-sided pairs, and the whole corpus; document intentional differences rather than silently regenerate fingerprints. ED3 remains an acknowledged evidence limitation.
- **Historical indication corruption remains** → approved PM1 preservation policy and concrete manual recovery documentation; valid booleans, malformed values, and absent keys remain protected. No promise of original-value recovery.
- **Sparse hover removes user overrides within the new managed pair** → approved PM3 scope includes remove-to-default semantics; disclose this behavior and test preservation of all other JDT values. Journaling/conflict-aware restoration remains R3.
- **Preference listeners/defaults vary** → ED2 targeted runtime smoke on existing 2024-09 target, including the default-enabled deprecated-member role. Honor existing accessibility/high-contrast pathways; do not assert universal rendering or expand OS support.
- **CDT not included in the target definition** → exact isolated adapter tests for unchanged enumerator destination and separated input; record the runtime evidence limitation. Do not add CDT to the target as incidental cleanup.
- **Existing digest fixtures couple tokens to generated values** → use explicit named colors/styles in new regressions; adding TokenKey fields shifts reflection-generated values even in otherwise unchanged adapters. Audit human-readable projections for unrelated mappings before approving new digests.
- **Parser compatibility spec currently preserves aliases/styles** → the delta replaces that requirement explicitly and retains its existing scenarios with a bounded exception; security controls and metadata interpretation remain unchanged.

## Verification approach

Reuse `test/com/github/eclipsethemes/...`, JUnit 4, the existing `MemoryPreferences` double, and Surefire. Add tests that fail against the verified baseline before fixing each defect. Required assertions:

- exact debug RGB keys/values, unchanged true/false/absent/legacy-RGB indication keys during apply/reset, and exclusion from managed sets;
- each distinct pair in both orders and all both/one/neither presence cases, with independent flags; explicit GitHub Dark Dimmed, Catppuccin Frappe and Jupyter expectations;
- canonical/alias conflicts in both orders, repeated spelling, equivalent candidates, invalid higher-priority candidate, and captured winner diagnostics;
- all four optional JDT roles: initially sparse, rich → sparse → rich, repeated sparse, ancestor-present/role-absent, editor defaults true/false, exact absence of cleared styles, unchanged unrelated keys;
- hover explicit → omitted → explicit, exact color/system-default keys, absent JDT discovery, bounded reset and persistence failure paths;
- all six adapters' unchanged mapping projections outside approved differences; existing reset absence/failure tests; XML security/setup-failure/reference regressions.

Review `AdapterBaselineTest` and `XmlThemeParserCompatibilityTest` carefully: adding registry fields changes their reflection-derived corpus/adapter fingerprints. Preserve unaffected named role values independently of that fixture numbering, assert each intended difference in plain terms, and only then update digest expectations. Do not rewrite bundled XML to make the old digest pass.

Run focused parser/adapter/reset tests first, then the full `mvn clean verify -B` reactor on JDK 21 while retaining JavaSE-17 output. Run targeted real-editor smoke for ED2 on a disposable workspace before final handoff; no packaging/extension change currently calls for a new p2-install matrix. Run change and all-spec strict OpenSpec validation and inspect the whole diff. Implementation must report concrete missing-runtime blockers instead of treating declaration checks as visual proof.

## Migration Plan

1. PM1–PM4 Option A and the BREAKING review are resolved by the maintainer's 2026-10-13 approval, with ED3 explicitly acknowledged; the proposal, deltas, design, and tasks record the approved decisions. A later explicit implementation request is still required before executing the remaining tasks.
2. Ship the bounded parser/adapter changes with regression coverage and a compatibility/recovery note. Do not rewrite imported XML, backfill identity, or silently apply a startup migration.
3. Reapplying a theme uses corrected roles/preferences. Existing selected/current equality can skip reapplication through some UI paths (R2/R3/R4 debt); document the existing explicit Apply route without changing lifecycle semantics here.
4. Under approved PM1-A, instruct affected users to inspect the current/secondary instruction-pointer annotation settings in Eclipse's editor annotation preferences and choose their desired indication values manually. Do not claim their originals are known or suggest whole-node deletion. Verify the exact UI path in ED2 before publishing instructions.
5. Stop/reset removes the corrected appearance overrides and exposes editor defaults, preserving historical indication keys. No preference snapshot is created. Downgrades/reapplication can restore old defects; they are not a recovery mechanism. Prior unknown user values remain unrecoverable by this change.

## Original planning decision and evidence status

PM1–PM4 Option A are **approved and resolved** in the proposal's 2026-10-13 Decision Gate record. The maintainer explicitly acknowledged the documented BREAKING consequences and ED3's unavailable non-bundled legacy corpus. No blocking Product / Maintainer question remains. ED1 is resolved by current code and target-version declaration evidence; ED2 remains the required bounded runtime verification; ED3 remains an acknowledged evidence limitation, not verified compatibility. General roadmap questions remain deferred exactly as listed in the proposal. No uncertainty is converted into an expanded compatibility promise, and this planning request does not authorize implementation.

## Planning verification

On 2026-10-13, both the initial proposal and the approval-record synchronization were checked with OpenSpec 1.14.1. All four artifact categories are present (proposal, four capability deltas, design, tasks). `openspec validate correct-editor-preference-mappings --strict` passed; `openspec validate --all --strict` passed all nine items (the change and eight canonical capabilities). Reviewed all seven Markdown artifacts for coherent approved decisions, scope, requirements, and task dependencies; local Markdown links resolve and no trailing whitespace was found. Only approval-record task 1.1 is complete (1/23 tasks). Git status contains only the existing change directory and HEAD remains `c438a29`; production sources, tests, accepted specs, historical audit, roadmap, queue, and change metadata were not edited. No archive, commit, push, or new change was performed.

The shell lacked an `openspec` shim, so commands used Node with the installed entry point `C:/Users/cw/.local/share/mise/installs/npm-fission-ai-openspec/1.14.1/node_modules/@fission-ai/openspec/bin/openspec.js`. Scaffolding/schema-aware commands needed execution outside the filesystem sandbox after its schema trust-boundary resolution failed; the installed tool succeeded without changing the schema or project config.

No production implementation, new executable regression, reactor build, or runtime smoke has been performed. Only task 1.1, recording and synchronizing the explicit maintainer approval, is complete; all implementation and runtime verification tasks remain unchecked. Product approval comes from the maintainer's explicit PM1–PM4 resolution, while validation proves artifact structure rather than runtime behavior. The current request requires stopping after artifact updates and strict validation, without code, archive, commit, push, or another change.
