# R4 Target Spec — Existing Product UX Hardening

Traceability: Q11, Q13, U1-U9 plus accessibility/onboarding observations.

## Requirements

### Requirement: Preview reflects actual local resolved theme data
Preview SHALL render from the selected resolved theme and supported mappings rather than depending on a remote page keyed only by theme ID.

#### Scenario: Imported theme has no remote preview
- GIVEN a valid imported theme exists only locally
- WHEN it is selected
- THEN a representative local preview can still be shown

### Requirement: Preview failure does not break theme management
Preview infrastructure SHALL be optional/resilient and SHALL not make catalog selection unusable when browser/network/native backend support is unavailable.

#### Scenario: Browser backend unavailable
- WHEN the preference page opens
- THEN the theme list remains usable
- AND preview unavailability is handled separately

### Requirement: Apply, OK, Cancel, and Defaults have coherent semantics
The preference page SHALL use one documented staging/preview policy so Cancel does not unexpectedly preserve mutations caused by nominally reversible UI actions.

#### Scenario: User previews then cancels
- GIVEN a temporary preview changes managed appearance
- WHEN the user presses Cancel
- THEN the documented pre-dialog state is restored where safely known

### Requirement: Long-running file work does not block UI callbacks unnecessarily
Parsing, copying, and directory operations that can block SHALL execute outside the SWT UI thread where safe, with widget updates marshalled back correctly.

#### Scenario: Large import validation
- WHEN validation requires non-trivial file work
- THEN the UI remains responsive according to the chosen Job/cancellation model

### Requirement: Import conflicts and validation diagnostics are actionable
The UI SHALL present validation errors, warnings, and identity conflicts before commit with enough context for the user to act.

#### Scenario: Candidate conflicts with existing identity
- THEN the user receives the supported conflict choices
- AND the committed repository object is unambiguous

### Requirement: Preference UI is keyboard and assistive-technology usable
Interactive controls SHALL expose meaningful labels/descriptions, predictable focus order, keyboard access, durable operation status, and adaptable sizing.

#### Scenario: Keyboard-only theme import and apply
- GIVEN a user does not use a pointer
- WHEN navigating the preference page
- THEN supported import/select/apply operations are reachable and understandable

### Requirement: High-contrast choices are preserved
The product SHALL not blindly override an OS/Eclipse high-contrast accessibility mode.

#### Scenario: High-contrast environment
- WHEN a theme is selected
- THEN the product follows the documented preservation/opt-in policy rather than forcing incompatible colors

### Requirement: User-facing strings are externalizable
UI and plugin metadata strings intended for users SHALL be externalized through Eclipse-compatible localization mechanisms.

#### Scenario: Localized product build
- THEN translatable labels do not require editing Java source

### Requirement: Imported metadata links are validated
External links from imported theme metadata SHALL be escaped/validated and limited to explicitly supported schemes.

#### Scenario: Unsupported URI scheme
- WHEN the user activates the metadata link
- THEN the plugin refuses or safely handles it rather than blindly launching it

### Requirement: Supported integrations are inspectable
The product SHALL expose which editor/workbench integrations are available, skipped, unsupported, or version-incompatible.

#### Scenario: CDT is not installed
- THEN the user can distinguish unavailable CDT support from a broken theme

### Requirement: First-run state is unambiguous
The UI SHALL distinguish selected, previewed, and applied theme states and start with a valid stable identity or explicit unmanaged state.

#### Scenario: First preference-page open
- THEN details and selection do not disagree because a display name was stored where a stable ID is required
