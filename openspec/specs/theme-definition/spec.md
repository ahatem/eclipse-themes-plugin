# Theme Definition

## Purpose

Define the durable behavioral contract of a theme independently from the editor integrations that consume it.

## Requirements

### Requirement: Theme metadata is available to the product

The system SHALL represent a loaded theme with user-facing metadata sufficient to identify and present it in the theme UI, including a display name and theme appearance classification where provided by the source format.

#### Scenario: Displaying a bundled theme
- **GIVEN** a bundled theme document is successfully loaded
- **WHEN** the theme catalog is shown
- **THEN** the theme has a displayable name and its parsed metadata is available to the UI

### Requirement: Theme tokens are addressable by semantic role

The system SHALL expose parsed theme colors/styles through canonical semantic token keys so editor integrations do not need to interpret raw XML element names directly.

#### Scenario: Legacy token is mapped
- **GIVEN** a supported legacy XML property maps to a canonical token role
- **WHEN** the theme is parsed
- **THEN** consumers can retrieve the resolved token by the canonical role

### Requirement: Known defaults may complete sparse themes

The parser MAY provide documented fallback tokens needed for supported integrations when the source omits them, but fallback behavior SHALL be deterministic.

#### Scenario: Missing supported token
- **GIVEN** a valid theme omits a token for which the format defines a fallback
- **WHEN** the theme is parsed twice
- **THEN** the same resolved token value is produced each time

> Known identity/equality defects from the audit are intentionally not normalized here; they are addressed by a planned change.

### Requirement: Distinct legacy appearance roles remain independent

The theme definition SHALL retain separate canonical roles for selection background and source-hover background, generic comment and XML comment, and static-final field and constant. Explicit values for one role SHALL NOT replace explicit colors or any style flags of the other role, regardless of source document order.

#### Scenario: Three distinct role pairs in either order
- **GIVEN** a supported theme defines both roles of each pair with distinct colors and style flags
- **WHEN** the theme is parsed with either order of each pair
- **THEN** both canonical roles expose their respective complete color/style values
- **AND** neither role is resolved by overwriting its counterpart

#### Scenario: Bundled GitHub Dark Dimmed selection and static-final colors
- **GIVEN** the bundled GitHub Dark Dimmed document defines selection `#3d4149`, hover `#2d333b`, static-final field `#e0823d`, and constant `#6cb6ff`
- **WHEN** it is parsed
- **THEN** each distinct role retains its specified color

#### Scenario: Bundled Catppuccin Frappe styles
- **GIVEN** the bundled Catppuccin Frappe document defines italic generic comments, XML comments without explicit style flags, bold/italic static-final fields, and constants without explicit style flags
- **WHEN** it is parsed
- **THEN** the generic-comment and static-final-field styles remain intact
- **AND** XML-comment and constant effective style flags are false

### Requirement: Specific-role fallbacks do not reverse semantic ownership

An omitted XML-comment role SHALL inherit the generic-comment token. An omitted static-final-field role SHALL inherit the constant token and its existing fallback chain. Generic comments SHALL NOT inherit XML comments, and constants SHALL NOT inherit static-final fields. Fallback lookup SHALL NOT create explicit token presence or alter unrelated inheritance chains.

#### Scenario: Generic-only legacy theme
- **GIVEN** a theme defines generic comment and constant but omits XML comment and static-final field
- **WHEN** the specific roles are resolved
- **THEN** XML comment resolves the generic comment's whole token
- **AND** static-final field resolves the constant's whole token
- **AND** the omitted roles remain absent as explicit source tokens

#### Scenario: Specific-only legacy theme
- **GIVEN** a theme defines XML comment and static-final field but omits generic comment and constant
- **WHEN** generic comment and constant are resolved
- **THEN** they use their existing generic fallback chains
- **AND** neither is populated from the specific-only counterpart

#### Scenario: Both roles and neither role
- **GIVEN** a theme either supplies both roles of a pair or omits both
- **WHEN** the roles are resolved repeatedly
- **THEN** explicit paired values stay independent when present
- **AND** otherwise the documented inheritance chains produce repeatable values without synthesizing explicit presence

### Requirement: Hover fallback lookup does not imply explicit hover control

A missing source-hover role SHALL remain absent as an explicit token. General background fallback lookup SHALL NOT substitute the source-hover value into selection background or activate a JDT hover override. Applied omission behavior SHALL follow the editor integration's documented editor-default policy.

#### Scenario: Selection-only sparse theme
- **GIVEN** a theme specifies selection background and general background but omits source-hover background
- **WHEN** the theme definition is consumed
- **THEN** selection retains its specified color
- **AND** source hover remains absent as an explicit token
- **AND** a background fallback lookup does not count as explicit hover presence

#### Scenario: Hover-only sparse theme
- **GIVEN** a theme specifies source-hover background and general background but omits selection background
- **WHEN** selection background is resolved
- **THEN** selection uses its existing background fallback
- **AND** it does not use the explicit source-hover background

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
