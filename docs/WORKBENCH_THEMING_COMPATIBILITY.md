# Workbench theming (R8) — compatibility and coverage

Status: implemented by the `dynamic-workbench-theming` change (workbench coordination) and the `plugin-color-coordination` change
(colors of other plug-ins). The behavioral contracts are `openspec/specs/workbench-theming/spec.md`, `plugin-color-coordination` and
`preference-ownership`; runtime evidence is in the `evidence/` folders of the archived changes under `openspec/changes/archive/`.

## What it does

When the user ticks **Also theme the Eclipse workbench** on the Eclipse Themes page and applies a theme, the plugin converts that
theme into an eight-role palette (background, foreground, surface, raised, border, muted, selection, accent), publishes it to the
workbench color registry and switches Eclipse to one of its own static CSS themes, which read those colors. The editor colors are
applied by the same single Apply, after the workbench switch (Eclipse's own theme switch deletes editor preference values that the
previous stock theme overrode, so the order matters). Importing a legacy XML theme alone never touches the workbench; it is
coordinated only when the box is ticked and the theme is applied.

## Deterministic semantic conversion

The conversion is pure and deterministic and never reads a syntax token (keyword, string, number, comment, class, method, ...);
syntax colors never become chrome. Each role is resolved in this order:

1. a suitable authored candidate of the same or a related non-syntax role: `sourceHoverBackground` / `currentLine` for the surface and
   raised colors, `lineNumber` for secondary text, `selectionBackground` for the selection;
2. a perceptual derivation from the theme's own background in OKLCH, so hue and chroma character are kept (a cool charcoal stays
   cool, a warm earth tone stays warm, a cream stays cream);
3. a readability correction that moves only lightness (keeping hue) until text reaches 4.5:1 on the surfaces it is drawn on and the
   foreground reaches 3:1 on the selection;
4. the family fallback palette, last, when background or foreground is missing or the result still fails those rules.

An authored candidate is accepted only when it reads as a chrome color: a small lightness step from the background, no stronger
chroma than the background plus a small allowance, and readable under the foreground. A neon current-line color therefore never
turns tab bars neon, and a low-contrast line-number color never becomes secondary text. The accent (links, active-stack indicator) is
the one role without authored source: it is a fixed neutral blue corrected for readability on each theme. Cost measured on Windows: about
15 ms for the first conversion in a session and 0.15-0.35 ms afterwards.

## Support matrix

Support is decided by the exact combination of `os`, `ws` and the version of the `org.eclipse.e4.ui.css.swt.theme` bundle
(major.minor.micro), not by an Eclipse release name.

| State | Meaning | Combinations |
|---|---|---|
| **VERIFIED** | Measured on a real workbench | `win32`/`win32` with theme bundle `0.15.100` (Eclipse 4.40) and `0.15.200` (Eclipse 4.41) |
| **EXPERIMENTAL** | Not measured; off unless the user opts in for that exact combination | any other `os`/`ws`/version where the theme engine is usable (includes Linux/GTK and macOS) |
| **UNAVAILABLE** | Cannot be used; the checkbox is disabled and the reason is shown | no theme engine, theme bundle not linkable, the plugin's own themes not registered, operating-system high contrast, or a failed switch earlier in the session |

The VERIFIED list is evidence, not an allow-list: a newer micro version is EXPERIMENTAL, not blocked. Linux, GTK3/GTK4, X11/Wayland
and macOS are distinct rendering environments and are not claimed equivalent to Windows. High contrast is never overridden.

## Coverage boundary

Classification: **THEMED** (Eclipse CSS styles it from the palette), **SYSTEM/NATIVE** (drawn by the operating system or a native
widget that Eclipse CSS cannot restyle; the stock Eclipse Dark and Light themes show the same behavior), **NOT APPLICABLE** (outside
the workbench-chrome contract) and **THIRD-PARTY** (colors owned by another plug-in). Verified on Windows with Eclipse 4.40 and
4.41; Linux/GTK and macOS are not claimed.

| Surface | Class | Notes |
|---|---|---|
| Window, part sash containers, trim, tool bars, status line | THEMED | |
| Part stacks and tabs: active, inactive, selected, unselected, hot; editor tab row | THEMED | tab text uses foreground (selected) and muted (unselected) |
| Views: Package Explorer, Outline, Console, Problems, other tree/table/list views | THEMED | background, foreground, header; the selection/hover row color is the platform's |
| Dialogs and Preferences: shell, composites, labels, groups (background and text), links, filtered trees, forms/sections | THEMED | the dialog stays themed when open during an Apply |
| Text, Spinner, List, Combo, CCombo, read-only fields, enabled and disabled | THEMED | field background and text; read-only and disabled use the same field colors with dimmed text |
| Push, check and radio buttons | THEMED (labels, background) / SYSTEM-NATIVE (frame, check mark) | the native frame and focus ring stay light on dark, as in stock Dark |
| Dialog-level `CTabFolder` | THEMED | selected fill, outline and text |
| Group frame lines | SYSTEM/NATIVE | not exposed to CSS; light frame on dark as in stock Dark |
| Native `TabFolder` tab strip, progress bar, scale | SYSTEM/NATIVE | white-ish in stock Dark too |
| Combo (non-CCombo) drop-down arrow and popup list | SYSTEM/NATIVE | the field itself is themed |
| Scroll bars | SYSTEM/NATIVE | follow the operating-system theme; after switching to a light theme they stay dark on a dark Windows, identically to stock Dark to Light |
| Menu bar, menus, title bar, window frame, tooltips | SYSTEM/NATIVE | |
| Browser/HTML content, icons | SYSTEM/NATIVE / NOT APPLICABLE | |
| Editor text area | NOT APPLICABLE | follows the editor colors, not the workbench CSS |
| Forms (sections, form text, read-only form fields, form heading error/warning/info text), Properties view tab list and title, compare and progress panes, Marketplace items | THEMED | same selectors as stock Dark/Light; form heading message colors read the platform `FORM_HEADING_*` definitions |
| Fonts of part tabs and tree/table views | THEMED (platform fonts) | read the platform *Tab text* and *Tree and Table* font definitions, as stock does, so a font chosen under Colors and Fonts applies; theme files set no fonts |
| Widgets of other plug-ins that the stock sheets style by class (EGit, Marketplace, JDT, PDE, Debug, Mylyn, Buildship, WTP wizard and dialog pages, breadcrumbs) | THEMED | styled in the same sheets by palette role, selector for selector with stock; selectors that would also match an editor's text control are left out |
| Preference-based colors of other plug-ins that stock Dark re-tunes (popups, hyperlinks, error and form-heading text, Compare, EGit diff and label colors, search/revision/coverage/breakpoint annotations, JDT/Debug/Ant/Mylyn colors) | THEMED in dark coordinated themes, ownership-checked (see below); THIRD-PARTY otherwise | 68 declared keys; written only while a dark coordinated theme is active and only where the user has no value of their own. Light themes and editor-only mode write none |

Selectors are plain type, class and descendant selectors, almost all of them identical to the stock Eclipse Dark/Light sheets, whose
widget, class and id selectors were compared one by one against ours (the missing ones were added). Those that are not identical (`.MPart`-scoped Text/Combo/List/Canvas, `SashForm`, `ScrolledForm`, `.MCompositePart`, `.MToolBar`) are ordinary
descendant selectors and each had its effect observed on the widgets. Eclipse is not asked to restyle anything it cannot: no
`Group`, scroll bar or menu property is attempted. The sheets contain no `IEclipsePreferences` or `ColorDefinition` rules: Eclipse
turns both into instance preference writes (the owning plug-in's node, or `org.eclipse.ui.workbench` for color definitions), overwriting a user's value on entry and deleting it
on the next theme change (measured; a test keeps them out). High-contrast mode disables coordination, so accessibility choices are never
overridden.

## Plug-in colors and the ownership rule

Colors that other plug-ins keep in their own preferences (popups, hyperlinks, diff and status colors, annotation colors, Mylyn and
Debug colors) cannot be set through the static CSS without overwriting and later deleting the user's own values, which is what stock
Dark does. The plugin coordinates a fixed, declared table of such keys (`PluginColorTable`, 68 keys, four of them inert on 4.40,
each with the owning bundle and a palette role) by this rule:

1. Only while a **dark** coordinated theme is active. Light themes, editor-only mode and disabled coordination write none of them.
2. A key that already has an instance value is the user's and is never written, whatever its origin.
3. An absent key may be taken; the exact value written is recorded.
4. On a later Apply the key is updated only if it still holds the recorded value; otherwise it is released and left alone.
5. Release (light theme, coordination disabled, or another Eclipse theme chosen on the Appearance page) removes a key only if it
   still holds the recorded value. The plugin never writes a guessed or default value and never clears a preference node.

Status colors are not authored by themes: error, warning, success and information use fixed hues corrected for readability on the
theme's surfaces, and diff, coverage and search highlights are the theme background pulled toward those hues only as far as the
theme's foreground stays readable on top. Nothing depends on a theme's name.

Not coordinated, by design: syntax and editor-surface colors (owned by the editor integrations), operating-system colors (for example
the range indicator, which defaults to the system list-selection color), and colors whose Eclipse default is already readable on a
dark workbench. `evidence/key-classification.md` in the archived `plugin-color-coordination` change lists every stock-Dark key with its class.

If the plugin is uninstalled while a dark theme is active, its colors stay in place (the plugin has no uninstall hook; stock Dark has
the same exposure). To remove them, untick **Also theme the Eclipse workbench** and apply before uninstalling, or reset the colors
under Colors and Fonts.

The page shows only counts ("N plug-in colors kept your own values"); **Copy diagnostics** adds the key names for owned, kept and
released colors. Nothing is sent anywhere.

## Consequences users and contributors will notice

- **Appearance preference page** lists four extra Eclipse themes: `Eclipse Themes Dark`, `Eclipse Themes Dark (alternate)`,
  `Eclipse Themes Light`, `Eclipse Themes Light (alternate)`. Eclipse offers no supported way to hide them. The pair per family
  exists because Eclipse re-reads CSS color references only when the theme *id* changes; the plugin alternates between them when
  the family stays the same. Choosing either one by hand is harmless.
- **Colors and Fonts** does not list the workbench palette (the definitions are non-editable). Eclipse also writes
  `com.github.eclipsethemes.workbench.<family>.<role>` entries into the instance `org.eclipse.ui.workbench` preferences while a
  palette is published; they are removed again when the plugin hands the workbench back. Eclipse ignores stored values for these
  definitions at startup, so the palette is re-derived from the selected theme on each start. Preference recorders that replay
  recorded keys (for example Oomph's preference task) can overwrite a published palette until the next Apply or restart.
- **Start-up**: the plugin registers an `org.eclipse.ui.startup` entry that is a no-op unless coordination is enabled. When it is
  enabled and one of the plugin's themes is still active it republishes the palette; the first paint after start is the family
  fallback palette until then.
- **Ownership**: the plugin remembers the previously active Eclipse theme and whether Eclipse had a saved theme choice at all.
  Unticking the box and applying restores exactly that, including removing the saved choice if there was none. If another Eclipse
  theme was chosen on the Appearance page in the meantime, the plugin never reasserts its theme; the page says that coordination is
  not the current effective state. Clicking Apply again with the box ticked is an explicit new request and coordinates again.
- **Apply timing** (Windows, measured): about 0.1–0.4 s per apply after the first; the first apply in a session was 0.2–1.0 s.
- **Internal API**: `IThemeEngine` is `x-friends` API of `org.eclipse.e4.ui.css.swt.theme`. It is used only in
  `EclipseWorkbenchThemeEngine`, through the public `getThemes`, `getActiveTheme` and `setTheme(ITheme, true)` calls, with the
  bundle as an optional `Require-Bundle` and `LinkageError` handled.

## Diagnostics

The page's **Copy diagnostics** action copies the combination (os, ws, theme bundle, platform, Java), the capability state and
reason, the active Eclipse theme, the stored coordination state and the plug-in color counts and key names (owned, kept as the
user's, released) to the clipboard. Nothing is sent anywhere.
