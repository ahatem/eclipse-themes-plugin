# Theme Catalog

## Purpose

Define how bundled and workspace-installed themes become available for selection.

## Requirements

### Requirement: Bundled themes are discoverable

The system SHALL load bundled theme resources into the theme catalog when the theme manager/repository is initialized or reloaded.

#### Scenario: Fresh workspace
- **GIVEN** no user-imported themes exist
- **WHEN** the catalog loads
- **THEN** bundled themes are available for selection

### Requirement: Installed themes are discoverable

The system SHALL load supported theme files that are direct children of its workspace-specific installed-theme storage and present valid themes together with bundled themes. Discovery SHALL accept only supported `.xml` extensions independent of the default locale and SHALL NOT descend into repository-owned subdirectories or admit staging files. Existing installed files SHALL remain discoverable at their current locators.

#### Scenario: Restart after import
- **GIVEN** a supported theme was successfully stored in the plugin state area
- **WHEN** the repository is reconstructed in the same workspace
- **THEN** the installed theme can be rediscovered
- **AND** it retains the binding established at admission

#### Scenario: Staging artifacts are ignored
- **GIVEN** an in-progress or abandoned file under the managed staging subdirectory
- **WHEN** the catalog loads
- **THEN** it is not parsed, bound, or presented as an installed theme
- **AND** discovery remains limited to supported direct children

#### Scenario: Removed theme stays removed
- **GIVEN** a successful removal deleted the managed file and its binding
- **WHEN** the catalog reloads or is reconstructed
- **THEN** the removed logical identity does not reappear
- **AND** unrelated records remain available

### Requirement: Catalog presentation is deterministic for a fixed repository state

For an unchanged set of valid theme sources, repeated reloads SHALL produce a deterministic selectable catalog order.

#### Scenario: Reload without file changes
- **GIVEN** the underlying bundled and installed theme files are unchanged
- **WHEN** the catalog is reloaded repeatedly
- **THEN** the user-visible theme ordering remains stable

### Requirement: Catalog lookup distinguishes identity from labels

The catalog SHALL address established themes by logical identity and retain independently stored records with equal display names. Presentation SHALL order equal labels by logical identity. A display label SHALL NOT be used as a unique persisted theme reference.

#### Scenario: Independent records share a name
- **GIVEN** two readable supported installed records with established distinct identities and the same display name
- **WHEN** the catalog loads or reloads without file changes
- **THEN** both records remain independently addressable
- **AND** their order and logical identities remain stable

### Requirement: Installed logical identities persist independently of XML claims

The system SHALL retain installed logical identity in plugin-owned workspace metadata without rewriting source XML or relocating existing files. A supported direct-child file that is already bound SHALL reuse that identity. Only a pre-existing supported file with no binding SHALL acquire one lazily on admission. Reload SHALL NOT derive identity from name, filename, path, source ID, or content.

#### Scenario: Existing legacy installation is first admitted
- **GIVEN** a supported stored XML file with no identity binding
- **WHEN** the catalog admits it and binding persistence succeeds
- **THEN** a stable local logical identity is retained for subsequent reload and reconstruction
- **AND** its XML bytes and file location remain unchanged

#### Scenario: Current definition changes at an established record
- **GIVEN** a retained binding for an established installed record
- **WHEN** that record is loaded with a changed label or current contents
- **THEN** the retained logical identity is reused
- **AND** this does not establish a policy for choosing an incoming import's replacement target

#### Scenario: Import-created record is already bound
- **GIVEN** a successful import returned an admitted logical identity
- **WHEN** the catalog immediately reloads
- **THEN** it reuses the persisted binding rather than generating a second identity

### Requirement: Identity binding failure does not fabricate continuity

If an identity binding cannot be read consistently or saved, the system SHALL report the failure, preserve existing XML, binding data and selected intent, and SHALL NOT advertise a newly generated transient ID as durable. Unaffected established themes SHALL remain available. An operation that requires an established binding SHALL refuse rather than lazily create one.

#### Scenario: First binding cannot be saved
- **GIVEN** an accepted stored theme has no binding and the metadata write fails
- **WHEN** catalog admission is attempted
- **THEN** that new logical identity is not admitted as stable
- **AND** its XML, existing bindings, unrelated files and selected intent remain unchanged
- **AND** an operation diagnostic identifies the identity-persistence failure

#### Scenario: Existing binding is unreadable
- **GIVEN** retained binding data cannot be interpreted consistently
- **WHEN** the catalog loads
- **THEN** affected identity admission fails diagnostically without silently overwriting the binding with a fresh ID
- **AND** unaffected bound or bundled themes remain available

#### Scenario: Update or removal finds no established binding
- **GIVEN** a selected installed record whose locator has no valid established binding
- **WHEN** update or removal is requested
- **THEN** the operation is refused diagnostically
- **AND** no new binding, identity, or managed write is produced

### Requirement: Managed installed locators are repository-owned and stable

A newly imported installed record SHALL receive a locator allocated by the repository that is unused by both the filesystem and identity metadata. The locator SHALL be an opaque direct-child filename with a supported extension and SHALL NOT be derived from source basename, display name, source ID, or content. An established record's locator SHALL NOT be renamed or reused for a different logical identity by import or update.

#### Scenario: New import allocates an opaque locator
- **GIVEN** a supported source named `My Theme.xml`
- **WHEN** import succeeds
- **THEN** the managed filename is allocated by the repository rather than copied from the source
- **AND** the display name remains available from theme metadata

#### Scenario: Established basename locator is preserved
- **GIVEN** an existing installed file at a basename-derived locator
- **WHEN** the catalog loads or that record is updated
- **THEN** its locator remains unchanged
- **AND** no migration rename occurs

#### Scenario: Removed locator is not silently reassigned
- **GIVEN** a logical identity and locator were successfully removed
- **WHEN** a later import succeeds
- **THEN** the new record receives its own allocation
- **AND** it does not inherit the removed logical identity

### Requirement: Catalog records change only after durable file and binding commits

A new import SHALL be admitted only after its staged file has been moved to the allocated locator and its binding persisted. An update SHALL replace an existing catalog record only after the target locator has been atomically replaced, and SHALL retain the established logical identity. Removal SHALL remove the catalog record only after the binding and file are gone. A failed operation SHALL NOT leave a catalog record claiming a managed file that was not committed.

#### Scenario: New import is admitted after commit
- **GIVEN** staged bytes were parsed and a new binding was persisted
- **WHEN** import returns success
- **THEN** the catalog contains exactly one new record for that logical identity
- **AND** the record's file is the committed staged bytes

#### Scenario: Update replaces only the target record
- **GIVEN** an established installed record and an unrelated installed record
- **WHEN** update succeeds
- **THEN** the target logical identity maps to the replacement content
- **AND** the unrelated record and its locator are unchanged

#### Scenario: Failed commit does not enter the catalog
- **GIVEN** a new import whose staged move or binding persistence failed
- **WHEN** the repository state is inspected or reloaded
- **THEN** no new admitted record exists
- **AND** any attempt-created unbound file is not attributed to the target logical identity
