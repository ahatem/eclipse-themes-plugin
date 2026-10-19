# Full-IDE Theming Target Architecture

This document captures the audit's Part 5 direction. It is a target/reference, not current behavior.

## Three mechanisms that must be coordinated

1. **Workbench CSS** (`org.eclipse.e4.ui.css.*` / `IThemeEngine`) for supported SWT/workbench styling.
2. **Workbench color/font definitions** (`org.eclipse.ui.themes` / theme manager registries) where participating components consume them.
3. **Editor/plugin-specific preferences** for JDT, CDT, PDE, Ant, TM4E/LSP-oriented editors, consoles, annotations, and similar integrations.

The e4 application model describes parts/stacks/toolbars; renderers produce SWT widgets. There is no single universal "theme everything" property.

## Coverage boundaries

Likely public/cooperative coverage includes many workbench backgrounds/tabs/stacks, editor syntax/preferences, registered fonts/colors, and owned/cooperating icons. Browser/help/welcome HTML, custom third-party drawing, native menus/dialogs/title bars, and arbitrary third-party icons have separate owners or hard limits.

## Target logical architecture

```text
core       immutable models, validation, legacy conversion, palette resolution
api        versioned contributor/change-plan contracts
runtime    catalog, install, persistence, ownership journal, coordinator
ui         preference UI, local preview, diagnostics, recovery
workbench  CSS/theme-engine/color-font integrations
editor.*   optional editor integrations
themes.*   curated declarative packages
```

These are logical boundaries first. Physical OSGi bundle splits happen only when optional dependencies, API ownership, lifecycle, or independent testing justify them.

## Theme package shape

A future `.eclipsetheme` package may contain declarative metadata, editor tokens, workbench CSS resources by platform, optional supported icons/fonts, and licenses/notices. It must not execute arbitrary imported code.

## Application model

Contributors should produce a `ThemeApplicationPlan` containing intended preference changes, ownership metadata, workbench-theme selection, supported font/color changes, refresh actions, warnings/skips, and restart requirements. The coordinator validates, snapshots, journals, applies deterministically, aggregates failures, commits active revision only after success, and performs compensating recovery when needed.

## Dynamic CSS lifecycle

Public workbench APIs support registration/selection/application but do not imply a universal live unregister/replace lifecycle. R7 must prove what can be safely supported. MVP should prefer built-in/p2-contributed themes until arbitrary imported CSS lifecycle is bounded.

## Coexistence policy

Component ownership is explicit: editor-only, workbench-only, coordinated, and later optional fonts/icons. External changes are detected and preserved; the plugin must not continuously overwrite another theme manager in a listener loop or silently disable competitors.

## Persistence target

Durable state should include package ID + revision, enabled components, prior workbench selection, original key values/absence, last-written values, transaction/recovery state, and package provenance/validation result. e4 context is a live-service mechanism, not the durable user-state store.

## Migration

Legacy XML remains editor-only input. Preserve valid identities, keep legacy IDs separate, generate persisted IDs when missing, resolve conflicts explicitly, migrate stored active identity, and do not claim historical pre-plugin colors can be reconstructed where no snapshot exists.

## Compatibility policy direction

Maintain editor-only backward validation separately from new workbench support. New workbench features should target a recent bounded Eclipse matrix and expand backward only after lifecycle evidence exists. Platform claims must distinguish Windows, GTK/Linux, macOS and relevant display/toolkit backends.
