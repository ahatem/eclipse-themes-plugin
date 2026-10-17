# Theme Application Delta

PM4/PM5 are maintainer-approved (2026-10-18). These requirements concern selected intent and application dispatch, not a truthful successful-applied-state journal or R3's aggregate success/rollback contract. Existing current-theme terminology does not expand this boundary.

## MODIFIED Requirements

### Requirement: Selected theme intent is persisted

The system SHALL persist selected intent as a logical theme reference in plugin workspace preferences. New logical references SHALL be distinguishable from legacy raw source-ID values. A legacy nonblank reference SHALL resolve only when exactly one loaded candidate has that source ID. Missing, blank or ambiguous intent SHALL remain unresolved without guessing a theme.

#### Scenario: Reopen preference page
- **GIVEN** a theme selection was persisted
- **WHEN** the preference page is opened again in the same workspace
- **THEN** the system can attempt to resolve that selection in the catalog
- **AND** a resolved logical reference identifies the same established theme independently of its label or source filename

#### Scenario: Unique legacy selection
- **GIVEN** only a legacy raw nonblank source-ID preference is present and exactly one loaded theme has that source ID
- **WHEN** selected intent is recovered
- **THEN** that theme is selected without treating recovery as proof of successful application
- **AND** opening the page does not apply colors or rewrite XML

#### Scenario: Ambiguous or unavailable legacy selection
- **GIVEN** old intent is blank, missing, matches multiple themes, or matches no loaded theme
- **WHEN** the preference page opens
- **THEN** intent remains unresolved without selecting a fallback for implicit application
- **AND** existing explicit intent and unrelated preferences, including their absence, remain unchanged
- **AND** opening or confirming without a user selection does not apply a fallback theme

#### Scenario: Fresh workspace has no applied selection claim
- **GIVEN** no explicit selected intent exists
- **WHEN** the preference page opens
- **THEN** the product does not interpret a default display-name string as a logical or successfully applied theme
- **AND** the user can choose a theme explicitly

## ADDED Requirements

### Requirement: Logical identity does not suppress explicit application

An explicit Apply or OK request with a selected theme SHALL dispatch its current loaded definition even when its logical identity equals remembered current or selected intent. Dispatch SHALL NOT require a revision identifier, content digest or history record. Catalog reload or page opening alone SHALL NOT trigger application.

#### Scenario: Changed current content has the same identity
- **GIVEN** the currently loaded selected definition retains the established logical identity but has changed tokens
- **WHEN** the user requests Apply or OK
- **THEN** the application path receives the changed definition
- **AND** the request is not skipped because logical identity is unchanged

#### Scenario: Unchanged content is explicitly reapplied
- **GIVEN** the selected definition has the same identity and contents as the previously selected definition
- **WHEN** the user requests Apply or OK
- **THEN** application is dispatched again without a content-comparison optimization

#### Scenario: Catalog reload is passive
- **GIVEN** stored or bundled content changed while retaining its established identity
- **WHEN** the catalog reloads without an explicit application request
- **THEN** the new definition becomes available without automatically mutating editor preferences
