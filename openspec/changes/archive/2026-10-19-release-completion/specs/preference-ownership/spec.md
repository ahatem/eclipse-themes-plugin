## ADDED Requirements

### Requirement: Theme fonts are owned value-exactly and restored conservatively

For each font definition it changes, the system SHALL record the exact value it wrote and the prior state (no stored value, or a
value). It SHALL update or restore a definition only while it still holds the recorded value, SHALL leave and report a definition the
user has since changed, SHALL change no other preference key, and SHALL NOT report success before the write has been flushed.

#### Scenario: Restore untouched fonts
- **GIVEN** the plugin set the editor font over an absent value and nothing changed it
- **WHEN** theme fonts are turned off
- **THEN** the stored editor font value is removed

#### Scenario: User changed the font
- **GIVEN** the plugin set a font and the user then chose another under Colors and Fonts
- **WHEN** theme fonts are turned off
- **THEN** the user's font is left as it is and ownership is cleared
