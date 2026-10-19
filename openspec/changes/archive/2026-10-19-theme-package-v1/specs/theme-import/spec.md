## MODIFIED Requirements

### Requirement: Supported extension acceptance is locale-independent

Import and update SHALL accept a source only when its final extension is `.xml` or `.ectheme`, compared case-insensitively without the default locale. The extension SHALL select the format: `.xml` is a legacy theme and `.ectheme` is a theme package. Catalog discovery and theme-type classification used by admission SHALL use the same locale-independent case mapping. Unsupported extensions SHALL be refused before staging, binding, or catalog mutation. A newly imported record's repository-allocated locator SHALL carry the extension of its format.

#### Scenario: Uppercase extension
- **GIVEN** a valid theme filename ending in `.XML`
- **WHEN** catalog discovery or import classification runs
- **THEN** it is treated as a supported theme file
- **AND** the parser is selected consistently

#### Scenario: Package import
- **GIVEN** a valid package file `Nightfall.ectheme`
- **WHEN** the user imports it
- **THEN** a new independent installed record is admitted under a new local logical identity
- **AND** its managed locator is a repository-allocated name ending in `.ectheme` containing the exact parsed bytes

#### Scenario: Locale with different case mapping
- **GIVEN** the process default locale maps `I` differently from the root locale
- **WHEN** an `.xml` filename is classified
- **THEN** extension acceptance is unchanged

#### Scenario: Unsupported extension
- **GIVEN** a selected file whose extension is neither `.xml` nor `.ectheme`
- **WHEN** import is requested
- **THEN** it is refused with a diagnostic before any staged or managed file is created

## ADDED Requirements

### Requirement: Update preserves the record's format

An update SHALL be refused, before staging and with a diagnostic, when the replacement's format differs from the selected record's
format. The selected record's file, binding, identity, catalog entry and selected intent SHALL remain unchanged. Converting a legacy
record into a package is an import of the package followed by an explicit removal of the legacy record.

#### Scenario: Legacy record updated with a package
- **GIVEN** a selected installed legacy theme
- **WHEN** the user updates it from a `.ectheme` file
- **THEN** update is refused with a diagnostic naming the format mismatch
- **AND** no managed file, binding or catalog record changes

#### Scenario: Package updated with a newer package
- **GIVEN** a selected installed package
- **WHEN** the user updates it from a valid `.ectheme` file claiming a different package id
- **THEN** the record keeps its logical identity and locator and holds the new bytes
- **AND** the package id claim does not affect the target
