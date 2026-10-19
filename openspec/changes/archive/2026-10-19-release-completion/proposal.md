# Proposal — Release completion (release 1.2.0)

## Why

Coordinated editor and workbench theming (R8) and Theme Package v1 are merged, but a package author still cannot ask for the fonts
that make a theme complete, an installed theme cannot be saved back out as a package, and a legacy theme has no path into the new
format. This change finishes the remaining release scope in one bounded step so 1.2.0 can be prepared.

Baseline: `develop` @ `d40cb615af2a58303aa20ae21b50803b97991ee7` (Theme Package v1).

## What Changes

- **Theme fonts.** An optional `<fonts>` section in a package (`<editor family size/>`, `<workbench family size/>`, each
  independently optional, installed families only). The package stays `schemaVersion="1"`; no font file, URL or resource.
- **Apply theme fonts** — a separate opt-in toggle. Applying colors never changes a font; a package without fonts changes nothing; a
  missing family is reported and skipped. Fonts are written as stored font preferences under the platform's own font-definition ids
  and published through the platform font registry; ownership and restoration are value-exact.
- **Export as Package.** Any installed theme can be saved to a new `.ectheme` file the user chooses. A legacy theme is converted
  explicitly into a new file, with the generated id `legacy.<slug of name>`; derived workbench values are omitted; the installed
  theme is never rewritten.
- **Preference page polish.** Format row, Fonts section, export button, import/update success dialogs that list accepted warnings,
  format-neutral wording ("Replace the installed theme content of ...").
- **Diagnostics.** A `font` diagnostic category for invalid font declarations; export failures are reported with their reason.
- **Release engineering.** Version 1.2.0, release notes, README, feature and category descriptions, docs.

Out of scope: icons, CSS authoring or import, per-color or font designer, marketplace, remote repository, contributor API,
WTP/TM4E, embedded or downloaded fonts, title-bar or third-party-widget hacks.

## Decision Gate

**No new maintainer decision is required.** The maintainer's release-completion instruction fixed the product choices: optional
package-provided fonts, independent editor and workbench entries, family and size only, installed families only, no embedded or
downloaded fonts, schema version unchanged, one simple toggle, explicit legacy conversion with a documented generated id, omission
of derived values, and conservative restoration. Nothing here is `BREAKING`: existing packages and legacy themes behave as before,
and the new keys are additive.

- **Evidence-dependent (resolved by reading Eclipse sources):** the font-definition ids (`org.eclipse.jface.textfont`,
  `org.eclipse.ui.workbench.TAB_TEXT_FONT`, `TREE_TABLE_FONT`, `org.eclipse.jface.dialogfont`) and that a stored font preference
  under the plain id plus an update of the theme font registry is the public mechanism. Not runtime-verified here: how each
  surface reacts without a restart (the UI says some may need one), and CSS-theme-overridden definitions (not changed).
- **Engineering (recorded in `design.md`):** one ownership record, the workbench font mapped to three definitions, export
  validated by round-trip parsing.
- **Deferred:** icons and embedded fonts (future theme-package roadmap work), per-surface coverage UI (R9), other-platform
  verification.
