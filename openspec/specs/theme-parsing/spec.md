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

### Requirement: A theme document has a validated minimum structure

A parsed theme document SHALL be accepted only when its root element is `colorTheme` and it carries a present,
non-blank display name. A document that fails either check SHALL NOT produce a theme; parsing SHALL fail with a
finding that identifies the offending element or attribute. The system SHALL NOT define a minimum token set,
require `background` or `foreground`, or otherwise convert sparse themes into rejected documents.

#### Scenario: Correct root and displayable name

- **GIVEN** an XML document whose root element is `colorTheme` with a non-blank `name`
- **WHEN** it is parsed as a supported theme
- **THEN** parsing returns a theme definition
- **AND** the document is subject only to the remaining documented validation rules

#### Scenario: Arbitrary root element

- **GIVEN** a well-formed XML document whose root element is not `colorTheme`
- **WHEN** it is parsed as a supported theme
- **THEN** parsing fails
- **AND** the failure finding identifies the actual root element name
- **AND** no theme definition is produced

#### Scenario: Missing or blank display name

- **GIVEN** an XML document with root `colorTheme` whose `name` is absent or blank
- **WHEN** it is parsed as a supported theme
- **THEN** parsing fails with a finding identifying the `name` attribute
- **AND** no theme definition is produced

#### Scenario: Sparse document remains valid

- **GIVEN** a supported document that supplies no token elements beyond its minimum structure
- **WHEN** it is parsed
- **THEN** it is accepted
- **AND** the documented deterministic default tokens complete it as before
- **AND** repeated parsing of the same document produces the same resolved token values

### Requirement: Theme metadata is validated against an explicit field policy

Metadata fields SHALL be validated against an explicit policy. `name` is required and non-blank. `type` is
optional; its absence produces no finding, and a supplied value is validated against `dark`/`light`
case-insensitively. `ect-id` is optional legacy/source metadata and never participates in logical identity. Legacy
`id` and `modified` are recognised and ignored. `author` and `description` are optional unconstrained text.
Unrecognised root attributes are ignored without a diagnostic.

#### Scenario: Source ID is absent

- **GIVEN** a supported document with no `ect-id`
- **WHEN** it is parsed for admission through any source
- **THEN** it is accepted
- **AND** no finding is produced for the absent source ID
- **AND** its logical identity is established independently of the source document

#### Scenario: Legacy source id and modification date

- **GIVEN** a supported document carrying legacy `id` and `modified` root attributes
- **WHEN** it is parsed
- **THEN** both attributes are ignored
- **AND** no finding is produced for either

#### Scenario: Unrecognised root attribute

- **GIVEN** a supported document with a root attribute the format does not define
- **WHEN** it is parsed
- **THEN** the attribute is ignored
- **AND** no finding is produced for it

#### Scenario: Type attribute is absent

- **GIVEN** a supported document with no `type` attribute
- **WHEN** it is parsed
- **THEN** the theme is classified light, exactly as before
- **AND** no finding is produced for the absent type

#### Scenario: Type attribute is not a supported value

- **GIVEN** a supported document whose `type` is present but is neither `dark` nor `light`, compared
  case-insensitively, including a blank value
- **WHEN** it is parsed
- **THEN** the theme is classified light, exactly as before
- **AND** a warning finding identifies the `type` attribute and the value seen

### Requirement: Token, colour and style interpretation is diagnosed rather than silent

Every token element the parser cannot fully interpret SHALL produce a warning finding naming the element and, where
relevant, the attribute and value. This covers unknown element names, known tokens with an absent or blank
`color`, known tokens whose `color` is outside the documented colour grammar, non-boolean style flag values, and
nested child elements beneath a direct child. Unrecognised token attributes SHALL be ignored without a
diagnostic.

#### Scenario: Interpretable outcome is unchanged

- **GIVEN** any of the conditions covered by this requirement
- **WHEN** the document is parsed
- **THEN** the resulting theme definition is identical to the outcome produced before this validation existed
- **AND** the only difference is that the condition is now reported instead of dropped in silence

#### Scenario: Unknown element name

- **GIVEN** a supported document containing a direct child element whose name is neither a known token nor a
  supported legacy alias
- **WHEN** it is parsed
- **THEN** the element is ignored as before
- **AND** a warning finding identifies the element name

#### Scenario: Known token without a colour

- **GIVEN** a supported document containing a known token element with no `color` attribute or a blank one
- **WHEN** it is parsed
- **THEN** the token is not interpreted, exactly as before
- **AND** a warning finding identifies the element and the missing `color` attribute

#### Scenario: Known token with an unaccepted colour

- **GIVEN** a supported document containing a known token element whose `color` value is outside the documented
  colour grammar
- **WHEN** it is parsed
- **THEN** the token is skipped, exactly as before
- **AND** a warning finding identifies the element, the `color` attribute and the offending value

#### Scenario: Documented colour grammar

- **GIVEN** a colour value consisting of `#` followed by exactly six hexadecimal digits, with or without
  surrounding whitespace and in either case
- **WHEN** it is interpreted
- **THEN** it is accepted as that colour

#### Scenario: Shorthand colour form remains supported

- **GIVEN** a colour value consisting of `#` followed by exactly three hexadecimal digits
- **WHEN** it is interpreted
- **THEN** it is accepted as the equivalent six-digit colour

#### Scenario: Non-boolean style value

- **GIVEN** a known token element whose `bold`, `italic`, `underline` or `strikethrough` value is neither `true`
  nor `false`, compared case-insensitively
- **WHEN** it is parsed
- **THEN** that flag is interpreted as false, exactly as before
- **AND** a warning finding identifies the element, the attribute and the offending value

#### Scenario: Nested child elements

- **GIVEN** a supported document containing a direct child element that itself has child elements
- **WHEN** it is parsed
- **THEN** the nested subtree is ignored
- **AND** a warning finding identifies the element containing the ignored children

#### Scenario: Unrecognised token attribute

- **GIVEN** a known token element carrying an attribute the format does not define
- **WHEN** it is parsed
- **THEN** the attribute is ignored
- **AND** no finding is produced for it

### Requirement: Validation findings are structured and distinguishable

Validation SHALL produce findings that carry a severity, a stable kind, a human-readable message, the source file
when one is known, and the XML element and attribute involved where applicable. Errors SHALL fail parsing and
SHALL travel with the parsing failure; warnings SHALL continue parsing and SHALL be delivered to the
caller-supplied diagnostic sink without being retained on the parser. A caller that supplies no sink SHALL still
receive the documented outcome and no retained parser state.

#### Scenario: Error finding accompanies a failure

- **GIVEN** a document that fails a validation rule
- **WHEN** parsing fails
- **THEN** the failure carries a finding with error severity, the source file when one was supplied, and the
  element or attribute involved

#### Scenario: Warning finding reaches the caller

- **GIVEN** a document that produces only warnings
- **WHEN** it is parsed with a caller-supplied diagnostic sink
- **THEN** the sink receives one finding per warning, in document order
- **AND** the returned theme definition reflects the documented fallbacks

#### Scenario: Parser retains no per-parse state

- **GIVEN** a parser instance used for several parses
- **WHEN** one parse produces warnings and a later parse produces none
- **THEN** no finding from the earlier parse is reported for the later one

### Requirement: Website metadata never determines theme admission

The system SHALL treat a theme's `website` value as optional display metadata. A theme with an absent, blank,
relative, malformed, control-character-bearing or otherwise unusable `website` SHALL still be accepted as a theme,
and SHALL NOT be refused for its metadata alone.

#### Scenario: Absent website

- **GIVEN** a supported document with no `website` attribute
- **WHEN** it is parsed
- **THEN** it is accepted
- **AND** no website finding is produced

#### Scenario: Unusable website value

- **GIVEN** a supported document whose `website` is blank, a relative reference, is not a URI at all, or contains
  embedded control characters
- **WHEN** it is parsed
- **THEN** the theme is still accepted
- **AND** a warning finding identifies the `website` attribute and the value seen

### Requirement: Website metadata is launchable only for an allowed absolute URI

A `website` value SHALL be reported as launchable only when, after surrounding whitespace is removed, it is a
syntactically absolute URI whose scheme is on the documented allowed list — `http` or `https`, compared
case-insensitively — and it carries no embedded control characters. Any other value SHALL be reported as not
launchable. A value with a scheme outside the allowed list SHALL additionally produce a warning finding identifying
the `website` attribute and the scheme seen.

#### Scenario: Allowed web scheme

- **GIVEN** a theme whose `website` is an absolute URI with an allowed scheme
- **WHEN** its metadata link is evaluated
- **THEN** the value is reported as launchable

#### Scenario: Scheme comparison ignores case

- **GIVEN** a theme whose `website` uses an allowed scheme in a different letter case
- **WHEN** its metadata link is evaluated
- **THEN** the value is reported as launchable

#### Scenario: Disallowed scheme

- **GIVEN** a theme whose `website` is an absolute URI with a scheme outside the allowed list
- **WHEN** its metadata link is evaluated
- **THEN** the value is reported as not launchable
- **AND** a warning finding identifies the `website` attribute and the scheme seen

#### Scenario: Local file and script-like schemes

- **GIVEN** a theme whose `website` is a `file:` or script-like URI
- **WHEN** its metadata link is evaluated
- **THEN** the value is reported as not launchable
