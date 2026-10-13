# Theme Application

## Purpose

Define how a selected theme is applied to supported Eclipse editor integrations.

## Requirements

### Requirement: Applying a theme updates supported integrations

The system SHALL translate canonical theme tokens into the preference values expected by every currently available supported adapter.

#### Scenario: Apply with optional editors absent
- **GIVEN** only a subset of supported editor plugins is installed
- **WHEN** a theme is applied
- **THEN** available integrations are updated and unavailable optional integrations do not prevent discovery of the others

### Requirement: Selected theme intent is persisted

The system SHALL persist the selected theme identifier in the plugin's workspace-scoped preferences so the preference UI can recover the user's selection intent.

#### Scenario: Reopen preference page
- **GIVEN** a theme selection was persisted
- **WHEN** the preference page is opened again in the same workspace
- **THEN** the system can attempt to resolve that selection in the catalog

### Requirement: Application failures are observable

A failure while applying a theme to supported integrations SHALL be observable by the caller and SHALL NOT be indistinguishable from complete success.

#### Scenario: Adapter persistence failure
- **GIVEN** an adapter cannot persist its required preference changes
- **WHEN** theme application is attempted
- **THEN** the caller can identify that application did not completely succeed

> The existing implementation is known to violate the final requirement in some paths; R3 makes the result/transaction contract explicit.
