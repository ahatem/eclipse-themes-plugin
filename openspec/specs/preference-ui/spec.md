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

The preference UI SHALL expose the supported import flow and allow removal of themes managed by the plugin.

#### Scenario: Manage an imported theme
- **GIVEN** a user has a supported theme file
- **WHEN** the user imports it successfully
- **THEN** the imported theme can be selected and later removed from plugin-managed storage

### Requirement: User-facing operation status reflects actual outcome

The preference UI SHALL distinguish successful operations from failures and SHALL NOT deliberately show a success state for a known failed operation.

#### Scenario: Apply fails
- **GIVEN** the selected theme cannot be fully applied
- **WHEN** the user confirms application
- **THEN** the UI presents a failure/diagnostic state rather than an unconditional success indication

> Preview fidelity and coherent Apply/OK/Cancel/default semantics are planned R4 changes.
