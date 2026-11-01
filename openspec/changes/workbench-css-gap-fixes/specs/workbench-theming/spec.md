# Spec Delta

## ADDED Requirements

### Requirement: Coordinated workbench themes declare their appearance explicitly
Each coordinated workbench theme the plugin contributes SHALL declare its appearance through the theme contribution's explicit dark/light attribute: dark-family themes as dark, light-family themes as light. Theme ids SHALL keep containing `dark` for the dark family only, so releases that infer appearance from the id behave unchanged.

#### Scenario: Release that reads the explicit attribute
- **GIVEN** an Eclipse release whose theme engine honors the explicit dark/light attribute
- **WHEN** a dark-family coordinated theme becomes active
- **THEN** Eclipse treats it as dark because of the declared attribute
- **AND** a light-family theme is treated as light

#### Scenario: Release that infers appearance from the id
- **GIVEN** Eclipse 4.40 or 4.41, whose theme engine ignores the explicit attribute
- **WHEN** the plugin is installed
- **THEN** all four coordinated themes are registered and selectable as before
- **AND** the dark family is still treated as dark because its ids contain `dark`

### Requirement: Measured progress-bar and separator surfaces follow the palette
Where a runtime measurement on a verified combination shows that Eclipse CSS can recolor them, progress bars (including those nested in progress indicators) and separator labels in coordinated dialogs, wizards and views SHALL take colors from the published palette roles only. Surfaces without such a measurement SHALL remain classified SYSTEM/NATIVE in the coverage documentation.

#### Scenario: Wizard progress bar on a dark coordinated theme
- **GIVEN** a dark coordinated theme active on a verified combination where the measurement succeeded
- **WHEN** a wizard shows its progress monitor
- **THEN** the progress bar's track uses a palette role color instead of the operating-system default light fill

#### Scenario: Surface the engine cannot recolor
- **GIVEN** a surface whose measurement showed no change from the platform default
- **WHEN** the coverage documentation is read
- **THEN** that surface is listed as SYSTEM/NATIVE
- **AND** the stylesheets contain no rule claiming to style it

#### Scenario: No literal colors and no preference writes
- **WHEN** the stylesheets are inspected after this change
- **THEN** the new rules reference only the family's published palette role ids
- **AND** they contain no `IEclipsePreferences` or `ColorDefinition` rules
