# Static compatibility matrix, Eclipse 4.33–4.41 (Windows scope, no new runtime)

Step 1 of the 4.34–4.40 verification plan. **Static only**: nothing in this file was run in an IDE.
Runtime evidence exists only for 4.33 (R7 spike, §1–§33) and 4.41 (§36). Classification labels:
`RUNTIME VERIFIED`, `STATICALLY EQUIVALENT TO <release>`, `UNVERIFIED`, `UNSUPPORTED`.

## Method and limits

- Source: the official p2 release repositories `https://download.eclipse.org/eclipse/updates/<4.x>/R-…`
  (R-4.34-202411201800, R-4.35-202502280140, R-4.36-202505281830, R-4.37-202509050730,
  R-4.38-202512010920, R-4.39-202602260420, R-4.40-202606010713). 4.33 and 4.41 came from the
  local installs already used for the earlier runs. Only individual bundle jars were fetched.
- Compared with `javap -p -c` after stripping constant-pool indices: `ThemeEngine` (per method),
  `ITheme`, `IThemeEngine`, `IThemeManager`, `PartRenderingEngine.setCSSTheme` (startup caller),
  `ViewsPreferencePage` (Appearance page), `EclipsePreferencesHelper`, the theme bundle's
  `plugin.xml` and manifest, `org.eclipse.ui.themes` theme IDs, `ui.ide` page registration, and
  the whole `css.core`/`css.swt` class sets.
- **Not compared**: the rest of `ui.workbench`/`ui.ide`/`workbench.swt`/`renderers.swt`, SWT,
  Equinox, OS or JRE behaviour, GTK/macOS. Static equality of bytecode is not proof of equal
  rendering. Byte-identical theme-engine code is inherited **only for theme-engine semantics**;
  Appearance page, startup caller and contribution inputs were checked separately below.

## Bundle identity (sha256 prefix)

| Release | theme bundle | css.core | css.swt |
| --- | --- | --- | --- |
| 4.33 | 0.14.400 `4518cc8f6898` | 0.14.500 `fdec437c91dd` | 0.15.400 `032ea202f585` |
| 4.34 | 0.14.400 `4518cc8f6898` | 0.14.500 `fdec437c91dd` | 0.15.400 `032ea202f585` |
| 4.35 | 0.14.500 `f2e453a7f085` | 0.14.500 `fdec437c91dd` | 0.15.500 `107c880d7a8b` |
| 4.36 | 0.14.500 `f2e453a7f085` | 0.14.500 `fdec437c91dd` | 0.15.600 `4cd11e4ee969` |
| 4.37 | 0.14.500 `f2e453a7f085` | 0.14.500 `fdec437c91dd` | 0.15.600 `4cd11e4ee969` |
| 4.38 | 0.14.600 `5ff74ab61156` | 0.14.600 `f1640c09a625` | 0.15.700 `6e0f2545aed5` |
| 4.39 | 0.15.0 `c66e74f7eef9` | 0.14.600 `f1640c09a625` | 0.16.0 `29ff1ec5c961` |
| 4.40 | 0.15.100 `8694da84401f` | 0.14.700 `56311b9cb2ea` | 0.17.0 `3c34aedda309` |
| 4.41 | 0.15.200 `95d992f4f9c0` | 0.14.800 `a1c646759f62` | 0.17.100 `0aed7fe44e43` |

## Behaviour matrix

Letters A/B/C mark distinct `setTheme(ITheme,boolean,boolean)` code; "same" means the same bytecode as the 4.33 column.

| P4-relevant item | 4.33 | 4.34 | 4.35–4.37 | 4.38–4.39 | 4.40 | 4.41 |
| --- | --- | --- | --- | --- | --- | --- |
| `Export-Package` `x-friends` line (renderers.swt, workbench.swt, ui.workbench) | same | same | same | same | same | same |
| `ITheme`, `IThemeEngine`, `IThemeManager`, `Events` bytecode | identical in all nine releases (no `isDark`) | | | | | |
| `getThemes()`, `getActiveTheme()`, ctor/registration, `registerTheme`, `getStylesheets` | identical in all | | | | | |
| theme bundle `plugin.xml` (extension declarations) | identical in all | | | | | |
| `setTheme(ITheme,boolean,boolean)` | A | A | A | A | B (+`Display.setDarkThemePreferred(id.contains("dark"))`) | C (B + `publishEffectiveThemeId()`, new `parseStyleSheet(InputStream,String)`) |
| writes instance `themeid` + flush when `restore=true` | yes | yes | yes | yes | yes | yes |
| `DefaultScope themeid` published | no | no | no | no | no | **yes** |
| `getPreferenceThemeId()` | instance only | same | same | same | `IPreferencesService` lookup | adds config/instance scopes |
| `restore(String)`, saved theme **found** | `setTheme(theme,false)` | same | same | same | same | same |
| `restore(String)`, saved theme **unavailable** | legacy | legacy | dark-substring choice (`4e127ef1f1`) | same as 4.35 | no `e4_classic` remap | same as 4.40 |
| `setTheme(String,…)` unknown ID | no-op | no-op | no-op | no-op | default Light + persist | + OS high-contrast check |
| `Display.getHighContrast()` inside `ThemeEngine` | none | none | none | none | none | `setTheme(String)` only |
| `setTheme(ITheme,…)` high-contrast guard | none | none | none | none | none | none |
| `setDarkThemePreferred` anywhere (engine, css.*, workbench, workbench.swt, renderers.swt) | none | none | none | none | engine only | engine only |
| startup caller `PartRenderingEngine.setCSSTheme` | identical in all (calls `restore`/`setTheme(String)`, HC check) | | | | | |
| Appearance page ID `org.eclipse.ui.preferencePages.Views` (ui.ide, `appearancePreferencePage` factory) | present | present | present | present | present | present |
| Appearance `performOk` applies via `setTheme(ITheme,true)` then `showRestartDialog` | yes | yes | yes | yes | yes | yes |
| Appearance gating | `engine` + HC | same | same | `isThemingPossible()` (engine non-null, not HC) | same | same |
| Appearance "Manage default…" dialog | no | no | no | no | yes | yes |
| `EclipsePreferencesHelper.setPreviousThemeId` | same | same | same | same | same | same (two helper methods added in 4.35) |
| Built-in theme IDs (`ui.themes`) | classic, dark, default, preview, HC | classic, dark, default, preview, HC | 4.35/4.36: classic, dark, default, HC; 4.37: classic, dark, default | classic, dark, default | dark, default | dark, default |
| Theme bundle execution environment | JavaSE-17 | JavaSE-17 | JavaSE-17 | 4.38 JavaSE-17; **4.39 JavaSE-21** | JavaSE-21 | JavaSE-21 |

Note: `org.w3c.css.sac` is imported through 4.40 and dropped by 4.41.

## CSS engine families (rendering is **not** statically provable)

The selection API is stable, but the CSS parser/engine that applies the contributed stylesheet
is not. Normalized `javap` differences between neighbouring builds (css.swt / css.core):

- 4.34 → 4.35: css.swt 336 lines; css.core identical.
- 4.35 → 4.36: css.swt 164 lines; css.core identical. 4.36 ≡ 4.37 (identical jars).
- 4.37 → 4.38: css.core 115 lines; css.swt ≈ 564 lines vs 4.36.
- 4.38 ≡ 4.39: css.core identical jar; css.swt normalized identical (0 differing lines, recompiled).
- 4.38 → 4.40: css.core 460 lines, 5 serializer/color-config classes removed.
- 4.40 → 4.41: **parser replaced**: 75 SAC-based classes removed from css.core (26 `impl/sac`,
  22 `impl/dom`…), new `impl/parser` and `impl/engine/selector` added; css.swt loses 21 classes;
  `org.w3c.css.sac` no longer imported.

So CSS parse/apply is the same code for 4.33/4.34 (and 4.36/4.37, and 4.38/4.39) but differs
across those groups and across 4.40 and 4.41.

## Classification per release

"Engine semantics" = selection/enumeration/persistence/startup/Appearance-path. "CSS apply" = the
contributed stylesheet loads and styles at least the proof surfaces.

| Release | Engine semantics | CSS apply | Overall |
| --- | --- | --- | --- |
| 4.33 | RUNTIME VERIFIED (R7, Windows + GTK3/X11; wide probe, not the narrow v3 probe) | RUNTIME VERIFIED (R7) | RUNTIME VERIFIED (baseline) |
| 4.34 | STATICALLY EQUIVALENT TO 4.33 (identical theme, css.core, css.swt jars; same startup caller and Appearance theme path) | STATICALLY EQUIVALENT TO 4.33 | STATICALLY EQUIVALENT TO 4.33, if R7 is accepted as the 4.33 anchor |
| 4.35 | STATICALLY EQUIVALENT TO 4.33 for `setTheme(ITheme)`/`getThemes`/`getActiveTheme`/found-case `restore` | **UNVERIFIED** (css.swt drift) | UNVERIFIED |
| 4.36 | same as 4.35 | **UNVERIFIED** | UNVERIFIED |
| 4.37 | STATICALLY EQUIVALENT TO 4.36 (theme, css.core, css.swt jars identical; Appearance theme calls identical; only `ui.themes` drops the HC theme) | STATICALLY EQUIVALENT TO 4.36 | UNVERIFIED until 4.36 runs |
| 4.38 | same code as 4.35 for the critical methods; Appearance adds `isThemingPossible()` | **UNVERIFIED** (css.core changed) | UNVERIFIED |
| 4.39 | STATICALLY EQUIVALENT TO 4.38 | STATICALLY EQUIVALENT TO 4.38 | UNVERIFIED until 4.38 runs |
| 4.40 | **differs** from 4.39 and 4.41 (dark-preferred call, preference lookup, restore, parser) | **UNVERIFIED** | UNVERIFIED, needs runtime |
| 4.41 | RUNTIME VERIFIED (§36) | RUNTIME VERIFIED | RUNTIME VERIFIED |

No release is classed `UNSUPPORTED` by this pass; that is a maintainer decision (§30).

## Corrections and cautions this pass adds

1. **Dark preference is 4.40+**: `setTheme(ITheme,…)` calls `Display.setDarkThemePreferred` only from
   4.40. §36.10 describes 4.41; in 4.33–4.39 the engine never informs SWT, so a dark family's
   native-chrome behaviour there is unmeasured. Also relevant to any dark-ID heuristic claim.
2. **High contrast**: no `ThemeEngine` HC check exists in 4.33–4.40; 4.41 has one only on the
   `String` path. The boundary's own `Display.getHighContrast()` refusal is needed on every release.
3. **DefaultScope publication** exists only in 4.41; code must use `getActiveTheme()` (as P4 does),
   not `DefaultScope`, on 4.40 and earlier.
4. **HC theme** is in `getThemes()` through 4.36; Classic through 4.39. The boundary selects
   by exact own ID, so this does not matter for P4.
5. **Unavailable saved theme** (e.g. our plugin uninstalled) falls back differently in four
   groups (4.33/4.34, 4.35–4.39, 4.40, 4.41); not a P4 dependency, but a user-visible difference.

## Which releases need runtime

The criterion: a release needs its own run when the P4-critical code, or the CSS engine that
applies the contributed stylesheet, is not equal to a runtime-verified release.

| Release | Run? | Why |
| --- | --- | --- |
| 4.40 | **Yes** | Engine code differs from 4.39 and 4.41; CSS engine differs from both. |
| 4.38 | **Yes if the floor is ≤4.38** | CSS engine differs from 4.33/4.37 and 4.40. Also covers 4.39. |
| 4.36 | **Yes if the floor is ≤4.37** | css.swt differs from 4.34/4.35 and 4.38. Also covers 4.37. |
| 4.35 | **Yes if the floor is ≤4.35** | css.swt differs from 4.34 and 4.36. |
| 4.34 | Optional | Static-equivalent to 4.33; run only if R7 is not accepted as the anchor. |
| 4.37, 4.39 | No | Inherit 4.36 / 4.38 (compared components only). |

Minimum run set by candidate floor: 4.40 → {4.40}; 4.38 → {4.40, 4.38}; 4.36/4.37 → add 4.36;
4.35 → add 4.35; 4.34 → add nothing if R7 is accepted (else 4.34 too).
