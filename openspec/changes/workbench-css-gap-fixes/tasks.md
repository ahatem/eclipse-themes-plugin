# Tasks

## 1. Runtime evidence (before any CSS)

- [x] 1.1 Build a probe from the archived `parity` harness that, under each coordinated family on Windows Eclipse 4.40 and 4.41, opens a `ProgressMonitorPart`, a bare `ProgressBar` and a separator `Label`, and records getter colors and sampled pixels without and with candidate rules for `ProgressBar`, `ProgressIndicator > *` and `Label[style~='SWT.SEPARATOR']`. Verify: logs and screenshots saved under `evidence/runtime/` for all four runs.
- [x] 1.2 Record per selector whether the pixel changed to the palette role, in `evidence/progress-separator-measurement.md`. Trim the spec delta and design to the selectors that passed. Verify: `openspec validate workbench-css-gap-fixes --strict` passes after trimming.

## 2. Explicit appearance

- [x] 2.1 Add `isDarkTheme="true"` to `com.github.eclipsethemes.workbench.dark` and `.dark.alt`, and `isDarkTheme="false"` to `.light` and `.light.alt` in `plugin.xml`. Extend `WorkbenchContributionConsistencyTest.pluginXmlDeclaresTheFourThemesWithTheirFamilyStylesheet` to assert that each value matches its family. Verify: the focused test passes and fails when one value is flipped.
- [x] 2.2 Confirm in the runtime logs that all four themes are registered and selectable on 4.40 and 4.41 with the attribute present. Verify: theme ids listed by `IThemeEngine.getThemes()` in the probe log.

## 3. Stylesheet rules

- [x] 3.1 Add only the selectors that passed task 1.2 to `css/workbench-dark.css`, and mirror them byte-for-byte (apart from the family name) in `css/workbench-light.css`. Verify: `theTwoFamilySheetsAreIdenticalApartFromTheFamilyName`, `eachCssFileReadsOnlyItsOwnFamilyAndEveryRole` and `noColorIsHardCodedOutsideTheSplashRules` pass.
- [x] 3.2 Update the header comment of both sheets and the coverage table in `docs/WORKBENCH_THEMING_COMPATIBILITY.md` (progress bar and separator rows) to match the measured result. Verify: each row's class matches `evidence/progress-separator-measurement.md`.

## 4. Integration

- [x] 4.1 Re-run the probe with the final stylesheets on 4.40 and 4.41 and confirm the measured surfaces take palette colors under both families. Verify: final screenshots and logs saved in `evidence/runtime/`.
- [x] 4.2 Run `mvn clean verify -B` and `openspec validate workbench-css-gap-fixes --strict`, and review `git diff` for unrelated churn. Verify: build green, validation passes, diff limited to the files listed in proposal.md plus this change's directory.

## Workflow follow-up

- Archive the change after maintainer review, syncing the `workbench-theming` delta into the main spec.
