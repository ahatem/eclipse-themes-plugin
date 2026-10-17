# Theme Parsing Delta

PM3–PM4 Option A approved by the maintainer on 2026-10-13; see the resolution record in `proposal.md`. These are approved deltas for this change, not promoted to canonical specs. PM5 Option A was approved on 2026-10-17 for the bounded PDE/Ant runtime correction. PM1–PM4 remain unchanged; PM6 Option A now explicitly authorizes only the isolated optional R4_33 PDE live-store accessor. The approved behavior remains unchanged, with public/upstream replacement deferred to R6.

## MODIFIED Requirements

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

## ADDED Requirements

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
