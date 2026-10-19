# Proposal

## Why

The current "restore Eclipse original colors" path clears entire preference nodes owned by Eclipse/JDT/CDT/PDE/Ant/debug plugins. That can delete unrelated user configuration, making the reset action a user-state-loss risk and blocking any credible ownership/recovery model.

## What Changes

- Stop whole-node clearing as a theme restoration mechanism.
- Introduce the behavioral boundary that Eclipse Themes may restore only preference keys it intentionally managed.
- Preserve unrelated keys in the same foreign preference nodes.
- Establish conservative behavior for existing installations that do not have historical snapshots of pre-plugin values.
- Add regression protection proving unrelated preferences survive the operation.

This change does **not** attempt to finish the future conflict-aware ownership journal for all theme application flows. It creates the minimum safe reset behavior and the capability contract that later R3 work will extend.

## Capabilities

### New Capabilities

- `preference-ownership`: constraints for theme-owned preference mutation/restoration and preservation of unrelated Eclipse/plugin state.

### Modified Capabilities

- `editor-integrations`: strengthen reset/stop-management behavior so integration cleanup cannot erase unrelated settings.
- `preference-ui`: reset/default operation must communicate conservative behavior and cannot imply restoration of unknown historical values that were never recorded.

## Impact

- Runtime bundle preference/reset code.
- Regression tests for preference-node contents and absent/present values.
- User-facing reset/default messaging if current text overpromises restoration.
- No OSGi bundle split and no full-workbench/CSS theming work.

## Migration and recovery note

Existing installations do not contain a complete snapshot of pre-plugin values. This change must not fabricate those values. Where the exact original value is unknowable, preservation/conservative removal based on explicitly managed keys is preferred to destructive node reset.
