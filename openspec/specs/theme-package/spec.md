# Theme Package Specification

## Purpose

Define the declarative XML theme package format, its metadata, validation rules, and compatibility behavior.

## Requirements

### Requirement: A theme package is one declarative XML document with the `.ectheme` extension

The system SHALL recognise a theme package only as a single file whose final extension is `.ectheme`, compared case-insensitively
without the default locale, containing one XML document whose root element is `themePackage`. The format SHALL be decided by the
extension alone. A package SHALL NOT contain or reference executable code, stylesheets, scripts, archives or external resources, and
no part of a package SHALL gain execution semantics.

#### Scenario: Package file is recognised
- **GIVEN** a file `Nightfall.ECTHEME` whose root element is `themePackage`
- **WHEN** it is classified for import
- **THEN** it is treated as a theme package

#### Scenario: Wrong root under the package extension
- **GIVEN** a `.ectheme` file whose root element is `colorTheme`
- **WHEN** it is parsed
- **THEN** parsing fails with an error finding identifying the root element
- **AND** it is not re-interpreted as a legacy theme

#### Scenario: Package content under the legacy extension
- **GIVEN** a `.xml` file whose root element is `themePackage`
- **WHEN** it is parsed
- **THEN** the legacy root-element refusal applies unchanged

### Requirement: Package XML uses the existing secure parsing policy

Package parsing SHALL apply every theme XML security requirement — DOCTYPE refusal, no external resolution, fail-closed configuration
— identically to legacy theme parsing, for every source and entry point.

#### Scenario: Package with an external entity
- **GIVEN** a `.ectheme` document containing a DOCTYPE with an external entity
- **WHEN** parsing is attempted
- **THEN** parsing fails before the entity is resolved and no theme is returned

### Requirement: Package documents are size-bounded before parsing

A package document larger than 256 KiB SHALL be refused before XML parsing with an error finding, leaving all managed state unchanged.

#### Scenario: Oversized package
- **GIVEN** a `.ectheme` file of 300 KiB
- **WHEN** it is imported
- **THEN** import is refused with a finding stating the size limit
- **AND** no parse is attempted and no managed file, binding or catalog record changes

### Requirement: Schema version is explicit and only version 1 is accepted

The root SHALL carry `schemaVersion`. A value that is absent or not a decimal integer SHALL refuse the document. An integer other
than `1` SHALL refuse the document with a finding stating that a newer Eclipse Themes is required. No part of a refused document is
admitted.

#### Scenario: Missing schema version
- **GIVEN** a package root without `schemaVersion`
- **WHEN** it is parsed
- **THEN** parsing fails with an error identifying `schemaVersion`

#### Scenario: Newer schema version
- **GIVEN** a package with `schemaVersion="2"`
- **WHEN** it is parsed
- **THEN** parsing fails with an error stating the version is unsupported
- **AND** no partially interpreted theme is returned

### Requirement: Unknown package content is handled deterministically

Unknown direct children of the root and unknown children of `workbench` SHALL be ignored with one warning each. Unknown attributes SHALL
be ignored without a finding. Ignored content SHALL remain in the stored file bytes unchanged. A feature that older readers must not
ignore SHALL require a new schema version; v1 defines no capability negotiation.

#### Scenario: Future optional section
- **GIVEN** a version 1 package containing an unknown `<fonts>` element
- **WHEN** it is imported
- **THEN** the package is admitted with a warning naming `fonts`
- **AND** the installed file contains the exact imported bytes, including the `<fonts>` element

#### Scenario: Unknown attribute
- **GIVEN** a package root with an attribute the format does not define
- **WHEN** it is parsed
- **THEN** it is accepted and no finding is produced for the attribute

### Requirement: Package metadata follows an explicit field policy

The root SHALL require `id` (lower-case dotted identifier matching `[a-z0-9]+(\.[a-z0-9][a-z0-9-]*)+`, at most 128 characters), a
non-blank `name` and `appearance`. `author`, `description`, `website` and `version` (display text, at most 64 characters) are
optional. `website` follows the existing website validation and launch rules. No revision, digest, compatibility range or legacy
`ect-id` is part of the format.

#### Scenario: Invalid package id
- **GIVEN** a package whose `id` is `My Theme`
- **WHEN** it is parsed
- **THEN** parsing fails with an error identifying `id`

#### Scenario: Overlong version
- **GIVEN** a package whose `version` exceeds 64 characters
- **WHEN** it is parsed
- **THEN** the package is accepted without a version and a warning identifies `version`

### Requirement: The package id is a source claim, never catalog identity

A package `id` SHALL be retained as untrusted source metadata. It SHALL NOT become or derive the catalog logical identity, SHALL NOT
cause an import to replace, merge with or deduplicate against any record, SHALL NOT participate in equality, and SHALL NOT be used as
the legacy source ID for selected-intent recovery or remote preview.

#### Scenario: Two packages claim the same id
- **GIVEN** an installed package claiming `io.github.example.nightfall`
- **WHEN** a different package claiming the same id is imported
- **THEN** a new independent record with its own logical identity is created
- **AND** the existing record is unchanged

### Requirement: Appearance is a required binary classification

`appearance` SHALL be `dark` or `light`, compared case-insensitively; any other or absent value SHALL refuse the document. The package
SHALL be classified with that appearance for editor and workbench purposes. No automatic, high-contrast or unspecified appearance
exists; the operating system's high-contrast mode continues to make workbench theming unavailable while editor theming applies.

#### Scenario: Unsupported appearance
- **GIVEN** a package with `appearance="auto"`
- **WHEN** it is parsed
- **THEN** parsing fails with an error identifying `appearance`

### Requirement: The editor section uses the legacy token rules and must be complete at the base

A package SHALL contain exactly one `editor` element. Its children SHALL be interpreted with exactly the legacy token vocabulary,
aliases, canonical precedence, colour grammar, style flags and warnings. The section SHALL supply valid `background` and `foreground`
tokens; otherwise the document is refused. Packages SHALL NOT receive the legacy hard-coded root default colours.

#### Scenario: Legacy alias in a package
- **GIVEN** a package editor section containing `singleLineComment`
- **WHEN** it is parsed
- **THEN** the comment role resolves exactly as it would in a legacy theme with the same element

#### Scenario: Missing base token
- **GIVEN** a light package whose editor section omits `background`
- **WHEN** it is parsed
- **THEN** parsing fails with an error identifying the missing `background` token
- **AND** no dark default background is substituted

#### Scenario: Missing or repeated editor section
- **WHEN** a package has no `editor` element or more than one
- **THEN** parsing fails with an error identifying the section

### Requirement: The workbench section declares optional palette roles

A package MAY contain one `workbench` element whose children name palette roles `background`, `foreground`, `surface`,
`surfaceRaised`, `border`, `mutedForeground`, `selection` and `accent`, each with a `color` in the legacy colour grammar. A repeated
role or repeated section SHALL refuse the document. A role with a missing or invalid colour SHALL be treated as not authored, with a
warning. Style attributes on roles SHALL be ignored.

#### Scenario: Partial workbench section
- **GIVEN** a package authoring only `surface` and `accent`
- **WHEN** it is parsed
- **THEN** exactly those two roles are authored and the others remain unauthored

#### Scenario: Repeated role
- **GIVEN** a workbench section with two `accent` elements
- **WHEN** it is parsed
- **THEN** parsing fails with an error identifying `accent`

### Requirement: Provenance fields are declared in one optional element

A package MAY contain one `provenance` element with `license` (SPDX expression or `LicenseRef-` text, not checked against a license
list), `copyright` (the rights holder, distinct from `author`) and `source` (an upstream URL under the website launch rules). A
repeated `provenance` element SHALL refuse the document.

#### Scenario: Repeated provenance
- **GIVEN** a package with two `provenance` elements
- **WHEN** it is parsed
- **THEN** parsing fails with an error identifying `provenance`

### Requirement: Missing provenance is never assumed

Absent provenance SHALL NOT block admission of a user import and SHALL be presented as not stated. The product SHALL NOT present the
plugin's own license as covering a package.

#### Scenario: Package without provenance
- **GIVEN** a valid package with no `provenance` element
- **WHEN** it is imported
- **THEN** it is admitted without a provenance finding
- **AND** its license is presented as not stated

#### Scenario: Author differs from copyright holder
- **GIVEN** a package whose author is a porter and whose copyright names the original designer
- **WHEN** its details are shown
- **THEN** both values are presented separately

### Requirement: A package may state an editor font and a workbench font

A package MAY contain one `<fonts>` section with an optional `<editor>` and `<workbench>` entry, each a `family` (at most 128
characters) and a `size` of 4 to 96 whole points. A package SHALL NOT embed or reference font files, and `<fonts>` SHALL NOT change
`schemaVersion`. An invalid entry SHALL give a `font` warning and be ignored; a repeated entry or section SHALL refuse the package.

#### Scenario: Both fonts declared
- **GIVEN** a package with `<fonts><editor family="Consolas" size="11"/><workbench family="Segoe UI" size="9"/></fonts>`
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

### Requirement: Fonts follow the user's per-target choice and never change by applying colors alone

The editor font and the workbench font SHALL each be *Keep current*, *Use theme* or *Custom*, defaulting to Keep. A theme's fonts
SHALL be used only for *Use theme*; a custom font is a user preference and SHALL NOT be written to a package. A theme without
fonts SHALL NOT set a font. A requested family that is not installed SHALL be reported and the font left unchanged.

#### Scenario: Keep current
- **GIVEN** a target is set to Keep current and the theme has fonts
- **WHEN** the theme is applied
- **THEN** no font preference is written for that target

#### Scenario: Missing family
- **GIVEN** the editor font is Use theme and its family is not installed
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
