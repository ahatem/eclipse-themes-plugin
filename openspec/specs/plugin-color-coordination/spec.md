# plugin-color-coordination Specification

## Purpose
Defines which color preferences owned by other plug-ins follow a coordinated dark workbench theme, when they are written, and how their
values are derived from the coordinated workbench palette.

## Requirements

### Requirement: A declared set of plug-in color keys follows a coordinated dark workbench theme

While workbench coordination is enabled and its dark coordinated theme is active, the system SHALL coordinate exactly the keys declared
in one table of colors that stock Eclipse Dark re-tunes and whose defaults are unreadable on a dark workbench (popups, hyperlinks,
status, Compare, EGit, annotations, JDT, Debug, Mylyn, Marketplace). A key SHALL be written only when its owning bundle is installed.
Syntax and editor-surface keys SHALL stay with the editor integrations.

#### Scenario: Dark coordinated theme applied
- **GIVEN** coordination is enabled, the selected theme belongs to the dark family and none of the declared keys has an instance value
- **WHEN** the user clicks Apply or OK and the workbench switch succeeds
- **THEN** each declared key whose owning bundle is installed holds the value derived from the published palette
- **AND** popups, hyperlinks and the listed plug-in surfaces show those colors without a restart

#### Scenario: Light coordinated theme applied
- **GIVEN** coordination is enabled and the selected theme belongs to the light family
- **WHEN** the theme is applied
- **THEN** no declared key is written, and keys still holding the plugin's dark values are removed

#### Scenario: Editor-only mode
- **GIVEN** workbench coordination is disabled
- **WHEN** an editor theme is applied
- **THEN** no declared key is written

#### Scenario: Owning bundle not installed
- **GIVEN** the bundle that owns some declared keys is not installed
- **WHEN** a dark coordinated theme is applied
- **THEN** those keys are not written and the outcome is not degraded by their absence

### Requirement: Plug-in colors derive deterministically from the published palette

Values SHALL be a pure function of the published palette and SHALL NOT depend on a theme's name. Surface colors come from the palette's
raised surface, background or border; text colors from its foreground, muted or accent. Status colors use fixed hues corrected to 4.5:1
on the palette backgrounds. Status tints pull the background toward a status hue while the foreground stays at 4.5:1 on top.

#### Scenario: Same palette, same values
- **GIVEN** two applications that publish the same workbench palette
- **THEN** every declared key receives the same value in both

#### Scenario: Low-contrast accent
- **GIVEN** a palette whose accent moved toward the foreground would fall below 4.5:1 on one of the three backgrounds
- **THEN** the derived active-hyperlink value is adjusted until it reaches 4.5:1 on all three

#### Scenario: Palette with little contrast to spare
- **GIVEN** a dark palette whose foreground has little contrast margin over its background
- **THEN** each status tint still keeps the foreground at least 4.5:1 on top of it, or equals the background

### Requirement: Plug-in color coordination follows the workbench outcome and never reasserts

Declared keys SHALL be written only after the workbench switch postcondition holds, in the same Apply. When another Eclipse theme
becomes active, the system SHALL remove the keys that still hold its own values and SHALL NOT write any declared key again until the
user explicitly applies a coordinated theme.

#### Scenario: Workbench switch fails
- **GIVEN** a dark coordinated theme is applied and the workbench switch postcondition fails
- **THEN** no declared key is written by that Apply

#### Scenario: External Appearance choice
- **GIVEN** the plugin's dark values are in place and the user chooses another theme on the Appearance page
- **THEN** keys still holding the plugin's values are removed without a restart
- **AND** keys changed by anyone else keep their values
- **AND** no automatic path writes a declared key again

#### Scenario: Restart while another theme is active
- **GIVEN** the plugin still owns unchanged keys and another workbench theme is active at startup
- **THEN** startup removes those keys and writes nothing

### Requirement: Plug-in color coordination is truthful in the outcome and diagnostics

The aggregate outcome SHALL NOT report plug-in color coordination as complete when a declared key failed to write; it is reported as
degraded. The preference page SHALL show only aggregate counts. Per-key state (owned by the plugin, kept as the user's, released after an outside change)
SHALL appear only in the copyable debug text, and nothing SHALL be sent anywhere.

#### Scenario: A user value is kept
- **GIVEN** the user had set the hover background before the theme was applied
- **WHEN** a dark coordinated theme is applied
- **THEN** the page states how many plug-in colors kept the user's values
- **AND** the debug text shows that key as held by a user value

#### Scenario: A write fails
- **GIVEN** a write or flush of a declared node fails
- **THEN** the outcome is degraded and the keys that were not committed are not recorded as owned
