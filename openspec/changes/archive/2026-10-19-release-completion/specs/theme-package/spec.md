## ADDED Requirements

### Requirement: A package may state an editor font and an interface font

A package MAY contain one `<fonts>` section with an optional `<editor>` and `<interface>` entry, each a `family` (at most 128
characters) and a `size` of 4 to 96 whole points. Each entry is a semantic intent; the platform font definitions it coordinates are
not part of the format. A package SHALL NOT embed or reference font files, and `<fonts>` SHALL NOT change
`schemaVersion`. An invalid entry SHALL give a `font` warning and be ignored; a repeated entry or section SHALL refuse the package.

#### Scenario: Both fonts declared
- **GIVEN** a package with `<fonts><editor family="Consolas" size="11"/><interface family="Segoe UI" size="9"/></fonts>`
- **WHEN** it is parsed
- **THEN** the theme carries both fonts and no finding is reported

#### Scenario: Invalid size
- **GIVEN** a package whose `<editor>` font has `size="2"`
- **WHEN** it is parsed
- **THEN** a `font` warning is reported, the editor font is ignored and the package is still accepted

#### Scenario: Repeated entry
- **GIVEN** a `<fonts>` section with two `<editor>` entries
- **WHEN** it is parsed
- **THEN** the package is refused

### Requirement: Fonts are applied only by explicit opt-in and never by applying colors

Fonts SHALL be applied only when the user has enabled *Apply theme fonts*. Applying a theme's colors SHALL NOT change a font. A
theme without fonts SHALL NOT set or reset any font. A requested family that is not installed SHALL be reported and the
corresponding font SHALL be left unchanged.

#### Scenario: Toggle off
- **GIVEN** *Apply theme fonts* is off and the theme has fonts
- **WHEN** the theme is applied
- **THEN** no font preference is written

#### Scenario: Missing family
- **GIVEN** the toggle is on and the editor font family is not installed
- **WHEN** the theme is applied
- **THEN** the outcome reports the family as not installed and the editor font is unchanged

### Requirement: A theme can be exported as a new package

Exporting SHALL write a deterministic, valid version 1 package to a user-chosen file, containing only the theme's metadata, editor
values, authored workbench values and fonts, validated with the package parser before the file appears. A legacy theme SHALL convert
the same way with the id `legacy.<slug of its name>` and the installed theme SHALL stay unchanged.

#### Scenario: Package round trip
- **GIVEN** an installed package with workbench colors and fonts
- **WHEN** it is exported and the file is imported again
- **THEN** the metadata, editor tokens, workbench colors and fonts are equal

#### Scenario: Legacy conversion
- **GIVEN** an installed legacy theme named `Atom One Dark`
- **WHEN** it is exported
- **THEN** the file is a package with id `legacy.atom-one-dark` and no `<workbench>` section
- **AND** the installed legacy file and identity are unchanged
