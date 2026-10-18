# Preference UI Delta

## ADDED Requirements

### Requirement: The page operates on catalog records without managed paths

The preference UI SHALL present and manage `ThemeRecord`s: lifecycle actions branch on `record.origin()`, identity display and reselection use `record.logicalId()`, and visual details use `record.theme()` metadata. The UI SHALL NOT infer repository state from theme file presence or logical-ID prefixes, and SHALL NOT receive or construct absolute managed paths. Visible UX is unchanged.

#### Scenario: Installed marker follows record origin

- **GIVEN** bundled and installed records in the catalog
- **WHEN** the list renders and update/removal availability is decided
- **THEN** installed-only markers and actions follow `origin == INSTALLED`
