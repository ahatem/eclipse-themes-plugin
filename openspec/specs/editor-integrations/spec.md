# Editor Integrations

## Purpose

Define the currently advertised adapter-based integration surface.

## Requirements

### Requirement: Base text editor integration

The product SHALL support applying relevant theme colors to the Eclipse text editor preference surface when that integration is available.

#### Scenario: Base text editor is available
- **GIVEN** the Eclipse text editor integration is installed and available
- **WHEN** a theme is applied
- **THEN** the adapter can map relevant canonical tokens to that editor's supported appearance preferences

### Requirement: Optional editor integrations

The product SHALL discover and apply mappings for supported optional integrations when their target plugins are present, including the currently contributed JDT, CDT, PDE, Ant, and debug-console adapters.

#### Scenario: Optional target plugin not installed
- **GIVEN** an adapter targets a plugin that is not installed
- **WHEN** available adapters are determined
- **THEN** the missing integration does not make the whole theme catalog unusable

### Requirement: Integrations do not intentionally modify unrelated settings

An editor integration's theme operation SHALL be limited to appearance/highlighting preferences that belong to that integration's declared theme responsibility.

#### Scenario: Theme application beside unrelated editor settings
- **GIVEN** the target preference node contains unrelated user settings
- **WHEN** a theme is applied or theme management is stopped
- **THEN** unrelated settings remain unchanged

> Exact mapping corrections and ownership-safe restoration are delivered by R1/R3 changes.
