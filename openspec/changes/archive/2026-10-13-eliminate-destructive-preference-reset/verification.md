# Implementation verification

Implemented on `fix/preference-reset-safety`; change remains active and unarchived.

## Baseline and scope

- Re-verified the six destructive node clears in the current preference page.
- Fetched remote `develop` (`744eedc`): runtime is identical to the checkout base
  (`ebec341`); only README/funding content differs. The existing README changes
  prevented a fast-forward and were preserved without stashing or replacing them.
- Apply/reset now share exact mapping declarations. No preference mapping fixes,
  general application-result redesign, XML work, target changes, manifest changes,
  extension registration changes, or package exports were introduced.

## Automated regression coverage

Nine JUnit tests run in the runtime module through Maven Surefire. All preference
doubles are isolated; unexpected preference operations, including `clear`, fail.

- Full/sparse dark/light adapter mutation sequences match the original adapters
  at `ebec341`. Baselines use distinct deterministic colors and varied style flags
  for canonical tokens, so source-token swaps cannot hide behind identical values.
- A fixture regression checks color uniqueness and repeatability, then swaps
  keyword/string values and proves the baseline detects the swap even when
  preference keys and operation order stay unchanged.
- Ownership equals the union of keys actually written/removed by those sequences.
- Multiple unrelated values in every affected node survive, including multiline
  and Unicode values and similarly named unmapped keys.
- Present overrides are removed; absent keys stay absent; repeated reset is safe.
- A trustworthy absent snapshot differs from explicit empty/nonempty values.
- Removal, node-access and multiple flush failures retain integration/node/key
  context, permit independent cleanup, never clear nodes, and never run the
  success callback that clears active-theme selection.

Focused command (JDK 21 build JVM, JavaSE-17 runtime compilation):

```text
mvn clean test -B -pl releng/com.github.eclipsethemes.target,bundles/com.github.eclipsethemes -am
```

Result after the P3 review follow-up: **9 tests, 0 failures, 0 errors, 0 skipped**.

## Reactor and specification checks

```text
mvn clean verify -B
openspec validate eliminate-destructive-preference-reset --strict
openspec validate --all --strict
```

Results: **all five reactor projects succeeded**; the bundle, feature and p2
repository were built. Strict OpenSpec validation passed for the change and all
seven accepted specs (8 items total). Test classes/JUnit are not shipped in the
runtime jar. The runtime Java baseline and Eclipse 2024-09 target are unchanged.

## Runtime smoke

Loaded the built runtime jar in Eclipse 2024-09 Equinox on Windows/JDK 21, with
framework and workspace storage entirely under the repository's ignored `tmp/`
directory. Resolved the product bundle, started the Eclipse preference services,
seeded all **537 managed keys** in the six real instance nodes, and ran reset.
All managed overrides were removed and flushed. Multiple unrelated values,
unrelated child-node contents and explicit lower-scope defaults survived.
The optional CDT integration did not need to be installed to clean its legacy
preference node. This was a headless preference/runtime smoke, not an interactive
SWT dialog or cross-OS rendering test.

## Final acceptance review

- No foreign node `clear()` or `removeNode()` remains in runtime code. Remaining
  `clear()` calls affect in-memory catalog/registry collections only.
- Reset changes only declared managed instance keys, never writes guessed
  originals/default values and never touches CSS bookkeeping or child nodes.
- Dialog text describes stop-management and explicitly explains that previous
  custom values were not recorded and cannot be recovered.
- UI active-state clearing is gated by complete reset/persistence success;
  partial failure displays/logs contextual diagnostics and returns before normal
  defaults completion. The success callback is protected by regression tests.
- Reviewed all modified/new production and test files against proposal, design,
  deltas and Q1 criteria; `git diff --check` passes. Unrelated working-tree
  content is excluded from this change. Full historical/conflict-aware ownership
  restoration remains explicitly deferred to R3, as planned.

## P3 review follow-up

Strengthened only `AdapterBaselineTest` and this verification record. The fixture
sorts canonical token names before assigning unique RGB values and varied styles;
full and sparse fixtures retain the same values for tokens they have in common.
All 18 expected mutation hashes were regenerated using the **original adapter
sources from `ebec341`**, not the refactored implementation. Existing ordered
put/remove, optional-token and flush-count assertions remain in place.

The focused Maven command above passed all 9 tests. `openspec validate --all
--strict` passed all 8 items, and `git diff --check` passed. Production source
hashes are unchanged from the start of this follow-up. No archive, commit, push
or new change was performed.
