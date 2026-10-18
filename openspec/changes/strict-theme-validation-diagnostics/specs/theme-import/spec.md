# Theme Import Delta

## ADDED Requirements

### Requirement: Validation findings determine admission and are returned by the operation

Import and update SHALL admit a candidate only when validation of the staged bytes reports no error, and SHALL
leave every prior managed file, identity binding, logical identity, catalog record and selected intent unchanged
when it does not. Warnings SHALL NOT prevent the operation from committing. The operation result SHALL distinguish a
committed theme from a refused operation, and SHALL carry the finding that prevented admission, or the warnings when
admission succeeded.

#### Scenario: Rejected import names the reason

- **GIVEN** a supported source file whose staged bytes fail validation
- **WHEN** the user imports it
- **THEN** import reports failure and admits no theme
- **AND** the result carries a finding identifying the reason, including the element or attribute involved
- **AND** no managed file, identity binding, catalog record or selected intent is created or changed

#### Scenario: Rejected update preserves the previous valid record

- **GIVEN** an installed record that is selected for update
- **WHEN** the replacement source's staged bytes fail validation
- **THEN** update reports failure and admits no theme
- **AND** the prior installed file, its identity binding, its logical identity, its catalog record and the selected
  intent remain unchanged
- **AND** the result carries a finding identifying the reason

#### Scenario: Warning-only candidate is committed

- **GIVEN** a supported source file whose staged bytes produce warnings but no errors
- **WHEN** the user imports it
- **THEN** the theme is admitted and committed exactly as an unremarkable successful import
- **AND** the warnings are available on the result

#### Scenario: The caller does not need the log to know why

- **GIVEN** a refused operation
- **WHEN** the caller inspects the returned result
- **THEN** it can determine both that the operation was refused and why
- **AND** it does not need to read plugin log output to do so

#### Scenario: Both operations use the same validity rule

- **GIVEN** one supported source file
- **WHEN** it is used for import and, separately, as an update replacement for a selected record
- **THEN** both operations reach the same admission verdict and carry the same findings for the same bytes