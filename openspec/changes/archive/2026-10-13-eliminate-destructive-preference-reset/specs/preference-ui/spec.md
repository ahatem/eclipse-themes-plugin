# Preference UI Delta

## ADDED Requirements

### Requirement: Reset messaging does not overpromise historical restoration

The preference UI SHALL describe reset/stop-management behavior according to what the system can safely restore from available ownership information.

#### Scenario: No legacy snapshot exists
- **GIVEN** an existing installation has no trustworthy record of the user's pre-theme values
- **WHEN** the reset action is presented or completed
- **THEN** the UI does not claim that all original user preferences were reconstructed
- **AND** it communicates any conservative recovery limitation that materially affects the user
