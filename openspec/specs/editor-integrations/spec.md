# Editor Integrations

## Purpose

Define the currently advertised adapter-based integration surface.

## Requirements

### Requirement: Base text editor integration

The product SHALL support applying relevant theme colors to the Eclipse text editor preference surface when that integration is available.

#### Scenario: Base text editor is available
- **GIVEN** the Eclipse text editor integration is installed and available
- **WHEN** a theme is applied
- **THEN** the adapter can map relevant canonical tokens to that editor's supported appearance preferences

### Requirement: Optional editor integrations

The product SHALL discover and apply mappings for supported optional integrations when their target plugins are present, including the currently contributed JDT, CDT, PDE, Ant, and debug-console adapters.

#### Scenario: Optional target plugin not installed
- **GIVEN** an adapter targets a plugin that is not installed
- **WHEN** available adapters are determined
- **THEN** the missing integration does not make the whole theme catalog unusable

### Requirement: Integrations do not intentionally modify unrelated settings

An editor integration's theme operation SHALL be limited to appearance/highlighting preferences that belong to that integration's declared theme responsibility, including when theme management is stopped or reset.

#### Scenario: Theme application beside unrelated editor settings
- **GIVEN** the target preference node contains unrelated user settings
- **WHEN** a theme is applied or theme management is stopped
- **THEN** unrelated settings remain unchanged

#### Scenario: Stop theme management beside unrelated editor settings
- **GIVEN** a target preference node contains unrelated user settings
- **WHEN** Eclipse Themes stops or resets its managed appearance settings
- **THEN** unrelated settings remain unchanged
- **AND** no foreign preference node is cleared wholesale

### Requirement: PDE and Ant consume separated XML comments without listener corruption

On Eclipse 2024-09, PDE/Ant application SHALL deliver the selected XML-comment color and supported bold/italic flags to existing consumers without color corruption or suppressed listener exceptions. PM5 SHALL preserve the approved fallback and four independent model flags; it adds no unsupported underline/strikethrough rendering.

#### Scenario: Existing XML consumers receive explicit comment style
- **GIVEN** existing PDE/Ant XML consumers and a theme with different generic and XML comments
- **WHEN** the theme is applied
- **THEN** their comment attributes use the XML comment color and supported bold/italic flags
- **AND** neither black color substitution nor boolean-as-RGB listener errors occur

#### Scenario: Supported styles transition through the approved sparse fallback
- **GIVEN** an XML comment was explicitly styled by a rich theme
- **WHEN** a generic-only sparse theme and then a different rich theme are applied
- **THEN** existing XML consumers follow the one-way fallback and then the new explicit XML token
- **AND** supported false flags clear earlier true flags without color corruption or listener exceptions

#### Scenario: Bounded correction preserves unrelated integration behavior
- **GIVEN** unrelated preferences, absent keys, and other integrations
- **WHEN** the PDE/Ant runtime correction is applied repeatedly
- **THEN** unrelated state and existing mapping semantics remain unchanged
- **AND** no generic refresh/transaction mechanism, UI change, public API, mandatory dependency, internal API commitment or supported-platform expansion is inferred from PM5 approval

#### Scenario: Explicitly approved PDE live-store boundary
- **GIVEN** PM6 authorizes the single R4_33 PDE live-store accessor
- **WHEN** PDE mappings are applied
- **THEN** supported style changes reach the actual store as Boolean notifications, including repeated and default-valued flags
- **AND** the internal access stays isolated to the optional PDE integration, without a new public Eclipse Themes API

#### Scenario: PDE is absent or incompatible
- **GIVEN** the approved live-store accessor is unavailable or incompatible
- **WHEN** theme application attempts PDE integration
- **THEN** PDE failure is diagnostic and unrelated integrations can continue
- **AND** no ineffective separate-store fallback or PDE success claim is made

### Requirement: Debug instruction colors use color preferences

The base text editor integration SHALL write instruction-pointer RGB values to `org.eclipse.ui.editors/currentIPColor` and `org.eclipse.ui.editors/secondaryIPColor`. Theme application SHALL NOT write or remove the boolean `currentIPIndication` or `secondaryIPIndication` settings or other instruction-pointer presentation switches.

#### Scenario: Distinct debug colors beside user presentation settings
- **GIVEN** a theme specifies different current and secondary instruction-pointer colors
- **AND** the editor node contains explicit indication, highlight, text-style, and ruler settings
- **WHEN** the theme is applied
- **THEN** each color key contains its respective token's Eclipse RGB value
- **AND** every presentation setting remains unchanged

#### Scenario: Indication settings were absent
- **GIVEN** both indication settings are absent
- **WHEN** a theme is applied repeatedly
- **THEN** both indication settings remain absent
- **AND** neither receives an RGB string

### Requirement: Distinct source roles reach distinct editor preferences

Supported integrations SHALL apply generic comments to JDT/CDT comment preferences, XML comments to PDE/Ant comment preferences, static-final fields to JDT static-final highlighting, and constants to CDT enumerator highlighting. Supplying both roles in any pair SHALL preserve their independent colors and all supported style flags.

#### Scenario: Java and XML comments have different styles
- **GIVEN** generic and XML comments have different colors and bold, italic, underline, and strikethrough values
- **WHEN** a theme is applied to available JDT, CDT, PDE, and Ant integrations
- **THEN** Java/C++ generic comment mappings use the generic comment token
- **AND** PDE/Ant XML comment mappings use the XML comment token
- **AND** the applicable complete style belongs to the selected role

#### Scenario: Static-final field and constant styles differ
- **GIVEN** a theme supplies different static-final-field and constant colors/styles
- **WHEN** available JDT and CDT integrations are updated
- **THEN** `semanticHighlighting.staticFinalField` uses the static-final-field token
- **AND** `semanticHighlighting.enumerator` uses the constant token
- **AND** neither role acquires the other's style flags

### Requirement: JDT source hover is independent of text selection

When JDT is available, an explicit source-hover background SHALL set `sourceHoverBackgroundColor` and set `sourceHoverBackgroundColor.SystemDefault=false` in `org.eclipse.jdt.ui`. Omission SHALL remove only those two instance overrides, exposing editor/product defaults. Source-hover mapping SHALL NOT replace the text selection background or promise control of other hover surfaces.

#### Scenario: Explicit hover and selection backgrounds differ
- **GIVEN** a theme specifies distinct hover and selection backgrounds
- **WHEN** available text-editor and JDT integrations apply the theme
- **THEN** text selection receives the selection background
- **AND** JDT source hover receives the hover background with system-default selection disabled

#### Scenario: Explicit hover transitions to omitted hover
- **GIVEN** a previous theme wrote the two hover overrides
- **AND** an unrelated JDT preference and an explicit selection background are present
- **WHEN** a theme without an explicit hover background is applied
- **THEN** both hover overrides become absent, including the system-default override
- **AND** the new selection background is applied independently
- **AND** the unrelated preference remains unchanged

#### Scenario: JDT is unavailable
- **GIVEN** JDT is not installed
- **WHEN** a theme with an explicit source-hover background is applied
- **THEN** available text-editor selection mappings still apply independently
- **AND** no universal hover support is implied

### Requirement: Optional JDT semantic enablement survives sparse transitions

For inherited method invocation, inherited field, type argument, and deprecated member highlighting, explicit token presence SHALL set the role's `.enabled=true` and replace its color and four style flags. Absence SHALL retain `.enabled=false` while removing only that role's color and style overrides. Inherited fallback token availability SHALL NOT count as explicit presence. This SHALL be a bounded legacy-XML policy, not a universal future missing-token semantic.

#### Scenario: Rich to sparse beside a default-enabled role
- **GIVEN** a theme explicitly enables an optional role with color and style overrides
- **AND** the editor default enables that role
- **WHEN** a theme omitting the role is applied
- **THEN** the instance `.enabled` preference is explicitly false
- **AND** its color, bold, italic, underline, and strikethrough overrides are absent
- **AND** editor-default enablement does not re-enable the role

#### Scenario: Inherited color does not enable an omitted optional role
- **GIVEN** a sparse theme supplies an ancestor token that can provide a fallback color
- **WHEN** that theme is applied without an explicit optional role token
- **THEN** the optional role remains explicitly disabled
- **AND** mandatory semantic mappings retain their existing fallback and enablement behavior

#### Scenario: Sparse to rich replaces stale style
- **GIVEN** the role is disabled by a sparse theme
- **WHEN** a new rich theme explicitly defines it with a different color and style
- **THEN** the role is explicitly enabled with the new color and all four new flags
- **AND** false or unspecified new flags do not retain previous true overrides

#### Scenario: Sparse application is repeatable and scoped
- **GIVEN** a preference node contains unrelated semantic keys and unrelated user preferences
- **WHEN** the same sparse theme is applied twice
- **THEN** the four managed optional roles have the same explicit disabled state after each application
- **AND** unrelated keys remain value-equivalent, with absent unrelated keys still absent

### Requirement: Adapters consume only the visual definition

Every adapter SHALL continue to satisfy `apply(Theme, prefs)` using token resolution, theme type, and visual metadata only. No adapter SHALL read logical identity, managed file, or origin, and no `ThemeRecord` SHALL appear in adapter APIs. The extension schema's conceptual adapter contract is unchanged.

#### Scenario: Adapter input is a bare theme

- **GIVEN** any built-in or contributed adapter
- **WHEN** a theme is applied
- **THEN** the adapter receives the record's theme and produces the same preference mutations as before
