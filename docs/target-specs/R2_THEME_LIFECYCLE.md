# R2 Target Spec — Theme Lifecycle Integrity

Traceability: Q3, Q4, Q5, Q10, Q12, Q14, Q15, U4, U5, U8.

## Requirements

### Requirement: Theme identity is stable and independent of presentation
A stored theme SHALL have a stable identity distinct from display name, source filename, legacy external ID, and content revision.

#### Scenario: Two legacy themes lack modern identity metadata
- GIVEN two distinct valid legacy theme files
- WHEN they are imported
- THEN they do not compare equal merely because a modern identity field is absent

### Requirement: Theme revisions are distinguishable
The system SHALL distinguish package identity from revision/content so edited content with the same theme identity can be detected and reapplied.

#### Scenario: Active theme content changes without changing identity
- GIVEN a stored theme keeps its stable ID but its validated content changes
- WHEN the updated revision becomes selected
- THEN application is not skipped solely because the ID matches the previous active theme

### Requirement: Import validates before replacement
A candidate import SHALL be parsed and validated before any previously valid stored revision is replaced.

#### Scenario: Invalid replacement
- GIVEN a valid stored theme exists
- WHEN an invalid candidate attempts to replace it
- THEN the valid stored revision remains intact
- AND the import reports failure

### Requirement: Import storage avoids basename collisions
Stored identity SHALL not depend solely on the original source basename.

#### Scenario: Different themes share a filename
- GIVEN two different valid themes arrive with the same source filename
- WHEN both are imported according to supported conflict policy
- THEN one cannot silently destroy the other solely due to basename collision

### Requirement: Conflict policy is deterministic across import and reload
Duplicate IDs, names, revisions, and bundled-vs-installed conflicts SHALL resolve according to one documented policy used consistently by import and repository reload.

#### Scenario: Imported theme conflicts with bundled theme
- GIVEN an imported candidate conflicts with an existing bundled theme under the defined identity rules
- WHEN import and later reload occur
- THEN both operations resolve the conflict consistently

### Requirement: File mutation failures are truthful
Directory creation, removal, and replacement failures SHALL be observed and reported rather than converted into success.

#### Scenario: Stored theme cannot be deleted
- GIVEN deletion fails at the filesystem layer
- WHEN the user removes the imported theme
- THEN the operation reports failure
- AND the repository does not claim the file is gone

### Requirement: Machine identifiers are locale-stable
File extension and machine identifier normalization SHALL use locale-stable rules and documented case policy.

#### Scenario: Uppercase XML extension
- GIVEN a supported theme file uses an uppercase extension
- WHEN import/parser selection evaluates the file
- THEN behavior follows the documented case-insensitive extension policy consistently

### Requirement: Validated themes are immutable snapshots
A committed theme definition SHALL not expose mutable live state that can change behind repository/application decisions.

#### Scenario: Caller obtains a theme token collection
- WHEN the caller inspects the validated theme
- THEN it cannot mutate the committed theme through a live collection view
