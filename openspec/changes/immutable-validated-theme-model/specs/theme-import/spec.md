# Theme Import Delta

## ADDED Requirements

### Requirement: Import and update admit records with unchanged ordering

Import SHALL follow the existing stage, validate, move, bind, record, admit order. Update SHALL resolve the managed target from the selected record's locator plus binding, stage, validate, atomically replace, then replace the catalog entry. Removal SHALL use record identity, origin, and locator with existing guarantees.

#### Scenario: Import order is unchanged

- **GIVEN** a valid supported source file
- **WHEN** import commits
- **THEN** staging, validation, move, binding, record creation, and admission occur in the existing order

#### Scenario: Update preserves record coordinates

- **GIVEN** a selected installed record and valid replacement bytes
- **WHEN** update succeeds
- **THEN** the catalog holds a new record with the same logical identity, origin, and locator and the replacement theme

### Requirement: Operation outcomes carry the admitted record

A successful import or update SHALL return the committed `ThemeRecord`, not the raw theme. Diagnostics semantics are unchanged. No second result abstraction is introduced.

#### Scenario: Successful import returns its record

- **GIVEN** a valid supported source file
- **WHEN** import commits
- **THEN** the outcome carries the admitted record with its new logical identity, installed origin, and managed locator
