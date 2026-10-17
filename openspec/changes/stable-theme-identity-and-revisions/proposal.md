# Proposal

## Why

Themes currently have incompatible identity rules: XML `ect-id` controls equality and selected intent, display name keys the catalog, and source basename controls stored replacement. Q3/U4 remain directly evidenced on merged `develop`; missing IDs collapse equality and same-ID edited content can be skipped by OK ([evidence](evidence.md)).

## What Changes

**Approved for bounded implementation on 2026-10-18.** The maintainer explicitly approved PM1–PM4 Option A and PM5 Option D and requested approval recording/commit followed directly by implementation. Alternatives below remain historical decision context; validation alone is not approval.

- Separate stable logical identity, legacy source ID, display metadata, installed-file locator, and selected intent. Retain one current content representation; introduce no revision/history/digest field.
- Recommend preserving curated bundled IDs in a bundled identity domain and allocating persistent local logical IDs for installed themes, independently of untrusted source claims. A storage record binds an ID to a file; it is not a second identity system.
- Keep identity across metadata/content changes to an established theme. A newly arriving file does not establish continuity by matching label, path, bytes, or `ect-id`; choosing an update target remains later import work.
- Make catalog references and equality use established logical identity, including separate same-name themes. Honor explicit Apply/OK requests without an ID-only skip.
- Introduce identity bindings lazily for existing installed files without rewriting XML or relocating files. Read old selection intent conservatively; represent unresolved intent separately from an applied theme.
- **BREAKING, approved:** separate imported themes claiming the same source ID; expose distinct same-name catalog entries; change internal selected references and fresh/unresolved default handling; repeated OK with a selection can reapply unchanged colors. These compatibility and fallback consequences are covered by PM1–PM5 approval.

## Capabilities

### New Capabilities

None. Existing capabilities own this behavior.

### Modified Capabilities

- `theme-definition`: stable logical identity distinct from legacy source metadata and current content.
- `theme-catalog`: persistent identity bindings and lookup independent of display-name uniqueness.
- `theme-application`: logical selection references, conservative legacy resolution, and no ID-only application suppression.

`theme-import` is deliberately unchanged: replacement targeting, collision choices, staging, atomicity and removal correctness belong to the next change. These remain known defects during this intermediate foundation; identity alone does not make import safe.

## Decision Gate

**Resolution record (2026-10-18): PM1 A, PM2 A, PM3 A, PM4 A and PM5 D are explicitly maintainer-approved.** The same request authorizes implementation after the separate planning commit. The alternatives/rationale below are retained as historical context, not open decisions.

- Identity survives label/content changes and supported movement of its associated storage record; incoming-file matches do not establish continuity.
- A generated ID is not durably established until its binding successfully persists. Failure preserves XML/files, previous bindings and old intent, diagnoses through the existing boundary, and withholds affected admission rather than exposing a transient ID.
- This R2 change manages selected intent, not truthful successful-applied state. Existing `currentTheme` terminology does not authorize an R3 journal/transaction redesign.
- No source trust infrastructure, automatic re-import linking or update policy is approved. Material new product questions or a generalized storage/import design must return to this gate.
- Verification starts with isolated deterministic persistence tests. Use existing runtime facilities only if they prove a boundary those tests cannot; no new runtime system or provisioning is required for completeness.

### Evidence-dependent

- **ED1 resolved:** current behavior, call sites, persisted references, 33-theme corpus and Q3/U4 are traced in `evidence.md`. R1 changes did not fix identity/import behavior.
- **ED2 unavailable evidence:** no representative external import corpus or real user workspace inventory was supplied. Their ID reliability and incidence of ambiguous stored selections are unknown. Use synthetic missing/duplicate-ID and selection fixtures during implementation; do not inspect/mutate personal workspaces or infer universal compatibility.
- **ED3 implementation check:** prove a small lazy binding can be durably saved and reread through the existing state-area boundary before exposing its ID. An isolated failure test is required; if this needs a storage framework or import transaction redesign, stop and return to this gate. Planning performs no runtime experiments.

### Product / Maintainer decision

| ID / exact question | Options and consequences | Recommendation and smallest-sufficient rationale |
| --- | --- | --- |
| **PM1: What survives rename, source movement and changed content?** | A: an established logical ID survives name, origin path and content changes; an incoming file still needs an explicit continuity decision. B: any such change creates another theme, breaking future update/customization attachment. | **A.** An opaque stable reference removes accidental presentation/content identity without implementing updates, overrides or inheritance. Moving the external source does not modify an already installed copy. Arbitrary manual moves of managed storage are not a supported relocation operation. |
| **PM2: Who establishes identity for bundled/imported themes, including duplicate names or `ect-id` claims?** | A: bundled identity uses the existing curated ID in a bundled domain; each independent installed record gets a persistent local logical ID, retaining `ect-id` as untrusted source metadata. Same names/claims do not merge records. B: use namespaced source `ect-id` as shared logical identity whenever present; fewer generated IDs, but requires deciding duplicate claims, bundled shadowing and fork semantics now. C: require author-supplied modern IDs; rejects currently accepted legacy input. | **A.** One identity concept with different assignment mechanisms; no trust registry or new author requirement. Local identity is not proof of upstream equivalence. Independent installations are distinguishable; automatic re-import deduplication/update policy remains deferred. Catalog entries with identical names remain distinct, with stable ordering, without a new conflict UI. |
| **PM3: Are generated IDs allowed, where do they live, and when do old files acquire them?** | A: lazily persist plugin-owned ID/file bindings when existing installed files are admitted, and bind new records only at successful import admission; preserve XML and locations. B: add identity to copied XML, modifying documents and entangling parsing/import. C: defer all persisted identity until the next import change, leaving old installed themes without stable references. | **A.** Minimal durable metadata only; no eager workspace sweep, file moves or general migration engine. This covers present missing/duplicate source IDs without requiring source XML to own installation identity. On failed/inconsistent binding persistence, diagnose and withhold affected identity admission while preserving files and old intent; do not substitute an unstable ID. Exact serialization is engineering; ownership and failure admission are this product decision. |
| **PM4: How do old, absent and ambiguous selected references resolve?** | A: new references resolve logical IDs; old nonblank raw `ect-id` resolves only with exactly one loaded candidate. Blank, missing and ambiguous matches remain unresolved, preserve old explicit intent and do not apply a fallback. Fresh default is no selected/applied claim. B: retain first-match/default-name lookup, risking a different theme or false current marker. C: reset ambiguous intent to a chosen default, losing intent and potentially changing appearance. | **A.** A narrow compatibility reader; no XML migration and no guessed ownership. The historical default label is not evidence of prior application. An explicit subsequent selection writes the new reference; opening alone does not apply colors. R3 still owns truthful applied-state transactions. |
| **PM5: Is revision/digest/versioning necessary, and should unchanged OK reapply?** | A: stable ID plus current content digest; detects changes but needs definition/canonicalization and applied-content tracking. B: stable ID plus structural content comparison, requiring a stable content projection. C: immutable history/versioning, with unsupported storage/UI costs. D: stable ID plus replaceable current content, always honoring an explicit Apply/OK when selected; unchanged content can be reapplied. | **D.** The demonstrated requirement is that new content is not skipped. Existing double-click already reapplies. Removing the optimization needs no digest, revision counter, history or user-visible version. No automatic startup apply or live file watching is implied. |

### Engineering

- **EN1:** keep the existing bundle, parser/token architecture and APIs internal; use simple opaque identity values and a small plugin-state binding, no new service hierarchy or physical module. Details and rationale are in `design.md`.
- **EN2:** use logical identity for equality/reference matching; keep content out of hash/equality. Preserve the legacy source ID for the existing preview URL. Never send a generated local ID as a website theme ID.
- **EN3:** retain display-name sorting with logical ID as a tie-breaker. A binding locator may use the current stored relative filename; the filename must not generate the ID. A supported future relocation must carry its binding.

### Deferred

Re-import as update versus another installation, duplicate-claim resolution, bundled override policy, content deduplication, atomic file replacement and removal result handling: **R2 `atomic-deterministic-theme-import`**. Broad validation/diagnostics and source-ID grammar: **R2 `strict-theme-validation-diagnostics`**. Immutable snapshots: **R2 `immutable-validated-theme-model`**. Application-result/commit truth and recovery: **R3**. Coherent Cancel/defaults and offline preview: **R4**. Public identity/API or package/ecosystem guarantees: **R6/R9**. Semantic workbench expansion: **R7–R10**.

## Impact

Implementation touches theme identity/model consumers, repository catalog/binding persistence, selected-reference lookup/defaults and the narrow OK guard. Preserve JavaSE-17, current target/platform scope, token semantics, XML security, adapters, unrelated preferences and files. No new dependencies, exported API, schema/package format, database, UUID infrastructure or migration framework is justified.

The compatibility reader and lazy binding are approved by PM3/PM4. Older versions will ignore new identity metadata and cannot interpret newly written selected references; downgrade may require reselection. They must still be able to read untouched XML at its existing location. No rollback claim is made for colors already applied by either version. See `design.md` for failure boundaries and `verification.md` for phase-specific results.
