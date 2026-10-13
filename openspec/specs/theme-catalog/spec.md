# Theme Catalog

## Purpose

Define how bundled and workspace-installed themes become available for selection.

## Requirements

### Requirement: Bundled themes are discoverable

The system SHALL load bundled theme resources into the theme catalog when the theme manager/repository is initialized or reloaded.

#### Scenario: Fresh workspace
- **GIVEN** no user-imported themes exist
- **WHEN** the catalog loads
- **THEN** bundled themes are available for selection

### Requirement: Installed themes are discoverable

The system SHALL load supported theme files from its workspace-specific installed-theme storage and present valid themes together with bundled themes.

#### Scenario: Restart after import
- **GIVEN** a supported theme was successfully stored in the plugin state area
- **WHEN** the repository is reconstructed in the same workspace
- **THEN** the installed theme can be rediscovered

### Requirement: Catalog presentation is deterministic for a fixed repository state

For an unchanged set of valid theme sources, repeated reloads SHALL produce a deterministic selectable catalog order.

#### Scenario: Reload without file changes
- **GIVEN** the underlying bundled and installed theme files are unchanged
- **WHEN** the catalog is reloaded repeatedly
- **THEN** the user-visible theme ordering remains stable
