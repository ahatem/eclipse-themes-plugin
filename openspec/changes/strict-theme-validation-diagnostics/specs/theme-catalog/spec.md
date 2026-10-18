# Theme Catalog Delta

## ADDED Requirements

### Requirement: Catalog reload admits only valid theme documents

The catalog SHALL admit a bundled or workspace-installed theme only when its document passes the same validation
rules applied to import and update. Structural and metadata validity SHALL NOT vary by source. A rejected theme
SHALL NOT create a catalog record, SHALL NOT have its file, identity binding or logical identity removed or
altered, and SHALL produce a diagnostic identifying the source and the reason.

#### Scenario: Blank-name installed record is no longer admitted

- **GIVEN** a workspace-installed theme file whose document has no usable display name
- **WHEN** the catalog reloads
- **THEN** that record is not admitted
- **AND** the managed file, its identity binding and its logical identity are unchanged
- **AND** a diagnostic identifies the file and the reason

#### Scenario: Same content is treated the same way regardless of source

- **GIVEN** a document that fails validation
- **WHEN** it is supplied as a bundled resource and as a workspace-installed file
- **THEN** it is rejected in both cases for the same reason

#### Scenario: Rejected bundled resource is reported as a product defect

- **GIVEN** a bundled theme resource that fails validation
- **WHEN** the catalog loads
- **THEN** the resource is not admitted
- **AND** the diagnostic identifies the bundled resource rather than treating it as user input

#### Scenario: Valid installed record is unaffected

- **GIVEN** a workspace-installed theme file that passes validation
- **WHEN** the catalog reloads
- **THEN** the record is admitted exactly as before

#### Scenario: Rejected theme does not disturb other records

- **GIVEN** a catalog containing several installed records and one unvalidatable file
- **WHEN** the catalog reloads
- **THEN** every other record remains admitted with unchanged identity
- **AND** catalog ordering remains deterministic for the resulting state