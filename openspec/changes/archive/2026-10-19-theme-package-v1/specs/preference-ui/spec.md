## ADDED Requirements

### Requirement: Package provenance is shown truthfully

The details panel SHALL show, for a package, its version, license, copyright holder and source, presenting an absent value as not
stated. The source SHALL follow the same inert-unless-launchable rule as `website`. The UI SHALL NOT state or imply that the plugin's
own license covers a package. Legacy themes SHALL show their details as before.

#### Scenario: Package without license
- **GIVEN** a selected package with no provenance
- **WHEN** the details panel renders
- **THEN** license and copyright are shown as not stated

#### Scenario: Disallowed source scheme
- **GIVEN** a selected package whose source is a `file:` URI
- **WHEN** the details panel renders
- **THEN** the source is inert text and activating it does not reach the platform launcher

### Requirement: Import offers package files and never previews them remotely

The import and update file dialogs SHALL offer `.ectheme` alongside `.xml`. The remote preview SHALL NOT be requested for a theme
without a legacy source id, so no package id or package metadata is sent to the preview server.

#### Scenario: Package selected
- **GIVEN** a selected package
- **WHEN** the preview area renders
- **THEN** no preview request is made and the existing unavailable state is shown
