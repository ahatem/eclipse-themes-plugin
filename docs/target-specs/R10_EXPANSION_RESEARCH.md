# R10 Research Spec — Expansion Boundaries

Traceability: F4 plus Part 5 limits and long-term roadmap.

## Candidate areas

- arbitrary workbench CSS author/import lifecycle after public-API evidence;
- participating icon packs;
- theme export/share/authoring tools;
- OS-following profiles;
- broader WTP/TM4E/third-party integrations;
- upstream Eclipse Platform changes for missing public APIs.

## Non-promises

### Requirement: Universal icon interception is not assumed
The project SHALL NOT advertise arbitrary third-party icon replacement without a supported public/cooperative mechanism and lifecycle evidence.

#### Scenario: Third-party plugin owns an icon contribution
- THEN lack of a universal public replacement contract is treated as a limitation, not silently bypassed with invasive hooks in stable default code

### Requirement: Native chrome is outside guaranteed theming scope
The project SHALL NOT promise arbitrary recoloring of native menus, file dialogs, title bars, or OS-owned chrome.

#### Scenario: Platform-native title bar
- THEN coverage documentation identifies it as OS/platform controlled unless a supported API proves otherwise

### Requirement: Invasive hooks remain isolated experiments
Weaving/internal renderer interception/platform patches SHALL remain outside the stable default path unless a reviewed architecture decision justifies them.

#### Scenario: Public API cannot achieve a requested surface
- THEN the project prefers an explicit limitation or upstream proposal over unbounded hidden internal coupling
