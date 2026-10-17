# Theme Catalog Delta

PM2/PM3 are maintainer-approved (2026-10-18). A generated ID is durably established only after binding persistence succeeds. Import replacement targeting and atomicity remain outside this delta.

## ADDED Requirements

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
