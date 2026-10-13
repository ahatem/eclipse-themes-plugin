# Proposal

## Why

At the pre-fix baseline, the XML theme parser created an unconfigured DOM builder and parsed untrusted input immediately, leaving the external-resolution weakness described in [audit finding Q2](../../../docs/audits/2026-10-complete-project-audit.md#confirmed-defects-and-quality-issues) unresolved. After the preference-reset safety change, this is the next bounded R1 outcome in [the roadmap](../../../docs/ROADMAP.md#r1--safety-and-security-foundation): parsing must reject DTD/entity attacks and fail closed when its security controls cannot be established.

## What Changes

The following plan is approved under PM1 Option A and implemented in the subsequent maintainer-authorized apply request. Implementation and verification evidence is recorded in [verification.md](verification.md).

- Reject every DOCTYPE declaration, including internal-only DTDs, before processing declared entities or retrieving external DTDs.
- Block external general/parameter entity resolution, external DTD/schema access, and XInclude retrieval through explicitly configured parser-local controls and a rejecting resolver.
- Refuse to consume theme input or return a theme if any required security control cannot be applied or verified; expose the security failure with a useful diagnostic and retained cause through the existing parsing exception contract.
- Add focused regression coverage for prohibited documents, zero external access, unavailable security controls, and continued parsing of the bundled legacy corpus.
- **BREAKING — approved by the maintainer (PM1 Option A)**: reject every DOCTYPE declaration, including empty and internal-only DTDs; legacy themes that depend on DTD defaults or custom entity declarations will no longer load. Ordinary legacy themes, predefined XML escapes, and numeric character references remain supported under the approved target contract.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `theme-parsing`: constrain supported XML to documents without DOCTYPE; require zero external resolution and fail-closed security configuration with observable failures.

## Decision Gate

**Status: approved for implementation.** The maintainer explicitly resolved PM1 by approving Option A on 2026-10-13. No blocking Product / Maintainer decision remains unresolved. The delta specs, design, and tasks reflect the approved policy and unchanged scope. Approval comes from the maintainer's instruction, not artifact completeness or strict validation. The earlier approval-record request stopped before implementation; the subsequent apply request authorized implementation and the planned evidence checks. ED1/ED3 results are recorded below and in [verification.md](verification.md).

### Evidence-dependent

| ID / Question | Current evidence and required verification |
| --- | --- |
| ED1: Can the selected Java 17 and Eclipse/OSGi JAXP providers establish and report every required security control? | Resolved through executed control/readback tests on Temurin Java 17.0.20.1+1, JustJ Java 21.0.11+10-LTS, and the packaged product under Eclipse 2024-09 / Equinox 3.21.0. All selected providers discovered the JDK Xerces factory and established every required control; unsupported/misreported controls fail closed in injected-provider tests. See verification tasks 3.1/3.3 and the verification record. This evidence does not expand the support matrix. |
| ED2: How many existing user/third-party themes rely on internal-only DTDs or DTD defaults? | All 33 bundled files lack these declarations, but no representative non-bundled corpus has been inspected. The maintainer acknowledged this absence and accepted the unknown wider impact in the PM1 resolution; it does not block the approved implementation. Any broader compatibility claim would still require read-only review of a supplied representative legacy corpus. Do not infer universal legacy compatibility from the bundled themes. |
| ED3: Does the proposed configuration prevent retrieval and preserve safe-input interpretation in practice? | Resolved through the harmless pre-fix local-entity reproduction and passing tasks 1.1-1.2/2.2-2.7 tests after hardening. Monitored DTD rejection, XInclude/schema hints and OSGi smoke produced zero resolution callbacks and zero network connections. All 33 bundled themes preserve their pre-fix metadata/token/style/fallback fingerprint; ordinary references and all entry points pass. See verification.md for commands, providers and limits. |

These are technical evidence gaps, not questions for the maintainer to answer by intuition. If evidence shows a supported environment cannot satisfy the plan, bring the resulting compatibility choice back for explicit review; do not silently relax security controls or narrow support.

### Engineering

No blocking engineering choice remains unresolved. Retain the significant choices and rationale in design decisions 1, 3, and 4: DOM with fresh factory/builder per parse, a parser-local configuration helper and package-private test seam, the existing exception contract, and the existing JUnit harness. They implement the approved behavior without new public APIs or global provider changes. Test-fixture and diagnostic wording details may be resolved during apply within the approved contract; they must respect resolved PM1 Option A and cannot change compatibility/fallback policy.

### Product / Maintainer decision

**PM1 — Which DOCTYPE compatibility policy should this security change adopt? Status: resolved; Option A explicitly approved by the maintainer on 2026-10-13.**

| Realistic option | Consequences / trade-offs |
| --- | --- |
| A. Reject every DOCTYPE, including empty and internal-only DTDs (**recommended and approved**) | Keeps the current bounded plan and gives an auditable rejection boundary before DTD defaults or declared-entity expansion. Existing DTD-dependent themes stop loading; authors must supply flattened trusted data. All bundled themes lack declarations, but the wider legacy impact is unknown and accepted (ED2). |
| B. Permit a narrowly defined internal-only DTD subset while prohibiting external resolution and failing closed (**not selected**) | Could preserve some legacy defaults/entities, but would require evidence for safe expansion/resource handling, an explicit subset contract, a planning revision, and additional tests. It is not part of the approved change. External entities/DTDs and insecure fallback would remain prohibited. |

**Recommendation adopted:** Option A matches the existing security design, avoids DTD interpretation/expansion policy in this bounded slice, and has no known bundled-theme dependency. The maintainer explicitly approved its breaking compatibility behavior; the roadmap and audit remain supporting evidence rather than the source of approval.

**Resolution record (2026-10-13):** the maintainer explicitly instructed, "Approve PM1 Option A."

- **Approved policy:** Eclipse Themes will reject every DOCTYPE declaration, including empty and internal-only DTDs.
- **Accepted compatibility consequence:** legacy themes depending on DTD defaults or custom entity declarations will no longer load.
- **Acknowledged evidence limitation:** no representative non-bundled legacy corpus is available; universal legacy compatibility must not be inferred from the bundled themes. ED2's wider impact remains unknown, not verified.
- **Authorization history:** implementation of the established bounded change was authorized, with the approval-record turn restricted to updating the records and strict validation before stopping. The subsequent apply request explicitly authorized implementation and verification, while prohibiting archive, commit, push, PR actions, and another change. ED1/ED3 are resolved by the apply evidence; task progress is tracked in tasks.md.

### Deferred

| Question outside this change | Future owner / boundary |
| --- | --- |
| Required roots, identity, colors/booleans, and richer validation diagnostics | R2 `strict-theme-validation-diagnostics` / `theme-parsing`; no new validation policy here. |
| Safe replacement, same-file imports, conflict identity, migration and recovery | R2 `atomic-deterministic-theme-import` and `stable-theme-identity-and-revisions` / `theme-import`; Q4/Q5 remain disclosed, not decided or fixed. |
| General parser diagnostics/activator separation | R6 architecture/API cleanup (A2); retain the existing exception boundary. |
| Custom input-size/depth quotas and future package bounds | A later bounded parser-security slice (not yet named); package bounds belong to R9 Theme Package v1. This change does not select quota values or promise comprehensive resource-exhaustion protection. |
| Broader supported Eclipse/JDK/platform matrix and test-module restructuring | R5 compatibility work and R1 regression-harness work; current verification does not expand or narrow published support. |

## Impact

- Implementation surface: `XmlThemeParser` builder creation/error handling and parser tests under the runtime bundle's existing JUnit 4/Surefire test setup. All existing stream/file entry points and bundled/installed/imported theme callers use this parser.
- Compatibility: retain JavaSE-17, parser method signatures, current JAXP provider discovery, and metadata/token/alias/fallback behavior for security-safe input. Providers that cannot establish the required controls must fail rather than parse insecurely. No new runtime library, bundle split, exported API, Eclipse internal API, or execution-environment increase is planned.
- State/recovery: no migration, preference mutation, or persisted format change. Previously accepted DTD-dependent installed themes will fail on reload. Existing import copy/delete behavior can still damage a prior file when parsing fails (Q4); this change does not make replacement safe or add recovery. Do not bypass the security policy to restore compatibility.
- Scope boundary: promote only the XML external-resolution/security slice of [R1's target requirements](../../../docs/target-specs/R1_SAFETY_SECURITY.md). Root/identity/color/boolean/schema validation (Q10), file-size/depth quotas, import staging/atomicity/conflicts (Q4/Q5), general diagnostics redesign, and parser/activator decoupling (A2) remain separate work.
