# R6 Target Spec — Architecture and Contributor API Boundaries

Traceability: A1-A4 plus layering/fragile-dependency observations.

## Requirements

### Requirement: Core parsing/model logic is Eclipse-UI independent
Core parsing/model code SHALL not require the bundle activator, SWT/JFace, or preference stores merely to parse and validate a theme.

#### Scenario: Standalone parser test
- WHEN parser/model tests run without active Eclipse UI lifecycle
- THEN diagnostics can still be collected and asserted

### Requirement: External contributor contracts are intentionally public
If third-party theme contributors/adapters are supported, required interfaces/model types SHALL live in exported, versioned API packages with documented compatibility semantics.

#### Scenario: External bundle contributes an adapter
- THEN OSGi wiring resolves only documented public API packages

### Requirement: Contributor discovery is deterministic
Descriptor discovery SHALL define availability/version checks, ordering, ownership metadata, and collision handling before contributor instantiation/application.

#### Scenario: Two contributors claim the same capability/key
- THEN deterministic policy selects/rejects the conflict with diagnostics rather than HashMap iteration order

### Requirement: Version-fragile mappings are isolated
Internal/implementation-derived Eclipse identifiers SHALL be centralized with provenance, supported-version metadata, and focused compatibility tests.

#### Scenario: Eclipse target version changes
- THEN fragile integration mappings can be reviewed/tested without searching arbitrary adapter literals

### Requirement: Physical bundle boundaries require evidence
The project SHALL not split runtime bundles solely to mirror a conceptual architecture diagram.

#### Scenario: Proposed new bundle
- THEN the design identifies an API-ownership, optional-dependency, lifecycle, or independent-test benefit
