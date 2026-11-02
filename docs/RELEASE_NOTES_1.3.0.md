# Eclipse Themes 1.3.0

## Highlights

- **23 new bundled themes, 56 in total.** The full Kintsugi set (all eight variants, six of them new), plus Dracula, Ayu
  (Dark, Mirage, Light), Night Owl (and Light), Tokyo Night (Night, Storm, Light), Palenight, SynthWave '84, Shades of Purple,
  Andromeda, Nord and Rosé Pine (with Moon and Dawn). Each comes with editor colors, a workbench palette and author and license
  provenance. Many are ports of well-known color schemes by their original designers; this project does not claim to have
  designed them.
- **Every bundled theme refreshed from its upstream original.** The existing bundled themes were regenerated from pinned
  upstream sources. Each one now has its own workbench palette and provenance (author, license, source) shown under *About
  this theme*. Ids, names and light/dark appearance are unchanged, so saved selections keep working.
- **Catppuccin is the new default.** When no theme has been chosen, the plugin now uses Catppuccin Mocha for dark and
  Catppuccin Latte for light. If you have already chosen or saved a theme, you keep it.
- **Wider plug-in color coverage.** Coordinated plug-in colors now also cover three Mylyn task colors and eight Spring Tools
  colors that Eclipse only sets in its stock dark theme. They follow the same ownership rule as the existing plug-in colors:
  restored only while they still hold what the plugin wrote.
- **Workbench appearance is declared.** The coordinated workbench themes now declare `isDarkTheme`, which Eclipse honors from
  the 2026-12 release. Older releases ignore it and keep telling dark from light as before.
- **Themed progress bars.** With workbench theming on and a dark theme, progress bars in wizards and dialogs now use the
  theme's colors. With a light theme they stay native.
- **Documented styling limits.** [Workbench theming compatibility](WORKBENCH_THEMING_COMPATIBILITY.md#known-platform-limits-for-theme-authors)
  now lists known Eclipse CSS limits for theme authors, such as tree and table selection colors that CSS cannot set.

## Things you may notice

- **Some bundled themes look different.** The refresh follows each theme's upstream original instead of the older port, so a few
  themes changed visibly, for example Darcula, IntelliJ Dark and the GitHub themes. To keep an old look, export that theme
  with *Export...* in 1.2.0 before upgrading and import the file afterwards.
- **Bundled themes now ship as `.ectheme` packages** instead of legacy `.xml` files. This is internal: legacy `.xml` themes you
  import keep working.

## Compatibility

- Runtime: Java 17 and later, as before.
- Workbench theming is still verified on **Windows with Eclipse 4.40 and 4.41**. Other platforms and Eclipse versions remain
  experimental and need an explicit opt-in. The progress bar change was checked on Windows only.
- Native menus, dialogs, title bars and browser content are still not themed.

## Upgrading

No migration is needed. Saved theme choices, imported themes and settings are unchanged. Themes from the
[online collection](https://eclipse-color-themes.vercel.app/) in `.ectheme` format need 1.2.0 or later.

## For developers

- The preference page is split into one panel per section (library, preview, appearance, fonts, about) under a small
  coordinating page. No behavior change.
- One-file packages were folded into their parents, a layering test keeps SWT, JFace and Eclipse UI out of `core`, and
  low-value tests were pruned.
- OpenSpec was dropped and the docs were slimmed to user and developer guides. See [Architecture](ARCHITECTURE.md) and
  [Contributing](../CONTRIBUTING.md).
