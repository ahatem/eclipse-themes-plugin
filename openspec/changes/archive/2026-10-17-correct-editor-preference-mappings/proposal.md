# Proposal

## Why

The merged `develop` baseline still writes debug RGB values to boolean indication preferences, collapses distinct legacy color/style roles, and removes the disabled flag it just wrote for missing optional semantic tokens. This is the next bounded R1 safety change after preference-reset and XML-parser hardening, traced to [Q6–Q8](../../../docs/audits/2026-10-complete-project-audit.md), [R1](../../../docs/target-specs/R1_SAFETY_SECURITY.md), the [roadmap](../../../docs/ROADMAP.md), and [queue](../../../docs/CHANGE_QUEUE.md).

## What Changes

- Write instruction-pointer colors to `currentIPColor` and `secondaryIPColor`, with matching managed-key declarations; stop treating boolean indication settings as color settings.
- Preserve selection, source-hover, generic-comment, XML-comment, static-final-field, and constant roles independently, including all four style flags.
- Document and test true-alias conflict resolution and optional-semantic-token transitions.
- Add exact parser/adapter/reset regression assertions using the existing test facilities, including bundled-theme examples; retain unrelated mappings and XML security behavior.
- **BREAKING behavior correction, approved:** themes relying on document-order collisions, cross-role overwrites, or editor-default re-enablement can render differently. The maintainer explicitly acknowledged these consequences and approved PM1–PM4 Option A on 2026-10-13; the deltas describe those approved decisions.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `editor-integrations`: correct debug preference destinations, consume distinct roles, and make optional JDT semantic enablement deterministic.
- `theme-definition`: preserve distinct canonical roles and specify their sparse-role fallbacks.
- `theme-parsing`: replace the bounded alias/collision behavior while preserving all unrelated interpretation and secure XML requirements.
- `preference-ownership`: explicitly exclude historical debug indication values from guessed repair and ongoing color-key cleanup under the recommended recovery policy.

## Impact

Baseline verified on 2026-10-13: remote/local `develop` is `c67ece1a15b6d752f4c4dfbeccbaba6d4a1392d3` (merged #20/#21). Current branch `fix/editor-preference-mappings` at `c438a29` has identical production sources/build descriptors and only the later XML-change archive/spec promotion. Working tree was clean and OpenSpec had no active changes. The roadmap's older baseline and queue's XML entry are historical sequencing notes, not evidence that XML work remains unimplemented.

Affected implementation: token registry, legacy mapper/parser, base text/JDT/PDE/Ant adapters, optional semantic helper, derived managed-key sets, and their focused tests. CDT continues to consume the distinct constant role. No new plugin dependency, exported package, extension contract, bundle split, Java execution environment, target definition, or supported-platform/version expansion is proposed. Corrected mappings remain implementation preference-key couplings verified against Eclipse 2024-09 declarations; runtime listener checks remain necessary.

No stored theme XML rewrite or identity migration is proposed. On reapplication, corrected appearance values replace existing managed overrides. Original debug boolean values cannot be reconstructed without snapshots. Removing an override exposes the editor/product/CSS default; it does not reconstruct a user's original setting. Downgrading can reinstate the defects and cannot recover prior values. General ownership journaling, application rollback, lifecycle/identity, workbench theming, UI redesign, and unrelated adapter cleanup remain outside this change.

## Decision Gate

**Final status — 2026-10-17:** PM1–PM6 Option A are explicitly approved and implemented within their recorded boundaries. ED2's bounded consumer/listener verification is complete; no blocking product decision remains. Historical pauses below explain the evidence and approvals, not current blockers. External legacy corpus and CDT runtime limits remain acknowledged; no support matrix expansion is claimed.

### PM6 resolution — 2026-10-17

**PM6 Option A is explicitly approved. PM1–PM5 remain unchanged.** The maintainer authorized only the version-fragile `PDEPlugin.getDefault().getPreferenceStore()` access at the PDE integration boundary for the declared Eclipse 2024-09 / R4_33 target. It must remain optional, isolated and unexported, with no parser/model/general application internal dependency. Absent or incompatible PDE must report unavailable application without crashing other integrations; the ineffective separate-store route is not a fallback. A public/upstream replacement is deferred to R6/upstream compatibility work.

Implementation and remaining verification are resumed. Local PDE typed Boolean notifications will accompany explicit node writes, preserving instance override presence even when values equal defaults; this is confined to existing PDE color/bold/italic mappings. PDE and Ant will no longer emit or claim unsupported underline/strikethrough keys. Those historical unsupported values remain untouched by apply/reset; this is not guessed cleanup or migration. All other token destinations, core flags and integrations remain unchanged. Broader internal APIs, dependencies, target expansion, generic listeners or new product choices still require returning to this gate. No archive or remote action is authorized.

### PM6 — historical internal-store access decision, resolved as A on 2026-10-17

PM5 Option A is approved; PM1–PM4 remain unchanged. The required root-cause investigation has now executed its isolated controls on Eclipse 2024-09. Raw node Boolean writes deliver **String** events to the existing PDE store, which does not enable bold. The actual PDE store's public typed setter delivers **Boolean** events and enables bold. A separate newly constructed public scoped store emits Boolean events only to its own listeners: the existing PDE store still receives a String and remains unstyled. Both PDE's black result and Ant's RGB exception were independently reproduced by an unsupported style-suffix notification. See [pm5-root-cause-evidence.txt](pm5-root-cause-evidence.txt) and [verification.md](verification.md).

**New blocking decision:** authorize production access to the actual PDE preference store through the version-fragile internal `org.eclipse.pde.internal.ui.PDEPlugin.getDefault().getPreferenceStore()` entry point, or keep that internal access excluded? The public alternatives investigated do not retrieve it: `Platform.getPlugin` is a null stub in the loaded 2024-09 binary; the PDE bundle exposes neither an `IPreferenceStore` adapter nor an `IPreferenceStore` service. Constructing a separate store does not fix existing consumers. This is evidence about the investigated routes, not a claim that all possible Eclipse mechanisms have been exhausted.

| Option | Consequence |
| --- | --- |
| **A — recommended: explicitly authorize this one PDE internal-store entry point** | Isolate and document the optional, target-version-bound access, use public typed store operations once obtained, and regression-test live consumers, failure behavior and state ownership. Keep Ant on its existing supported node path: its bold/italic handler accepts String booleans. Correct only the demonstrated unsupported PDE/Ant style notifications. No mandatory dependency, manifest/target change, persistent bridge, editor refresh or new public API is authorized. This accepts a new internal-access compatibility coupling that PM5's narrow approval explicitly required returning to the gate for. |
| B — keep new internal access excluded | Leave the current implementation incomplete while a supported public integration path or upstream PDE correction is pursued. The approved live PDE style contract cannot be claimed satisfied by the verified node/separate-store routes. Any decision to reduce that contract remains separate and unapproved. |

The maintainer selected A on 2026-10-17. The earlier pause before production/test-suite correction is retained as historical evidence. Instance-override/default and reset semantics must remain preserved and tested. No ineffective fallback, support-matrix expansion or exception suppression is authorized.

### PM5 resolution — 2026-10-17

The maintainer explicitly approved **PM5 Option A**: establish the concrete PDE/Ant runtime root causes and correct only the existing integration behavior necessary for the approved separated XML-comment contract on Eclipse 2024-09. PM1–PM4 remain unchanged. Synchronization of proposal, affected deltas, design and tasks, followed by implementation and verification in this same change, is authorized.

This is bounded authorization for the demonstrated runtime failures. It excludes a generic listener/refresh architecture, adapter framework, public API, mandatory dependency, manifest/target expansion, application transactions, UI work and unrelated integration cleanup. If the correction needs a new architectural mechanism, an internal Eclipse API commitment beyond the existing bounded integration approach, broader platform assumptions, or another product/compatibility choice, implementation must return to this gate. No exception suppression may stand in for fixing the cause. No archive, commit, push, PR or new change is authorized.

### Implementation return to gate — 2026-10-13

The later explicit apply request authorized implementation without revisiting PM1–PM4 Option A. Those decisions remain approved. **Implementation is now paused because ED2 found a runtime contradiction**, as the maintainer required. The 2024-09 PDE XML comment consumer ends black and unstyled after the corrected adapter writes the approved specific color/bold token; Ant listeners also throw `DataFormatException` during existing style writes. Exact stored values, consumer attributes, sources and reproduction are in [verification.md](verification.md) and [runtime-smoke-evidence.txt](runtime-smoke-evidence.txt). Passing isolated mapping tests and the reactor do not resolve this failure.

**Evidence-dependent:** the loaded PDE/Ant consumers recognize bold/italic suffixes; other existing style notifications enter color-handling paths. PDE's style-change handler expects a Boolean, while direct preference-node notifications are not the editor preference-store's typed setter route. The real PDE consumer observed RGB `0,0,0`, style `0`, beside stored RGB `69,103,137` and `_bold=true`. Further implementation requires resolving the application/notification behavior, not changing the separated token policy.

**PM5 — historical scope/compatibility decision, resolved as A on 2026-10-17:** how should this change handle the newly verified PDE/Ant listener behavior while its approved scope requires preserving unrelated mappings and lifecycle semantics?

| Option | Consequence |
| --- | --- |
| **A — recommended: authorize a bounded PDE/Ant listener-safe application correction** | Investigate and revise the design/tasks for the smallest necessary change to supported style writes, event typing and unsupported style notifications, with exact ownership/reset and real-consumer regressions. This extends the currently approved mapping-only application scope; it must preserve unrelated state and must not imply new APIs, dependencies or version support. It can deliver the intended runtime behavior. |
| B — explicitly revise the runtime contract and defer listener repair | Retain exact current preference writes, document the demonstrated live-consumer limitations, and assign repair to the appropriate editor-integration roadmap work. This reduces the approved observable outcome; it cannot be silently treated as successful implementation. |

The maintainer selected A on 2026-10-17 because correct stored keys alone do not deliver the promised comment appearance. The 2026-10-13 pause above is retained as historical evidence. Manual recovery UI-path verification, available/absent JDT discovery, and complete PDE/Ant transition verification remain pending. PM1–PM4 are not reopened.

**Original planning handoff status: PM1–PM4 Option A approved.** The maintainer explicitly resolved all four original blocking Product / Maintainer decisions on 2026-10-13 and acknowledged the documented BREAKING consequences and ED3's unavailable non-bundled legacy corpus. No original PM1–PM4 decision remains unresolved. The later ED2 return to gate and new PM5 are recorded above; the approved policies remain unchanged.

The original approval request authorized artifact synchronization and strict validation only. The later explicit apply request supplied implementation authority. OpenSpec artifact completeness alone supplied neither that authority nor evidence of runtime success.

### Resolution record — 2026-10-13

- **PM1 — approved A:** preserve `currentIPIndication` and `secondaryIPIndication` exactly during apply and stop/reset, including absence and historically malformed/RGB-shaped values. Remove both from ongoing managed-color responsibility. No heuristic cleanup or recovery claim for unknown original values.
- **PM2 — approved A:** for the four existing optional JDT roles, explicit presence enables the role and writes its complete style; omission retains `.enabled=false` and removes color/style overrides. This is the bounded legacy-XML policy only, not a universal future missing-token semantic.
- **PM3 — approved A:** keep source-hover, selection, XML-comment, generic-comment, static-final-field, and constant roles distinct. XML comment inherits generic comment and static-final field inherits constant/the existing constant chain when omitted, never the reverse. Only an explicit source-hover token controls the JDT hover pair; omission removes those managed overrides and exposes editor defaults.
- **PM4 — approved A:** valid canonical spelling wins over a valid legacy alias regardless of order; equal rank/spelling uses the last valid occurrence. Use the winning complete token without style merging and report meaningful accepted conflicts with the proposed mapping diagnostic.
- **Compatibility acknowledgement:** the maintainer explicitly accepted the documented BREAKING consequences and acknowledged ED3's unavailable non-bundled legacy corpus. This acknowledgement does not supply missing compatibility evidence or resolve ED2's pending runtime checks.

### Evidence-dependent

- **ED1 — resolved by inspection:** Q6–Q8 remain in current production code; upstream 2024-09 debug declarations distinguish boolean indication keys from RGB color keys. JDT consumes a separate hover color/system-default pair; its deprecated-member semantic default is enabled. Exact evidence, corpus counts, and links are in `design.md`.
- **ED2 — runtime evidence pending:** verify real preference consumption/listeners on the existing 2024-09 target for debug colors, JDT hover, and rich → sparse → rich semantic transitions. Source evidence establishes the defect, but no graphical runtime probe was performed during planning. Implementation must run the bounded smoke or document a concrete blocker; no cross-platform rendering equivalence is asserted.
- **ED3 — external legacy corpus unavailable, acknowledged:** all 33 bundled themes were inspected; imported/third-party theme distributions were not available. The maintainer explicitly acknowledged this uncertainty when approving PM1–PM4. Tests must still cover synthetic one-sided roles and true-alias conflicts; approval does not establish compatibility with unexamined imports.

### Product / Maintainer decision

The following table preserves the alternatives and trade-offs considered. **Option A is approved in every row; B and C are not selected and are outside the approved plan.**

| ID / decision | A — approved | Alternatives considered, not selected | Selection rationale |
| --- | --- | --- | --- |
| **PM1: historical debug indication state** | Preserve `currentIPIndication` / `secondaryIPIndication` exactly, including absence, during apply and stop/reset; remove them from ongoing managed-color keys. Document manual recovery of historical corruption. | B: on explicit reset only, remove RGB-shaped invalid indication values while preserving valid booleans/absence; cannot prove provenance or restore originals, and changes reset semantics. C: retain unconditional historical-key reset cleanup; removes valid newer user boolean choices too. | A avoids guessing ownership or deleting newer user state. The corrected color keys participate in the existing bounded reset policy; no journal/migration engine is added. |
| **PM2: missing optional semantic token** | For the four existing optional JDT roles, keep an explicit `.enabled=false` and remove only color/style overrides; explicit presence sets true with the complete style. This bounded legacy-XML policy establishes no universal future missing-token semantic. | B: remove enable/color/style overrides and intentionally inherit editor defaults; deprecated highlighting can remain enabled. C: retain previous values for omitted roles; result depends on the previous theme/user state. | A matches the helper's existing attempted disable and makes repeated/rich–sparse transitions predictable, but deliberately changes default-enabled deprecated-member behavior. |
| **PM3: distinct-role fallbacks and hover application** | Separate the three collision pairs. Missing XML comment inherits generic comment; missing static-final field inherits constant then the existing constant chain. Generic comment never inherits XML comment, and constant never inherits static-final field. Explicit source-hover background controls only JDT's hover color/system-default pair; omission removes those two overrides to inherit editor defaults. | B: separate explicit roles but cross-fill missing comment/static-final/constant counterparts for legacy compatibility; preserves historical cross-language coupling when a role is omitted. C: use A's comment/static-final rules but only preserve the hover token, deferring hover preference writes; fixes selection overwrite without delivering the specified hover color. | A respects distinct editor concepts and retains useful one-way general-to-specific fallback. XML-only and static-final-only themes may render generic comments/CDT enumerators differently; omitted hover deliberately exposes defaults rather than selection/background substitution. |
| **PM4: true aliases and repeated properties** | A valid canonical spelling wins over a valid legacy alias regardless of XML order. Within the same spelling/rank, the last valid occurrence wins. Select a complete token; never merge styles from losing entries. Conflicting accepted candidates produce a mapping warning identifying role, sources, and winner. | B: preserve last-valid document order for true aliases, with warnings; order remains meaningful. C: reject conflicting aliases/repetitions; gives stricter authoring rules and makes previously accepted themes fail. | A prevents alias-order surprises while retaining repeated-property compatibility. Duplicate rejection/general structural validation is unnecessary for this bounded fix. |

### Engineering

- **EN1:** retain the existing token-inheritance model and declarative `ManagedPreference` descriptors; add only the necessary distinct roles and mapping entries. Derive reset keys from corrected descriptors so write/reset responsibility stays aligned.
- **EN2:** resolve alias candidates by a small parser-local ranking rule; report only relevant mapping conflicts through a narrow diagnostic path without new core-to-activator/SWT dependencies or an exported diagnostics API.
- **EN3:** reuse the current JUnit, isolated preference double, and Surefire setup. Add readable exact assertions before revising historical digest expectations; do not establish the later general test-harness architecture here.

### Deferred

- Original-state snapshots, conflict-aware restoration, transactional apply results/rollback and recovery: **R3 / preference-ownership and theme-application**.
- Theme identity/revisions, general structure/value validation and import/storage semantics: **R2 / theme-definition, theme-parsing, theme-import**.
- Integration discovery/version matrix and broad preference-key/API isolation: **R5/R6 / editor-integrations**. CDT runtime coverage remains a known target gap, not a newly supported matrix.
- Apply/OK/Cancel, preview and other UI design: **R4 / preference-ui**; workbench/native surfaces: **R7–R10**.
