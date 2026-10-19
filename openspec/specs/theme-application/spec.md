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

### Requirement: Application failures are observable

A failure while applying a theme to supported integrations SHALL be observable by the caller and SHALL NOT be indistinguishable from complete success.

#### Scenario: Adapter persistence failure
- **GIVEN** an adapter cannot persist its required preference changes
- **WHEN** theme application is attempted
- **THEN** the caller can identify that application did not completely succeed

> The existing implementation is known to violate the final requirement in some paths; R3 makes the result/transaction contract explicit.

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

### Requirement: Selection resolves to catalog records; application receives themes

Selected intent SHALL remain the persisted logical identity (`SELECTED_THEME_LOGICAL_ID` with legacy source-ID fallback unchanged) and SHALL resolve to a `ThemeRecord`. Application SHALL dispatch `record.theme()` through the unchanged `EclipseThemeAdapter.apply(Theme, prefs)` path. No persistence keys migrate; successful-applied-state remains R3.

#### Scenario: Apply dispatches the record's theme

- **GIVEN** a selected catalog record
- **WHEN** Apply or OK is requested
- **THEN** adapters receive the record's immutable theme with unchanged mappings

### Requirement: Application reports an aggregate editor and workbench outcome

Applying a theme SHALL return one outcome with a distinguishable editor part (naming adapters that failed to persist) and workbench part (NOT_REQUESTED, APPLIED, RELINQUISHED, YIELDED, UNAVAILABLE or FAILED). The outcome SHALL be fully successful only when every requested part succeeded. A workbench failure SHALL NOT invalidate a successful editor application.

#### Scenario: Editor and workbench succeed
- **GIVEN** coordination is enabled and available
- **THEN** both parts succeed and the outcome is fully successful

#### Scenario: Editor succeeds, workbench unavailable or failed
- **THEN** the outcome states a partial result with the workbench reason and the editor part remains successful

#### Scenario: Editor adapter fails
- **GIVEN** an adapter cannot persist its preferences
- **THEN** the editor part fails and names the adapter
- **AND** the outcome is not fully successful

#### Scenario: No false persisted success
- **GIVEN** the workbench switch or its postcondition fails
- **THEN** no ownership record is persisted for it
