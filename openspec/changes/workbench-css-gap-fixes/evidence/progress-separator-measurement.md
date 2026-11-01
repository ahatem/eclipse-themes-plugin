# Evidence: progress bar and separator measurement

Probe: `runtime/probe` (run with `run.ps1`), Windows 11 (os.version 10.0), SWT win32 4973, plugin built from `origin/develop` CSS (byte-identical to this branch before the change). Eclipse 4.40 (`org.eclipse.e4.ui.css.swt.theme` 0.15.100) and 4.41 (0.15.200). For each coordinated family the probe opens a shell with a separator `Label`, a bare `ProgressBar` and a JFace `ProgressMonitorPart` (whose `ProgressIndicator` holds the nested bars). It samples rendered pixels through `PrintWindow(PW_RENDERFULLCONTENT)` with the shipped sheet ("base"), then after parsing the candidate rules into the live CSS engine ("candidate").

Raw logs: `runtime/out/measure-440.log`, `runtime/out/measure-441.log`. Screenshots: `runtime/out/measure-44{0,1}-{dark,light}-{base,candidate}.png`.

The results are identical on 4.40 and 4.41.

| Surface | Family | Base pixel (track / fill) | Candidate pixel (track / fill) | Verdict |
|---|---|---|---|---|
| `ProgressBar` nested in `ProgressMonitorPart > ProgressIndicator` (wizards, dialogs) | dark | 240,240,240 / 0,120,215: OS light track on a 37,37,38 page | 30,30,30 / 90,162,232 (background / accent roles) | **Fixed by `ProgressBar` rule** |
| Bare `ProgressBar` directly in a `Composite` | dark | 37,37,38 / 212,212,212: already themed through `Composite > *` | 30,30,30 / 90,162,232 | Consistent with nested bars |
| Any `ProgressBar` | light | 230,230,230 / 15–17,123–125,15–17: Windows visual style | unchanged | **Native**: Windows ignores colors while visual styles are active (light). Harmless on a light page |
| Separator `Label` (`SWT.SEPARATOR`) | both | 160,160,160 over 255,255,255 etched line | unchanged | **Native**: no rule shipped |

Why the dark family differs: SWT drops the visual style of these controls when the dark theme is preferred, which is decided from the theme id containing `dark` on these releases. Background and foreground are then honored.

Consequences for the change:
- Ship `ProgressBar { background-color: <background>; color: <accent>; }` in both family sheets (the sheets stay identical; the rule is inert on the light family on Windows).
- `ProgressIndicator > *` is not needed. The `ProgressBar` type selector matches the nested bars whatever their parent. The final re-run (task 4.1) confirms this with the shipped sheet.
- Do not ship the separator rule. The separator stays SYSTEM/NATIVE in the coverage table.
- All four themes register and stay selectable on 4.40 and 4.41 (`THEME` lines in both logs).

## Final run (shipped stylesheets, task 4.1 and 2.2)

`out/final-440.log` and `out/final-441.log` used the reactor-built jar that carries the `ProgressBar` rule and the `isDarkTheme` attributes. On both releases:

- All four coordinated themes are listed by `IThemeEngine.getThemes()` (`THEME` lines) and activate (`ACTIVE` lines) with the attribute present.
- Dark family, "base" phase (no candidate sheet): the progress monitor bar track samples 30,30,30 (background role) and the fill samples 90,162,232 (accent role), for the bare bar and the bar nested in `ProgressMonitorPart > ProgressIndicator`. So the shipped rule alone produces the result that the candidate run measured.
- Light family: the track stays at 230,230,230 and the fill at 15,123,15, which is the native Windows look.
- Separator lines stay at 160,160,160 / 255,255,255 (native).
