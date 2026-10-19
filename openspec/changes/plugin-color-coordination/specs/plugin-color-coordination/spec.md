# Spec Delta

## Purpose

Defines which popup and hyperlink color preferences owned by other plug-ins follow a coordinated dark workbench theme, when they are
written, and how their values are derived from the coordinated workbench palette.

## ADDED Requirements

### Requirement: A declared set of popup and hyperlink keys follows a coordinated dark workbench theme

While workbench coordination is enabled and its dark coordinated theme is the active workbench theme, the system SHALL coordinate exactly
the declared keys: workbench hover, information and content-assist popup colors, Javadoc hover colors, JDT content-assist proposal and
parameter popup colors, the JDT Javadoc dark-mode flag, and workbench hyperlink colors. It SHALL NOT write any other key for this purpose.

#### Scenario: Dark coordinated theme applied
- **GIVEN** coordination is enabled, the selected theme belongs to the dark family and none of the declared keys has an instance value
- **WHEN** the user clicks Apply or OK and the workbench switch succeeds
- **THEN** each declared key holds the value derived from the published palette
- **AND** hover, content-assist and Javadoc popups and hyperlinks show those colors without a restart

#### Scenario: Light coordinated theme applied
- **GIVEN** coordination is enabled and the selected theme belongs to the light family
- **WHEN** the theme is applied
- **THEN** no declared key is written, and keys still holding the plugin's dark values are removed

#### Scenario: Editor-only mode
- **GIVEN** workbench coordination is disabled
- **WHEN** an editor theme is applied
- **THEN** no declared key is written

#### Scenario: JDT not installed
- **GIVEN** the JDT UI bundle is not installed
- **WHEN** a dark coordinated theme is applied
- **THEN** only the declared workbench keys are coordinated and the outcome is not degraded by the missing JDT keys

### Requirement: Popup and hyperlink colors derive deterministically from the published palette

Values SHALL be a pure function of the published workbench palette: popup backgrounds from the raised surface, popup foregrounds from the
foreground, the hyperlink from the accent, and the active hyperlink from the accent moved toward the foreground. Each popup foreground and
each hyperlink color SHALL reach a contrast ratio of at least 4.5:1 against the palette background, surface and raised surface.

#### Scenario: Same palette, same values
- **GIVEN** two applications that publish the same workbench palette
- **THEN** every declared key receives the same value in both

#### Scenario: Low-contrast accent
- **GIVEN** a palette whose accent moved toward the foreground would fall below 4.5:1 on one of those backgrounds
- **THEN** the derived active-hyperlink value is adjusted until it reaches 4.5:1 on all three

### Requirement: Popup coordination follows the workbench outcome and never reasserts

Popup keys SHALL be written only after the workbench switch postcondition holds, in the same Apply. When another Eclipse theme becomes
active, the system SHALL remove the keys that still hold its own values and SHALL NOT write any declared key again until the user
explicitly applies a coordinated theme.

#### Scenario: Workbench switch fails
- **GIVEN** a dark coordinated theme is applied and the workbench switch postcondition fails
- **THEN** no declared key is written by that Apply

#### Scenario: External Appearance choice
- **GIVEN** the plugin's dark popup values are in place and the user chooses another theme on the Appearance page
- **THEN** keys still holding the plugin's values are removed without a restart
- **AND** keys changed by anyone else keep their values
- **AND** no automatic path writes a declared key again

#### Scenario: Restart while another theme is active
- **GIVEN** the plugin still owns unchanged popup keys and another workbench theme is active at startup
- **THEN** startup removes those keys and writes nothing

### Requirement: Popup coordination is truthful in the outcome and diagnostics

The aggregate outcome SHALL NOT report popup coordination as complete when a declared key was skipped or failed to write. Diagnostics SHALL
list, per declared key, whether it is owned by the plugin, held by a user value, or not installed, without sending anything.

#### Scenario: A user value is kept
- **GIVEN** the user had set the hover background before the theme was applied
- **WHEN** a dark coordinated theme is applied
- **THEN** the outcome states that one popup color kept the user's value
- **AND** diagnostics show that key as held by a user value

#### Scenario: A write fails
- **GIVEN** a write or flush of a declared node fails
- **THEN** the outcome reports the failure and the keys that were not committed are not recorded as owned
