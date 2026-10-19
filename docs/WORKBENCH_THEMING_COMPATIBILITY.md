# Workbench theming (R8a) — compatibility and coverage

Status: implemented by the `dynamic-workbench-theming` change. The behavioral contract is
`openspec/specs/workbench-theming/spec.md` once archived; runtime evidence is in
`openspec/changes/dynamic-workbench-theming/evidence/runtime/`.

## What it does

When the user ticks **Also theme the Eclipse workbench** on the Eclipse Themes page and applies a theme, the plugin derives a
fixed seven-color palette from that theme (background, foreground, surface, raised, border, muted, selection), publishes it to
the workbench color registry and switches Eclipse to one of its own static CSS themes, which read those colors. The editor colors
are still applied exactly as before. Importing a legacy XML theme alone never touches the workbench; it is coordinated only when
the box is ticked and the theme is applied.

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

Coordinated: the window, part stacks and their tabs, sash and part containers, tool bars and trim, the status line, trees and
tables. Not coordinated: native menus, native dialogs, title bars, native push buttons, Browser/HTML content (for example the
Eclipse welcome/news tab), editor text areas (those follow the editor colors), arbitrary third-party custom-drawn widgets and icons.
The runtime screenshots show the Browser content and native buttons staying dark or light independently of the chosen theme.
Fonts and icons are not touched.

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
reason, the active Eclipse theme and the stored coordination state to the clipboard. Nothing is sent anywhere.
