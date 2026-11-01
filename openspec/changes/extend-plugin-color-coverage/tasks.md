## 0. Decision Gate

- [x] 0.1 Maintainer resolves decision 1 (Spring Tools scope) and decision 2 (inclusion criterion): 1A and 2A, 2026-11-01. Update proposal, spec delta, design and evidence to match before continuing.

## 1. Table

- [x] 1.1 Add the three Mylyn Tasks keys (`past.due` ERROR, `overdue.for.others` WARNING, `incoming.background` INFO_TINT) to section 5 of `PluginColorTable`.
- [x] 1.2 If decision 1 is A or C, add the Spring Tools owning-bundle constants and section 6 with the eight keys and roles from `evidence/key-classification.md`.
- [x] 1.3 Update the table's provenance comment to the broadened criterion, citing both evidence files and, for Spring, the evidence commit.

## 2. Tests

- [x] 2.1 `PluginColorsTest`: assert the new keys are declared with their owning bundle and role, and that the terminal `cursorAccent` derives to the same value as the terminal background for a dark palette.
- [x] 2.2 `PluginColorsTest`: assert the rejected candidates (Mylyn Context, e4 tools, PDE target editor, WikiText) are not declared.
- [x] 2.3 Run the focused tests: `PluginColorsTest`, `PluginColorTableOwnershipTest`, `PluginColorWriterTest`. The existing writer tests iterate the table and cover gating, ownership and release for the new keys.

## 3. Docs and verification

- [x] 3.1 Update `docs/WORKBENCH_THEMING_COMPATIBILITY.md` with the key count, the surfaces list and, for Spring, the source-verified note.
- [x] 3.2 `openspec validate extend-plugin-color-coverage --strict` and `openspec validate --all --strict` (the change passes; `theme-application` and `workbench-theming` fail on `develop` already, with scenarios missing a GIVEN/WHEN step, and are untouched here).
- [x] 3.3 `mvn clean verify -B` (293 tests, BUILD SUCCESS).
- [x] 3.4 Review the full diff for unrelated churn.
