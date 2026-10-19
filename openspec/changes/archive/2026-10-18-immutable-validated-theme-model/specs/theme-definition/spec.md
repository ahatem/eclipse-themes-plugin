# Theme Definition Delta

## ADDED Requirements

### Requirement: A validated theme is an immutable visual definition

A successfully parsed theme SHALL be an immutable visual definition carrying source metadata (raw/source id where retained, name, author, website, description), appearance classification, and semantic tokens. After construction, no production path SHALL mutate its tokens, metadata, or classification. The token collection SHALL be an immutable snapshot: mutating the source map after construction SHALL NOT alter the theme, and accessors SHALL NOT expose a live mutable view.

#### Scenario: Source map cannot alter a constructed theme

- **GIVEN** a validated theme constructed from a token map
- **WHEN** the source map is subsequently mutated
- **THEN** the theme's resolved tokens are unchanged

#### Scenario: No post-validation token mutation exists

- **GIVEN** a validated theme
- **WHEN** production code holds its reference
- **THEN** no public operation adds, replaces, or removes its tokens

### Requirement: Visual definitions own no catalog identity or storage

A theme SHALL NOT carry logical catalog identity, lifecycle identity, managed file/path/locator, repository binding, or catalog equality. Two themes SHALL NOT compare equal by logical identity because themes no longer own one; themes use identity equality unless a future change proves a caller needs value equality.

#### Scenario: Catalog identity lives outside the theme

- **GIVEN** two validated themes with equal visual content admitted under different logical identities
- **WHEN** theme equality is evaluated
- **THEN** identity equality applies and no logical-ID comparison occurs on the themes

### Requirement: Semantic roles are a closed core vocabulary

Token roles SHALL be the fixed core vocabulary consumed by parsing and adapters. Declaring a duplicate role SHALL fail immediately. Lookup by id SHALL remain available for parsing. No contributor token registry SHALL exist in R2.4.

#### Scenario: Duplicate role declaration fails

- **GIVEN** a role name already declared in the core vocabulary
- **WHEN** it is declared again
- **THEN** declaration fails immediately rather than silently replacing the role

### Requirement: Sparse fallback rendering is preserved exactly

Parser fallback tokens for omitted roles, including the existing root defaults, SHALL produce byte/semantic-equivalent resolved values to current behavior. Fallback policy ownership remains at the parser boundary and is documented as accepted compatibility behavior, not as immutable-model semantics.

#### Scenario: Sparse theme resolves identically

- **GIVEN** a valid sparse theme relying on fallback tokens
- **WHEN** it is parsed under the immutable model
- **THEN** every resolved token value equals the value produced before this change
