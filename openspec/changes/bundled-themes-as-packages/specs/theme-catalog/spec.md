## MODIFIED Requirements

### Requirement: Bundled themes are discoverable

The system SHALL load bundled theme resources into the theme catalog when the theme manager/repository is initialized or reloaded. Bundled themes SHALL be Theme Package v1 documents (`.ectheme`) whose catalog identity is `bundled:<curated source id>`, where the curated source id comes from a bundled, package-id-keyed identity map and is the identity the same theme had when it was bundled as a legacy document. A bundled package SHALL NOT supply its own catalog identity.

#### Scenario: Fresh workspace
- **GIVEN** no user-imported themes exist
- **WHEN** the catalog loads
- **THEN** bundled themes are available for selection

#### Scenario: Identities are unchanged by the package format
- **GIVEN** the bundled themes before and after they became packages
- **WHEN** the catalog loads
- **THEN** every bundled theme has the same logical identity, display name, appearance and editor tokens as before
- **AND** the default light and default dark theme identities resolve to bundled records
- **AND** no two bundled records share an identity

#### Scenario: Bundled package without a curated identity
- **GIVEN** a bundled package whose package id has no entry in the identity map
- **WHEN** the catalog loads
- **THEN** it is not admitted and a product-defect diagnostic identifies the resource
- **AND** other bundled themes remain available

#### Scenario: Stored selection of a bundled theme survives
- **GIVEN** a stored selection that names a bundled theme by logical identity, or by the legacy source id used before logical identities existed
- **WHEN** the selection is resolved against the catalog
- **THEN** it resolves to that same bundled theme
- **AND** the online preview address for a bundled theme is built from the same source id as before

#### Scenario: Installed legacy themes are unaffected
- **GIVEN** a legacy `.xml` theme in installed-theme storage
- **WHEN** the catalog loads
- **THEN** it is admitted exactly as before
