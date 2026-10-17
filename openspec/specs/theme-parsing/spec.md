# Theme Parsing

## Purpose

Define the supported legacy XML parsing contract that turns theme documents into theme definitions.

## Requirements

### Requirement: Supported XML themes can be parsed

The system SHALL parse supported Eclipse Color Theme-style XML documents without a DOCTYPE into a theme definition containing metadata and canonical tokens when the required XML security controls are established. For these documents, the security change SHALL preserve existing metadata, token, style, legacy-alias, and fallback behavior, including predefined XML escapes and numeric character references.

#### Scenario: Bundled valid theme
- **GIVEN** a valid bundled legacy XML theme
- **WHEN** it is loaded
- **THEN** parsing returns a usable theme definition rather than requiring editor-specific parsing

#### Scenario: Existing legacy interpretation is preserved
- **GIVEN** a supported legacy theme without a DOCTYPE that uses metadata, mapped tokens, styles, and omitted tokens with existing fallbacks
- **AND** required XML security controls are available
- **WHEN** it is parsed
- **THEN** its metadata and resolved tokens/styles match the existing interpretation

#### Scenario: Ordinary XML character references remain supported
- **GIVEN** a supported legacy theme without a DOCTYPE containing predefined XML escapes and numeric character references in metadata
- **AND** required XML security controls are available
- **WHEN** it is parsed
- **THEN** those references decode to their intended characters
- **AND** no external resource is accessed

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
