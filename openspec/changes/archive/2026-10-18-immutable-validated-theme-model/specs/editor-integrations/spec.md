# Editor Integrations Delta

## ADDED Requirements

### Requirement: Adapters consume only the visual definition

Every adapter SHALL continue to satisfy `apply(Theme, prefs)` using token resolution, theme type, and visual metadata only. No adapter SHALL read logical identity, managed file, or origin, and no `ThemeRecord` SHALL appear in adapter APIs. The extension schema's conceptual adapter contract is unchanged.

#### Scenario: Adapter input is a bare theme

- **GIVEN** any built-in or contributed adapter
- **WHEN** a theme is applied
- **THEN** the adapter receives the record's theme and produces the same preference mutations as before
