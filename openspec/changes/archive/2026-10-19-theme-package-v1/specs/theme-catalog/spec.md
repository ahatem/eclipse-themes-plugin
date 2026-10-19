## MODIFIED Requirements

### Requirement: Installed themes are discoverable

The system SHALL load supported theme files that are direct children of its workspace-specific installed-theme storage and present valid themes together with bundled themes. Discovery SHALL accept only the supported `.xml` and `.ectheme` extensions independent of the default locale, SHALL parse each with the format its extension selects, and SHALL NOT descend into repository-owned subdirectories or admit staging files. Existing installed files SHALL remain discoverable at their current locators.

#### Scenario: Restart after import
- **GIVEN** a supported theme was successfully stored in the plugin state area
- **WHEN** the repository is reconstructed in the same workspace
- **THEN** the installed theme can be rediscovered
- **AND** it retains the binding established at admission

#### Scenario: Restart after package import
- **GIVEN** a package was successfully imported to a `.ectheme` locator
- **WHEN** the repository is reconstructed in the same workspace
- **THEN** the package is rediscovered with the same logical identity and its package metadata

#### Scenario: Staging artifacts are ignored
- **GIVEN** an in-progress or abandoned file under the managed staging subdirectory
- **WHEN** the catalog loads
- **THEN** it is not parsed, bound, or presented as an installed theme
- **AND** discovery remains limited to supported direct children

#### Scenario: Removed theme stays removed
- **GIVEN** a successful removal deleted the managed file and its binding
- **WHEN** the catalog reloads or is reconstructed
- **THEN** the removed logical identity does not reappear
- **AND** unrelated records remain available
