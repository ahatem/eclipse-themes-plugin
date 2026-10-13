# Tasks

## 1. Re-verify and characterize current behavior

- [x] 1.1 Re-open the current reset/default code on latest `develop` and confirm every foreign preference node currently cleared.
- [x] 1.2 Inventory the exact preference keys written by each built-in adapter and separate theme-owned keys from unrelated node contents.
- [x] 1.3 Record any adapter keys whose reset semantics require special treatment instead of simple override removal.

## 2. Add regression protection

- [x] 2.1 Add a test harness capable of exercising key-scoped preference reset logic without clearing real user state.
- [x] 2.2 Add regression tests that seed unrelated values in every affected preference node and prove they survive reset.
- [x] 2.3 Add tests for managed-key present/absent transitions and for failure reporting/no node-clear fallback.

## 3. Implement safe key-scoped reset

- [x] 3.1 Introduce a centralized managed-key representation reusable by apply/reset logic without broad node ownership.
- [x] 3.2 Replace whole-node `clear()` calls with key-scoped restoration/removal according to the verified legacy policy.
- [x] 3.3 Preserve exception context so failures identify the affected integration/node/key and cannot be mistaken for complete success.

## 4. Align user-facing behavior

- [x] 4.1 Review reset/default dialog/status text and remove any claim that unknown historical values can be reconstructed.
- [x] 4.2 Keep this change narrowly scoped; do not redesign general Apply/OK/Cancel semantics beyond what is necessary for truthful reset outcome.

## 5. Verify and hand off

- [x] 5.1 Run focused reset/adapter tests.
- [x] 5.2 Run `openspec validate eliminate-destructive-preference-reset --strict`.
- [x] 5.3 Run the reactor build (`mvn clean verify -B`) because production runtime code and tests changed.
- [x] 5.4 Inspect the full diff for unrelated preference, formatting, or architecture churn.
- [x] 5.5 Re-check Q1 acceptance criteria: no foreign node clearing; unrelated settings preserved; legacy limitation documented; failures not reported as success.
