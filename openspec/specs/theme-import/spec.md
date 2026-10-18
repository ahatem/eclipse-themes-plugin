# Theme Import

## Purpose

Define user-visible import/removal behavior for workspace-installed legacy theme files.

## Requirements

### Requirement: A valid supported theme can be imported

The system SHALL allow a user to select a supported theme file, parse it, store an installed copy in workspace-specific plugin state, and make the resulting theme available in the catalog.

#### Scenario: Successful import
- **GIVEN** a valid supported theme file
- **WHEN** the user imports it
- **THEN** the theme becomes selectable from the workspace catalog

### Requirement: Import failure is reported as failure

The system SHALL NOT report an import as successful when it cannot parse or persist the selected theme.

#### Scenario: Invalid input
- **GIVEN** a file that cannot be accepted as a supported theme
- **WHEN** import is attempted
- **THEN** the operation reports failure and no successful imported theme is returned

### Requirement: Imported themes can be removed from managed storage

The system SHALL provide a way to remove a user-installed theme from the plugin-managed workspace catalog.

#### Scenario: Remove installed theme
- **GIVEN** a theme that came from managed installed-theme storage
- **WHEN** the user removes it successfully
- **THEN** it no longer appears as an installed theme after reload

> Atomic replacement, conflict identity, and conservative failure semantics are strengthened by planned R2 changes.

### Requirement: Occupied or reserved managed locators are refused before mutation

Import SHALL refuse if its managed destination already exists or an identity binding owns/reserves that locator, including a binding whose file is missing. This check SHALL precede XML writes and binding, catalog or selection changes. Refusal SHALL preserve existing bytes, bindings, logical identity and catalog/selected intent, diagnose the conflict, and neither reuse the old ID nor allocate a replacement at that locator.

#### Scenario: Existing destination has an established binding
- **GIVEN** a managed XML file and its established identity binding
- **WHEN** an incoming source maps to the same managed filename
- **THEN** import is refused before mutation
- **AND** the existing XML bytes, binding bytes, logical identity, catalog records and selected intent remain unchanged
- **AND** no incoming theme is returned with the old or a replacement identity

#### Scenario: Existing destination has no binding yet
- **GIVEN** a managed destination exists without a binding
- **WHEN** an incoming source maps to that locator
- **THEN** import is refused without overwriting the file or creating a binding

#### Scenario: Managed file itself is selected for import
- **GIVEN** the incoming source is the existing managed destination
- **WHEN** import is requested
- **THEN** import is refused without opening that destination for writing or truncating its bytes

#### Scenario: Binding reserves a missing file
- **GIVEN** an existing binding reserves a locator whose managed file is missing
- **WHEN** import targets that locator
- **THEN** import diagnoses inconsistent existing managed state and is refused
- **AND** the binding, old logical identity, catalog and selected intent remain unchanged
- **AND** no destination file or replacement identity is created

#### Scenario: Locator is genuinely unused
- **GIVEN** neither a managed file nor an identity binding reserves the destination locator
- **WHEN** supported input is parsed, the destination is exclusively created/copied and the new binding persists
- **THEN** import returns an admitted theme with a new persistent local logical identity
- **AND** matching source metadata or display names alone do not link it to another theme

### Requirement: Failed new-locator admission leaves no new binding or managed destination

For an unused locator, import SHALL exclusively create/copy the managed destination before persisting a new binding and SHALL admit the catalog entry only after both succeed. On failure it SHALL best-effort remove only the destination created by that attempt and SHALL NOT change prior bindings, catalog records or selected intent. Cleanup SHALL never delete or mutate a pre-existing destination.

#### Scenario: Exclusive copy fails
- **GIVEN** an unused locator with no prior binding or catalog entry
- **WHEN** exclusive destination creation or copying fails
- **THEN** import reports failure without creating a binding or catalog entry
- **AND** no destination created by the attempt remains when cleanup succeeds
- **AND** a retry at that locator is not refused because the failed attempt manufactured a reservation

#### Scenario: Binding persistence fails after copy
- **GIVEN** the destination was exclusively created by the current attempt and prior bindings exist
- **WHEN** new binding persistence fails
- **THEN** import reports failure without catalog admission or an admitted generated identity
- **AND** the destination created by this attempt is removed when cleanup succeeds
- **AND** prior bindings, catalog records and selected intent remain unchanged
