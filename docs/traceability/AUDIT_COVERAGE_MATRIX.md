# Full Audit Coverage Matrix

This matrix answers a stricter question than `AUDIT_TO_ROADMAP.md`: **where did every major section of the 876-line audit go?**

| Audit area | Preserved evidence | Current contract / target artifact | Execution artifact |
|---|---|---|---|
| Scope/confidence + executable probes | full audit | `ENGINEERING_FINDING_REGISTER.md`; baseline specs where applicable | change-specific re-verification/tests |
| Part 1.1 module structure | full audit | `CURRENT_ARCHITECTURE.md`; `ARCHITECTURE_GUARDRAILS.md`; R6 target spec | R6 roadmap/change briefs generated when reached |
| Part 1.2 loading/parsing/applying/adapter runtime | full audit | `CURRENT_ARCHITECTURE.md`; `theme-parsing`, `theme-catalog`, `theme-application`, `editor-integrations` specs | R1-R3 changes |
| Part 1.3 extension points | full audit | `CURRENT_ARCHITECTURE.md`; R6/R9 target specs | contributor API work after prerequisites |
| Part 1.4 dependency/layering graph | full audit | `CURRENT_ARCHITECTURE.md`; architecture guardrails | A2/R6 work |
| Part 1.5 fragile/internal coupling | full audit | `ENGINEERING_FINDING_REGISTER.md`; R6 target spec | compatibility/mapping changes |
| Part 1.6 persistence | full audit | `CURRENT_ARCHITECTURE.md`; R3 target spec; `FULL_IDE_THEMING_TARGET.md` | R3 ownership/recovery work |
| Part 2.1 package/class + Q1-Q15 | full audit | ID traceability; engineering register; R1-R4/R6 target specs | bounded OpenSpec changes |
| Part 2.2 method complexity/side effects | full audit | engineering register; architecture guardrails | extraction only after tests/contracts |
| Part 2.3 error handling | full audit | R3/R4 targets | result/diagnostic changes |
| Part 2.4 threading | full audit | root AGENTS; engineering register; R4 target | Job/UI-thread work |
| Part 2.5 resource management | full audit | root AGENTS; engineering register | preserve as guardrail; no invented defect |
| Part 2.6 tests/Q16 | full audit | `TEST_STRATEGY.md`; R1/R5 targets | regression harness/runtime tests |
| Part 3.1 Tycho/target | full audit | R5 target; current architecture | build-toolchain/target changes |
| Part 3.2 CI/B1-B11 | full audit | ID traceability; R5 target | R5 changes |
| Part 3.3 release process | full audit | R5 target | release-channel hardening |
| Part 3.4 reproducibility | full audit | R5 target | reproducible-release artifacts |
| Part 3.5 missing tooling | full audit | engineering register; test strategy | add only high-value tools after tests |
| Part 4.1 preference page/U1-U9 | full audit | `preference-ui` current spec; R4 target | R4 bounded changes |
| Part 4.2 accessibility/contrast | full audit | R4 target; AGENTS compatibility rules | accessibility change(s) |
| Part 4.3 onboarding | full audit | R4 target | first-run/state semantics |
| Part 5A theming landscape/CSS limits/platforms/comparables | full audit | `FULL_IDE_THEMING_TARGET.md`; R7 research spec | workbench spike |
| Part 5B unified architecture/package/application/CSS/coexistence/extensions/persistence/migration | full audit | `FULL_IDE_THEMING_TARGET.md`; R8/R9 targets | after R3/R7 evidence |
| Part 5C feasibility/F1-F6/public-vs-invasive/version/licensing | full audit | R5/R7/R9/R10 targets | bounded support + legal inventory |
| Part 5D roadmap | full audit | `ROADMAP.md` | change queue/OpenSpec workflow |
| Part 6.1 blockers | full audit | `PRODUCT_STRATEGY.md` | informs roadmap/priorities |
| Part 6.2 differentiators | full audit | `PRODUCT_STRATEGY.md` | product prioritization |
| Part 6.3 success conditions | full audit | `PRODUCT_STRATEGY.md` | engineering/community/legal gates |
| Part 6.4 90-day plan | full audit | `ROADMAP.md` + change queue | dependency-ordered execution |
| Top 10 | full audit | `ROADMAP.md`, `CHANGE_QUEUE.md`, ID traceability | next-change selection |

## Coverage rule

A statement can be "covered" without becoming a canonical behavioral requirement. Descriptive architecture evidence belongs in architecture docs; positive findings remain guardrails/evidence; product strategy belongs in strategy; unknown workbench behavior becomes a research acceptance spec; desired future behavior belongs in target specs; only accepted current behavior belongs in `openspec/specs/`.
