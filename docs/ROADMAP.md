# Roadmap

Direction, not a promise. Safety, reversibility and honest coverage claims come before new scope. We do not claim to theme "every pixel" of the IDE.

## Done

- **Safe editor theming:** no destructive resets, value-exact preference ownership, conflict-aware restore, secure XML parsing, atomic import, stable theme identity, immutable validated theme model with structured diagnostics.
- **Workbench theming (opt-in):** an eight-role palette derived from the selected theme drives four static CSS themes, plus coordinated colors for other plug-ins on dark themes. Verified on Windows with Eclipse 4.40 and 4.41. See [WORKBENCH_THEMING_COMPATIBILITY.md](WORKBENCH_THEMING_COMPATIBILITY.md).
- **Theme packages (`.ectheme`, schema version 1):** declarative editor tokens, optional workbench roles, fonts and provenance, plus export. See [THEME_PACKAGE_FORMAT.md](THEME_PACKAGE_FORMAT.md) and the [1.2.0 release notes](RELEASE_NOTES_1.2.0.md).

## Next

- Tidy packages and prune redundant tests.
- Split the oversized preference page into testable pieces.
- Platform-CSS spike: what Eclipse's own CSS can style on Windows, GTK and macOS, and where it cannot.
- Refresh the bundled themes from upstream and publish a project site.

## Later, only with evidence

- Linux and macOS verification of workbench theming.
- Offline preview computed from local theme data.
- Recovery journal for workbench state; per-surface coverage view.
- Theme-authored plug-in colors and CSS, icons, archive packages with resources.
- Contributor API for third-party integrations (WTP, TM4E); optional bundle splits where they are justified.
- Build and release hardening: separate snapshot and stable channels, reproducible artifacts, p2 install smoke tests, a current-Eclipse target alongside the 2024-09 baseline.

Imported theme content stays declarative: no executable code in theme packages. Native menus, title bars, dialogs, browser content and non-participating third-party widgets are research areas, not promises.
