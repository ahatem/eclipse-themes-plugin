# Engineering Finding Register

This register captures audit observations that are broader than a single behavioral requirement. The detailed evidence remains in `docs/audits/2026-10-complete-project-audit.md`; the roadmap owns sequencing.

## Architecture and code structure

- A1: advertised external adapter contract is not yet a supported OSGi API surface.
- A2: core parser diagnostics depend on the plugin activator, creating a layering back-edge.
- A3: Eclipse/editor compatibility is under-specified and under-tested by version.
- A4: adapter discovery lacks deterministic ordering/collision/availability policy.
- Preference page is approximately 821 lines and combines widgets, preview, import/remove, application policy, persistence, dark-mode inference, and destructive reset behavior.
- Future extraction should prefer a declarative change-plan/application coordinator over generic abstraction for its own sake.

## Error/result handling

- parser/repository/service operations need structured diagnostics rather than `Optional.empty()` or log-and-continue outcomes for meaningful failures;
- theme application must aggregate contributor failures and must not persist/show success first;
- UI catches should retain causes and log/report through Eclipse-appropriate mechanisms;
- wrong adapter types and skipped integrations need diagnostics when materially actionable.

## Threading and blocking work

The audit did not demonstrate an existing background-thread SWT violation. The primary risk is blocking parse/copy/list/flush work in UI callbacks. Move file/parse work off-thread where safe and marshal widget updates to SWT; do not blindly move preference writes if target listeners assume UI-thread context.

## Resource management

No obvious SWT Image/Color/Font leak was found in the audited code. Continue to dispose resources the plugin owns and never dispose system/JFace-managed resources. SWT `Browser` remains a resilience/native-backend concern rather than a confirmed resource leak.

## Testability

The audited project had no automated test sources/modules. Core/parser/repository behavior becomes substantially easier to test once parser diagnostics stop depending on the activator and UI policy is extracted from the preference page.

## Tooling

High-value additions after meaningful tests exist:

- JUnit + Tycho runtime integration tests;
- formatter/Checkstyle policy;
- SpotBugs;
- JaCoCo;
- Dependabot/Renovate;
- dependency/license inventory that understands p2 inputs.

PMD, Error Prone, and OWASP Dependency-Check are optional layers and must not substitute for behavioral tests or create noisy duplicated checks.
