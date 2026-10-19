## ADDED Requirements

### Requirement: A validated theme may carry package metadata and authored workbench colors

A theme parsed from a package SHALL additionally carry immutable package metadata (package id, version, license, copyright, source)
and an immutable, possibly empty set of authored workbench role colors restricted to the eight palette roles. A theme parsed from
legacy XML SHALL carry no package metadata and no authored workbench colors, and its tokens, metadata and classification SHALL be
identical to the current interpretation. Neither addition participates in theme or catalog equality.

#### Scenario: Legacy theme is unchanged
- **GIVEN** any bundled legacy theme
- **WHEN** it is parsed after this change
- **THEN** it has no package metadata and no authored workbench colors
- **AND** every resolved token equals the value produced before this change

#### Scenario: Package theme exposes no legacy source id
- **GIVEN** a parsed package
- **WHEN** its legacy source id is read
- **THEN** it is absent, and the package id is available only as package metadata
