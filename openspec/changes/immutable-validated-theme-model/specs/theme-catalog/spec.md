# Theme Catalog Delta

## ADDED Requirements

### Requirement: Catalog entries are ThemeRecords with explicit origin

Each admitted catalog entry SHALL be one record carrying a stable nonblank logical identity, an explicit origin (`BUNDLED` or `INSTALLED`), an optional managed locator for installed records only, and its immutable visual theme. Product logic SHALL use the explicit origin for installed/bundled questions and SHALL NOT infer origin from logical-ID prefixes or from theme file presence.

#### Scenario: Bundled record carries no managed locator

- **GIVEN** a curated bundled theme admitted with identity `bundled:<curated-id>`
- **WHEN** its catalog record is inspected
- **THEN** its origin is `BUNDLED` and it carries no managed locator

#### Scenario: Installed record carries origin and locator

- **GIVEN** an installed theme admitted under an established `local:` identity
- **WHEN** its catalog record is inspected
- **THEN** its origin is `INSTALLED` and its locator is the repository-owned relative managed filename

#### Scenario: Prefix sniffing is gone

- **GIVEN** any catalog record
- **WHEN** the product decides installed-only behavior
- **THEN** it branches on the explicit origin, never on `startsWith("local:")`

### Requirement: Storage resolves from record locator plus repository root

The repository SHALL own the installed root and resolve `installed root + record locator` to the managed path. Themes SHALL NOT carry `java.io.File`, absolute paths, or locator abstractions; bundled records require no locator. Identity-binding persistence format is unchanged.

#### Scenario: Managed target derives from the record

- **GIVEN** a selected installed record with an established binding
- **WHEN** update or removal resolves its managed target
- **THEN** the target is the repository root resolved with the record's locator and verified against the binding
