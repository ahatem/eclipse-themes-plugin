# R7 Research Spec — Full-Workbench Theming Feasibility

Traceability: Part 5A, F1-F3, F5 and supported-version/platform-limit sections.

This is a research acceptance contract, not a promise to ship universal theming.

## Questions that must be answered with evidence

### Requirement: Public workbench theme switching is proven on the chosen matrix
The spike SHALL demonstrate `IThemeEngine` registration/selection/application behavior and reversion using public APIs on declared Eclipse/OS combinations.

#### Scenario: Switch registered workbench theme
- WHEN the spike activates a supported registered theme
- THEN representative workbench surfaces update and can be returned to the prior theme according to observed platform behavior

### Requirement: CSS coverage limits are documented
The spike SHALL classify representative surfaces as CSS-stylable, Java/integration-dependent, HTML/browser-owned, third-party/custom-drawn, or native/OS-owned.

#### Scenario: Native file dialog
- THEN the coverage report does not claim arbitrary Eclipse CSS control over OS-native chrome

### Requirement: Dynamic CSS lifecycle limitations are measured
The spike SHALL test create/update/delete/restart behavior for dynamically registered CSS/theme resources and explicitly account for missing public unregister/replace APIs.

#### Scenario: Imported workbench CSS package is removed
- THEN the report states what can be disabled live and what cleanup requires restart or upstream API work

### Requirement: Preference and CSS overlap is characterized
The spike SHALL identify preferences/defaults touched by both CSS and editor contributors and define sequencing/conflict implications.

#### Scenario: CSS changes an editor default also managed by an adapter
- THEN the report records effective behavior and restoration risk

### Requirement: Platform differences are evidence-based
The spike SHALL cover Windows, Linux/GTK, and macOS on a bounded matrix, with GTK3/GTK4 and X11/Wayland treated separately when relevant.

#### Scenario: Behavior differs by backend
- THEN support documentation records the difference rather than generalizing one platform result

### Requirement: High-contrast policy is tested
The spike SHALL verify that proposed workbench integration can respect high-contrast/accessibility choices.

#### Scenario: OS high-contrast enabled
- THEN the recommended architecture does not silently force incompatible theme colors

### Requirement: Coexistence with other appearance managers is investigated
The spike SHALL test or document behavior when Eclipse built-in theme selection and at least one external appearance solution can change overlapping state.

#### Scenario: External appearance plugin becomes owner
- THEN proposed coordination avoids listener rewrite loops and can yield/report conflict

## Exit decision

The spike ends with a go/no-go architecture decision, declared MVP surfaces, unsupported surfaces, restart requirements, and the minimum tested Eclipse/OS matrix. No universal "theme every pixel" claim is allowed.
