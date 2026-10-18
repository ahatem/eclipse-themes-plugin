# Workbench theme engine spike

RESEARCH ONLY

NO PRODUCTION BEHAVIOR CHANGE

GO / NO-GO DECISION GATE

## Why

R7 must establish the smallest honest mechanism for coordinating editor colours with named,
verified Eclipse workbench surfaces. The accepted product still coordinates editor preferences.
Documentation and a successful method return cannot prove workbench rendering, restart,
persistence, external switching, or safe disable semantics.

The required starting baseline is develop/origin/develop
`7ebb3433cbf91a173395740464d034dce1d72855` (R2 complete). The research branch includes the
separate, already-created R2.4 archive/promotion commit `326365c0ccb6ced788497a150555cbf6488515f6`.
This change resumes the existing branch and uncommitted planning/probe work; it does not recreate
that archive or rewrite its history. Findings distinguish inherited observations from new runs.

## What Changes

Only this research record is committed. `.openspec.yaml` declares `skip_specs: true` because
there is no spec-level behavior change. No delta specs or accepted workbench requirements are
invented. `findings.md` records measured results and engineering choices; a separate production
design is unnecessary. The probe, launch configurations, downloads, and workspaces stay under
ignored `tmp/workbench-spike/` and are not committed. Committed evidence may include text logs,
API signatures, preference differences and verified screenshots, but no probe implementation.

Compare independently:

| Candidate | Mechanism | Primary question |
| --- | --- | --- |
| A | Own declarative theme through `org.eclipse.e4.ui.css.swt.theme` | Smallest selectable, persistent, reversible workbench theme? |
| B | Declarative stylesheet with a built-in `themeid refid` | Can management stop while preserving that built-in theme? |
| C | Runtime `registerTheme` / `registerStylesheet` / `setTheme` | Is generated CSS justified later for R8b? |

Record the target definition and actual Eclipse build, theme bundle/API metadata, JDK, OS,
windowing backend, and IDs. Inspect the matching source bundle and extension schema, including
export restrictions. Public Java visibility is not proof of an unrestricted supported API.

## Research acceptance

1. Inventory registerTheme, registerStylesheet, registerResourceLocator, getThemes, both setTheme
   forms, restore, getActiveTheme and applyStyles; explicitly check unregister/reset availability.
2. Run A on Windows: inventory, select, resolve widget styles, inspect Appearance, persist true,
   restart, switch away/back and record remaining effects. Verify active ID after every setTheme.
3. Run B on a built-in theme: before/after identity, rendered effect, restart, switching away/back,
   and limits on runtime disabling. Use another built-in theme without the contribution as a control.
4. Run C separately: duplicate IDs, unknown selection, repeated stylesheet registration, refresh,
   restart without registration and with re-registration, bad CSS/resources and diagnostic visibility.
5. Compare restore=true/false using exact before/after preferences and restart evidence. Test the
   hypothesis of platform persistence plus our own previous-value snapshot; do not implement it.
6. Inspect Appearance selection, Apply/OK and external change detection; distinguish workbench
   selection from existing editor-theme intent. Inspect the current dark-mode heuristic exactly.
7. Record a surface matrix and explicit exclusions. Use resolved widget values plus usable visual
   evidence; reject blank or occluded captures. Do not equate a styled Browser control to HTML styling.
8. Investigate accessibility without changing the user's OS accessibility settings. Separate real
   OS high contrast from selecting Eclipse's named High Contrast theme.
9. Complete bounded GTK/Linux runtime verification of A and representative surfaces. macOS may
   remain NOT VERIFIED. WSLg X11 evidence is not native Wayland/GTK4 coverage.
10. Read stale commit a6ab335 and its worktree without mutation. Separate selector ideas from
    invalid API assumptions; never merge or delete it.
11. Compare A/B/C against every requested criterion; give a supported GO or NO-GO, minimal pre-R8a
    slice, proposed R8a scope, R8b recommendation and roadmap recommendation without implementing them.

## Decision Gate

### Locked Product / Maintainer decisions

D1 declarative first; D2 test `setTheme(id, true)` plus a previous-selection snapshot; D3 change
workbench on Apply/OK only; D4 name supported surfaces explicitly; D5 preserve stale Claude worktree;
D6 Windows primary plus bounded GTK, macOS unverified allowed; D7 avoid runtime registration for R8a
unless declarative evidence fails. These authorize the disposable experiments and research record.
No further approval is needed to conduct this research within that scope.

### Evidence-dependent

Actual rendering, restart, persistence, API export support, failure visibility, accessibility and
cross-platform behavior must be measured or explicitly marked unverified. If platform metadata
restricts a nominally public interface, the final verdict must retain that support-contract issue.
No failed runtime-registration experiment automatically invalidates A or B.

### Engineering

Use isolated workspaces/configurations, small loud styles and matching source/API inspection.
Prefer deterministic widget values and exact preference differences over subjective impressions.
Keep provisioning changes local and disclose their effect on experiment fidelity. Preserve raw text
observations with run IDs; summarize inherited failed attempts rather than presenting them as success.
Record significant final research choices in findings.md.

### Product / Maintainer decisions after research

Choosing the R8a mechanism, supported platform/version boundary, accessibility policy, restart UX,
and disable/restoration semantics requires a separately approved production proposal. Any exception
to the no-internal-API criterion requires explicit maintainer decision; this spike cannot approve it.
Present concrete options, trade-offs and recommendations in findings.md. No R8 implementation is
approved by this proposal.

### Deferred

R3 application outcomes, ownership journals, rollback/recovery, startup reconciliation and public
surface APIs; R8a production selector/CSS; R8b generated/runtime CSS; R9 packages and contributor APIs;
macOS, native Wayland/GTK4 and third-party appearance-plugin testing. Do not modify ROADMAP.md.

## Impact and verification

No product plugin.xml, MANIFEST.MF, source, build metadata or runtime compatibility changes.
No probe code enters Git. Preserve existing worktrees. No push, PR, tag or release.
Run `git diff --check`, `openspec validate workbench-theme-engine-spike --strict` and
`openspec validate --all --strict`. Do not run Maven for this documentation-only change.
The R7 change remains active for independent review; do not archive it in this task.
