# Theme Parsing Delta

## ADDED Requirements

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