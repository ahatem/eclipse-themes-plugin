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

### Requirement: Progress bars follow the palette where the platform honors it
Every progress bar in coordinated dialogs, wizards and views, including bars nested inside progress indicators, SHALL take its track color from the background role and its bar color from a palette role (accent, or the part foreground inside views), using palette role ids only. Where the platform draws the bar natively and ignores those colors, the coverage documentation SHALL classify it SYSTEM/NATIVE.

#### Scenario: Wizard progress bar on a dark coordinated theme
- **GIVEN** a dark coordinated theme active on Windows with Eclipse 4.40 or 4.41
- **WHEN** a wizard or dialog shows its progress monitor
- **THEN** the progress bar track is drawn in the background role color instead of the operating-system light fill
- **AND** the bar is drawn in the accent role color

#### Scenario: Light coordinated theme on Windows
- **GIVEN** a light coordinated theme active on Windows with visual styles
- **WHEN** a progress bar is shown
- **THEN** it keeps the native Windows look
- **AND** the coverage documentation lists the light-family progress bar as SYSTEM/NATIVE

#### Scenario: Separator lines stay native
- **WHEN** the coverage documentation and stylesheets are inspected after this change
- **THEN** separator labels are listed as SYSTEM/NATIVE
- **AND** the stylesheets contain no rule targeting separator labels

#### Scenario: No literal colors and no preference writes
- **WHEN** the stylesheets are inspected after this change
- **THEN** the progress bar rule references only the family's published palette role ids
- **AND** the stylesheets contain no `IEclipsePreferences` or `ColorDefinition` rules
