# Theme Definition Delta

PM1/PM2/PM5 are maintainer-approved (2026-10-18). This approved delta remains unarchived; see proposal for implementation authorization and scope.

## ADDED Requirements

### Requirement: Established logical identity is independent of presentation and content

An admitted theme SHALL have a stable nonblank logical identity separate from display metadata, source locator, legacy source ID and current contents. Changes to an established theme's label or current contents SHALL NOT regenerate its logical identity. A source-file move SHALL NOT change the identity of its already installed copy.

#### Scenario: Rename and current-content edit
- **GIVEN** an established theme with a logical identity
- **WHEN** its display name and current tokens change while that established theme is retained
- **THEN** its logical identity remains unchanged
- **AND** consumers receive the changed current definition without requiring a revision number or history entry

#### Scenario: External source moves
- **GIVEN** an installed copy of a theme
- **WHEN** its external source file is renamed or moved
- **THEN** the installed theme retains its logical identity
- **AND** identity alone does not decide whether importing that source again updates the installed theme

### Requirement: Source identity claims do not collapse independent themes

Independent installed records SHALL have distinct established logical identities even when source IDs are missing, blank or equal. Curated bundled identities SHALL remain stable across releases that retain the same logical theme. Source IDs SHALL remain separately available as legacy metadata and SHALL NOT establish equivalence to a bundled theme by themselves.

#### Scenario: Legacy inputs lack a reliable source ID
- **GIVEN** two independently admitted installed records whose XML source IDs are both absent, blank or the same nonblank value
- **WHEN** their logical identities are compared
- **THEN** the records remain distinct
- **AND** source metadata alone does not authorize overwriting or linking either record

#### Scenario: Bundled definition changes
- **GIVEN** a curated bundled theme retained in a later plugin release with changed contents or label
- **WHEN** the later catalog loads
- **THEN** the theme retains its established logical identity
- **AND** an imported source claiming the bundled source ID is not automatically the same logical theme

### Requirement: Logical equality does not claim content equality

Established theme equality and hashing SHALL depend on logical identity rather than source claims, label, location or current contents. Two candidates without established logical identity SHALL NOT compare equal merely because identity metadata is absent.

#### Scenario: Two current definitions of one theme
- **GIVEN** definitions with the same established logical identity and different tokens
- **WHEN** logical identity is compared
- **THEN** they identify the same theme without asserting equal contents or successful application

#### Scenario: Two unresolved candidates
- **GIVEN** two different candidate objects without established logical identity
- **WHEN** they are compared
- **THEN** they do not compare equal because both lack identity
