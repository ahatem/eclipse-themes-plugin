# R9 Target Spec — Theme Package v1 and Contributor Ecosystem

Traceability: Part 5B theme-package/extension/migration sections, A1, F6.

## Requirements

### Requirement: Theme packages have versioned declarative identity
A package SHALL declare schema version, stable namespaced ID, package revision/content digest, label/author/source, appearance metadata, compatibility/capabilities, and provenance/license metadata.

#### Scenario: Same package ID with new content
- THEN revision/digest distinguishes the new content without changing display identity

### Requirement: Package resources are declarative
Imported package archives SHALL NOT execute bundled Java/classes/scripts as an implicit theming capability.

#### Scenario: Archive contains executable integration payload
- THEN validation rejects/ignores it according to the format contract

### Requirement: Package resources are bounded and path-safe
Archive extraction/reference handling SHALL enforce path traversal, file count, size, supported scheme, and resource-reference limits.

#### Scenario: Archive entry escapes package root
- THEN validation rejects the package before commit

### Requirement: Package model separates semantic appearance layers
The format SHALL support semantic palette/editor tokens and workbench resources, with optional fonts/icons only as explicitly supported components.

#### Scenario: User enables editor but not workbench component
- THEN package resolution can apply the supported editor portion independently

### Requirement: Contributor API produces change plans
Executable OSGi contributor bundles SHALL be separately installed and SHALL produce declarative application plans through the versioned public API.

#### Scenario: Third-party editor integration
- THEN it declares capability, target bundle/version range, ordering/dependencies, owned keys, refresh/restart behavior, and diagnostics

### Requirement: Legacy XML migrates as editor-only input
Existing XML themes SHALL remain importable through a documented compatibility/migration path without silently enabling workbench ownership.

#### Scenario: Legacy theme lacks modern stable ID
- THEN migration generates/persists identity according to the lifecycle spec while preserving available legacy identifiers separately

### Requirement: Redistribution rights are explicit
Bundled packages/assets SHALL carry documented provenance/licenses/notices and the project SHALL not assume its root license overrides third-party asset terms.

#### Scenario: Theme asset provenance is unknown
- THEN it is not newly redistributed as a curated package until rights are established
