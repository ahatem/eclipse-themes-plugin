# Design

## Context

Audit Q1 verified that `resetToEclipseOriginal` clears six entire foreign preference nodes. Eclipse default resolution is layered: removing an instance override does not necessarily reconstruct the value a user had before Eclipse Themes wrote it, because defaults may come from CSS/product customization and existing installations have no ownership snapshot.

The immediate objective is therefore **stop destructive reset now**, while designing the code so a later ownership journal can provide exact/conflict-aware restoration for new writes.

## Design goals

- No `clear()` on foreign preference nodes.
- Centralize the exact keys Eclipse Themes considers managed for each current integration.
- Preserve unrelated settings under all reset/default paths.
- Preserve the distinction between remove-a-managed-override and write-a-specific-value.
- Make legacy recovery limitations explicit and testable.
- Avoid introducing the complete R3 transaction/journal architecture in this change.

## Proposed approach

### 1. Define managed-key ownership from existing adapter mappings

Create a single source of truth per integration for keys Eclipse Themes writes/clears as part of theme application. Reuse that source for safe reset rather than maintaining a second broad node-level list.

Do not expand ownership to keys merely because they share a preference node.

### 2. Replace node clearing with key-scoped restoration/removal

The reset path iterates only managed keys. For the legacy no-snapshot case, use the most conservative behavior supported by current Eclipse preference semantics and the key's integration contract. If removing an instance override is the chosen behavior, tests must establish that unrelated keys survive and no guessed value is written.

The implementation must not claim that this reconstructs historical user values for installations that never recorded them.

### 3. Preserve future ownership-journal compatibility

Shape helper APIs around `(node, key, original-state?, last-written-state?)` concepts rather than a `clearNode(node)` abstraction. The future R3 journal can then supply exact original state without another reset redesign.

### 4. UI semantics

If current dialog text says or strongly implies "restore all original Eclipse colors/settings," revise it to describe safe stop-management/reset behavior and legacy limitations. Do not mix broader Apply/Cancel redesign into this PR.

## Alternatives rejected

### Clear foreign nodes then reload defaults

Rejected: destroys unrelated user/plugin state and still cannot prove reconstruction of the user's previous values.

### Use CSS default bookkeeping as the universal original snapshot

Rejected: CSS default bookkeeping is not a complete ownership journal for every preference Eclipse Themes modifies.

### Build the full ownership journal in this change

Deferred: desirable R3 architecture, but unnecessarily expands the critical safety patch. This change must establish a safe baseline first.

## Failure handling

A failure restoring/removing one managed key must be surfaced with the affected integration/node/key context. It must not trigger whole-node clearing as fallback. Partial failure should not be reported as complete success.

## Compatibility

No runtime Java baseline, target Eclipse baseline, extension-point contract, or OSGi package export is intentionally changed by this work.

## Verification

### Verified implementation and legacy policy

The runtime at `ebec341` is identical to remote `develop` at `744eedc` (the latter
changes only README/funding content). The existing reset clears the instance
nodes `org.eclipse.ui.editors`, `org.eclipse.jdt.ui`, `org.eclipse.cdt.ui`,
`org.eclipse.pde.ui`, `org.eclipse.ant.ui`, and `org.eclipse.debug.ui`, then discards
active selection even if a clear/flush fails.

Ownership is the union of each built-in adapter's existing mappings, including
conditional removals. Legacy styles include the color key and `_bold`, `_italic`,
`_underline`, `_strikethrough`; semantic styles include `.color`, `.enabled` and
the four dot-separated flags. Plain color mappings own only their exact key.
Text editor mappings additionally own four `SystemDefault` switches and the
three conditional version-control annotation colors. Neighbouring settings,
child nodes, CSS theme bookkeeping, and similarly named unmapped keys are excluded.

All these managed **instance overrides are removed**, including boolean switches
and optional style/annotation keys, allowing Eclipse's existing lower preference
scopes to resolve values. Reset never writes `true`, default colors, CSS values,
or a dark-theme deletion fallback. These values are not trustworthy historical
snapshots. Cleanup still covers all six built-in nodes even if an optional editor
is now uninstalled. Third-party extension mappings are not inferred as owned.

The current optional-semantic application order removes `.enabled` after writing
`false` when a token is missing; this patch preserves it. Known mapping issues
(including instruction-pointer keys) remain outside this change. Tests capture
the original full/sparse dark/light application mutation sequences to prevent
accidental corrections or behavior drift during ownership centralization.

Reset aggregates key-removal, node-access and flush failures; flush diagnostics
include the managed keys whose persistence is uncertain. Independent nodes and
keys are still attempted. No rollback/whole-node fallback is attempted; a failed
reset can be partial, and UI active selection is retained until reset succeeds.
The user receives contextual diagnostics and can retry. Calls stay on the caller's
UI thread. No target, runtime baseline, manifest, or extension contract changes
are needed. Tests use isolated in-memory preference doubles, never instance scope.

Automated tests must prove:
- unrelated keys remain after reset;
- multiple unrelated keys in each touched node remain;
- managed keys are the only keys changed/removed;
- originally absent state can be represented by the future helper/snapshot model;
- failure cannot invoke whole-node clear fallback;
- no unconditional success is shown when the reset operation reports failure.
