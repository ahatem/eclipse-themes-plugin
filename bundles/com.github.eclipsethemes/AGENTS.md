# Runtime Bundle Agent Guide

This subtree contains the product runtime bundle. Root `AGENTS.md` applies in full; these rules are additional.

## Boundaries

- Keep model/parser behavior independent from Eclipse UI and bundle lifecycle wherever possible.
- Do not introduce new calls from parser/model code into the activator for logging or service lookup.
- Adapter code is integration code: centralize fragile preference keys, document their source/version assumptions, and test exact writes/removals.
- `plugin.xml`, `MANIFEST.MF`, extension schemas, and exported packages form compatibility contracts. Treat edits as API/installation changes.

## Theme application

- Prefer declarative change planning over direct scattered preference mutation as the architecture evolves.
- Never clear a whole foreign preference node.
- A missing semantic token and an explicit "disabled" state are not automatically equivalent; preserve intended editor behavior.
- Applying a theme must produce a result that can represent partial/aggregate failure. UI success state must not be committed early.

## Parser/import

- Treat theme XML as untrusted input.
- Preserve legacy compatibility intentionally; do not silently accept arbitrary roots or malformed identity as valid themes.
- Make alias collisions explicit and test precedence.
- Stable identity must not depend on display name or source filename.

## UI

- `EclipseThemesPreferencePage` should become thinner over time; move policy/state transitions into testable services/models rather than adding more branches to the page.
- Keep long-running file/parse work off the UI thread.
- Preview must eventually be derived from actual local theme data, not a remote lookup keyed only by theme id.

## Tests

When touching parser, repository, adapters, apply/reset behavior, or theme identity, add/update regression tests before broad refactoring.
