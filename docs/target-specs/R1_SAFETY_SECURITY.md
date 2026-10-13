# R1 Target Spec — Safety and Security Foundation

Traceability: Q1, Q2, Q6, Q7, Q8, Q10, Q16, B3.

## Requirements

### Requirement: Foreign preference nodes are never cleared wholesale
Eclipse Themes SHALL modify only explicitly managed appearance/highlighting keys in preference nodes owned by Eclipse or another plugin.

#### Scenario: Stop theme management beside unrelated settings
- GIVEN a foreign preference node contains managed theme keys and unrelated user/plugin keys
- WHEN Eclipse Themes stops or resets its management
- THEN unrelated keys remain unchanged
- AND the foreign node is not cleared wholesale

### Requirement: XML parsing rejects external resolution
Theme parsing SHALL reject DOCTYPE/external-entity behavior and SHALL NOT resolve local/network external entities.

#### Scenario: Theme contains an external entity
- GIVEN an imported XML theme declares an external entity
- WHEN the theme is parsed
- THEN parsing fails with an actionable diagnostic
- AND no external resource is read

### Requirement: Invalid theme structure is rejected explicitly
The parser SHALL validate required theme structure/identity and report malformed values rather than silently accepting arbitrary roots or misleading fallbacks.

#### Scenario: Arbitrary XML root
- GIVEN an XML document that is not a supported theme document
- WHEN import validation runs
- THEN the candidate is rejected
- AND no empty-identity theme is committed

### Requirement: Editor mappings preserve distinct semantics
Legacy aliases and editor preference mappings SHALL not overwrite semantically distinct roles merely because they share historical names.

#### Scenario: Distinct source-hover and selection roles
- GIVEN a theme specifies different values for roles that map to distinct Eclipse concepts
- WHEN the theme is resolved and applied
- THEN one role does not silently overwrite the other

### Requirement: Semantic enablement transitions are deterministic
A missing optional semantic token SHALL not accidentally restore editor-default highlighting when Eclipse Themes intentionally disables that semantic role.

#### Scenario: Rich theme to sparse theme
- GIVEN the current theme explicitly controls a semantic role
- WHEN a sparse theme without that role is applied
- THEN the resulting enabled/disabled state follows the documented policy
- AND stale style properties do not re-enable the role implicitly

### Requirement: Safety defects have regression protection
Every reproduced R1 defect SHALL receive focused automated coverage at the narrowest practical layer.

#### Scenario: Previously reproduced defect
- GIVEN a defect has a deterministic reproducer
- WHEN the fix is implemented
- THEN an automated regression test fails without the fix and passes with it
