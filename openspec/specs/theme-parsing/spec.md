# Theme Parsing

## Purpose

Define the supported legacy XML parsing contract that turns theme documents into theme definitions.

## Requirements

### Requirement: Supported XML themes can be parsed

The system SHALL parse supported Eclipse Color Theme-style XML documents into a theme definition containing metadata and canonical tokens.

#### Scenario: Bundled valid theme
- **GIVEN** a valid bundled legacy XML theme
- **WHEN** it is loaded
- **THEN** parsing returns a usable theme definition rather than requiring editor-specific parsing

### Requirement: Legacy aliases resolve through a canonical mapping

Legacy property names SHALL be resolved through one mapping layer before editor adapters consume tokens.

#### Scenario: Legacy editor-specific name
- **GIVEN** a supported legacy property name
- **WHEN** the XML parser encounters it
- **THEN** the resulting theme contains the corresponding canonical semantic token

### Requirement: Parsing diagnostics are distinguishable from successful values

Consumers SHALL be able to distinguish a successfully parsed theme from a parsing failure.

#### Scenario: Unreadable or malformed document
- **GIVEN** a document that cannot be parsed as a supported theme
- **WHEN** parsing is attempted
- **THEN** the caller receives a failure rather than a fabricated successful theme

> Security hardening, structural validation, and detailed diagnostics are expanded by planned R1/R2 changes.
