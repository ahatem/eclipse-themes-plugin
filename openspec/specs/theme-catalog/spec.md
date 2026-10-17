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

The system SHALL load supported theme files from its workspace-specific installed-theme storage and present valid themes together with bundled themes.

#### Scenario: Restart after import
- **GIVEN** a supported theme was successfully stored in the plugin state area
- **WHEN** the repository is reconstructed in the same workspace
- **THEN** the installed theme can be rediscovered

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

The system SHALL retain installed logical identity in plugin-owned workspace metadata without rewriting source XML or relocating existing files. Existing accepted installed records SHALL acquire bindings lazily on admission. Reload SHALL reuse a retained binding rather than deriving identity from name, filename, path, source ID or content.

#### Scenario: Existing legacy installation is first admitted
- **GIVEN** a supported stored XML file with no identity binding
- **WHEN** the new catalog admits it and binding persistence succeeds
- **THEN** a stable local logical identity is retained for subsequent reload and reconstruction
- **AND** its XML bytes and file location remain unchanged

#### Scenario: Current definition changes at an established record
- **GIVEN** a retained binding for an established installed record
- **WHEN** that record is loaded with a changed label or current contents
- **THEN** the retained logical identity is reused
- **AND** this does not establish a policy for choosing an incoming import's replacement target

### Requirement: Identity binding failure does not fabricate continuity

If an identity binding cannot be read consistently or saved, the system SHALL report the failure, preserve existing XML, binding data and selected intent, and SHALL NOT advertise a newly generated transient ID as durable. Unaffected established themes SHALL remain available.

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
