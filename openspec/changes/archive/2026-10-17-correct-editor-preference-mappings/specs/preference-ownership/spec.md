# Preference Ownership Delta

PM1 and PM3 Option A approved by the maintainer on 2026-10-13; see the resolution record in `proposal.md`. These are approved deltas for this change, not promoted to canonical specs. PM5 Option A was approved on 2026-10-17 for the bounded PDE/Ant runtime correction. PM1–PM4 remain unchanged; PM6 Option A now explicitly authorizes only the isolated optional R4_33 PDE live-store accessor. The approved behavior remains unchanged, with public/upstream replacement deferred to R6.

## ADDED Requirements

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
