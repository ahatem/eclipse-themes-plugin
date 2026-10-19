# Architecture Guardrails

## Current shape

The repository currently has one runtime bundle plus feature/target/update-site modules. This is acceptable for the present size. Logical boundaries matter before physical OSGi splitting.

Current runtime flow:

`Preference UI → manager/repository/registry/service → parser/models + adapters → Eclipse preference nodes`

The workbench theming surface is bounded to four static, opt-in Eclipse e4 CSS themes that read an eight-role palette converted deterministically from the selected theme (R8a); see [workbench theming compatibility](WORKBENCH_THEMING_COMPATIBILITY.md). No theme or stylesheet is registered at runtime and no imported content reaches CSS.

## Target logical layers

```text
UI
  ↓
Application coordinator / use cases
  ↓
Contributor registry + change plans
  ↓
Persistence/ownership + Eclipse integration
  ↓
Core models/validation/API contracts
```

Core model/parsing must remain usable without SWT/JFace or bundle activator lifecycle.

## Physical bundle split trigger

Create separate bundles only when at least one is true:
- the package is a supported external OSGi API;
- an integration has optional heavy dependencies (for example JDT/CDT/WTP/TM4E) that should not be required by core;
- lifecycle/activation differs materially;
- independent Tycho/runtime testing or release compatibility requires the boundary.

Do not split simply for aesthetic layering.

## Theme model direction

A future validated theme/package needs separate concepts for:
- stable namespaced identity;
- display label;
- legacy external id;
- package/revision/content digest;
- provenance/license metadata;
- appearance metadata;
- semantic palette;
- editor tokens;
- workbench resources;
- capabilities/platform/version compatibility.

Display name and filename are never identity.

## Application direction

Contributors should converge on producing a declarative `ThemeApplicationPlan` rather than mutating state immediately. A plan can contain preference changes, workbench selection, refresh/restart requirements, diagnostics, and ownership metadata.

Coordinator responsibilities:
1. validate/resolve off-thread where safe;
2. detect compatibility/conflicts;
3. snapshot original owned state including absence;
4. persist recovery intent before risky mutation;
5. apply UI-thread-sensitive work correctly;
6. apply contributors deterministically;
7. collect failures;
8. commit active revision only after success;
9. compensate/rollback on failure where possible.

This is a compensating transaction, not an ACID transaction.

## External changes

On disable/restore, do not overwrite a value changed after Eclipse Themes wrote it. Compare current value to the last-written value and preserve newer external/user state, surfacing a conflict when necessary.

## Full-workbench limits

Public Eclipse mechanisms can support a coordinated product, not a universal pixel-perfect renderer. Workbench CSS, workbench theme registries, editor preferences, HTML/browser content, third-party custom widgets, icons, and OS-native chrome have different ownership systems.

All future product language must publish supported coverage and exceptions.
