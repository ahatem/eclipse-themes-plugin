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
