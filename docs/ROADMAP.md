# Eclipse Themes Roadmap

## Purpose

This roadmap converts the 2026-10 audit into dependency-ordered engineering work. It is sequencing guidance, not a substitute for OpenSpec behavior contracts. Each production change should be proposed and implemented through a bounded OpenSpec change. Audit-derived future requirements live in `docs/target-specs/` until their dependency-safe slice is promoted into an active change.

## Baseline

- Current `develop` checked: `744eedc8fa05c0c59c360c0e97f3a4ca3b98330c`.
- Audit baseline: `ebec341b7c6580ffa5ab7340eeef861a5f32be68`.
- The later `develop` commit was documentation/funding only, so the audit is materially applicable, but every finding must still be re-verified before implementation.

## R0 — Spec-driven engineering baseline

**Goal:** establish durable agent instructions, behavioral specs, traceability, and a repeatable change workflow without altering production behavior.

Deliverables:
- repository/scoped `AGENTS.md` guidance;
- OpenSpec project configuration and baseline capabilities;
- audit preservation and finding traceability;
- this roadmap and test strategy.

Exit: future work can start from a bounded OpenSpec change instead of the raw audit.

## R1 — Safety and security foundation

**Goal:** remove behaviors that can damage user state or process untrusted data unsafely.

Planned changes, in dependency order:
1. `eliminate-destructive-preference-reset` — Q1.
2. `secure-xml-theme-parsing` — Q2, Q10 (security/validation subset).
3. `correct-editor-preference-mappings` — Q6, Q7, Q8.
4. `formalize-regression-testing` — Q16/B3, initially focused on the above contracts.

Exit:
- stopping theme management cannot erase unrelated Eclipse/plugin preferences;
- external entities/DOCTYPE behavior is rejected;
- known destructive/colliding mappings have regression coverage;
- failures are reproducible in automated tests.

## R2 — Theme lifecycle integrity

**Goal:** make a theme a stable, validated, safely stored entity.

Planned changes:
1. `stable-theme-identity-and-revisions` — Q3, U4.
2. `atomic-deterministic-theme-import` — Q4, Q5, Q12, Q15.
3. `strict-theme-validation-diagnostics` — Q10, Q15, U5, U8.
4. `immutable-validated-theme-model` — Q14 and parser/model cleanup needed by later work.

Exit:
- identity is independent of display name/filename;
- replacing a theme cannot destroy the previous valid revision on validation failure;
- duplicate/conflict behavior is deterministic;
- invalid input produces actionable diagnostics.

## R3 — Application ownership, recovery, and truthful state

**Goal:** make theme application reversible and explicit instead of a set of direct mutations.

Planned changes:
1. `theme-application-result-contract` — Q9, error-handling gaps.
2. `declarative-theme-change-plan` — architectural prerequisite for ownership/preview.
3. `preference-ownership-journal` — persistence/original-value/absence tracking.
4. `conflict-aware-restoration` — preserve newer user/plugin changes.
5. `application-rollback-and-recovery` — aggregate failure and compensating rollback.

Exit:
- active theme state commits only after successful application;
- owned keys/original state are known;
- disable/reset is conservative and conflict-aware;
- failures surface affected contributor/file/key information.

## R4 — Existing product UX hardening

**Goal:** make the editor-theme product coherent before expanding scope.

Planned changes:
1. `offline-accurate-theme-preview` — U1/U2.
2. `coherent-apply-ok-cancel-semantics` — U3 plus Q9.
3. `theme-import-conflict-and-diagnostic-ui` — U5.
4. `preference-page-accessibility` — U6 and contrast/high-contrast checks.
5. `externalize-ui-strings` — U7.
6. `supported-integration-inspector` — U9, A3/A4 support metadata.
7. `extract-preference-page-policy` — Q13, after behavior is protected by tests.

Exit: users can tell selected, previewed, and applied states apart; preview works offline; Apply/Cancel behavior is predictable; supported integrations are visible.

## R5 — Build, compatibility, and release engineering

**Goal:** make a clean clone verifiable and releases predictable.

Planned changes:
1. `build-toolchain-contract` — B1; align docs/wrapper/enforcement with Tycho requirements.
2. `target-and-compatibility-matrix` — A3, B2, current Eclipse validation, CDT integration coverage.
3. `ci-behavioral-and-p2-smoke-tests` — B3.
4. `release-channel-hardening` — B4/B5/B6.
5. `reproducible-release-artifacts` — B7/B8.
6. `ci-permission-and-action-hardening` — B9/B10.
7. `bundle-metadata-and-license-packaging` — B11/F6.

Exit: PRs accepted by the project are actually validated; stable and snapshot channels are separated; release artifacts are traceable to one tag/commit/build.

## R6 — Architecture/API boundary cleanup

**Goal:** prepare for external contributors and optional integrations without speculative bundle explosion.

Planned changes:
- extract parser diagnostics from activator dependency — A2;
- define stable adapter/contributor API surface only when third-party use is ready — A1;
- deterministic descriptor discovery, ordering, availability/version checks — A4;
- isolate version-fragile editor mappings and document provenance — A3 plus fragile coupling findings;
- split physical OSGi bundles only where optional dependencies/API ownership/testing justify it.

Exit: the logical architecture supports independent testing and bounded public contracts.

## R7 — Full-workbench theming feasibility spike

**Goal:** collect evidence before committing to a universal architecture.

Research change: `workbench-theme-engine-spike`.

Must prove on a bounded matrix:
- public `IThemeEngine` registration/selection/application behavior;
- switching/revert behavior;
- interaction with editor preference changes and Eclipse built-in themes;
- restart/lifecycle limits for dynamically registered CSS;
- Windows/Linux/macOS differences;
- behavior when DevStyle/other appearance plugins are present;
- unsupported surfaces and high-contrast policy.

Exit: documented go/no-go architecture decision with explicit unsupported surfaces. No "theme every pixel" claim.

## R8 — Coordinated Theme MVP (complete)

**Goal:** one safe selector that coordinates supported editor colors with supported workbench themes.

Scope:
- built-in/p2-contributed workbench CSS themes first;
- editor/workbench components independently opt-in;
- ownership/recovery journal reused from R3;
- live switch where proven; restart requirements surfaced where not;
- external changes detected without rewrite loops.

Exit: editor + workbench coordination works on the declared matrix, disable restores owned state conservatively, and coverage is inspectable.

**R8a (delivered, change `dynamic-workbench-theming`):** opt-in coordination of the selected theme with the workbench through a deterministically converted eight-role palette and four static CSS themes; VERIFIED on Windows with Eclipse 4.40 and 4.41, other combinations EXPERIMENTAL and off unless opted in. Not part of R8a: fonts and icons, user overrides, runtime-registered themes, imported CSS, the R3 recovery journal, Linux/macOS verification. See [workbench theming compatibility](WORKBENCH_THEMING_COMPATIBILITY.md).

**R8 closure (delivered, change `plugin-color-coordination`):** the colors of other plug-ins follow a coordinated dark workbench theme through a declared 68-key table with value-exact ownership (written only where the user has no value, updated and released only while unchanged, released when another Eclipse theme is chosen), plus static CSS for plug-in widgets selector-for-selector with stock Dark. Every stock-Dark preference key is classified in the change's `key-classification.md`. Light themes, editor-only mode and disabled coordination write nothing. Verified on Windows with Eclipse 4.40 and 4.41. **Still open (R9 / follow-up):** the R3 recovery journal for workbench state, per-surface coverage UI, theme-authored plug-in colors and CSS, fonts and icons, Linux and macOS verification. See the [R8 target spec](target-specs/R8_COORDINATED_THEME_MVP.md).

## R9 — Theme Package v1 and contributor ecosystem

**Goal:** portable, declarative coordinated themes and stable contributor contracts.

Scope:
- versioned package identity, revision/digest, provenance, license metadata;
- semantic palette/editor tokens/workbench resources;
- validated package bounds and resource references;
- contributor change-plan API;
- selected WTP/TM4E integrations;
- author documentation and example contributor.

Executable code is not allowed inside imported theme archives.

## R10 — Expansion / research

Possible work only after evidence:
- workbench CSS author/import lifecycle;
- participating icon packs;
- export/share and authoring tools;
- OS-following profiles;
- broader third-party editor integrations;
- upstream Eclipse Platform changes for missing public APIs.

Universal icon interception, native chrome recoloring, and non-participating third-party widgets are research areas, not promises.

## Roadmap operating rule

When the user says "continue with the next roadmap item," the agent must inspect current specs, active changes, merged history, and dependencies before selecting the next change. Do not blindly execute the next bullet if prior work changed the dependency graph.
