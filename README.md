# Eclipse Themes

[![Build Status](https://github.com/Philipp0205/eclipse-themes-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/Philipp0205/eclipse-themes-plugin/actions)
![Eclipse Marketplace License](https://img.shields.io/eclipse-marketplace/l/eclipse-themes?color=brightgreen)
![Eclipse Marketplace Last Update](https://img.shields.io/eclipse-marketplace/v/eclipse-themes)
![Eclipse Marketplace Downloads](https://img.shields.io/eclipse-marketplace/dt/eclipse-themes)


A plugin for finding and applying color themes across Eclipse editors and the
surrounding workbench.

This project started when the original `eclipsecolorthemes.org` website went offline. To help the community, I built a new, modern, open-source alternative: **[eclipse-color-themes.vercel.app](https://eclipse-color-themes.vercel.app/)**.

I then realized the old plugin was also outdated, so I decided to build this one to provide a simple, direct way to use the themes from the new site.

## ✨ Features

- **Dozens of popular themes** included right out of the box.
- A link to **[download hundreds more](https://eclipse-color-themes.vercel.app/)** from the community collection.
- A clean preference page with a **live preview** for your code.
- **Import support** for your own favorite `.xml` theme files.
- Syntax highlighting support for Java, C++, XML, and more via an adapter system.
- Optional whole-workbench styling for views, tabs, toolbars, trees, and tables.
- A **Colors** preference page, modeled on Java syntax highlighting, for every theme token.
- An application-only GTK overlay on Linux for native backgrounds and selections.

## 📸 Screenshots

![Eclipse Themes Show Case 1](assets/show_case_1.png)
![Eclipse Themes Show Case 2](assets/show_case_2.png)

## 🚀 Installation

### From the Update Site (Recommended)

1.  Go to `Help -> Install New Software...`.
2.  Click **Add...** and enter the URL: `https://philipp0205.github.io/eclipse-themes-plugin/`
3.  Give it a name (like `Eclipse Themes`) and complete the installation.

This update site is built from this repository and carries its changes. The
upstream project publishes a separate site at
`https://ahatem.github.io/eclipse-themes-plugin/`, which does not include them.

The `Build and Deploy` workflow rebuilds the p2 repository and republishes it to
GitHub Pages on every push to `main` or to the repository's default branch, so a
fork serves its own update site. It needs
`Settings -> Pages -> Build and deployment -> Source` set to **GitHub Actions**;
with the default **Deploy from a branch** source, Pages renders this README
instead of the p2 repository and Eclipse cannot install from the URL.

### From the Eclipse Marketplace

The Marketplace entry is published by the upstream project, so it ships upstream
releases rather than the changes in this repository.

1.  Go to `Help -> Eclipse Marketplace...`.
2.  Search for `Eclipse Themes`.
3.  Click **Install**.

### From a locally built update site

Useful for trying out an unreleased branch.

1.  Run `mvn clean verify`. The p2 repository is written to
    `releng/com.github.eclipsethemes.updatesite/target/com.github.eclipsethemes.updatesite-*.zip`.
2.  Go to `Help -> Install New Software...`, click **Add...**, then **Archive...**
    and pick that zip.
3.  Select **Eclipse Themes** and complete the installation.

## 💻 Usage

1.  Go to `Window -> Preferences` (or `Eclipse -> Settings...` on macOS).
2.  Navigate to `General -> Appearance -> Eclipse Themes`.
3.  Pick a theme from the list to see how it looks.
4.  Leave **Apply theme to the whole Eclipse workbench** selected to also style the IDE chrome.
5.  Open **Colors** under Eclipse Themes to change any token—editor, syntax, workbench, and GTK—using a Java-style element list, color picker, and bold/italic/underline/strikethrough flags.
6.  Click **Apply and Close** to set your theme and custom colors.

### Platform styling limits

Eclipse workbench controls use E4 CSS while some controls are drawn by the
platform toolkit. On Linux the plugin injects GTK CSS into Eclipse only; it does
not change your desktop GTK theme or other applications. Window decorations,
some GTK/Adwaita widgets, native controls on Windows and macOS, and third-party
views that hard-code colors may retain their platform colors.

## ☕ Support

Eclipse Themes is free and open source. If it improves your daily setup, support helps with compatibility fixes, maintenance, and new releases.

[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-ahmedhatem-FFDD00?style=flat&logo=buy-me-a-coffee&logoColor=black)](https://www.buymeacoffee.com/ahmedhatem)

## 🤝 Contributing

Contributions are always welcome! The best way to help is to add a new theme to the collection.

Please read the [**Contributing Guidelines**](CONTRIBUTING.md) to get started.

## 📜 License

This project is licensed under the **Eclipse Public License 2.0**. See the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- This project wouldn't exist without the original [Eclipse Color Theme](https://github.com/eclipse-color-theme/eclipse-color-theme) plugin.
- I also learned a lot from the clean architecture of the [BetterThemes](https://github.com/TheKodeToad/BetterThemes/) project by TheKodeToad.
- And a huge thank you to everyone who has created and shared a theme with the community.
