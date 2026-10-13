# Eclipse Themes — Repository Agent Guide

## Mission

Eclipse Themes is a brownfield Eclipse IDE plugin that currently coordinates editor-oriented color themes through Eclipse preference integrations. The long-term product direction is a safe, open, composable place to customize supported Eclipse appearance surfaces, but safety, reversibility, compatibility, and truthful behavior take precedence over expanding scope.

Treat this repository as a maintained Eclipse/OSGi product, not a generic Java application.

## Instruction precedence and scope

- Follow the nearest `AGENTS.md` that applies to every file you touch. Nested files narrow or override this root guide for their subtree.
- Direct maintainer instructions and the active OpenSpec change take precedence over general workflow preferences.
- `docs/audits/` is evidence from a historical baseline, not canonical current behavior.
- `openspec/specs/` is the accepted behavioral contract. Active `openspec/changes/*` describes proposed deltas until archived.

## Before changing anything

1. Inspect `git status`, current branch, and recent history. Preserve unrelated user work.
2. Read the relevant `AGENTS.md` chain.
3. Read `docs/ROADMAP.md` for sequencing and `openspec/config.yaml` for planning rules.
4. Check `openspec list` and `openspec status --change <name>` before creating overlapping work.
5. For a bug, reproduce or establish direct code evidence before designing the fix.
6. For behavior changes, prefer the OpenSpec workflow. Pure editorial/tooling changes may use `skip_specs: true` only when they truly do not change product behavior.
7. Re-check any audit claim against current code before acting on it.


## Spec program layers

- `openspec/specs/` = accepted current behavioral source of truth.
- `openspec/changes/` = active bounded deltas only. Do not pre-create dozens of dependent future changes.
- `docs/target-specs/` = audit-derived future-state requirements/research acceptance criteria. They are planning inputs, not current behavior. Re-verify and convert only the next bounded slice into an OpenSpec change.
- OpenSpec-generated skills under `.agents/skills/`, `.claude/skills/`, or other tool directories are generated artifacts. Refresh them with `openspec update`; do not hand-edit them.
- `openspec/AGENTS.md` is obsolete in modern OpenSpec and must not be recreated.

## Development workflow

Use the current OpenSpec workflow installed for the active coding tool. Conceptually:

`explore (when needed) → propose → review artifacts → apply → verify → archive`

- Use `explore` for ambiguous architecture, Eclipse API questions, cross-version behavior, or full-workbench theming research.
- Use `propose` for a bounded behavior change. Keep one coherent outcome per change.
- During `apply`, implement tasks in dependency order and keep the spec/design coherent when evidence changes.
- Run `openspec validate <change>` while working and `openspec validate --all --strict` before final handoff when OpenSpec files changed.
- Before archive, verify implementation against the proposal, delta specs, design, tasks, tests, and supported compatibility assumptions.

Do not create a giant "modernize everything" change. Prefer small, independently reviewable changes that advance the roadmap.

## Decision gates before finalizing a change

Before finalizing any new OpenSpec proposal, classify material unresolved questions into the following categories:

### Evidence-dependent
Questions that require repository inspection, experiments, Eclipse/runtime verification, compatibility testing, or external API research.

The agent must investigate these before turning an assumption into a contract. If evidence cannot be obtained during planning, record the uncertainty and required spike/test explicitly.

### Engineering
Implementation choices that do not materially change product semantics, supported compatibility, user-visible behavior, data/state ownership, or extension contracts.

The agent may choose these independently, but must record significant choices and rationale in `design.md`.

### Product / Maintainer decision
Questions that materially affect user-visible behavior, compatibility, migration, supported platforms/versions, fallback semantics, destructive/recovery behavior, extension/API commitments, theme-author requirements, or product scope.

The agent MUST NOT silently choose these.

For each such question, present:
- the decision to make;
- realistic options;
- consequences/trade-offs;
- the agent's recommended option and rationale.

The proposal must remain unapproved for implementation until the maintainer explicitly resolves every blocking Product / Maintainer decision.

### Deferred
Questions intentionally outside the bounded change.

Record the owning roadmap/spec area when known and ensure the current change does not accidentally decide them.

### Decision Gate section

Every behavioral OpenSpec proposal must contain a `## Decision Gate` section before implementation begins.

If no maintainer decision is required, state that explicitly and list any material evidence-dependent, engineering, or deferred questions.

Any `BREAKING` compatibility behavior automatically requires explicit maintainer review, even when the recommended behavior is security-motivated.

Do not ask the maintainer to decide questions that can first be resolved through technical evidence. Investigate first, then present the actual product choice.

## Branch and PR policy

- Integration branch: `develop`.
- Start production work from an up-to-date `develop` unless the maintainer explicitly selects another base.
- Prefer descriptive branches such as `fix/preference-reset-safety`, `fix/xml-parser-security`, `feat/theme-identity`, `chore/ci-release-hardening`.
- PRs should normally target `develop`.
- One PR should have one primary reason to exist. Do not mix opportunistic refactors, formatting churn, dependency upgrades, and feature work.
- Do not push, merge, tag, publish, or alter releases unless the user explicitly asked for that remote action.

## Build model

The repository is a Maven/Tycho reactor with:

- target definition: `releng/com.github.eclipsethemes.target`
- runtime bundle: `bundles/com.github.eclipsethemes`
- installable feature: `features/com.github.eclipsethemes.feature`
- p2 repository: `releng/com.github.eclipsethemes.updatesite`

The runtime bundle targets JavaSE-17. Tycho 5.x builds require a newer build JVM; CI currently uses JDK 21. Do not "fix" this difference by raising the bundle execution environment without an explicit compatibility decision.

Primary full reactor verification:

```bash
mvn clean verify -B
```

Use the smallest meaningful verification first. A docs-only change does not justify expensive runtime testing. A preference-adapter or parser change does.

## Architecture rules

- Preserve logical layering even while the project remains physically small.
- Core theme parsing/model logic must not gain new SWT/JFace/activator dependencies.
- Prefer public Eclipse APIs. If an internal API, implementation preference key, or renderer detail is unavoidable, isolate it, document provenance and supported versions, and test it.
- Do not split OSGi bundles merely to match a future architecture diagram. Split only when API ownership, optional dependencies, independent testing, or lifecycle boundaries justify it.
- External extension contracts must be intentionally versioned and export only the minimum supported API surface.
- Keep third-party/editor integrations optional where practical.
- Avoid hidden global state and activator service-locator dependencies in domain code.

## Preference safety — mandatory

Theme code writes into preference namespaces owned by Eclipse and other plugins. Treat every preference mutation as potentially destructive.

- Never clear an entire foreign preference node to restore theme state.
- Modify only keys explicitly owned by the active operation.
- Preserve whether an original key was absent, not just its value.
- Restore a key only when doing so will not overwrite a newer external/user change; conflict-aware restoration is the target architecture.
- Do not report a theme as successfully applied until the application result is known.
- Do not persist "active" state before successful application/commit.

## XML/import security — mandatory

Theme files are untrusted input.

- XML parsing must reject unsafe external entity/DTD behavior and fail closed when security controls cannot be established.
- Validate before replacing an existing imported theme.
- Use staged/atomic file replacement where practical.
- Bound archive/file size, path traversal, and external resource behavior for any future package format.
- Imported theme packages must remain declarative; do not execute arbitrary Java or bundle classes from imported theme content.

## Eclipse UI/threading/resource rules

- SWT widget access belongs on the SWT UI thread.
- Do not move preference writes to background threads blindly; listeners may assume UI-thread behavior.
- Move parsing/file I/O off the UI thread when it can block, then marshal UI updates back safely.
- Dispose SWT resources you create. Do not dispose system/JFace-managed resources you borrow.
- Test UI semantics such as Apply/OK/Cancel/defaults as behavior, not merely widget construction.

## Compatibility rules

- Treat Windows, GTK/Linux, and macOS as distinct rendering environments.
- GTK3/GTK4 and X11/Wayland differences require evidence before claims of equivalence.
- Native menus, dialogs, title bars, browser content, custom-drawn third-party widgets, and arbitrary icons are not universally themeable through Eclipse CSS.
- Do not promise "every pixel" or "theme the entire IDE" without a published coverage boundary.
- Preserve high-contrast/accessibility choices instead of overriding them blindly.

## Testing expectations

For a reproduced bug, add a regression test at the narrowest useful layer whenever feasible.

Verification order:

1. focused unit/contract tests for changed logic;
2. relevant adapter/repository tests;
3. OpenSpec validation when specs changed;
4. reactor `mvn clean verify -B` for production/build-impacting changes;
5. p2 install/runtime smoke tests when packaging, extension registration, or Eclipse runtime behavior changes;
6. OS/Eclipse-version matrix only when the change can differ across those environments.

Do not compensate for missing behavioral tests by adding many static-analysis tools at once.

## Completion standard

Before calling work complete:

- behavior matches the active spec;
- the root cause, not only the symptom, is addressed;
- unrelated preferences/files/state remain untouched;
- error paths are truthful and diagnostic;
- focused tests cover the changed contract;
- relevant build/runtime checks pass or a concrete blocker is documented;
- no accidental internal Eclipse dependency or compatibility expansion was introduced;
- docs/specs are updated when the public behavior or support matrix changed;
- `git diff` contains no unrelated churn.

## Review style

Prioritize findings in this order: user data/state loss, security, correctness, compatibility, lifecycle/threading/resource safety, API design, maintainability, UX/accessibility, style.

When reviewing, cite concrete files/methods and distinguish verified defects from hypotheses. Do not manufacture severity or numeric complexity metrics without evidence.
