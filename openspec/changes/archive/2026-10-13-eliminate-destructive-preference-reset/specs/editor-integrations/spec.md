# Editor Integrations Delta

## MODIFIED Requirements

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
