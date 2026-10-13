# preference-ownership Specification

## Purpose
Define ownership-safe mutation and restoration of Eclipse/plugin preferences managed by Eclipse Themes.

## Requirements

### Requirement: Theme restoration is limited to managed preference keys

When Eclipse Themes stops managing editor appearance, it SHALL modify or remove only preference keys that are explicitly part of its supported theme integration contract.

#### Scenario: Foreign preference node contains unrelated settings
- **GIVEN** a preference node used by a supported editor integration contains both theme-related keys and unrelated user/plugin settings
- **WHEN** the user invokes the operation that stops/restores Eclipse Themes management
- **THEN** unrelated settings remain byte-for-byte/value-equivalent unchanged
- **AND** the operation does not clear the entire preference node

### Requirement: Unknown historical state is handled conservatively

The system SHALL NOT invent a pre-theme preference value when no trustworthy original snapshot exists.

#### Scenario: Existing installation lacks an original snapshot
- **GIVEN** Eclipse Themes previously wrote a managed key
- **AND** no trustworthy pre-write value/absence snapshot exists
- **WHEN** the user requests restoration
- **THEN** the system follows an explicitly documented conservative recovery policy
- **AND** it does not clear unrelated settings to approximate a factory reset

### Requirement: Preference absence is semantically distinct from an explicit value

Any restoration mechanism that records original state SHALL preserve whether a managed key originally existed.

#### Scenario: Originally absent key
- **GIVEN** a managed preference key was absent before Eclipse Themes wrote it
- **WHEN** a trustworthy ownership snapshot is later used to restore that key
- **THEN** restoration returns the key to the absent state rather than writing a guessed/default value
