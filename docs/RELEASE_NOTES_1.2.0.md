# Eclipse Themes 1.2.0

## Highlights

- **Coordinated editor and workbench theming.** Optionally color the window, tabs, tool bars, trees, tables and status line from
  the selected theme, together with the editor. Off by default; turning it off hands the workbench back.
- **Theme Package v1 (`.ectheme`).** A declarative single-file theme format with author and license provenance. It is parsed with
  the same hardened XML policy as legacy themes and never contains code, CSS or resources. Legacy `.xml` themes keep working.
- **Explicit workbench colors.** A package can state its eight workbench roles instead of relying on colors derived from the
  editor. Text roles are still adjusted when needed to stay readable.
- **Optional theme fonts.** A package can ask for an editor font and a workbench font (family and size). A separate
  *Apply theme fonts* toggle controls them; applying colors never changes a font, a missing family is reported and skipped, and
  turning the toggle off restores the fonts that were replaced, except fonts you have changed yourself since.
- **Package import and export.** *Export as Package...* saves any installed theme as a new `.ectheme` file. A legacy theme is
  converted into a new file with a generated `legacy.<name>` id; the installed theme is not changed.
- **Preference page improvements.** Format row, Fonts section, export action, warnings listed after an import or update, and
  format-neutral wording.
- **Safety and ownership.** Every setting the plugin writes for the workbench, other plug-ins' editors and fonts is tracked
  value-exactly: it is restored only while it still holds what the plugin wrote, so your own later choices are never overwritten.

## Compatibility

- Runtime: Java 17 and later, as before.
- Workbench theming is verified on **Windows with Eclipse 4.40 and 4.41**. Other platforms and Eclipse versions are experimental
  and need an explicit opt-in. Theme fonts follow the same boundary and are not separately verified elsewhere.
- Native menus, dialogs, title bars and browser content are not themed. Some workbench surfaces may pick up a changed font only
  after restarting Eclipse.
- Not included: icon theming, CSS authoring or import, embedded or downloaded fonts, a marketplace or remote repository.

## Upgrading

No migration is needed. Existing themes, imports and settings are unchanged. Installed `.ectheme` files require this version or
later.

See [theme package format](THEME_PACKAGE_FORMAT.md) and [workbench theming compatibility](WORKBENCH_THEMING_COMPATIBILITY.md).
