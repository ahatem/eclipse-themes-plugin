## MODIFIED Requirements

### Requirement: A small semantic palette is derived deterministically from the selected theme

The system SHALL convert the selected theme into `background`, `foreground`, `surface`, `surfaceRaised`, `border`, `mutedForeground`, `selection` and `accent` deterministically. A role authored by a theme package SHALL take that authored color verbatim. Every other role SHALL use, in order: a suitable authored non-syntax candidate (`sourceHoverBackground`, `currentLine`, `lineNumber`, `selectionBackground`), an OKLCH derivation from the background keeping its hue and chroma character, then a lightness-only readability correction; a derivation that depends on another role SHALL use that role's final value, authored or not. A theme without authored roles SHALL resolve exactly as before this change.

#### Scenario: Dark and light themes
- **WHEN** a dark theme and a light theme are resolved
- **THEN** the dark surfaces are lighter than the background and the light surfaces are darker than the background

#### Scenario: Suitable authored candidate is used
- **GIVEN** a theme whose authored current line is a small, low-chroma lightness step from its background
- **THEN** that color is the palette surface

#### Scenario: Unsuitable authored candidate is rejected
- **GIVEN** a theme whose authored current line is neon, saturated or far from its background
- **THEN** it is not used for any chrome role

#### Scenario: Tint is kept
- **GIVEN** a cool, warm or cream-tinted theme with no authored chrome candidates
- **THEN** the derived surface, raised and border colors stay within 30 degrees of the background's hue

#### Scenario: Unreadable authored text is corrected
- **GIVEN** a theme whose foreground (or line-number color) is nearly invisible on its background
- **THEN** the palette foreground (or muted foreground) has the same hue, a different lightness and the required contrast

#### Scenario: Explicit selection color is kept when usable
- **GIVEN** a theme with an explicit selection background visibly different from the background and readable under the foreground
- **THEN** the palette selection equals that color

#### Scenario: Package-authored surface and accent
- **GIVEN** a package authoring `surface` and `accent` that satisfy the contrast rules
- **WHEN** it is resolved
- **THEN** the palette surface and accent equal the authored colors
- **AND** `surfaceRaised` and `border` are derived from the authored surface

#### Scenario: Legacy theme unchanged
- **GIVEN** any bundled legacy theme
- **WHEN** it is resolved after this change
- **THEN** the palette equals the palette produced before this change

## ADDED Requirements

### Requirement: Authored workbench colors keep the readability floor

When a palette containing authored roles fails the coherence rules, the system SHALL correct only the failing text roles
(`foreground`, `mutedForeground`, `accent`) by lightness, keeping authored surfaces, and SHALL fall back to the family palette only if
the corrected palette still fails. Each corrected authored role SHALL be reported as a warning when the package is admitted. A package
SHALL NOT enable, disable or otherwise change workbench coordination intent.

#### Scenario: Unreadable authored accent
- **GIVEN** a dark package authoring an accent with 2:1 contrast on its surfaces
- **WHEN** it is imported and later resolved
- **THEN** admission reports a warning naming `accent`
- **AND** the resolved accent keeps its hue and reaches 4.5:1

#### Scenario: Package with workbench section, coordination off
- **GIVEN** workbench coordination is disabled
- **WHEN** a package with a workbench section is imported and applied
- **THEN** only the editor outcome exists and no workbench state is written
