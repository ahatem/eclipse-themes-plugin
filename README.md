# Eclipse Themes

[![Build Status](https://github.com/ahatem/eclipse-themes-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/ahatem/eclipse-themes-plugin/actions)
![Eclipse Marketplace License](https://img.shields.io/eclipse-marketplace/l/eclipse-themes?color=brightgreen)
![Eclipse Marketplace Last Update](https://img.shields.io/eclipse-marketplace/v/eclipse-themes)
![Eclipse Marketplace Downloads](https://img.shields.io/eclipse-marketplace/dt/eclipse-themes)


A plugin for finding and applying color themes in the Eclipse IDE: editor syntax colors, and optionally the workbench (window, tabs, tool bars, trees, tables) and theme-provided fonts.

This project started when the original `eclipsecolorthemes.org` website went offline. To help the community, I built a new, modern, open-source alternative: **[eclipse-color-themes.vercel.app](https://eclipse-color-themes.vercel.app/)**.

I then realized the old plugin was also outdated, so I decided to build this one to provide a simple, direct way to use the themes from the new site.

## ✨ Features

- **Dozens of popular themes** included right out of the box.
- A link to **[download hundreds more](https://eclipse-color-themes.vercel.app/)** from the community collection.
- A clean preference page with a **live preview** for your code.
- **Import support** for your own favorite legacy `.xml` themes and for **theme packages** (`.ectheme`), a declarative format with provenance, explicit workbench colors and optional fonts. See the [theme package format](docs/THEME_PACKAGE_FORMAT.md).
- **Export** any installed theme as a new `.ectheme` package, which also converts a legacy theme into the package format.
- **Optional workbench theming** and **optional theme fonts**, both off by default and both handed back cleanly when turned off.
- Syntax highlighting support for Java, C++, XML, and more via an adapter system.

## 📸 Screenshots

![Eclipse Themes Show Case 1](assets/show_case_1.png)
![Eclipse Themes Show Case 2](assets/show_case_2.png)

## 🚀 Installation

### From the Eclipse Marketplace (Recommended)

1.  Go to `Help -> Eclipse Marketplace...`.
2.  Search for `Eclipse Themes`.
3.  Click **Install**.

### From the Update Site

1.  Go to `Help -> Install New Software...`.
2.  Click **Add...** and enter the URL: `https://ahatem.github.io/eclipse-themes-plugin/`
3.  Give it a name (like `Eclipse Themes`) and complete the installation.

## 💻 Usage

1.  Go to `Window -> Preferences` (or `Eclipse -> Settings...` on macOS).
2.  Navigate to `General -> Appearance -> Eclipse Themes`.
3.  Pick a theme from the list to see how it looks.
4.  Click **Apply and Close** to set your new editor theme.

### Theming the Eclipse workbench (optional)

Tick **Also theme the Eclipse workbench** before applying to also color the window, tabs, tool bars, trees, tables and status line
from the selected theme. It is off by default, never changes the workbench for a theme you only import, and un-ticking it and
applying hands the workbench back to the theme you had before. It is verified on Windows with Eclipse 4.40 and 4.41; other
platforms are experimental and need an explicit opt-in. Native menus, dialogs, title bars and Browser content are not themed.
See [workbench theming compatibility](docs/WORKBENCH_THEMING_COMPATIBILITY.md) for the support matrix and side effects
(four extra entries on the Appearance page).

### Theme fonts (optional)

A theme package may ask for an editor font and a workbench font (family and size only; fonts are never embedded or downloaded).
Tick **Apply theme fonts** before applying to use them. It is separate from colors: applying colors never changes a font, a
theme that names no fonts changes nothing, and a font family that is not installed is reported and skipped. Un-ticking it
restores the fonts it replaced, except any font you have since chosen yourself. Some workbench surfaces may update only after
restarting Eclipse. See the [theme package format](docs/THEME_PACKAGE_FORMAT.md#fonts-optional).

### Theme packages and export

Import accepts `.ectheme` files as well as `.xml` files; the format is chosen by the file extension. **Export as Package...**
saves the selected theme as a new `.ectheme` file. For a legacy theme this is an explicit conversion into a new file that you
choose; the installed theme is not changed. An installed theme can only be updated with a file of the same format.

### Importing XML themes

Theme XML must not contain a `DOCTYPE`, even an empty or internal-only declaration.
Themes that rely on DTD defaults or custom entities must be flattened into trusted,
ordinary XML offline before loading. Predefined XML escapes (such as `&amp;`) and
numeric character references remain supported. All 33 bundled themes are covered
by compatibility tests; this does not establish compatibility with every third-party theme.

The parser blocks external entities, DTDs, schemas and XInclude retrieval. If the
selected Java/Eclipse XML provider cannot establish and verify the required security
controls, loading fails with a parsing error. There is no insecure opt-out. This
hardening does not add general file-size or depth quotas.

Import always creates an independent installed record. Selecting a file whose
name, display name, source ID, or content matches an existing theme does not
overwrite or silently replace that theme; same-named sources coexist under
repository-allocated filenames. The imported bytes are copied to staging and
validated there before the managed file and identity are committed.

Bundled loading, import and update share one validation boundary. A document whose
root element is not `colorTheme`, or whose `name` is missing or blank, is refused,
and the catalog, the identity bindings and the previous installed bytes are left
untouched; the dialog names the rule that failed. Everything else is reported as a
warning and the theme is still admitted: unknown or nested elements, tokens without
a usable color, non-boolean style flags, mapping conflicts, a present but
unrecognised `type`, and a `website` that is not an absolute `http`/`https` URI.
Warnings never block an import, and an invalid installed file is skipped on load
without deleting it or changing its identity. A `website` value becomes a
clickable link only when it is an `http` or `https` URI; any other value is shown
as inert text and is never passed to the platform launcher.

Use **Update Selected Theme...** to replace the content of the selected installed
theme while keeping its identity, and **Remove** to permanently delete its
managed file after confirmation. Updating a theme does not reapply its colors
automatically.

See [editor mapping compatibility](docs/EDITOR_MAPPING_COMPATIBILITY.md) for
distinct roles, alias precedence, sparse JDT semantics, source-hover defaults and
the limits of recovery for historical debug indication values.

## ☕ Support

Eclipse Themes is free and open source. If it improves your daily setup, support helps with compatibility fixes, maintenance, and new releases.

[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-ahmedhatem-FFDD00?style=flat&logo=buy-me-a-coffee&logoColor=black)](https://www.buymeacoffee.com/ahmedhatem)

## 🤝 Contributing

Contributions are always welcome! The best way to help is to add a new theme to the collection.

Please read the [**Contributing Guidelines**](CONTRIBUTING.md) to get started.

## ☕ Support

If this plugin helped you find a theme that makes Eclipse feel like home, a coffee goes a long way!

[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-ahmedhatem-FFDD00?style=flat&logo=buy-me-a-coffee&logoColor=black)](https://www.buymeacoffee.com/ahmedhatem)

## 📜 License

This project is licensed under the **Eclipse Public License 2.0**. See the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- This project wouldn't exist without the original [Eclipse Color Theme](https://github.com/eclipse-color-theme/eclipse-color-theme) plugin.
- I also learned a lot from the clean architecture of the [BetterThemes](https://github.com/TheKodeToad/BetterThemes/) project by TheKodeToad.
- And a huge thank you to everyone who has created and shared a theme with the community.
