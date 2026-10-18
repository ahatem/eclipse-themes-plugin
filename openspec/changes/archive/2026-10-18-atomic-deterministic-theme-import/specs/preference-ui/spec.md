# Preference UI Delta

## MODIFIED Requirements

### Requirement: Import/remove actions are available for user-installed themes

The preference UI SHALL expose new import, explicit update, and removal actions for plugin-managed installed themes. Import SHALL always create a new record. Update SHALL target only the selected installed record and SHALL replace that exact record's XML while preserving its identity; a record without an established binding SHALL be refused by the repository with a diagnostic. Removal SHALL target only an installed record and SHALL require confirmation that the managed file is permanently deleted. Bundled themes SHALL NOT be update or removal targets.

#### Scenario: Manage an imported theme
- **GIVEN** a user has a supported theme file
- **WHEN** the user imports it successfully
- **THEN** the imported theme can be selected and later removed from plugin-managed storage

#### Scenario: Update a selected installed theme
- **GIVEN** an installed theme is selected
- **WHEN** the user chooses the update action and confirms a valid replacement file
- **THEN** that installed theme keeps its identity and receives the replacement content
- **AND** the catalog refreshes with the same logical record reselected

#### Scenario: Bundled selection
- **GIVEN** a bundled theme is selected
- **WHEN** the user views the management actions
- **THEN** update and removal are unavailable
- **AND** no managed write is attempted

#### Scenario: Removal confirmation
- **GIVEN** an installed theme is selected
- **WHEN** the user initiates removal
- **THEN** the confirmation states that the installed file will be permanently deleted
- **AND** cancellation performs no mutation

### Requirement: User-facing operation status reflects actual outcome

The preference UI SHALL distinguish successful operations from failures and SHALL NOT deliberately show a success state for a known failed operation. For import, update, and removal, success SHALL be shown only after the repository operation reports committed success and the refreshed catalog contains the expected result. A failure message SHALL identify the operation that failed. If removal succeeds but the subsequent theme reset fails, the UI SHALL report that removal succeeded and reset failed rather than reporting removal failure.

#### Scenario: Apply fails
- **GIVEN** the selected theme cannot be fully applied
- **WHEN** the user confirms application
- **THEN** the UI presents a failure/diagnostic state rather than an unconditional success indication

#### Scenario: Import succeeds but record is absent after refresh
- **GIVEN** the repository returned an imported logical identity
- **WHEN** reload does not contain that identity
- **THEN** the UI reports failure rather than an import success
- **AND** it does not select an unrelated record

#### Scenario: Update fails
- **GIVEN** a selected installed theme
- **WHEN** update reports failure because staging, parsing, binding preconditions, or atomic replacement failed
- **THEN** the UI reports update failure
- **AND** the previous selection and installed record remain represented

#### Scenario: Removal file deletion fails
- **GIVEN** removal cannot delete the managed file and restores its binding
- **WHEN** the repository reports failure
- **THEN** the UI reports removal failure
- **AND** the record remains selectable

#### Scenario: Removed theme cannot be reset
- **GIVEN** removal committed and the removed theme was the current theme
- **WHEN** the reset attempt fails
- **THEN** the UI reports that the theme was removed and reset failed
- **AND** it does not claim the removed theme is still installed

## ADDED Requirements

### Requirement: Management actions target only the explicitly selected record

The preference UI SHALL invoke update and removal with the currently selected installed record's established object and logical identity. It SHALL NOT infer an update or removal target from the replacement file's name, source ID, basename, or content, and SHALL refuse the action when no installed record is selected.

#### Scenario: No installed selection
- **GIVEN** no theme or only a bundled theme is selected
- **WHEN** update or removal is requested through the UI
- **THEN** no repository mutation is invoked

#### Scenario: Replacement metadata names another theme
- **GIVEN** installed theme A is selected
- **WHEN** the user selects a replacement whose metadata names theme B
- **THEN** the UI updates only A
- **AND** it does not prompt to replace B

### Requirement: Successful operations reselect by logical identity

After a successful import or update, the preference UI SHALL reload the catalog and select the resulting record by its returned or preserved logical identity, not by display label or list position. After a successful removal, it SHALL reload and select a remaining record or no record without selecting a bundled or unrelated record as if it were the removed one.

#### Scenario: Duplicate display names after import
- **GIVEN** two installed records share a display name
- **WHEN** import succeeds
- **THEN** the new record is selected by its logical identity
- **AND** the other same-named record is not mistaken for it

#### Scenario: Update preserves selection
- **GIVEN** installed theme A is selected and update succeeds
- **WHEN** the catalog reloads
- **THEN** A remains selected by its preserved logical identity

#### Scenario: Removal refreshes selection
- **GIVEN** the selected installed theme was removed successfully
- **WHEN** the catalog reloads
- **THEN** the UI does not select the removed identity
- **AND** it presents only records that still exist
