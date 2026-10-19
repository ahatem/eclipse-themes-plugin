# R8 Target Spec — Coordinated Theme MVP

Traceability: Part 5B application/switching/coexistence/persistence sections and R3/R7 prerequisites.

## Requirements

### Requirement: Users can opt into appearance components independently
The product SHALL allow editor colors and workbench theming to be enabled/disabled independently, with fonts/icons separate unless a later supported capability says otherwise.

#### Scenario: Editor-only mode
- WHEN workbench theming is disabled
- THEN selecting a coordinated package does not take ownership of workbench theme state

### Requirement: MVP workbench themes use proven registered mechanisms
The first MVP SHALL prefer built-in or p2-contributed workbench CSS themes whose lifecycle was proven by R7.

#### Scenario: Unsupported arbitrary CSS import
- THEN the MVP refuses or defers that capability rather than pretending dynamic lifecycle is solved

### Requirement: Live switching occurs only where verified
The product SHALL switch editor/workbench appearance live on surfaces proven safe and SHALL surface restart/window-recreation requirements elsewhere.

#### Scenario: Operation requires restart
- THEN the user is told before/after commit according to the documented flow

### Requirement: Ownership/recovery is shared across components
Workbench and editor changes SHALL participate in the same ownership/result/recovery model established in R3.

#### Scenario: Coordinated application partially fails
- THEN the aggregate result and compensating recovery cover both participating component types

### Requirement: External changes are preserved
The product SHALL observe/report externally changed owned state without continuously rewriting it back in a listener loop.

#### Scenario: User changes Eclipse workbench theme externally
- THEN Eclipse Themes records/yields/conflicts according to policy rather than fighting the user

### Requirement: Coverage is inspectable
The UI SHALL expose which workbench/editor surfaces are supported, skipped, or require restart on the current platform/version.

#### Scenario: Unsupported native surface
- THEN the product does not imply that the active coordinated theme controls it

## Delivery status (R8 closed)

Delivered by the OpenSpec changes `dynamic-workbench-theming` (workbench coordination) and `plugin-color-coordination` (colors of other
plug-ins). Evidence: the `evidence/` folders of those changes, archived with them. Verified on Windows with Eclipse 4.40 and 4.41.

| Requirement | Status |
|---|---|
| Independent opt-in | Delivered. Workbench coordination is a separate checkbox; editor-only mode writes no workbench state and no plug-in colors |
| Registered mechanisms only | Delivered. Four static CSS themes contributed by the plugin; no imported CSS and no runtime registration |
| Live switching only where verified | Delivered. VERIFIED / EXPERIMENTAL / UNAVAILABLE by exact os, ws and theme-engine version; restart is never required on the verified matrix |
| Shared ownership and recovery | Delivered for the workbench theme (prior-theme record, relinquish) and for plug-in colors (value-exact ownership record). The R3 recovery journal is not reused; this is the R9/follow-up item |
| External changes preserved | Delivered. Choosing another theme on the Appearance page releases owned plug-in colors once and never reasserts; a changed key is never overwritten or removed |
| Coverage inspectable | Delivered as documentation (`docs/WORKBENCH_THEMING_COMPATIBILITY.md`, `key-classification.md`) and as counts and key names in the page and its copyable diagnostics. A per-surface UI is not built |

Not delivered and not claimed: fonts and icons, native menus, title bars, Browser content, scroll bars, theme-supplied CSS, Linux and
macOS verification, syntax and editor colors beyond the existing editor integrations.

