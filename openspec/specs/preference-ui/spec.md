# Preference UI

## Purpose

Define the user-visible theme management surface contributed under Eclipse Appearance preferences.

## Requirements

### Requirement: Theme management is discoverable in Eclipse preferences

The product SHALL contribute an Eclipse preference page under the Appearance preference area where users can browse and select themes.

#### Scenario: Open Appearance preferences
- **WHEN** a user opens Eclipse Appearance preferences with the plugin installed
- **THEN** the Eclipse Themes preference page is available

### Requirement: Users can browse theme metadata and filter the catalog

The preference UI SHALL present the available catalog and expose the current theme's relevant metadata, with filtering/search behavior that does not mutate theme content.

#### Scenario: Filter themes
- **GIVEN** multiple themes are available
- **WHEN** the user changes the catalog filter/search
- **THEN** the visible theme list changes without modifying stored theme definitions

### Requirement: Import/remove actions are available for user-installed themes

The preference UI SHALL expose new import, explicit update, and removal actions for plugin-managed installed themes. Import SHALL always create a new record. Update SHALL target only the selected installed record and SHALL replace that exact record's XML while preserving its identity. Removal SHALL target only an installed record and SHALL require confirmation that the managed file is permanently deleted. Bundled themes SHALL NOT be update or removal targets.

#### Scenario: Manage an imported theme
- **GIVEN** a user has a supported theme file
- **WHEN** the user imports it successfully
- **THEN** the imported theme can be selected and later removed from plugin-managed storage

#### Scenario: Update of an installed record without an established binding
- **GIVEN** an installed record whose locator has no established binding
- **WHEN** the user requests the update action
- **THEN** the repository refuses the update with a diagnostic
- **AND** no managed bytes are modified

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

The preference UI SHALL distinguish successful operations from failures and SHALL NOT deliberately show a success state for a known failed operation. For import, update, and removal, success SHALL be shown only after the repository operation reports committed success and the refreshed catalog contains the expected result. A failure message SHALL identify the operation that failed.

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
- **AND** it does not report removal failure
- **AND** it does not claim the removed theme is still installed

### Requirement: Reset messaging does not overpromise historical restoration

The preference UI SHALL describe reset/stop-management behavior according to what the system can safely restore from available ownership information.

#### Scenario: No legacy snapshot exists
- **GIVEN** an existing installation has no trustworthy record of the user's pre-theme values
- **WHEN** the reset action is presented or completed
- **THEN** the UI does not claim that all original user preferences were reconstructed
- **AND** it communicates any conservative recovery limitation that materially affects the user

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

### Requirement: The page operates on catalog records without managed paths

The preference UI SHALL present and manage `ThemeRecord`s: lifecycle actions branch on `record.origin()`, identity display and reselection use `record.logicalId()`, and visual details use `record.theme()` metadata. The UI SHALL NOT infer repository state from theme file presence or logical-ID prefixes, and SHALL NOT receive or construct absolute managed paths. Visible UX is unchanged.

#### Scenario: Installed marker follows record origin

- **GIVEN** bundled and installed records in the catalog
- **WHEN** the list renders and update/removal availability is decided
- **THEN** installed-only markers and actions follow `origin == INSTALLED`

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
