# Tasks

**PM1–PM6 Option A are approved and implemented within the bounded change.** The isolated optional PDE live-store accessor is documented against R4_33; public/upstream replacement remains deferred R6 work. The 2026-10-17 focused/reactor/runtime evidence supersedes earlier pauses. Checkboxes reflect completed verification; the change remains active with no archive or remote action authorized.

## 1. Record the resolved approval gate and refresh baseline evidence

- [x] 1.1 Record the maintainer's explicit 2026-10-13 approval of PM1–PM4 Option A, BREAKING compatibility acknowledgement, and ED3 corpus limitation; verify the proposal resolution record, all four delta specs, design, and this checklist consistently describe those approved decisions and the planning-only execution boundary.
- [x] 1.2 On a later explicit apply request, inspect status/history, applicable guides, current merged `develop`, and active OpenSpec changes; compare affected parser/adapter/reset code with the baseline evidence in `design.md` and record any superseding changes before starting regressions.

## 2. Correct debug destinations and preserve indication state

- [x] 2.1 Add a focused text-editor regression with distinct current/secondary RGB values and true, false, absent, and RGB-shaped historical indication values; show exact color-destination assertions fail on the verified baseline before changing descriptors.
- [x] 2.2 Replace the two RGB destinations and derive the corrected managed-key sets; verify exact `org.eclipse.ui.editors` color writes, unchanged indication/highlight/text-style/ruler settings, and exclusion of both indication keys from apply/reset responsibility using the focused tests.
- [x] 2.3 Extend reset regressions for correct color keys, present/absent indication preservation, repeated reset, unrelated values, removal and flush failures; verify existing contextual failure/success gating and zero whole-node clears, then run the focused debug/reset suite.
- [x] 2.4 Document target-version debug-key provenance and the approved historical recovery limitation, including manual choice of desired indication values rather than invented originals; verify documentation matches PM1 and defer exact UI-path publication until the runtime smoke confirms it.

## 3. Preserve independent roles and resolve true aliases

- [x] 3.1 Add parser regressions for all three pairs in both document orders and both/one/neither presence cases, including four independent style flags, GitHub Dark Dimmed colors, Catppuccin Frappe styles, and Jupyter static-final styles; show the collision assertions fail on the baseline.
- [x] 3.2 Add only source-hover, XML-comment, and static-final canonical roles with the approved inheritance rules and correct the three mapper entries; verify exact values/options, unchanged unrelated inheritance, no reverse fallback or cycles, and absence remaining distinct from fallback availability.
- [x] 3.3 Add exact alias tests for the table in `design.md`, canonical-versus-alias ordering, repeated properties, equivalent effective styles, invalid/missing higher-priority colors, and no style merging; implement the approved ranking and verify whole-token winners and captured diagnostics identify role, sources/occurrences, and winner.
- [x] 3.4 Wire bounded conflict warnings through a Java-only callback at the existing Eclipse loading boundary for bundled, installed, and import parsing; verify each route emits the approved warning while still returning accepted themes, without a new parser-to-activator lookup, stored mutable per-parse global state, or exported diagnostics API.
- [x] 3.5 Review all 33 bundled resolutions and revise `XmlThemeParserCompatibilityTest` expectations only after readable assertions/projections identify every intended role/collision difference; run focused mapping/compatibility plus existing XML security/setup-failure/reference tests and verify no unrelated metadata, invalid-value, or security behavior changed.
- [x] 3.6 Document approved role names, one-sided fallbacks, true-alias precedence, warning semantics, and legacy compatibility consequences; verify the author-facing examples match parser assertions without rewriting bundled/imported XML or defining a new format/schema.

## 4. Route separated roles through the existing integrations

- [x] 4.1 Add exact JDT/CDT/PDE/Ant mapping regressions with distinct generic/XML-comment and static-final/constant colors/styles; update only affected consumers and verify JDT static-final, CDT enumerator, PDE XML-comment, and Ant comment destinations use the correct complete role tokens with approved sparse fallbacks.
- [x] 4.2 Add a narrowly scoped JDT hover descriptor for its color/system-default pair under approved PM3; verify explicit → omitted → explicit and repeated omission transitions, color-before-switch mutation ordering, fallback lookup not counting as presence, and independent selection mapping.
- [x] 4.3 Test available/absent JDT discovery without changing registry semantics, exact hover managed-key coverage, bounded hover reset, removal/flush failure, and unchanged unrelated JDT preferences; verify the targeted integration/reset suite passes before updating broad fingerprints.
- [x] 4.4 Add a compatibility note and provenance beside affected mapping descriptors describing exactly the supported JDT source-hover surface, approved default-exposure behavior, comment/static-final fallbacks, and CDT runtime evidence gap; verify it makes no universal hover/workbench/OS claim.
- [x] 4.5 Record PM5 Option A approval; isolate and document the PDE black/unstyled and Ant exception causes on the 2024-09 consumers, including raw-event types, supported suffixes, actual-store versus separate-store behavior and public-API feasibility; synchronize the approved bounded runtime contract across artifacts before implementation.
- [x] 4.5a Record PM6 approval and isolate the single optional PDE live-store accessor with R4_33 provenance; synchronize supported suffix responsibility, explicit/default-valued typed notifications and absent/incompatible failure expectations without a mandatory dependency or ineffective fallback.
- [x] 4.6 Add focused regressions reproducing both PDE/Ant runtime failures and preserving unrelated preference values/absence, role fallbacks, and supported bold/italic transitions; establish their failing baseline before correction.
- [x] 4.7 Correct only the demonstrated PDE/Ant integration causes through the bounded supported application path; verify no swallowed listener exceptions, unrelated mapping/state changes, new architecture/API/dependency/manifest/target commitments, or reset/lifecycle regression. Return to the Decision Gate if such a commitment is required.
- [x] 4.8 Repeat the targeted 2024-09 rich/sparse/rich and repeated-application smoke against real PDE/Ant consumers with exact stored values, attributes and captured errors; update provenance, compatibility documentation and ordered baseline expectations only after reviewing every difference.

## 5. Make existing optional semantic transitions deterministic

- [x] 5.1 Add failing regressions for all four optional JDT roles covering initially sparse, rich → sparse → rich, repeated sparse, role-absent/ancestor-present, effective editor defaults true and false, false/unspecified flags replacing old true flags, and unrelated semantic keys; verify the baseline loses its explicit disabled state.
- [x] 5.2 Separate color/style cleanup from the approved enable-state decision; verify exact `.enabled` values and absence of cleared `.color`, `.bold`, `.italic`, `.underline`, and `.strikethrough` overrides in the focused suite while mandatory JDT/CDT mapping behavior stays intact.
- [x] 5.3 Confirm stop/reset still removes the managed optional `.enabled` overrides and exposes defaults, with absent keys staying absent and unrelated values preserved; run focused semantic/reset failure tests and document the approved bounded legacy-XML missing-token policy, its lack of universal future semantics, and the default-enabled deprecated-member consequence.

## 6. Verify the bounded change as a whole

- [x] 6.1 Compare readable exact writes/removals and named token values for all six adapters against the baseline, excluding only approved differences; audit reflection-generated fixture shifts from new TokenKey fields, update `AdapterBaselineTest` digests only after that review, and run the combined parser/adapter/reset/security suite after PM5 corrections (earlier 2026-10-13 evidence retained).
- [x] 6.2 Repeat `mvn clean verify -B` with JDK 21 after PM5 corrections; verify the reactor and behavioral tests pass, runtime output still targets JavaSE-17, and packaging/manifests/extensions/dependencies contain no unintended changes (earlier 2026-10-13 build retained as baseline).
- [x] 6.3 Complete ED2 in a disposable Eclipse 2024-09 workspace: verify debug colors with indication switches preserved, JDT hover color/system-default transitions, default-enabled deprecated-member rich → sparse → rich behavior, and PDE/Ant separated comment styles; capture exact preference values and observed listener behavior, verify the manual recovery UI path, or document a concrete runtime blocker without claiming graphical success or an expanded support matrix.
- [x] 6.4 Repeat `openspec validate correct-editor-preference-mappings --strict` and `openspec validate --all --strict`; review the full diff and requirement/task/evidence traceability after PM5, verify no unrelated code/preferences/files/spec changes, and record any remaining compatibility or runtime limitation before calling implementation complete.

## Workflow follow-up

- Archive, commit, push, PR creation and any new change remain outside this implementation request.
