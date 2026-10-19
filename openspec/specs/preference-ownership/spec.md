# preference-ownership Specification

## Purpose
Define ownership-safe mutation and restoration of Eclipse/plugin preferences managed by Eclipse Themes.

## Requirements

### Requirement: Theme restoration is limited to managed preference keys

When Eclipse Themes stops managing editor appearance, it SHALL modify or remove only preference keys that are explicitly part of its supported theme integration contract.

#### Scenario: Foreign preference node contains unrelated settings
- **GIVEN** a preference node used by a supported editor integration contains both theme-related keys and unrelated user/plugin settings
- **WHEN** the user invokes the operation that stops/restores Eclipse Themes management
- **THEN** unrelated settings remain byte-for-byte/value-equivalent unchanged
- **AND** the operation does not clear the entire preference node

### Requirement: Unknown historical state is handled conservatively

The system SHALL NOT invent a pre-theme preference value when no trustworthy original snapshot exists.

#### Scenario: Existing installation lacks an original snapshot
- **GIVEN** Eclipse Themes previously wrote a managed key
- **AND** no trustworthy pre-write value/absence snapshot exists
- **WHEN** the user requests restoration
- **THEN** the system follows an explicitly documented conservative recovery policy
- **AND** it does not clear unrelated settings to approximate a factory reset

### Requirement: Preference absence is semantically distinct from an explicit value

Any restoration mechanism that records original state SHALL preserve whether a managed key originally existed.

#### Scenario: Originally absent key
- **GIVEN** a managed preference key was absent before Eclipse Themes wrote it
- **WHEN** a trustworthy ownership snapshot is later used to restore that key
- **THEN** restoration returns the key to the absent state rather than writing a guessed/default value

### Requirement: PM5 listener correction remains bounded in state ownership

The PDE/Ant runtime correction SHALL preserve unrelated values and absence, bounded reset/failure behavior and the caller's thread. It SHALL NOT clear foreign nodes, guess prior values, swallow errors or add a general ownership/transaction mechanism. Changes beyond PM5 authorization SHALL return to the Decision Gate before implementation.

#### Scenario: Runtime correction beside unrelated state
- **GIVEN** related XML-comment settings coexist with unrelated present and absent preferences
- **WHEN** the corrected PDE/Ant application repeats or an application/persistence failure occurs
- **THEN** unrelated state remains value-equivalent, with absent keys absent
- **AND** no whole-node clearing, guessed restoration or speculative compensation occurs

#### Scenario: Unsupported PDE and Ant style keys are left untouched
- **GIVEN** stored or absent underline/strikethrough suffix keys unsupported by these consumers
- **WHEN** corrected application or bounded reset runs
- **THEN** those keys retain their values or absence instead of generating false color notifications
- **AND** only supported color/bold/italic destinations participate in current PDE/Ant management

#### Scenario: A PDE style equals the editor default
- **GIVEN** a supported Boolean style equals its effective default
- **WHEN** a theme applies that explicit style repeatedly
- **THEN** its instance override remains explicit and its typed live style is correct
- **AND** stop/reset still removes the declared managed override instead of reconstructing prior user state

### Requirement: Historical debug indication values are preserved without guessed repair

The system SHALL exclude `org.eclipse.ui.editors/currentIPIndication` and `org.eclipse.ui.editors/secondaryIPIndication` from ongoing theme color mutation and stop/reset cleanup. Without trustworthy original-state information it SHALL preserve their stored values or absence, including historical invalid RGB values, and SHALL document that correcting color mappings does not repair these historical boolean preferences.

#### Scenario: Newer valid user indication settings
- **GIVEN** the user has stored valid true/false indication settings
- **WHEN** a corrected theme is applied or theme management is stopped/reset
- **THEN** both settings remain exactly as stored
- **AND** no guessed boolean default or historical RGB value replaces them

#### Scenario: Unknown historical corruption
- **GIVEN** an indication key contains an RGB-shaped string from an unknown historical writer
- **AND** no trustworthy original-state snapshot exists
- **WHEN** a corrected theme is applied or stop/reset runs
- **THEN** the stored indication value remains unchanged
- **AND** the documented recovery limitation does not claim that the original boolean was restored

#### Scenario: Originally absent historical indication keys
- **GIVEN** both indication keys are absent
- **WHEN** application or stop/reset runs repeatedly
- **THEN** both keys remain absent rather than receiving explicit defaults

### Requirement: Corrected appearance keys participate in bounded reset

Stop/reset SHALL remove only declared managed instance overrides, including the corrected debug color keys and JDT source-hover color/system-default pair. It SHALL preserve unrelated keys and absent-key semantics, never clear a foreign node, and retain the existing contextual failure-reporting and success-commit behavior. Removal SHALL NOT be described as reconstructing pre-theme user values.

#### Scenario: Reset corrected mappings beside unrelated values
- **GIVEN** corrected debug color and hover overrides are present beside user indication settings and unrelated editor/JDT values
- **WHEN** theme management is stopped/reset successfully
- **THEN** corrected managed color and hover overrides become absent
- **AND** user indication and unrelated values remain value-equivalent
- **AND** no foreign preference node is cleared

#### Scenario: Reset with corrected keys already absent
- **GIVEN** the corrected appearance overrides are already absent
- **WHEN** stop/reset runs twice
- **THEN** they remain absent
- **AND** neither run writes guessed values to simulate original state

#### Scenario: Persistence fails during corrected-key reset
- **GIVEN** removal or persistence of a corrected managed preference fails
- **WHEN** stop/reset is attempted
- **THEN** the existing result identifies the affected integration, node, key or uncertain keys, operation, and cause
- **AND** success state is not committed
- **AND** failure does not trigger whole-node clearing or speculative indication-key repair

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

### Requirement: Theme fonts are owned value-exactly and restored conservatively

For each font definition it changes, the system SHALL record the exact value it wrote and the prior state (no stored value, or a
value). It SHALL update or restore a definition only while it still holds the recorded value, SHALL leave and report a definition the
user has since changed unless the user explicitly chose a different font on the page, SHALL change no other preference key, and
SHALL NOT report success before the write has been flushed.

#### Scenario: Restore untouched fonts
- **GIVEN** the plugin set the editor font over an absent value and nothing changed it
- **WHEN** the editor font is set back to Keep current and applied
- **THEN** the stored editor font value is removed

#### Scenario: User changed the font
- **GIVEN** the plugin set a font and the user then chose another under Colors and Fonts
- **WHEN** the editor font is set back to Keep current and applied
- **THEN** the user's font is left as it is and ownership is cleared
