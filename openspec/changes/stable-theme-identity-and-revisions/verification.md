# Planning verification inventory

## Approval and implementation authority

On 2026-10-18 the maintainer explicitly approved PM1 A, PM2 A, PM3 A, PM4 A and PM5 D and requested a separate `docs: define stable theme identity contract` commit followed directly by implementation. The approval includes durable binding before ID exposure and preservation on failure, and limits this change to selected intent rather than truthful successful-applied-state tracking. It permits deterministic isolated persistence evidence when sufficient, without mandatory runtime provisioning. All alternatives and discovery below remain historical context. The archive commit `aee20ba` must remain unchanged.

## Historical discovery phase

The initial 2026-10-18 handoff was discovery/planning only, with PM1–PM5 unresolved and no implementation. This historical status is superseded by the approval above. Planning-file existence reported by OpenSpec is not itself maintainer approval.

## Evidence inventory

| Planning acceptance | Evidence |
| --- | --- |
| Exact baseline and archive hygiene | `evidence.md` baseline; fetch confirmed PR #24 `9f708fa`, archive-only local commit preserved/rebased as `aee20ba`; no remote writes |
| Current identity roles and persistence | `evidence.md` E1–E10, role map and concept matrix |
| All twelve lifecycle scenarios | `evidence.md` code-trace table, with uncertainty/conditional behavior identified |
| Bundled IDs, duplicate names/IDs, legacy acceptance | 33 source root tags inventoried: no missing/blank IDs or duplicate literal IDs/names; existing missing-ID parser/import fixtures read, not executed |
| Demonstrated problems | Current parser/equality, catalog/storage and page guards directly support Q3/U4; Q4/Q5/removal hazards remain scoped to later work |
| Revision alternatives challenged | `design.md` options A–D; no history/version/digest requirement demonstrated; proposed explicit dispatch is enough for the bounded bug |
| Migration and product choices | Proposal PM1–PM5; lazy metadata/selection compatibility are recommendations, not approved behavior; external import-corpus reliability is unknown |
| Future sanity and scope | `design.md` four future scenarios and required-now/deferred table; no R9 package assumption promoted as a present dependency |
| Future verification | Unstarted tasks contain focused tests/ED3 and bounded runtime/reactor checks only for a later authorized implementation |

## Planning commands

Executed successfully:

- `openspec validate stable-theme-identity-and-revisions --strict` — valid.
- `openspec validate --all --strict` — 10 items passed, zero failed (9 main specs and this draft change).
- `git diff --check` — no whitespace errors in tracked differences; equivalent per-file no-index whitespace inspection also passed for all 9 new untracked planning files.

Read-only inspection, OpenSpec context/status/instructions and the requested baseline Git operations were also used. No Maven/reactor, Eclipse runtime, provisioning, broad suite, CI or production build ran. The pre-existing R1 verification results were reviewed as historical evidence only.

## Manual review and stop

At discovery handoff, manual review covered every new planning file and the archive-only commit diff. Production/build files under `bundles`, `features`, `releng` and the root POM had no difference from `develop`. The three listed capabilities had corresponding deltas and the modified selection requirement preserved its existing scenario. At that time draft SHALL language was conditional on PM1–PM5; approval above now resolves that condition. The twelve lifecycle traces remain historical current-state evidence. No digest/history or import conflict engine was added to the recommendation.

Discovery handoff branch: `feat/stable-theme-identity-and-revisions`; HEAD `aee20ba`, one archive-only commit above `9f708fa`, message `chore: archive regression testing OpenSpec change`. R2 planning files were uncommitted/untracked at that handoff. The existing dated R1 archive was preserved, not duplicated or redated. No production implementation, push, PR or next R2 change had started.

## Approval recording verification

Proposal, design, three deltas and tasks record the approved options and preserve their alternatives/rationale. No schema metadata change is needed. Strict change validation passed and strict all-validation passed all 10 items; `git diff --check` passed. The staged planning files receive whitespace/full-scope review before the separate planning commit; implementation results will be recorded below after execution.
