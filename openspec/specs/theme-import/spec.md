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
