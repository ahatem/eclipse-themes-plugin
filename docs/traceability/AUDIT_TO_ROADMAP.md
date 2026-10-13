# Audit → Roadmap Traceability

This table ensures actionable findings are not lost while avoiding one-change-per-bullet planning.

| Finding | Summary | Roadmap | Primary planned change/group |
|---|---|---|---|
| A1 | Adapter extension contract not usable as supported external API | R6/R9 | contributor API boundary |
| A2 | Parser logs through activator, creating layering back-edge | R6 | parser diagnostics decoupling |
| A3 | Weak dependency/version compatibility declaration | R5/R6 | compatibility matrix + integration metadata |
| A4 | Registry ordering/availability/collision policy weak | R6 | deterministic contributor discovery |
| Q1 | Whole foreign preference nodes cleared on reset | R1 | eliminate-destructive-preference-reset |
| Q2 | XML external entities allowed | R1 | secure-xml-theme-parsing |
| Q3 | Theme identity/equality/revision unsafe | R2 | stable-theme-identity-and-revisions |
| Q4 | Import overwrites before validation | R2 | atomic-deterministic-theme-import |
| Q5 | Import/reload conflict policy inconsistent | R2 | atomic-deterministic-theme-import |
| Q6 | Wrong debug instruction preference keys | R1 | correct-editor-preference-mappings |
| Q7 | Legacy aliases collide and lose theme semantics | R1/R2 | correct-editor-preference-mappings |
| Q8 | Sparse semantic token transition can re-enable editor defaults | R1 | correct-editor-preference-mappings |
| Q9 | Application failure can be logged yet reported/persisted as success | R3 | theme-application-result-contract |
| Q10 | Theme structure/metadata/color validation too permissive | R1/R2 | secure parser + validation diagnostics |
| Q11 | Parse/copy/list/flush work can block UI callbacks | R4 | UI/application extraction + jobs |
| Q12 | Filesystem mutation return/failure handling weak | R2 | atomic import/removal behavior |
| Q13 | Preference page mixes UI and application policy | R4 | extract-preference-page-policy |
| Q14 | Mutable/live model/global registration rough edges | R2/R6 | immutable validated model |
| Q15 | Extension/case/locale normalization inconsistencies | R2 | validation/import normalization |
| Q16 | No automated test sources/modules | R1/R5 | regression harness + runtime tests |
| B1 | Documented toolchain below Tycho 5 build requirements | R5 | build-toolchain-contract |
| B2 | Target resolution not fully pinned; CDT absent from integration target | R5 | target-and-compatibility-matrix |
| B3 | CI lacks behavioral tests/p2 install smoke | R5 | ci-behavioral-and-p2-smoke-tests |
| B4 | Release event incompatible with documented draft/publish flow | R5 | release-channel-hardening |
| B5 | Main snapshots and releases share deployment channel | R5 | release-channel-hardening |
| B6 | Version updates manual/inconsistent | R5 | release-channel-hardening |
| B7 | Rebuild reproducibility not established | R5 | reproducible-release-artifacts |
| B8 | Signing/checksum/SBOM/inventory absent | R5 | reproducible-release-artifacts |
| B9 | Broad workflow permissions/moving action tags/PR target gap | R5 | ci-permission-and-action-hardening |
| B10 | Maven cache key omits target/toolchain inputs | R5 | ci-permission-and-action-hardening |
| B11 | Bundle schema/license packaging incomplete | R5 | bundle-metadata-and-license-packaging |
| U1 | Remote preview does not reflect actual parsed theme | R4 | offline-accurate-theme-preview |
| U2 | Browser preview is mandatory/network/native-backend fragile | R4 | offline-accurate-theme-preview |
| U3 | Immediate/staged UI actions have inconsistent Cancel semantics | R4 | coherent-apply-ok-cancel-semantics |
| U4 | Default active id and lookup identity disagree | R2 | stable-theme-identity-and-revisions |
| U5 | Import diagnostics/conflicts are opaque | R2/R4 | validation diagnostics + import UI |
| U6 | Accessibility/focus/status/high-contrast gaps | R4 | preference-page-accessibility |
| U7 | UI/metadata strings not externalized | R4 | externalize-ui-strings |
| U8 | Imported website URI/link handling unsafe/weak | R2/R4 | validation diagnostics + safe metadata UI |
| U9 | Support claims/integration matrix inaccurate or invisible | R4/R5 | supported-integration-inspector |
| F1 | Whole-IDE/native/custom-widget coverage limits | R7+ | workbench spike + coverage contract |
| F2 | Dynamic CSS removal/replacement public API limits | R7 | workbench-theme-engine-spike |
| F3 | CSS/preferences can compete for same state | R3/R7 | ownership + workbench spike |
| F4 | Global icon replacement lacks universal public contract | R10 | research only |
| F5 | Version/platform matrix maintenance burden | R5/R7 | bounded compatibility matrix |
| F6 | Theme asset provenance/licensing uncertainty | R5/R9 | asset/license inventory |

## Audit observations not represented as separate changes

Positive findings (for example, no demonstrated current SWT background-thread violation and no obvious SWT resource leak) remain evidence in the audit and should not be converted into work merely to make the table symmetrical.
