# Evidence

| # | Finding | Source |
|---|---|---|
| E1 | 33 bundled files, all `<colorTheme ect-id=… type=…>`; every one has an `ect-id` and `type`. | `bundles/com.github.eclipsethemes/themes/*.xml` |
| E2 | Bundled identity today is `"bundled:" + theme.getId()`, where `getId()` is the raw `ect-id`; a missing id is logged and skipped. | `ThemeRepository.loadPackedThemes()` |
| E3 | `Theme.getId()` is absent for packages. Other consumers: `ThemeSelection.resolve` (legacy `ACTIVE_THEME_ID` matches raw id) and `EclipseThemesPreferencePage.previewUrl` (preview server URL + id). Both would regress for bundled packages without a fix. | those two files |
| E4 | No license/copyright/source metadata is verifiable for any bundled theme. Only `jupyter`, `vitesse_light`, `vitesse_light_soft` carry a comment "Copyright (c) 2024 Eclipse Themes / License: EPL v1.0", a boilerplate header that conflicts with the project license (EPL-2.0) and names no upstream. README thanks the original Eclipse Color Theme plugin generally. `<provenance>` is therefore omitted everywhere. | `grep -i "license\|copyright"`; `README.md` |
| E5 | The `modified` attribute is not read anywhere in `src`. | `grep modified` |
| E6 | `Constants.DEFAULT_LIGHT_THEME_ID` = `bundled:e541c789-…` (GitHub Light Default), `DEFAULT_DARK_THEME_ID` = `bundled:91d7ea64-…` (GitHub Dark Dimmed); both match legacy `ect-id`s. | `Constants.java`, theme headers |
| E7 | Legacy numeric `id` values are duplicated in places (e.g. 4009, 4010) but unused by the product. | theme headers |
| E8 | The `openspec` CLI is not installed in this environment; artifacts were authored from the archived-change layout and validated once the CLI was available (see verification notes). | tooling |
