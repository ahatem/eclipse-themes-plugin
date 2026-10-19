# Tasks

## 1. Contract

- [x] 1.1 Close R7 (P4 GO, Option D GO, findings 39) and archive `workbench-theme-engine-spike`.
- [x] 1.2 Answer the production-prep gate: private color keys (Q1) and hiding the twin (Q2); record evidence.
- [x] 1.3 Write proposal, delta specs and design.

## 2. Pure logic (no SWT/JFace in `core`)

- [x] 2.1 `WorkbenchFamily`, `WorkbenchColor`, `WorkbenchPalette`, `WorkbenchPaletteResolver` as a deterministic semantic conversion (authored non-syntax candidates, OKLCH derivation, readability correction, family fallback; 8 roles).
- [x] 2.2 Capability model: environment, probe, classifier, VERIFIED table, diagnostics text.
- [x] 2.3 `WorkbenchSlots` target rule.

## 3. Eclipse integration

- [x] 3.1 `WorkbenchThemeEngine` interface and `EclipseWorkbenchThemeEngine` (sole `IThemeEngine` user), `PaletteSink` over the current theme registry.
- [x] 3.2 `WorkbenchCoordinator`: apply, relinquish, reconcile, safe revert, ownership record, session failure flag.
- [x] 3.3 `plugin.xml` (4 themes, 16 non-editable color definitions, startup), two CSS files, optional `Require-Bundle`, `WorkbenchStartup`.
- [x] 3.4 `ThemeApplyOutcome`; `ThemeService` and `ThemeManager` return and thread it; `PreferenceKeys`.
- [x] 3.5 Preference page: Workbench section driven by `WorkbenchSectionModel`; apply/OK/Cancel, stop management, Appearance route, diagnostics copy.

## 4. Tests (focused)

- [x] 4.1 Palette derivation: dark, light, determinism, authored-candidate acceptance/rejection, hue preservation (cool/warm/tinted light), readability correction, minimal/malformed fallback, no syntax-token dependence.
- [x] 4.2 Capability classification including VERIFIED exact match, future micro, other platforms, each UNAVAILABLE reason.
- [x] 4.3 Slots: family choice, A/B alternation, same-family change is a real transition.
- [x] 4.4 Coordinator: ownership capture and restore (including absent saved id), external change wins, failed switch revert rule, publish compensation, unchanged palette no-op, experimental opt-in.
- [x] 4.5 Aggregate outcome and section model; static consistency test (Java mapping vs `plugin.xml` vs both CSS files).

## 5. Runtime verification (Windows, 4.40 and 4.41)

- [x] 5.1 Bundled dark to bundled dark, dark to light, light to light.
- [x] 5.2 Imported legacy theme, disable, external Appearance override, restart, apply timing.
- [x] 5.3 Experimental flow by controlled version metadata only if a clean seam exists. No clean seam exists on Windows (both installs are VERIFIED); covered by unit tests only and recorded as not runtime-verified.

## 6. Docs and validation

- [x] 6.1 Update compatibility/current-architecture/R8 docs that became stale; ROADMAP only after verification.
- [x] 6.2 `git diff --check`, `openspec validate --all --strict`, one full `mvn clean verify`.
- [x] 6.3 Proportionality review and final report.

## 7. Finishing pass (visual coverage, single Apply, conversion)

- [x] 7.1 Single Apply: editor preferences are written after the workbench switch (`ThemeService`); regression `ThemeServiceOrderTest`; runtime-verified for dark to dark, dark to light, light to light and light to dark.
- [x] 7.2 Semantic conversion with `PerceptualColor` (OKLCH), `accent` role, readability correction; `WorkbenchPaletteResolverTest` rewritten.
- [x] 7.3 CSS coverage per the stock Eclipse Dark/Light structure: global shell layer, inputs, tabs, trim, forms; coverage matrix in the compatibility document.
- [x] 7.4 Transition test: every role of the target family is published before the selection, every transition.
