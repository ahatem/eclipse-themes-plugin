## MODIFIED Requirements

### Requirement: A declared set of plug-in color keys follows a coordinated dark workbench theme

While workbench coordination is enabled and its dark coordinated theme is active, the system SHALL coordinate exactly the keys declared
in one table of plug-in colors whose defaults are unreadable on a dark workbench and that the owning plug-in reads. This covers popups,
hyperlinks, status, Compare, EGit, annotations, JDT, Debug, Mylyn, Marketplace and Spring Tools. A key qualifies whether its dark value
would otherwise come from stock Eclipse Dark, from a stylesheet the owning plug-in binds to the stock dark theme id only, or from no
stylesheet at all. A key SHALL be written only when its owning bundle is installed. Syntax and editor-surface keys SHALL stay with the
editor integrations. Keys that no installed code reads SHALL NOT be declared.

#### Scenario: Dark coordinated theme applied
- **GIVEN** coordination is enabled, the selected theme belongs to the dark family and none of the declared keys has an instance value
- **WHEN** the user clicks Apply or OK and the workbench switch succeeds
- **THEN** each declared key whose owning bundle is installed holds the value derived from the published palette
- **AND** popups, hyperlinks and the listed plug-in surfaces show those colors without a restart

#### Scenario: Plug-in dark values bound to the stock dark theme only
- **GIVEN** an installed plug-in ships dark values for its color definitions only in a stylesheet bound to the stock dark theme id, and those definitions are declared
- **WHEN** a dark coordinated theme is applied and none of those keys has an instance value
- **THEN** those keys hold values derived from the published palette instead of the plug-in's light defaults

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
