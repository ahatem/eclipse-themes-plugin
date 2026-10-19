# Eclipse Themes

[![Build Status](https://github.com/ahatem/eclipse-themes-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/ahatem/eclipse-themes-plugin/actions)
![Eclipse Marketplace License](https://img.shields.io/eclipse-marketplace/l/eclipse-themes?color=brightgreen)
![Eclipse Marketplace Last Update](https://img.shields.io/eclipse-marketplace/v/eclipse-themes)
![Eclipse Marketplace Downloads](https://img.shields.io/eclipse-marketplace/dt/eclipse-themes)

## One theme. One coherent Eclipse.

Pick a color theme once and have it carry through Eclipse: your **editors**, optionally the **workbench** around them (window,
tabs, tool bars, trees, tables), and, if you want, the **fonts** too. Themes come from a collection of dozens bundled with the
plugin, from the community collection online, or from your own files.

This project started when the original `eclipsecolorthemes.org` website went offline. To help the community I built a modern,
open-source alternative, **[eclipse-color-themes.vercel.app](https://eclipse-color-themes.vercel.app/)**, and this plugin is the
simple, direct way to use its themes in Eclipse.

![The Theme Manager with a dark theme package selected](assets/show_case_1.png)

## ✨ What you get

- **Editor themes.** Syntax and editor colors for Java, C++, XML and more through an adapter system. Dozens of popular themes are
  included, and you can **[download hundreds more](https://eclipse-color-themes.vercel.app/)**.
- **Coordinated workbench theming** (optional). Also color the window, tabs, tool bars, trees, tables and status line from the
  selected theme. Off by default, and handed back when you turn it off.
- **Theme Package v1 (`.ectheme`).** A declarative single-file theme format with author and license details, explicit workbench
  colors and optional font recommendations. See the [theme package format](docs/THEME_PACKAGE_FORMAT.md).
- **Fonts you control**, separately for the editor and the workbench: **Keep current font**, **Use theme font** or **Custom font**.
- **Import and export.** Import `.ectheme` packages and legacy `.xml` themes; export any theme as a new `.ectheme` package.
- **Legacy XML compatibility.** Existing Eclipse Color Theme `.xml` files keep working, and all 33 bundled themes are covered by
  compatibility tests.
- **A Theme Manager page** with a searchable library, a live preview and the selected theme's details in one place.

![The Theme Manager with a light theme selected](assets/show_case_2.png)

## 🚀 Installation

### From the Eclipse Marketplace (recommended)

1. Go to `Help -> Eclipse Marketplace...`.
2. Search for `Eclipse Themes`.
3. Click **Install**.

### From the update site

1. Go to `Help -> Install New Software...`.
2. Click **Add...** and enter the URL: `https://ahatem.github.io/eclipse-themes-plugin/`
3. Give it a name (like `Eclipse Themes`) and complete the installation.

Eclipse Themes runs on Java 17 and later.

## 💻 Using the Theme Manager

Open `Window -> Preferences` (or `Eclipse -> Settings...` on macOS), then `General -> Appearance -> Eclipse Themes`.

1. Search or browse the **Theme Library** on the left and select a theme to see its preview and details on the right.
2. Choose whether to theme the workbench and which fonts to use (below).
3. Click **Apply** or **Apply and Close**.

Selecting a theme, changing the font options and ticking the workbench option only stage your choices. They take effect when you
choose Apply or Apply and Close, and **Cancel** discards them. **Import**, **Export**, **Update** and **Remove** act on the theme
library straight away and are separate from Apply.

### Theming the workbench

Tick **Also theme the Eclipse workbench** before applying to color the window, tabs, tool bars, trees, tables and status line
from the selected theme. It never changes the workbench for a theme you only import, and un-ticking it and applying hands the
workbench back to the theme you had before. See [workbench theming compatibility](docs/WORKBENCH_THEMING_COMPATIBILITY.md) for
the coverage, the support matrix and side effects (four extra entries on the Appearance page).

### Fonts

The **Fonts** section has two independent choices: the **Editor font** (source code and text editors) and the **Workbench font**
(view and editor tabs, trees, tables and dialogs). Each is one of:

- **Keep current font** (the default): the plugin leaves that font alone.
- **Use theme font**: use the font the selected theme recommends. A theme that names none shows *Not specified by this theme*
  and changes nothing.
- **Custom font**: pick any installed font with the standard system font dialog (family and size).

Your custom fonts are your own preferences: they are remembered when you switch themes and are never written into exported
packages. Chosen fonts are set in Eclipse's own font definitions, so they also show up under `General -> Appearance -> Colors and
Fonts`. A font family that is not installed is reported and skipped. Going back to *Keep current font* gives back the font the
plugin replaced, unless you have changed that font yourself since. See the
[theme package format](docs/THEME_PACKAGE_FORMAT.md#fonts-optional) for what a theme can recommend.

### Importing and exporting

**Import...** accepts `.ectheme` and `.xml` files; the format is chosen by the file extension. **Export...** saves the selected
theme as a new `.ectheme` file. For a legacy theme this is an explicit conversion into a new file that you choose; the installed
theme is not changed. **Update...** replaces an imported theme with a file of the same format and **Remove** deletes it after
confirmation; updating does not reapply the theme automatically.

## ✅ Compatibility

- Editor themes use the Eclipse editors' own preferences and work wherever the supported editors do.
- Workbench theming is **verified on Windows with Eclipse 4.40 and 4.41**. Other operating systems and Eclipse versions are
  **experimental** and need an explicit opt-in on the page.
- The workbench is themed through Eclipse's own theme engine, so high-contrast and accessibility settings are left alone.

## ⚠️ Known limitations

- Native menus, dialogs, title bars, Browser content and icons are not themed by Eclipse CSS and may keep their usual look.
- Fonts are family and size only. They are never embedded or downloaded, and a font that a CSS theme itself overrides cannot be
  changed from here.
- Some workbench surfaces update a changed font only after restarting Eclipse.
- Previews exist only for themes from the online collection; imported packages show no preview.
- Not included: icon theming, CSS authoring or import, a theme marketplace or remote repository.

## 🧩 Theme files

Theme packages are documented in the [theme package format](docs/THEME_PACKAGE_FORMAT.md). Legacy XML themes work as before:

- Theme XML must not contain a `DOCTYPE`, even an empty or internal-only declaration. Themes that rely on DTD defaults or custom
  entities must be flattened into ordinary XML first. Predefined XML escapes (such as `&amp;`) and numeric character references
  remain supported.
- The parser blocks external entities, DTDs, schemas and XInclude retrieval. If the Java/Eclipse XML provider cannot establish and
  verify these controls, loading fails with a parsing error; there is no insecure opt-out.
- Import always creates an independent installed record. A file whose name, display name, source ID or content matches an
  existing theme never overwrites it; same-named themes coexist. Imported bytes are validated before anything is committed.
- A document whose root is not `colorTheme`, or whose `name` is missing or blank, is refused and nothing is changed. Everything
  else is reported as a warning and the theme is still admitted: unknown elements, tokens without a usable color, mapping
  conflicts, an unrecognised `type`, or a `website` that is not an absolute `http`/`https` URI. A `website` becomes a link only
  when it is `http` or `https`.

See [editor mapping compatibility](docs/EDITOR_MAPPING_COMPATIBILITY.md) for distinct roles, alias precedence, sparse JDT
semantics, source-hover defaults and the limits of recovery for historical debug indication values.

## 🤝 Contributing

Contributions are always welcome! The best way to help is to add a new theme to the collection. Please read the
[**Contributing Guidelines**](CONTRIBUTING.md) to get started.

## ☕ Support

Eclipse Themes is free and open source. If this plugin helped you find a theme that makes Eclipse feel like home, a coffee goes a
long way, and supports compatibility fixes and new releases.

[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-ahmedhatem-FFDD00?style=flat&logo=buy-me-a-coffee&logoColor=black)](https://www.buymeacoffee.com/ahmedhatem)

## 📜 License

This project is licensed under the **Eclipse Public License 2.0**. See the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- This project wouldn't exist without the original [Eclipse Color Theme](https://github.com/eclipse-color-theme/eclipse-color-theme) plugin.
- I also learned a lot from the clean architecture of the [BetterThemes](https://github.com/TheKodeToad/BetterThemes/) project by TheKodeToad.
- And a huge thank you to everyone who has created and shared a theme with the community.
