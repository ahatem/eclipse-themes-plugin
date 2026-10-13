# R3 Target Spec — Application Ownership, Recovery, and Truthful State

Traceability: Q9, error-handling observations, persistence observations, F3.

## Requirements

### Requirement: Application returns an aggregate result
Theme application SHALL report success, partial failure, and failure with contributor/key diagnostics rather than swallowing meaningful errors.

#### Scenario: One adapter fails
- GIVEN multiple contributors participate in an application
- WHEN one contributor fails
- THEN the application result identifies the failed contributor
- AND the UI does not report unconditional success

### Requirement: Active state commits only after successful application
The selected/active revision SHALL not be persisted as successfully active before required application work succeeds.

#### Scenario: Application fails after selection
- GIVEN a new theme revision is selected
- WHEN required application work fails
- THEN the new revision is not committed as successfully active

### Requirement: Contributors produce a declarative change plan
Theme contributors SHALL describe intended mutations, ownership, refresh actions, warnings, and restart needs before the coordinator commits them.

#### Scenario: Preview planned changes
- GIVEN a resolved theme and installed integrations
- WHEN an application plan is built
- THEN planned preference changes and skipped/unsupported integrations are inspectable before mutation

### Requirement: Ownership journal records original and last-written state
For managed keys, durable recovery state SHALL preserve original value-or-absence and the last value written by Eclipse Themes.

#### Scenario: Originally absent key
- GIVEN a managed key was absent before first write
- WHEN its ownership entry is stored
- THEN absence is recorded distinctly from an explicit default value

### Requirement: Restoration preserves newer external changes
A managed key SHALL be restored only when doing so does not overwrite a value changed later by the user or another component.

#### Scenario: External component changes a managed key
- GIVEN Eclipse Themes wrote value A
- AND another component later changes the key to B
- WHEN Eclipse Themes stops managing the key
- THEN B is preserved
- AND the conflict is surfaced according to policy

### Requirement: Failed application has compensating recovery
The coordinator SHALL attempt rollback/recovery for already-applied steps when a later required step fails and SHALL retain diagnostics if recovery is incomplete.

#### Scenario: Later contributor fails after earlier writes
- WHEN the transaction cannot complete
- THEN the coordinator executes the documented compensating recovery sequence
- AND does not pretend the operation was atomic if rollback is incomplete
