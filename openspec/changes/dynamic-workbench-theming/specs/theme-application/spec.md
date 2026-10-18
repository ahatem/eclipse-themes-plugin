# Theme Application Delta

## ADDED Requirements

### Requirement: Application reports an aggregate editor and workbench outcome

Applying a theme SHALL return one outcome with a distinguishable editor part (naming adapters that failed to persist) and workbench part (NOT_REQUESTED, APPLIED, RELINQUISHED, YIELDED, UNAVAILABLE or FAILED). The outcome SHALL be fully successful only when every requested part succeeded. A workbench failure SHALL NOT invalidate a successful editor application.

#### Scenario: Editor and workbench succeed
- **GIVEN** coordination is enabled and available
- **THEN** both parts succeed and the outcome is fully successful

#### Scenario: Editor succeeds, workbench unavailable or failed
- **THEN** the outcome states a partial result with the workbench reason and the editor part remains successful

#### Scenario: Editor adapter fails
- **GIVEN** an adapter cannot persist its preferences
- **THEN** the editor part fails and names the adapter
- **AND** the outcome is not fully successful

#### Scenario: No false persisted success
- **GIVEN** the workbench switch or its postcondition fails
- **THEN** no ownership record is persisted for it
