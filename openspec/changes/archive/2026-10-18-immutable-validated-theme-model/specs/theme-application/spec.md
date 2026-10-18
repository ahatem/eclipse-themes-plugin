# Theme Application Delta

## ADDED Requirements

### Requirement: Selection resolves to catalog records; application receives themes

Selected intent SHALL remain the persisted logical identity (`SELECTED_THEME_LOGICAL_ID` with legacy source-ID fallback unchanged) and SHALL resolve to a `ThemeRecord`. Application SHALL dispatch `record.theme()` through the unchanged `EclipseThemeAdapter.apply(Theme, prefs)` path. No persistence keys migrate; successful-applied-state remains R3.

#### Scenario: Apply dispatches the record's theme

- **GIVEN** a selected catalog record
- **WHEN** Apply or OK is requested
- **THEN** adapters receive the record's immutable theme with unchanged mappings
