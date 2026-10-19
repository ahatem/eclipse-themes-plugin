# Eclipse Themes 1.2.0

## Highlights

- **Coordinated editor and workbench theming.** Optionally color the window, tabs, tool bars, trees, tables and status line from
  the selected theme, together with the editor. Off by default; turning it off hands the workbench back.
- **Theme Package v1 (`.ectheme`).** A declarative single-file theme format with author and license provenance. It is parsed with
  the same hardened XML policy as legacy themes and never contains code, CSS or resources. Legacy `.xml` themes keep working.
- **Explicit workbench colors.** A package can state its eight workbench roles instead of relying on colors derived from the
  editor. Text roles are still adjusted when needed to stay readable.
- **User-selectable editor font.** Keep your current font, use the font the theme recommends, or pick any installed font.
- **User-selectable workbench font.** The same three choices for view and editor tabs, trees, tables and dialogs. The two fonts
  are chosen independently, and your custom fonts are remembered when you switch themes.
- **Theme-provided font recommendations.** A package can recommend an editor font and a workbench font (family and size). They
  are used only when you choose *Use theme font*; a theme that names none says so and changes nothing. Applying colors never
  changes a font on its own, a missing family is reported and skipped, and switching back to *Keep current font* restores the
  font that was replaced, unless you have changed it yourself since.
- **Redesigned Theme Manager.** The preference page now has a searchable Theme Library on the left and the selected theme on
  the right: header, preview, Appearance, Fonts and About this theme. Choices are staged until Apply or OK, and Cancel discards
  them. Import, Export, Update and Remove are library actions kept apart from Apply. Status messages appear inline, and the
  page resizes with the preferences dialog.
- **Package import and export.** *Export...* saves any installed theme as a new `.ectheme` file. A legacy theme is
  converted into a new file with a generated `legacy.<name>` id; the installed theme is not changed.
- **Safety and ownership.** Every setting the plugin writes for the workbench, other plug-ins' editors and fonts is tracked
  value-exactly: it is restored only while it still holds what the plugin wrote, so your own later choices are never overwritten.

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
