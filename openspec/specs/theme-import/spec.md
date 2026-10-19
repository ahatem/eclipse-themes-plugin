# Theme Import

## Purpose

Define user-visible import/removal behavior for workspace-installed legacy theme files.

## Requirements

### Requirement: A valid supported theme can be imported

The system SHALL allow a user to select a supported theme file and SHALL create exactly one new installed record. Import SHALL NOT overwrite, relocate, re-identify, or remove any existing installed or bundled record, regardless of display name, source ID, basename, or content equality.

#### Scenario: Successful import
- **GIVEN** a valid supported theme file and a managed directory whose staging area can be created
- **WHEN** the user imports it
- **THEN** the theme becomes selectable from the workspace catalog under a new persistent local logical identity
- **AND** its installed file contains the exact bytes that were parsed
- **AND** its managed filename is repository-allocated rather than derived from the source

#### Scenario: Import commits staged bytes before admission
- **GIVEN** a selected supported theme file
- **WHEN** import stages the selected bytes once and parses those staged bytes
- **THEN** a new unused managed locator is allocated and the staged file is moved there without replacement
- **AND** one new binding is persisted
- **AND** only then is the catalog record admitted

#### Scenario: Import survives repository reconstruction
- **GIVEN** a successful import
- **WHEN** the repository is reconstructed in the same workspace
- **THEN** the imported theme is rediscovered with the same logical identity
- **AND** its installed bytes are unchanged

#### Scenario: Existing legacy installations are not migrated
- **GIVEN** a bound installed theme at an existing basename-named locator
- **WHEN** a new import succeeds
- **THEN** that existing file, locator, binding, and logical identity remain unchanged

### Requirement: Import failure is reported as failure

The system SHALL NOT report an import as successful unless the staged file was parsed successfully, moved to a new locator, bound to a new identity, and admitted to the catalog. On any failure it SHALL report failure, SHALL NOT return an admitted theme, and SHALL best-effort remove only artifacts created by that attempt. It SHALL preserve all prior files, bindings, catalog records, and selected intent.

#### Scenario: Invalid input
- **GIVEN** a file that cannot be accepted as a supported theme
- **WHEN** import is attempted
- **THEN** the operation reports failure and no successful imported theme is returned
- **AND** no binding or catalog entry is created
- **AND** the source file remains unmodified

#### Scenario: Binding persistence fails after the staged move
- **GIVEN** staged bytes were parsed and moved to a newly allocated locator
- **WHEN** new binding persistence fails
- **THEN** import reports failure without catalog admission or an admitted generated identity
- **AND** the locator created by this attempt is removed when cleanup succeeds
- **AND** prior bindings, catalog records, and selected intent remain unchanged

#### Scenario: Allocation encounters an occupied locator
- **GIVEN** a repository-allocated candidate locator is unexpectedly occupied
- **WHEN** import prepares to commit
- **THEN** it chooses another unused locator or fails
- **AND** it never replaces the occupied file or reuses its identity

### Requirement: Imported themes can be removed from managed storage

The system SHALL provide an explicit removal operation for one selected installed theme. Removal SHALL target that record's established logical identity and managed locator, SHALL remove only that locator's binding, SHALL delete only that managed file with an observed result, and SHALL remove the catalog record only after both the binding and the locator's absence are confirmed. Bundled themes SHALL NOT be removal targets.

#### Scenario: Remove installed theme
- **GIVEN** a bound theme that came from managed installed-theme storage
- **WHEN** the user confirms its removal and both binding and file removal succeed
- **THEN** it no longer appears as an installed theme after reload
- **AND** unrelated installed themes remain available with unchanged identities
- **AND** unrelated files, bindings, catalog records, and selected intent remain unchanged

#### Scenario: Bundled theme cannot be removed
- **GIVEN** the selected theme is bundled
- **WHEN** removal is requested
- **THEN** removal is refused before any managed file or binding changes

#### Scenario: Managed file is already absent
- **GIVEN** an established installed record whose exact binding matches the selected identity and whose managed file is already absent
- **WHEN** the user explicitly confirms removal
- **THEN** the file-removal condition is treated as already satisfied
- **AND** the file is not recreated and no identity is regenerated
- **AND** the exact binding and the catalog record are removed
- **AND** removal reports success

#### Scenario: Managed path exists in an unexpected state
- **GIVEN** the selected installed record's managed path exists but is not the expected safe target type or state
- **WHEN** removal is attempted
- **THEN** removal fails closed rather than treating the path as absent
- **AND** the path, binding, and catalog record are left unchanged

### Requirement: New import never infers continuity

A new import SHALL always create an independent installed record. Equal display name, equal `ect-id`, equal source basename, identical bytes, or similarity to a bundled theme SHALL NOT cause replacement, silent deduplication, or reuse of another logical identity or locator.

#### Scenario: Same display name
- **GIVEN** an installed or bundled theme with display name `N`
- **WHEN** a supported source whose display name is also `N` is imported
- **THEN** a new independent record is added
- **AND** the existing record remains selectable and unchanged

#### Scenario: Same source ID
- **GIVEN** an existing record whose XML claims source ID `S`
- **WHEN** a different supported source also claims `S` is imported
- **THEN** source ID is treated only as metadata
- **AND** both records remain independently addressable

#### Scenario: Identical bytes
- **GIVEN** an installed theme with byte-identical content to the selected source
- **WHEN** import is performed
- **THEN** a new independent record is created rather than silently reusing the existing record

#### Scenario: Same basename as an existing managed file
- **GIVEN** an existing managed file whose basename equals the selected source basename
- **WHEN** import is performed
- **THEN** the new record receives a different repository-allocated locator
- **AND** the existing file and binding remain unchanged

#### Scenario: Source is itself an installed file
- **GIVEN** the selected source is an existing managed theme file
- **WHEN** import is performed
- **THEN** the source is read into staging and is never opened for writing
- **AND** the existing managed file, binding, and identity remain unchanged

### Requirement: Installed theme update is explicit and identity-preserving

The system SHALL provide an update operation only for an explicitly selected installed record, identified by that record's established logical identity and managed locator rather than incoming metadata, basename, or content. Update SHALL validate a staged copy, atomically replace the target's bytes, update the catalog only after replacement, and preserve the identity, locator, binding, and unrelated records. Bundled or unbound records SHALL NOT be update targets.

#### Scenario: Successful update
- **GIVEN** a selected installed theme with an established binding
- **WHEN** the user explicitly updates it from a valid supported file and the atomic replacement succeeds
- **THEN** the installed record retains its logical identity and locator
- **AND** its installed file contains exactly the validated staged bytes
- **AND** exactly one catalog record exists for that logical identity

#### Scenario: Replacement input is invalid
- **GIVEN** a selected installed theme
- **WHEN** update input cannot be staged or parsed
- **THEN** update reports failure
- **AND** the prior installed file, binding, catalog entry, and selected intent remain unchanged

#### Scenario: Atomic replacement is unsupported
- **GIVEN** the filesystem cannot atomically replace the target locator
- **WHEN** update is attempted
- **THEN** update reports failure without performing a non-atomic replacement
- **AND** the prior installed file remains unchanged

#### Scenario: Bundled theme is selected
- **GIVEN** the selected theme is bundled
- **WHEN** update is requested
- **THEN** update is refused before any managed write

#### Scenario: Binding is missing or inconsistent
- **GIVEN** a selected installed record whose locator has no established binding or has a different identity
- **WHEN** update is requested
- **THEN** update is refused diagnostically
- **AND** it neither generates a replacement identity nor modifies managed bytes

#### Scenario: Update target file is missing
- **GIVEN** a selected installed record with a consistent established binding whose managed file is absent
- **WHEN** update is requested
- **THEN** update reports failure and does not recreate the file under the established identity
- **AND** the binding, catalog record, and selected intent remain unchanged

#### Scenario: Incoming metadata matches another record
- **GIVEN** the replacement XML claims a name or source ID belonging to a different record
- **WHEN** update is performed on the explicitly selected record
- **THEN** only the selected logical identity is replaced
- **AND** the other record remains unchanged

### Requirement: Removal reports only committed removal

Removal SHALL report success only after the target binding is removed and the target managed file is observed absent. If file deletion fails after binding removal, the system SHALL attempt to restore the exact prior binding and SHALL report failure while preserving the file; if restoration also fails, it SHALL report both failures and SHALL NOT claim removal. Removal SHALL NOT delete, rewrite, or re-identify an unrelated file or binding.

#### Scenario: File deletion fails
- **GIVEN** the target binding was removed and the managed file remains
- **WHEN** file deletion fails
- **THEN** removal restores the prior binding when possible and reports failure
- **AND** the file is not reported as removed

#### Scenario: Binding removal fails
- **GIVEN** the target installed record exists
- **WHEN** its binding cannot be removed
- **THEN** removal reports failure
- **AND** the managed file, other bindings, catalog records, and selected intent remain unchanged

#### Scenario: Unrelated records are preserved
- **GIVEN** multiple installed records with distinct bindings
- **WHEN** one record is removed successfully
- **THEN** only that record's file and binding are removed
- **AND** all other record identities and files remain unchanged

### Requirement: Supported extension acceptance is locale-independent
Import and update SHALL accept final extensions `.xml` and `.ectheme`, matched case-insensitively with locale-independent mapping; the extension selects legacy XML or package format. Discovery and admission SHALL classify extensions identically. Unsupported extensions SHALL be refused before staging, binding or catalog mutation. New imports SHALL use repository-allocated locators with the format's extension.

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
