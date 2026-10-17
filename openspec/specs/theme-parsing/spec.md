# Theme Parsing

## Purpose

Define the supported legacy XML parsing contract that turns theme documents into theme definitions.

## Requirements

### Requirement: Supported XML themes can be parsed

The system SHALL parse supported Eclipse Color Theme-style XML documents without a DOCTYPE into a theme definition containing metadata and canonical tokens when required XML security controls are established. It SHALL preserve existing metadata, styles, aliases, fallbacks, predefined XML escapes, and numeric character references except for the distinct-role and alias-collision corrections specified by this change.

#### Scenario: Bundled valid theme
- **GIVEN** a valid bundled legacy XML theme
- **WHEN** it is loaded
- **THEN** parsing returns a usable theme definition rather than requiring editor-specific parsing

#### Scenario: Existing legacy interpretation is preserved
- **GIVEN** a supported legacy theme without a DOCTYPE that uses metadata, mapped tokens, styles, and omitted tokens with existing fallbacks
- **AND** required XML security controls are available
- **WHEN** it is parsed
- **THEN** its metadata and resolved tokens/styles match the existing interpretation except for the explicitly specified role-separation and alias-collision corrections

#### Scenario: Ordinary XML character references remain supported
- **GIVEN** a supported legacy theme without a DOCTYPE containing predefined XML escapes and numeric character references in metadata
- **AND** required XML security controls are available
- **WHEN** it is parsed
- **THEN** those references decode to their intended characters
- **AND** no external resource is accessed

#### Scenario: Mapping corrections do not relax security controls
- **GIVEN** a document contains otherwise supported mapped properties but has a DOCTYPE or the processor cannot establish a required security control
- **WHEN** parsing is attempted
- **THEN** the existing fail-closed XML security requirements still apply
- **AND** alias resolution does not return a theme from prohibited input

### Requirement: Legacy aliases resolve through a canonical mapping

Legacy property names SHALL be resolved through one mapping layer before editor adapters consume tokens.

#### Scenario: Legacy editor-specific name
- **GIVEN** a supported legacy property name
- **WHEN** the XML parser encounters it
- **THEN** the resulting theme contains the corresponding canonical semantic token

### Requirement: Parsing diagnostics are distinguishable from successful values

Consumers SHALL be able to distinguish a successfully parsed theme from a parsing failure.

#### Scenario: Unreadable or malformed document
- **GIVEN** a document that cannot be parsed as a supported theme
- **WHEN** parsing is attempted
- **THEN** the caller receives a failure rather than a fabricated successful theme

> Security hardening, structural validation, and detailed diagnostics are expanded by planned R1/R2 changes.

### Requirement: Theme XML rejects every DOCTYPE declaration

Theme parsing SHALL reject any document containing a DOCTYPE declaration before processing declared entities or retrieving a DTD. Rejection SHALL return a parsing failure with a diagnostic identifying the prohibited DTD/DOCTYPE behavior and retaining the underlying cause; it SHALL NOT return a theme.

#### Scenario: Local external general entity
- **GIVEN** a theme declares an external general entity referencing a local file in its DOCTYPE
- **WHEN** parsing is attempted
- **THEN** parsing fails with a diagnostic identifying the prohibited DTD/DOCTYPE behavior and retaining its cause
- **AND** the referenced file is not opened or read
- **AND** no theme is returned

#### Scenario: Network external DTD
- **GIVEN** a theme declares a DOCTYPE with a network DTD system identifier
- **WHEN** parsing is attempted
- **THEN** parsing fails
- **AND** no connection or request is made to retrieve the DTD
- **AND** no theme is returned

#### Scenario: External parameter entity
- **GIVEN** a theme's DOCTYPE declares a parameter entity referencing a local or network resource
- **WHEN** parsing is attempted
- **THEN** parsing fails before expanding the parameter entity
- **AND** the referenced resource is not accessed
- **AND** no theme is returned

#### Scenario: Internal-only DTD
- **GIVEN** a theme contains an internal-only DOCTYPE, with or without custom entity declarations
- **WHEN** parsing is attempted
- **THEN** parsing fails before expanding declared entities or applying DTD defaults
- **AND** no theme is returned

### Requirement: Theme XML never resolves external resources

Theme parsing SHALL NOT open, read, or retrieve external resources through XML general entities, parameter entities, DTDs, schemas, or XInclude. This prohibition SHALL apply to every parsing entry point and source, regardless of ambient parser defaults. Any attempted resolver invocation SHALL fail rather than supply replacement content.

#### Scenario: XInclude retrieval is disabled
- **GIVEN** XML contains an XInclude reference to a monitored local or network resource
- **AND** XML security controls are established
- **WHEN** parsing is attempted
- **THEN** the referenced resource is not accessed or incorporated through XInclude

#### Scenario: Schema location does not cause retrieval
- **GIVEN** XML contains an external schema-location reference
- **AND** XML security controls are established
- **WHEN** parsing is attempted
- **THEN** the referenced schema is not retrieved
- **AND** this operation does not introduce schema validation

#### Scenario: Resolver invocation cannot supply external content
- **GIVEN** the configured XML processor attempts to resolve an external identifier
- **WHEN** the external-resolution guard is invoked
- **THEN** it reports a parsing failure and retains the cause
- **AND** it does not return an external stream, an empty substitute document, or a successfully parsed theme

#### Scenario: Security policy is identical for every source and entry point
- **GIVEN** the same prohibited XML is supplied as a file, a stream, or a stream with source-file metadata from bundled, installed, or import loading
- **WHEN** parsing is attempted
- **THEN** each route rejects it without external resolution
- **AND** source provenance does not enable a relaxed parsing mode

### Requirement: XML security configuration fails closed

Theme parsing SHALL establish all required security controls before consuming theme XML. If a required control is unsupported, rejected, cannot be verified, or is reported with an unsafe value, parsing SHALL fail without consuming the input or returning a theme. The failure SHALL identify the unavailable control or configuration stage and retain the underlying cause. There SHALL be no fallback to an unsecured parser.

#### Scenario: Required security control cannot be applied
- **GIVEN** the selected XML processor rejects or does not support a required security control
- **WHEN** theme parsing is requested
- **THEN** configuration fails before any bytes of the theme stream are read
- **AND** the caller receives a diagnostic identifying the control or stage and retaining the cause
- **AND** no theme is returned and no unsecured retry occurs

#### Scenario: Required security control cannot be confirmed
- **GIVEN** a required security control's effective value cannot be read back or differs from the requested secure value
- **WHEN** theme parsing is requested
- **THEN** parsing fails before any bytes of the theme stream are read
- **AND** the caller receives a diagnostic identifying the control or stage and retaining the cause
- **AND** no theme is returned and no unsecured retry occurs

#### Scenario: XML processor cannot be created
- **GIVEN** the selected XML processor fails during factory or builder creation
- **WHEN** theme parsing is requested
- **THEN** the caller receives a configuration-stage parsing failure retaining the cause
- **AND** no bytes of the theme stream are read and no unsecured retry occurs

### Requirement: Canonical properties take precedence over true legacy aliases

When valid properties resolve to the same semantic role, the canonical spelling SHALL win over legacy alias spellings independently of document order. At equal spelling/rank, the last valid occurrence SHALL win. Resolution SHALL choose the winning complete color/style token without merging losing flags. Distinct roles SHALL NOT be ranked as aliases of each other.

#### Scenario: Canonical deprecated role and legacy deprecated-member alias conflict
- **GIVEN** a theme supplies different valid `deprecated` and `deprecatedMember` colors/styles
- **WHEN** either document order is parsed
- **THEN** the deprecated role contains the complete canonical `deprecated` token
- **AND** a style flag absent on the winner is not imported from the loser

#### Scenario: Canonical generic comment and single-line alias conflict
- **GIVEN** a theme supplies different valid `comment` and `singleLineComment` values
- **WHEN** either order is parsed
- **THEN** the complete `comment` token wins
- **AND** `xmlComment` remains an independent role

#### Scenario: Only a legacy alias is provided
- **GIVEN** a theme supplies a valid supported legacy alias with no valid canonical counterpart
- **WHEN** it is parsed
- **THEN** the alias still provides the corresponding canonical role's complete token

#### Scenario: Repeated same-spelling property
- **GIVEN** a theme repeats the same accepted property spelling with conflicting valid tokens
- **WHEN** the document is parsed
- **THEN** the last valid occurrence wins as a complete token
- **AND** a conflict diagnostic identifies the repeated property and winning occurrence

#### Scenario: Invalid higher-priority candidate does not erase a valid alias
- **GIVEN** a valid legacy alias and a canonical counterpart with an invalid or missing color
- **WHEN** the document is parsed
- **THEN** the existing invalid-color/skip behavior applies to the invalid candidate
- **AND** the valid alias remains eligible instead of being erased by the invalid candidate's priority

### Requirement: Mapping conflicts have bounded diagnostics

Accepted candidates for the same role with differing colors or effective style flags SHALL produce a mapping warning identifying the canonical role, conflicting source properties/occurrences, and selected winner. Equivalent candidates SHALL resolve without a conflict warning. Mapping warnings SHALL NOT turn an otherwise accepted theme into a parsing failure or introduce general structural validation.

#### Scenario: Conflict remains a successful parse with a warning
- **GIVEN** two accepted true-alias candidates conflict
- **WHEN** the supported document is parsed
- **THEN** the theme is returned with the documented winner
- **AND** a mapping warning provides the role, both source names/occurrences, and winner

#### Scenario: Equivalent aliases
- **GIVEN** two accepted aliases provide the same color and effective style flags
- **WHEN** they are parsed
- **THEN** the role resolves deterministically
- **AND** no conflict warning is emitted for that pair

#### Scenario: Distinct semantic roles are not diagnosed as aliases
- **GIVEN** selection/hover, generic/XML comment, or static-final/constant roles have different valid values
- **WHEN** the theme is parsed
- **THEN** both values are retained independently
- **AND** their differing values alone do not produce an alias-conflict warning
