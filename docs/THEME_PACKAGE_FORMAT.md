# Theme Package Format (`.ectheme`, schema version 1)

A theme package is one XML document with the extension `.ectheme`. It describes an editor color theme and, optionally, the colors
the Eclipse workbench should use when the user has turned on *Also theme the Eclipse workbench*, and the editor and workbench
fonts it recommends, used when the user sets a font to *Use theme font*. A package is purely declarative:
nothing in it is executed, and nothing is loaded from outside it.

Legacy `.xml` themes (`<colorTheme>`) keep working unchanged. Write a package when you want provenance metadata, explicit
workbench colors, or a format that refuses mistakes instead of silently filling in defaults.

## Example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<themePackage schemaVersion="1"
              id="io.github.example.nightfall"
              name="Nightfall"
              appearance="dark"
              author="Jane Example"
              description="A calm blue-grey dark theme"
              website="https://example.org/nightfall"
              version="2.1">
  <provenance license="MIT"
              copyright="Jane Example"
              source="https://github.com/example/nightfall"/>
  <editor>
    <background color="#1b1f2a"/>
    <foreground color="#c8d0e0"/>
    <keyword color="#c792ea" bold="true"/>
    <string color="#c3e88d"/>
    <singleLineComment color="#697098" italic="true"/>
  </editor>
  <workbench>
    <surface color="#222838"/>
    <accent color="#82aaff"/>
  </workbench>
  <fonts>
    <editor family="Consolas" size="11"/>
    <interface family="Segoe UI" size="9"/>
  </fonts>
</themePackage>
```

## Root element `<themePackage>`

| Attribute | Required | Rule |
|---|---|---|
| `schemaVersion` | yes | Must be `1`. A higher number is refused with "requires a newer Eclipse Themes"; a missing or non-integer value is refused. |
| `id` | yes | Lower-case dotted identifier, for example `io.github.you.mytheme` (`[a-z0-9]+(\.[a-z0-9][a-z0-9-]*)+`, at most 128 characters). |
| `name` | yes | Display name; must not be blank. |
| `appearance` | yes | `dark` or `light`. There is no default. |
| `author`, `description` | no | Free text. |
| `website` | no | Shown as a link only when it is an `http`/`https` URL; otherwise it is shown as plain text. |
| `version` | no | Display text of at most 64 characters. It is never compared or used for updates. |

Unknown attributes are ignored silently.

The `id` is a claim made by the author, **not** the theme's identity inside Eclipse. Every import creates an independent installed
theme, even if another installed package uses the same `id`. Updating an installed theme is always an explicit choice of the user.

## Sections

The sections may appear in any order. Each one may appear at most once; a repeated section refuses the package.

### `<provenance>` (optional)

| Attribute | Meaning |
|---|---|
| `license` | License of the theme, for example an SPDX expression such as `MIT` or `CC-BY-4.0`. It is not validated against the SPDX list. |
| `copyright` | Copyright holder, which may differ from `author` (for example for a port of someone else's theme). |
| `source` | Upstream URL. The same link rule as `website` applies. |

Anything you leave out is shown as **Not stated**. Nothing is assumed in its place; in particular, the plugin's own license never
covers your package.

### `<editor>` (required)

The children use **exactly** the same element names and rules as the children of a legacy `<colorTheme>`: canonical token names,
legacy aliases (such as `singleLineComment`, `javadoc` or `typeParameter`), the `color` attribute, and the `bold`, `italic`,
`underline` and `strikethrough` flags. If you are converting a legacy theme, you can copy its children unchanged.

`<background>` and `<foreground>` must both be present with a valid color. Unlike legacy themes, a package never receives default
colors for them.

### `<workbench>` (optional)

The children are the eight roles of the workbench palette, each with a `color` attribute:

| Role | Used for |
|---|---|
| `background` | Workbench base color (replaces the editor background for the workbench only) |
| `foreground` | Main workbench text |
| `surface` | Toolbars, unselected tabs, view backgrounds |
| `surfaceRaised` | Selected tabs |
| `border` | Separators and borders |
| `mutedForeground` | Secondary text |
| `selection` | Selection backgrounds |
| `accent` | Highlights and focused elements |

All roles are optional. A role you author is used as written. Every other role is derived from your colors, and roles that depend
on another role (for example `surfaceRaised` from `surface`) use your authored value.

Readability is protected. If the text roles (`foreground`, `mutedForeground`, `accent`) would not reach a 4.5:1 contrast against the
workbench surfaces, only their lightness is adjusted, keeping the hue; import reports a warning for each adjusted role. If the
palette still cannot be made readable (for example because the foreground is unreadable on your `selection`), the standard dark or
light workbench palette is used instead.

A `<workbench>` section never turns workbench theming on or off. It is used only when the user has enabled it.

### `<fonts>` (optional)

`<fonts>` may contain an `<editor>` and an `<interface>` entry. Each entry is independently optional and has exactly two
attributes:

| Attribute | Rule |
|---|---|
| `family` | The name of an **installed** font family, matched case-insensitively. Not blank, at most 128 characters, no control characters. |
| `size` | Whole points, 4 to 96. |

There is no weight, style, file, URL or fallback list: a package never embeds, downloads or references a font file.

The two entries are **semantic intents**, not lists of platform fonts. The user chooses, separately for the **Editor font** and
the **Interface font**, *Keep current font*, *Use theme font* or *Custom font*. A package's entries matter only for *Use theme
font*:

- `<editor>` is the font recommended for editors, consoles and supported code and text views. It is applied to the platform
  *Text Font* and to the definitions Eclipse itself declares to default to it (for example the Java editor, Console and compare
  fonts), plus a few text definitions that declare no inheritance but serve the same purpose.
- `<interface>` is the font recommended for dialogs, views, trees, tables and supported Eclipse chrome. It is applied to the
  platform *Dialog Font*, the definitions that declare it as their default, the view-tab (*Part title*) and *Tree and Table*
  fonts, and the wizard banner and section header fonts. Those two headings keep their bold weight and their size relative to
  the chosen font.

The exact definitions are an implementation detail documented in
[WORKBENCH_THEMING_COMPATIBILITY.md](WORKBENCH_THEMING_COMPATIBILITY.md); a package never names an Eclipse font id, and a
family and size are the only things a package can state. The coordinated values appear under *Preferences > General >
Appearance > Colors and Fonts*, using the platform's own storage; this plugin keeps no font settings of its own that Eclipse
does not know.

The top-level `<workbench>` section above is a different thing: it holds workbench *colors*. There is no `<workbench>` entry
inside `<fonts>`; an old one is reported as an unknown element and ignored.

A user's *Custom font* is a user preference, not package data: it is remembered across theme switches and is never written to an
exported package. Exporting a package keeps the fonts the package itself carries, and converting a legacy theme invents none.

Behavior to design for:

- Applying a theme's colors never changes a font unless the user chose *Use theme font* or *Custom font* for it. A package
  without `<fonts>` shows *Not specified by this theme* and changes nothing.
- A family that is not installed is reported ("Requested font ... is not installed") and the corresponding font is left as it
  is.
- Switching a font back to *Keep current font*, selecting a theme that names no font for a font set to *Use theme font*, or
  choosing **Stop managing**, restores each font definition the plugin changed to what it was before, but only while it still
  holds the value the plugin wrote. A definition the user has since changed under *Colors and Fonts* is never overwritten
  behind their back; it is left as found and no longer managed. Choosing **Apply** on the preference page is an explicit request
  and takes the whole font group over again; the page then says how many changed definitions it replaced.
- Fonts are stored in the platform preferences and applied through the platform font registry. Some workbench surfaces may
  update only after restarting Eclipse. Where a CSS theme overrides a font definition for itself, that override is not changed.

## What is refused and what is only reported

Refused (the package is not imported, and an existing installed theme is left untouched):

- a file larger than 256 KiB;
- a `DOCTYPE`, external entities or any other external reference;
- a root element other than `<themePackage>`;
- a missing or invalid required attribute or `<editor>` section, or a missing `background` or `foreground`;
- a repeated section or a repeated workbench role.

Reported as warnings (the package is still imported):

- unknown elements (they are ignored, but kept byte for byte in the installed file, so a later version can read them);
- unknown workbench roles, and invalid or missing colors;
- a blank `license`, a `website` or `source` that is not a launchable URL, or an over-long `version`;
- workbench text roles that had to be adjusted for readability;
- an invalid `<fonts>` entry (blank or over-long `family`, a `size` that is not a whole number from 4 to 96, or a missing
  attribute): that entry is ignored and the rest of the package is used. An unknown child of `<fonts>` is reported like any
  unknown element, and a repeated `<editor>` or `<interface>` entry inside `<fonts>` refuses the package.

## Exporting a theme

**Export as Package...** on the preference page saves the selected theme to a new `.ectheme` file that you choose; it never
changes the installed theme.

- A theme that is already a package is written back with its metadata, editor values, authored workbench colors and fonts. A custom font chosen on the page is a user preference and is never written. The
  output is deterministic and readable: `background` and `foreground` first, then the other tokens alphabetically.
- Nothing internal is written: no installed path, no catalog identity and no preference values. Workbench colors that were only
  derived at import time are not written, so an export states only what the theme itself says.
- The file is checked by reading it back with the package parser before it is written, and appears only when it is complete.
  An existing file of that name is replaced only after you confirm.
- **Converting a legacy theme.** A legacy `.xml` theme can be exported the same way. Its name, author and description are kept.
  The package `id` is generated from the name as `legacy.` followed by the name in lower case with each run of characters other
  than letters and digits replaced by one hyphen (`theme` when nothing is left), for example `Atom One Dark` becomes
  `legacy.atom-one-dark`. You may edit the `id` in the exported file. The legacy theme has no workbench colors or fonts, so none
  are written, and no provenance is invented. The installed legacy theme itself is not rewritten.

## Converting a legacy theme by hand

Exporting is the easy way. To do it manually:

1. Rename `<colorTheme>` to `<themePackage>` and add `schemaVersion="1"`, an `id` and `appearance` (previously `type`, which
   defaulted to light).
2. Move all color children into an `<editor>` element.
3. Make sure `<background>` and `<foreground>` are present.
4. Drop `ect-id`. Packages have no remote preview, so no id or metadata is ever sent to the preview server.
5. Optionally add `<provenance>` and `<workbench>`.
6. Save the file with the `.ectheme` extension. The format is chosen only by the extension.

## Compatibility notes

- An installed package can only be updated with another `.ectheme` file, and a legacy theme only with another `.xml` file.
- Versions of Eclipse Themes released before packages ignore installed `.ectheme` files; they reappear after upgrading again.
- `<fonts>` is an additive section: the format stays at schema version 1.
- Schema changes that an older reader must not ignore will raise `schemaVersion`.
