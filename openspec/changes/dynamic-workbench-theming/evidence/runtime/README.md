# R8a runtime verification (Windows)

Black-box runs of the packaged production bundle (`com.github.eclipsethemes_1.1.0.jar`, built from this branch) in two unmodified
Eclipse installs, driven through the real preference page (select a theme, tick the box, click Apply / Apply and Close / Cancel) by a
disposable probe bundle (`harness/`, not part of the product). The probe reads the active theme, the engine's persisted `themeid`
in each scope, the plugin's own preferences, the published palette and the resolved colors of the live widgets.

| | Eclipse 4.40 | Eclipse 4.41 |
|---|---|---|
| Platform / theme bundle | 4.40.0.v20260601-0713 / 0.15.100 | 4.41.0.v20260828-1142 / 0.15.200 |
| Java | 21.0.11 | 25.0.4.1 |
| OS / ws | Windows 11 / win32 | Windows 11 / win32 |
| Capability shown on the page | Verified on win32/win32 with theme engine 0.15.100 | Verified on win32/win32 with theme engine 0.15.200 |

Logs: `s1-*` dark/light/imported switches, `s2-*` restart + external Appearance override, `s3-*` restart + disable (saved id present),
`s4-*` cancel, enable, disable (saved id absent). Screenshots are `main`-shell prints of one dark, one light and the imported theme.

## Results (identical on 4.40 and 4.41 unless noted)

| Scenario | Result |
|---|---|
| dark to dark (Atom One Dark to Catppuccin Mocha) | Active theme alternated `...workbench.dark` to `...dark.alt`; palette republished (`#282c34` to `#1e1e2e`); shell, tabs, trees, tables and status line resolved the new colors |
| dark to light (to Atom One Light) | `...light`; family palette `#fafafa`/`#383a42`; the dark family kept its last palette (unused) |
| light to light (to Solarized Light) | `...light.alt`; `#fdf6e3`/`#657b83` |
| imported legacy XML (R8a Imported Plum, bg `#2B0A3D`, fg `#FFE066`, selection `#006666`) | Imported without touching the workbench; on Apply surfaces resolved exactly the derived colors (shell `#2b0a3d`, status line `#36153f`, fg `#ffe066`) |
| Cancel with box ticked | no change: theme, preferences and registry untouched |
| Enable, then disable (saved `themeid` absent before) | active theme back to `e4_dark`, instance `themeid` absent again, palette ids at the fallback, no `...workbench.*` entries left in `org.eclipse.ui.workbench.prefs`, ownership keys cleared |
| Disable (saved `themeid` present) | active theme and instance `themeid` back to the prior `e4_default` |
| External Appearance override | The page reports "Coordination is not the current effective state"; the published palette and the chosen Eclipse theme were left alone |
| Apply after an external override | Coordinates again (explicit request) and captures the external theme as the new prior state |
| Restart | the engine's saved theme persisted; the startup entry republished the palette from the selected theme (observed 5 s after boot: plum in the dark family, Atom One Light in the light family); the instant-of-first-paint state was not measured |
| Status line after Apply | `Applied (editors and workbench)`; `Applied (editors); workbench handed back` after disable |
| Apply duration (click to return, ms) | 4.40: 247 110 108 101 115 / 110 / 166 / 980 433; 4.41: 197 105 107 389 93 / 117 / 178 / 178 356. The 980 ms and 247 ms cases are first applies in a session |

Browser content (the Eclipse "Install Java 27 Support" view) and native push buttons keep their own colors in every screenshot, which is
the documented coverage boundary.

## Not verified at runtime

- The EXPERIMENTAL opt-in flow: both installs are VERIFIED, so there is no clean seam without faking version metadata. Covered by
  `WorkbenchCoordinatorTest` and `WorkbenchSectionModelTest` only.
- High contrast (UNAVAILABLE reason) and Linux/GTK/macOS: unit-level classification only.
- The failed-switch revert path: unit tests with a fake engine.

## Environment finding during the runs

The first 4.40 run showed the published palette replaced by another theme's palette about 300 ms after Apply. A registry listener with
a stack trace showed `org.eclipse.oomph.setup.impl.PreferenceTaskImpl` writing
`org.eclipse.ui.workbench/com.github.eclipsethemes.workbench.dark.*` from the user's global
`~/.eclipse/org.eclipse.oomph.setup/setups/user.setup`, which already contained recorded entries for those keys. The EPP test installs
contain Oomph, so every run replayed them. The runs reported above set `-Duser.home` to a sandbox, so they neither read nor wrote that file. Earlier, unsandboxed runs of the same
installs may have recorded entries into it (it holds `com.github.eclipsethemes.workbench.dark.*` and plugin-preference entries from this
feature). A copy of the file as found was taken before any change; the file itself was not edited. Maintainers who run Oomph-based
installs against it may want to remove those entries.

## Finishing pass: one Apply, deterministic conversion, stock parity

Sources: `finishing/` (logs and screenshots), `harness/finishing/` (probe sources, step files, the seven disposable QA themes; none of
them are in the product catalog). All runs use a fresh, non-git-ignored workspace and a sandboxed `-Duser.home`.

### One Apply is enough

Root cause: `ThemeEngine.setTheme` ends in `sendThemeChangeEvent`, which resets the preference defaults the previous CSS theme
overrode and deletes the instance values for the same keys. The editor keys written before the switch were therefore deleted when the
stock `e4_dark` theme (or any theme that overrides editor keys) was left. `ThemeService` now switches the workbench first and writes
the editor preferences afterwards, in every transition, with no delay or retry; only keys the editor writer owns are written.
Regression test: `ThemeServiceOrderTest` (platform fake that deletes editor keys on `select`). Runtime proof, one Apply per step:

| Run | Sequence | Result |
|---|---|---|
| `q1-r40-six-theme.log` (4.40) | Atom One Dark, Dracula, Catppuccin Mocha, Gruvbox Dark, Solarized Light, GitHub Light, Atom One Dark | after each single Apply the editor `Background`/`Foreground` prefs and the live `StyledText` colors are the selected theme's; family and twin slot are correct |
| `q6-r41-six-theme-teal.log` (4.41) | same, plus Unusual Teal, with the dialog reopened after every step | same; dark to light (Gruvbox to Solarized) and light to dark (GitHub to Teal) included; `lowContrastKinds=0` in every audit |
| `q5-r40-teal.log` (4.40) | Atom, Teal, Solarized, Teal, GitHub, Mocha | same |

Apply (click to return): 4.41 `847` (first in session), `217 213 222 206 193 181 344`; 4.40 `500` (first), `202 188 189 211 247`. Palette conversion:
about 15 ms cold (first call in the JVM) and 0.15 to 0.35 ms warm.

### Black Package Explorer label

The black "Demo" label under a dark theme is EGit's `IgnoredResourceForegroundColor`, which defaults to `COLOR_LIST_FOREGROUND` and is
overridden only by an EGit stylesheet bound to the stock `e4_dark` theme id (stock Dark sets 120,120,120). The earlier test workspace
was inside a git-ignored folder, so every project there was decorated as ignored. In a normal workspace the label is the tree
foreground (`#abb2bf` in `q5`/`q6` logs). It is a third-party stylesheet bound to a stock theme id, classified THIRD-PARTY in the
coverage matrix.

### Stock parity (control gallery, 4.40)

`q4-g-sdark.png`, `q4-g-slight.png` (stock Dark/Light) against `q4-g-atom.png`, `q4-g-solarized.png` (Atom One Dark, Solarized Light):
Combo and CCombo fields, disabled states, Spinner, List, Text, push/check/radio, CTabFolder (selected tab and body), Table, Tree and
Scale are themed the same way as stock. The remaining native appearances (Group frame line, button frames, the native `TabFolder`,
Combo/CCombo drop arrows, progress bar, scrollbars, menus, title bar) are identical in stock Dark and Light, so they are SYSTEM/NATIVE.
Stock Dark to stock Light also leaves dark native scrollbars; they follow the OS theme. A dialog-level `CTabFolder` selected tab was
white until the `CTabFolder` rule gained `swt-selected-tab-fill`, outline and keyline colors; it is themed in the captures above.

### Preferences dialog

`q5-teal-reopened-dlg.png`, `q6-gruvbox-reopened-dlg.png` (dark) and `q6-solarized-reopened-dlg.png` (light): the dialog applied while
open and the dialog reopened afterwards both use the new palette (text, fields, lists, group captions, buttons). The main-shell
`PrintWindow` captures (`q5-teal.png`) are reliable only for dark steps; the resolved-color audits in the logs are the authority.
