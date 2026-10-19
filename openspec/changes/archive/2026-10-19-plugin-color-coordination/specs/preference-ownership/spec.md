# Spec Delta

## ADDED Requirements

### Requirement: Coordinated plug-in color keys are written only where the user has no value

Before first writing a coordinated plug-in color key, the system SHALL check its instance value. A key with an instance value SHALL be
treated as the user's and SHALL NOT be written, including values of unknown origin. Only a key that was absent MAY be written; the system
SHALL record the exact value it wrote, and only after the node flush succeeds.

#### Scenario: Absent key
- **GIVEN** the hover background has no instance value
- **WHEN** a dark coordinated theme is applied
- **THEN** the key is written and the written value is recorded as owned

#### Scenario: User value present
- **GIVEN** the user set the hyperlink color in Colors and Fonts before coordination
- **WHEN** a dark coordinated theme is applied
- **THEN** the hyperlink color keeps the user's value and is not recorded as owned

#### Scenario: Flush fails
- **GIVEN** writing a node's keys succeeds but its flush fails
- **THEN** the keys of that node are not recorded as owned and the failure is reported

### Requirement: An owned key is rewritten only while it still holds the plugin's value

On a later application the system SHALL overwrite an owned key only when its stored value equals the recorded value. When the stored value
differs, or the key is absent, the system SHALL release ownership and leave the key as it is.

#### Scenario: Theme changed within the dark family
- **GIVEN** the plugin owns the hover background and it still holds the recorded value
- **WHEN** another dark theme is applied
- **THEN** the key holds the new derived value and the record is updated

#### Scenario: User changed an owned key
- **GIVEN** the plugin owns the hover background and the user then changed it
- **WHEN** any theme is applied, or coordination is disabled
- **THEN** the user's value is kept and the key is no longer owned
- **AND** the plugin never writes that key again while the user's value is present

### Requirement: Coordinated plug-in color keys are restored only while unchanged

When the system stops coordinating a key, it SHALL remove the key only when its stored value equals the recorded value, and SHALL then
drop the record. It SHALL NOT write a guessed or default value, SHALL NOT clear any preference node, and SHALL leave every key it does not
own, and every changed key, untouched.

#### Scenario: Coordination disabled
- **GIVEN** the plugin owns six unchanged keys and the user changed a seventh owned key
- **WHEN** workbench coordination is disabled and committed
- **THEN** the six keys are absent again
- **AND** the seventh keeps the user's value
- **AND** unrelated keys in the same nodes are unchanged

#### Scenario: Stock Dark selected externally
- **GIVEN** the plugin owns unchanged keys and the user selects stock Dark, whose stylesheet writes its own values to those keys
- **THEN** every key stock Dark overwrote is released and not removed by the plugin
- **AND** after the switch each key holds stock Dark's value, and returns to absent when stock Dark is left

#### Scenario: Ownership record lost
- **GIVEN** the plugin's ownership record is absent or unreadable
- **THEN** no coordinated key is removed and every present value is treated as the user's
