## ADDED Requirements

### Requirement: The page offers an opt-in fonts section, a format row and package export

The page SHALL show the selected theme's format, a Fonts section with one *Apply theme fonts* checkbox and a status line, and an
**Export as Package...** action. The status line SHALL name each requested font with its size, say the theme does not specify fonts,
or say a requested font is not installed. The checkbox intent SHALL be written only on Apply or OK, and *Stop managing* SHALL turn
fonts off and restore the fonts it changed.

#### Scenario: Legacy theme selected
- **GIVEN** a legacy theme is selected
- **THEN** the format reads "Legacy XML theme", the Fonts status reads "Theme does not specify fonts", and export is available as a conversion

### Requirement: Accepted warnings and failures are visible

Import and update success dialogs SHALL list up to five accepted warnings. A failed export SHALL state the reason and leave no
partial file. Wording about replacing installed content SHALL be format-neutral.

#### Scenario: Warnings on import
- **GIVEN** a package with an invalid font size
- **WHEN** it is imported
- **THEN** the success dialog lists the font warning
