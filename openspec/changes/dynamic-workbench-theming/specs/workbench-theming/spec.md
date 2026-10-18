# Workbench Theming

## Purpose

Define opt-in coordination of Eclipse workbench colors with the selected Eclipse Color Theme, using plugin-contributed static CSS and a
runtime-published semantic palette, within a published coverage boundary.

## ADDED Requirements

### Requirement: Workbench coordination is explicit opt-in and independent of the editor

The system SHALL change the Eclipse workbench theme only after the user enables workbench coordination and commits it with Apply or OK.
Importing or selecting a theme, including a legacy XML theme, SHALL remain editor-only until then. Highlighting a theme SHALL NOT change the
workbench, and Cancel SHALL leave the stored coordination intent unchanged.

#### Scenario: Editor-only default
- **GIVEN** coordination was never enabled
- **WHEN** the user applies any theme
- **THEN** editor preferences are applied and no workbench theme selection, palette value or workbench ownership record is written

#### Scenario: Imported legacy theme without opt-in
- **GIVEN** a legacy XML theme was imported and coordination is off
- **WHEN** the theme is applied
- **THEN** only the editor outcome exists

#### Scenario: Cancel
- **GIVEN** the user changed the coordination checkbox
- **WHEN** the user cancels the preference dialog
- **THEN** the stored intent and the active workbench theme are unchanged

### Requirement: A small semantic palette is derived deterministically from the selected theme

The system SHALL derive exactly `background`, `foreground`, `surface`, `surfaceRaised`, `border`, `mutedForeground` and `selection` from the selected theme's background, foreground, explicit selection background and light/dark type, deterministically. No syntax token SHALL influence it and no accent SHALL be derived. Missing source colors, or foreground/background contrast below 3.0, SHALL yield the family's fallback palette.

#### Scenario: Dark and light themes
- **WHEN** a dark theme and a light theme are resolved
- **THEN** the dark surfaces are lighter than the background and the light surfaces are darker than the background

#### Scenario: Explicit selection color is kept when usable
- **GIVEN** a theme with an explicit selection background readable against its foreground
- **THEN** the palette selection equals that color

#### Scenario: Unsafe or missing source colors
- **GIVEN** a theme whose foreground and background nearly match, or lacks either
- **THEN** the family fallback palette is produced

### Requirement: Workbench colors come from static declarative CSS reading published symbolic colors

The system SHALL style the workbench through two declaratively contributed CSS families (dark and light) whose colors are symbolic references to plugin-owned, non-editable color ids. It SHALL NOT register themes or stylesheets at runtime, generate CSS, or import CSS from theme files. It SHALL publish palette values to the current Colors-and-Fonts registry before the theme-ID change that makes Eclipse re-read them.

#### Scenario: Same family, different theme
- **GIVEN** two different dark themes are applied one after the other with coordination on
- **THEN** the workbench surfaces visibly take each theme's own derived palette
- **AND** the active workbench theme id differs after each apply

#### Scenario: Light to dark
- **WHEN** a dark theme is applied after a light theme
- **THEN** the dark CSS family is active with the dark theme's palette

#### Scenario: Unchanged palette
- **GIVEN** the matching family is active and the registry holds the derived palette
- **WHEN** the same theme is applied again
- **THEN** no workbench theme switch is performed

#### Scenario: Palette ids are hidden and not trusted from storage
- **THEN** the palette ids are not offered on the Colors and Fonts page and a stored value does not replace the fallback at startup

#### Scenario: Partial publication
- **GIVEN** publishing a palette value fails
- **THEN** previously held values are restored, no switch is attempted and the workbench outcome is FAILED

### Requirement: Capability is classified, and confidence is measured per combination

The system SHALL classify workbench theming as VERIFIED, EXPERIMENTAL or UNAVAILABLE. VERIFIED SHALL require an exact match of OS, windowing system and the theme bundle's normalized major.minor.micro version in a measured table; a passing but unmeasured combination is EXPERIMENTAL. The table is not an execution allow-list.

#### Scenario: Measured combination
- **GIVEN** win32, win32 and theme bundle 0.15.100.vTIMESTAMP, or 0.15.200.vTIMESTAMP
- **THEN** the state is VERIFIED

#### Scenario: Newer micro version
- **GIVEN** theme bundle 0.15.300
- **THEN** the state is EXPERIMENTAL, not VERIFIED

#### Scenario: Other platform
- **GIVEN** a compatible engine on linux/gtk or macosx/cocoa
- **THEN** the state is EXPERIMENTAL

#### Scenario: Hard gates
- **WHEN** the engine service is absent, the engine class cannot link, an own theme is not enumerated, or the OS high-contrast mode is active
- **THEN** the state is UNAVAILABLE with the reason NO_ENGINE, LINKAGE_ERROR, THEME_NOT_FOUND or HIGH_CONTRAST respectively
- **AND** POSTCONDITION_FAILED is a further UNAVAILABLE reason produced after a failed apply, and a user disabling the feature is not a capability state
- **AND** editor theming is unaffected

### Requirement: Experimental workbench theming is off until the user opts in

For EXPERIMENTAL combinations the system SHALL not change the workbench theme until the user has explicitly enabled experimental workbench
theming for that exact combination, and the UI SHALL name the Eclipse version, OS and windowing system being tried. An opt-in SHALL NOT carry
over to a different combination.

#### Scenario: No opt-in
- **GIVEN** an EXPERIMENTAL combination with no opt-in for it
- **WHEN** a theme is applied
- **THEN** the workbench outcome is NOT_REQUESTED and nothing is selected

### Requirement: Workbench ownership is minimal, recorded after success, and never overrides a newer external choice

The system SHALL capture the previously active workbench theme id and the prior saved theme-id state (including absence) before it first takes
ownership, and SHALL persist the ownership record only after the switch postcondition holds. It SHALL NOT clear or snapshot foreign preference
nodes, and SHALL NOT run a listener that reasserts its theme.

#### Scenario: Disable restores the prior theme
- **GIVEN** the plugin owns the active workbench theme and the prior theme is available
- **WHEN** coordination is disabled and committed
- **THEN** the prior theme is active, its persisted choice matches the original (including absence), and palette ids return to the fallback

#### Scenario: External Appearance choice wins
- **GIVEN** coordination is enabled and the user then chooses another Eclipse theme on the Appearance page
- **THEN** the plugin does not reassert its theme at any later time
- **AND** disabling coordination later does not select the old snapshot
- **AND** the page reports that coordination is no longer the effective state

#### Scenario: Prior theme unknown or gone
- **WHEN** the plugin is asked to relinquish and cannot restore a prior theme
- **THEN** it forces no theme, reports YIELDED and offers the Appearance route

### Requirement: A failed workbench apply never overwrites a newer choice and is never reported as success

If a workbench apply fails after a selection attempt, the system SHALL restore the previous theme only when the previous theme is known, the
attempted theme is still active and no other change occurred; otherwise it SHALL yield and report. The failing combination SHALL not be retried
in the same session, an unconfirmed experimental opt-in SHALL be cleared, and the editor outcome SHALL remain valid.

#### Scenario: Postcondition failure
- **GIVEN** the engine reports a different active theme than the one selected
- **THEN** the outcome is FAILED with reason POSTCONDITION_FAILED and the manual Appearance route is offered

### Requirement: Startup republishes from the selected theme, never from stored colors

When coordination is enabled, available and its theme is the active workbench theme, startup SHALL derive the palette from the selected theme
and republish it when the registry does not already hold it. It SHALL do nothing when the active workbench theme is not ours.

#### Scenario: Restart with persistence absent
- **GIVEN** the registry starts at the family fallback
- **THEN** the palette is republished from the selected theme and a single switch refreshes the workbench

### Requirement: Coverage and diagnostics are truthful and local

The system SHALL state that editor coloring is supported and core workbench surfaces are coordinated while native or system-controlled surfaces may remain unchanged, and SHALL NOT claim whole-IDE coverage. It SHALL provide a copyable, local-only diagnostics block describing the environment, capability, selected theme and observed workbench theme.

#### Scenario: Copy diagnostics
- **WHEN** the user requests diagnostics
- **THEN** a text block with the listed fields is available to copy and nothing is sent
