# Test Strategy

## Priority

Behavioral regression protection is the first testing investment. Add static-analysis tooling only after the core safety/lifecycle behaviors can be tested.

## Layer 1 — pure/core tests

Cover:
- XML security and structural validation;
- color/style parsing and fallback rules;
- legacy property mapping and collision precedence;
- identity/revision equality rules;
- immutable model behavior;
- import candidate validation where filesystem/Eclipse dependencies can be isolated.

## Layer 2 — repository/application contract tests

Cover:
- failed replacement preserves prior valid file;
- duplicate id/name/basename conflict policy;
- same-file import safety;
- staged/atomic replacement semantics;
- application result aggregation;
- ownership snapshot including absence;
- A → B → disable restoration;
- external modification conflict preservation.

## Layer 3 — adapter tests

For every supported editor integration, assert exact node/key/value transitions and exact keys that remain untouched. Include rich → sparse theme transitions and semantic-highlight enable/disable behavior.

## Layer 4 — Eclipse runtime/Tycho integration

Use a small runtime suite to establish:
- extension discovery;
- adapter availability/version behavior;
- real preference listener effects where relevant;
- Apply/OK/Cancel/defaults semantics;
- p2 installability;
- workbench CSS switching when that roadmap stage begins.

## Layer 5 — OS/version matrix

Run only for behavior that can vary by SWT/native/backend/Eclipse release. Initial full-workbench matrix should include the declared supported Windows, Linux/GTK, and macOS environments and at least the selected current + previous Eclipse releases.

## Required regression cases from the audit

- external XML entity/DOCTYPE rejection;
- invalid replacement import does not delete previous valid theme;
- distinct legacy themes do not collapse to equal identity;
- arbitrary XML root is rejected or handled by explicit legacy mode;
- `.XML`/extension policy is deterministic;
- optional semantic token transitions retain intended enabled/disabled state;
- GitHub Dark Dimmed selection mapping regression;
- static-final style collision regression;
- unrelated foreign preferences survive apply/reset;
- debug instruction/color keys are mapped correctly;
- application failure cannot persist/report success.
