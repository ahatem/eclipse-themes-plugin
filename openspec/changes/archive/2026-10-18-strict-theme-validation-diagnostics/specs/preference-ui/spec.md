# Preference UI Delta

## ADDED Requirements

### Requirement: A refused import or update states an actionable reason

When an import or update is refused, the preference UI SHALL present the actionable reason rather than a fixed
failure sentence. The message SHALL identify which operation failed and SHALL include the validation finding that
caused the refusal, including the element or attribute involved where one applies. The UI SHALL NOT present a
success state for a refused operation, and SHALL NOT add a diagnostics panel, a problems view, a warning
acknowledgement step, or a conflict wizard.

#### Scenario: Import refused for a structural reason

- **GIVEN** the user selects a file whose document root is not a supported theme document root
- **WHEN** the user imports it
- **THEN** an error dialog identifies the import as failed
- **AND** states the actual root element that was found
- **AND** no theme is selected or claimed as imported

#### Scenario: Update refused for a metadata reason

- **GIVEN** a selected installed record and a replacement file whose document lacks a usable display name
- **WHEN** the user updates the record
- **THEN** an error dialog identifies the update as failed
- **AND** states the reason and names the `name` attribute
- **AND** the installed record remains selected and unchanged

#### Scenario: Successful import remains a success

- **GIVEN** a supported file that imports successfully
- **WHEN** the import completes
- **THEN** the existing success message is shown and no failure reason is invented

### Requirement: Imported website metadata is presented inertly and launched only when allowed

The preference UI SHALL render a theme's `website` value as escaped display text and SHALL emit link markup only
when the value is reported as launchable. A value that is not launchable SHALL NOT be activatable, and activating it
SHALL NOT reach the platform's program launcher. The UI SHALL NOT introduce a browser, URL-fetching or
content-security framework, and a refused launch SHALL be an inert control rather than a silent external action.

#### Scenario: Allowed website becomes a link

- **GIVEN** a selected theme whose `website` is reported as launchable
- **WHEN** the details panel renders and the user activates the website entry
- **THEN** the value is presented as a link
- **AND** the activation is handed to the platform launcher

#### Scenario: Disallowed website is shown but not activatable

- **GIVEN** a selected theme whose `website` is not reported as launchable
- **WHEN** the details panel renders
- **THEN** the value is presented as inert text and is not presented as a link
- **AND** activating the entry does not reach the platform launcher

#### Scenario: No website

- **GIVEN** a selected theme with no `website`
- **WHEN** the details panel renders
- **THEN** the existing placeholder is shown
- **AND** the placeholder is not activatable

#### Scenario: Value containing markup characters

- **GIVEN** a selected theme whose `website` contains characters that carry meaning in display markup
- **WHEN** the details panel renders
- **THEN** the value is displayed as literal text rather than as markup