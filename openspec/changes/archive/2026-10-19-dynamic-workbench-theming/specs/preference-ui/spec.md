# Preference UI Delta

## ADDED Requirements

### Requirement: The preference page offers one compact Workbench section

The page SHALL add one compact Workbench section with a coordination checkbox, a status line, a truthful coverage line, a diagnostics-copy action and an action that opens Eclipse Appearance preferences through a public preference-dialog route. Its content SHALL follow the capability state and any conflict with an external Appearance choice.

#### Scenario: Verified
- **GIVEN** the capability is VERIFIED
- **THEN** the checkbox is enabled, unchecked by default, with a verified note

#### Scenario: Experimental
- **GIVEN** the capability is EXPERIMENTAL
- **THEN** the checkbox is labelled as experimental, states the combination, and checking it is the opt-in

#### Scenario: Unavailable
- **GIVEN** the capability is UNAVAILABLE
- **THEN** the checkbox is disabled, the reason is shown and the Appearance button is visible

#### Scenario: Apply result is truthful
- **GIVEN** the editor part succeeded and the workbench part did not
- **THEN** the status does not read as complete success and the workbench reason is shown with the Appearance route

#### Scenario: Conflict
- **GIVEN** coordination is enabled and the active Eclipse theme is not ours
- **THEN** the section says coordination is not the current effective state and offers the Appearance route

#### Scenario: Stop management
- **WHEN** the user chooses Stop Eclipse Themes management and it completes
- **THEN** workbench coordination is also relinquished under the ownership rules
