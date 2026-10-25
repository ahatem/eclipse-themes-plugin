# Eclipse Themes 1.2.0

## Highlights

- **Coordinated editor and workbench theming.** Optionally color the window, tabs, tool bars, trees, tables and status line from
  the selected theme, together with the editor. Off by default; turning it off hands the workbench back.
- **Theme Package v1 (`.ectheme`).** A declarative single-file theme format with author and license provenance. It is parsed with
  the same hardened XML policy as legacy themes and never contains code, CSS or resources. Legacy `.xml` themes keep working.
- **Explicit workbench colors.** A package can state its eight workbench roles instead of relying on colors derived from the
  editor. Text roles are still adjusted when needed to stay readable.
- **Two fonts: Editor font and Interface font.** Each is *Keep current font*, *Use theme font* or *Custom font* (any installed
  font, chosen with the standard system font dialog), set independently and remembered when you switch themes. The **Editor
  font** covers editors, consoles and supported code and text views; the **Interface font** covers dialogs, views, trees, tables
  and supported Eclipse chrome. Each one coordinates a small, documented set of real Eclipse font definitions, so the values
  show up under *Preferences > General > Appearance > Colors and Fonts* like any font you set yourself.
- **Theme-provided font recommendations.** A package can recommend an editor font and an interface font
  (`<fonts><editor/><interface/></fonts>`, family and size). They are used only when you choose *Use theme font*; a theme that
  names none says so and changes nothing. Applying colors never changes a font on its own and a missing family is reported and
  skipped.
- **Fonts you change yourself are respected.** Each font definition the plugin sets is tracked value-exactly. If you change one
  under *Colors and Fonts*, the plugin leaves it alone; choosing Apply again is an explicit request and takes the group back
  (the page tells you how many of your changes it replaced). Switching to *Keep current font* restores what was replaced.
- **Redesigned Theme Manager.** The preference page now has a searchable Theme Library on the left and the selected theme on
  the right: header, a compact preview, Appearance, Fonts and About this theme. The applied theme is shown in bold in the
  library. Choices are staged until Apply or OK, and Cancel discards them. Import, Export, Update and Remove are library actions kept apart from Apply. Status messages appear inline, and the
  page resizes with the preferences dialog.
- **Package import and export.** *Export...* saves any installed theme as a new `.ectheme` file. A legacy theme is
  converted into a new file with a generated `legacy.<name>` id; the installed theme is not changed.
- **Safety and ownership.** Workbench choices, coordinated plug-in colors and fonts are tracked value-exactly: they are restored
  only while they still hold what the plugin wrote, so your own later choices are never overwritten. Editor preference ownership
  was added after 1.2.0: from that version onward, the editor values a theme replaces are also recorded and restored under the
  same rule.

## Compatibility

- Runtime: Java 17 and later, as before.
- Workbench theming is verified on **Windows with Eclipse 4.40 and 4.41**. Other platforms and Eclipse versions are experimental
  and need an explicit opt-in. Font choices are not separately verified elsewhere.
- Native menus, dialogs, title bars and browser content are not themed. Some workbench surfaces may pick up a changed font only
  after restarting Eclipse.
- Not included: icon theming, CSS authoring or import, embedded or downloaded fonts, a marketplace or remote repository.

## Upgrading

No migration is needed. Existing themes, imports and settings are unchanged. Installed `.ectheme` files require this version or
later.

See [theme package format](THEME_PACKAGE_FORMAT.md) and [workbench theming compatibility](WORKBENCH_THEMING_COMPATIBILITY.md).
