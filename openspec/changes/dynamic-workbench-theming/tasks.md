# Tasks

## 1. Contract

- [x] 1.1 Close R7 (P4 GO, Option D GO, findings 39) and archive `workbench-theme-engine-spike`.
- [x] 1.2 Answer the production-prep gate: private color keys (Q1) and hiding the twin (Q2); record evidence.
- [x] 1.3 Write proposal, delta specs and design.

## 2. Pure logic (no SWT/JFace in `core`)

- [ ] 2.1 `WorkbenchFamily`, `WorkbenchColor`, `WorkbenchPalette`, `WorkbenchPaletteResolver` with the fixed blends and contrast rule.
- [ ] 2.2 Capability model: environment, probe, classifier, VERIFIED table, diagnostics text.
- [ ] 2.3 `WorkbenchSlots` target rule.

## 3. Eclipse integration

- [ ] 3.1 `WorkbenchThemeEngine` interface and `EclipseWorkbenchThemeEngine` (sole `IThemeEngine` user), `PaletteSink` over the current theme registry.
- [ ] 3.2 `WorkbenchCoordinator`: apply, relinquish, reconcile, safe revert, ownership record, session failure flag.
- [ ] 3.3 `plugin.xml` (4 themes, 14 non-editable color definitions, startup), two CSS files, optional `Require-Bundle`, `WorkbenchStartup`.
- [ ] 3.4 `ThemeApplyOutcome`; `ThemeService` and `ThemeManager` return and thread it; `PreferenceKeys`.
- [ ] 3.5 Preference page: Workbench section driven by `WorkbenchSectionModel`; apply/OK/Cancel, stop management, Appearance route, diagnostics copy.

## 4. Tests (focused)

- [ ] 4.1 Palette derivation: dark, light, determinism, missing value, contrast fallback, no syntax-token dependence.
- [ ] 4.2 Capability classification including VERIFIED exact match, future micro, other platforms, each UNAVAILABLE reason.
- [ ] 4.3 Slots: family choice, A/B alternation, same-family change is a real transition.
- [ ] 4.4 Coordinator: ownership capture and restore (including absent saved id), external change wins, failed switch revert rule, publish compensation, unchanged palette no-op, experimental opt-in.
- [ ] 4.5 Aggregate outcome and section model; static consistency test (Java mapping vs `plugin.xml` vs both CSS files).

## 5. Runtime verification (Windows, 4.40 and 4.41)

- [ ] 5.1 Bundled dark to bundled dark, dark to light, light to light.
- [ ] 5.2 Imported legacy theme, disable, external Appearance override, restart, apply timing.
- [ ] 5.3 Experimental flow by controlled version metadata only if a clean seam exists.

## 6. Docs and validation

- [ ] 6.1 Update compatibility/current-architecture/R8 docs that became stale; ROADMAP only after verification.
- [ ] 6.2 `git diff --check`, `openspec validate --all --strict`, one full `mvn clean verify`.
- [ ] 6.3 Proportionality review and final report.
