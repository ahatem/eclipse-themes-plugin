# Theme Package v1 — Runtime Verification (task 8.3)

Date: 2026-10-19. Product: `com.github.eclipsethemes` 1.1.0.202610191106, the Tycho-built bundle of commit `80f6d38`.
No production code was changed by this verification.

## Environment

| | Eclipse 4.40 | Eclipse 4.41 |
|---|---|---|
| Platform | 4.40.0.v20260601-0713 | 4.41.0.v20260828-1142 |
| CSS theme bundle | 0.15.100 | 0.15.200 |
| Java | 21.0.11 | 25.0.4.1 |
| OS | Windows 11 (win32), OS dark mode on | same |

The harness is the R8 runtime harness (the archived `dynamic-workbench-theming` evidence) renamed to r9. It uses:

- an unmodified install;
- a fresh `-configuration` and `-data` per scenario family, with `-clean` and a sandboxed `user.home`;
- a startup probe bundle driving the real preference page.

The native file dialog is driven from outside by `fd.ps1`, using Win32 messages plus UI Automation for reading.

Restart scenarios reuse the same configuration and workspace.

Evidence is in `evidence/runtime/`:

- probe logs `s4*.log` and file-dialog logs `*-uia.log`;
- the step files `*.steps.txt`;
- `Probe.java.txt`, `run.ps1` and `fd.ps1`;
- the QA corpus `qa/` (the 270 KB oversize file is not committed; it was `qa-e1` padded with an XML comment to 276,816 bytes);
- the Eclipse `.log` of each workspace, as `eclipse-s4*.log`;
- screenshots.

## QA corpus

| File | Purpose |
|---|---|
| A `qa-a-minimal-dark.ectheme` | No provenance, no `<workbench>` (automatic derivation). |
| B `qa-b-authored-dark.ectheme` | Full provenance; 7 distinctive authored roles. |
| B2 `qa-b2-authored-dark.ectheme` | Same package id, version 3.3, used for the same-format update. |
| C `qa-c-readability-dark.ectheme` | Authored `accent #3a4a7a`, with contrast 1.91:1 against the background. Source `file:///C:/Windows/win.ini`. |
| D `qa-d-light.ectheme` | Light family with authored surface and accent. |
| E1 `qa-e1-schema2.ectheme`, E2 `qa-e2-missing-fg.ectheme`, E4 oversize | Invalid packages. |
| `qa-legacy.xml`, `qa-legacy-replacement.xml` | Legacy control, and the cross-format update attempt. |

## Results

| # | Check | Result | Evidence |
|---|---|---|---|
| 1 | File dialog filters | **Pass (partial automation).** The dialog offers *Eclipse Themes (\*.ectheme, \*.xml)*, *Theme Packages (\*.ectheme)* and *Legacy Theme Files (\*.xml)*. The default filter lists every `.ectheme` and `.xml` and hides a `.txt` decoy. Both formats were selected and opened through it. | `s41a-uia.log` |
| 2 | Valid import through the UI (A, B, C, D, legacy) | **Pass.** "Import Successful" each time. Each gets an `local:<uuid>` identity; packages are stored as `<uuid>.ectheme` and the legacy file as `<uuid>.xml`. | `s41a.log` MSG/CATALOG/FILES |
| 3 | Invalid import diagnostics | **Pass.** "Import Failed" with actionable text: E1 *schema version 2 is not supported; it requires a newer Eclipse Themes*; E2 *`<editor>` requires a valid `<foreground>` token*; E4 *larger than 256 KiB*. Nothing was stored. | `s41a.log` |
| 4 | Details panel | **Pass.** B shows Version 3.2, License MIT, Copyright "R9 QA Holder" and a Source link. A shows "Not stated" for all four. C's `file:` source is shown as inert text, not a link. No row is clipped (preferred size ≤ bounds, inside the group). | `s41a.log` DETAILS imp-A/imp-B/pick-C |
| 5 | No remote preview for packages | **Pass.** A LocationListener on the preview Browser recorded only `about:blank` whenever a package was selected (A/B/C/D, all sessions). The legacy control navigated to `…/preview/r9-qa-legacy`, which is unchanged legacy behaviour. | `PREVIEW-NAV` lines |
| 6 | Explicit workbench roles (B) | **Pass, one Apply.** The palette shows `foreground #e8e0ff`, `surface #3a1d4a`, `raised #4b2a5e`, `border #ff8800`, `muted #b8c4a8`, `selection #005f5f` and `accent #ffcc00`, exactly as authored. Background `#1b1f2a` comes from the editor. Active theme `…workbench.dark`. | `REGS B` (4.41), `REGS B` (4.40) |
| 7 | Automatic derivation (A) | **Pass.** With no roles authored, the palette is derived from the editor: `surface #222632`, `selection #323848`, `accent #67b0f9`. | `REGS A` |
| 8 | Readability correction (C) | **Pass.** Authored `accent #3a4a7a` resolved to `#879bd1` (lightness only). The authored non-text roles were kept verbatim: `surface #262c3b` and `selection #005f5f`. Admission logged the WARNING *Workbench role 'accent' is not readable with the other workbench colors; it will be adjusted when applied*. The screenshot shows a readable workbench. | `REGS C`, `eclipse-s41.log`, `s41a-C.png` |
| 9 | Light package (D) | **Pass.** Light palette: `surface #efe6d2`, accent `#b03a00` resolved to `#a5431f`. The workbench switches to `…workbench.light`. | `REGS D` |
| 10 | Legacy XML regression | **Pass.** Legacy import, apply (palette `background #2b0a3d`, `selection #006666`) and remote preview all work as before. | `REGS legacy` |
| 11 | Coordination off (editor only) | **Pass.** With the checkbox cleared and Apply pressed, the stock `e4_dark` theme is restored, the plugin's workbench preferences are removed, and the editor colours of D are applied. | `s41d.log` |
| 12 | Restart / reload | **Pass on 4.40 and 4.41.** After restart: same `local:` ids and the `.ectheme` locators are intact; nothing was converted to `.xml`. Metadata comes back from the package. The authored palette is still active at boot. Details still show the provenance. Reapply works. | `s41b.log`, `s40b.log` |
| 13 | Update `.ectheme` → `.ectheme` | **Pass.** After confirming, "Update Successful". Same identity and same locator file; name and version change to v2 / 3.3. | `s41c.log` upd-same |
| 14 | Update with `.xml` replacement | **Pass.** Refused with *Replacement 'qa-legacy-replacement.xml' is not a .ectheme theme; an installed theme can only be updated with a file of the same format*. The stored file is unchanged and no file was copied. | `s41c.log` upd-cross |
| 15 | 256 KiB bound | **Pass on all three paths at runtime.** Import refused (row 3); update refused (*larger than 256 KiB*); reload: a file planted directly in `installed_themes` was skipped at every load with *Installed theme file is not valid: … larger than 256 KiB*, and the rest of the catalog loaded. All three go through `ThemePackageParser`. | `s41a/s41c.log`, `eclipse-s41.log` |
| 16 | Remove | **Pass.** After confirming, "Theme Removed"; the file and the catalog entry are gone. | `s41d.log` |
| 17 | 4.40 core positive | **Pass.** UI import of B, details, apply with roles verbatim, restart, reapply. | `s40a.log`, `s40b.log` |

The full negative, update and readability suite ran on 4.41 only. The 4.40 run covers the core positive path; the parser and repository paths are platform-independent and covered by unit tests.

## Log classification

All plugin entries in both workspace logs:

| Message | Count (4.41) | Class |
|---|---|---|
| `Failed to parse imported theme: …` (E1, E2, E4) | 3 | EXPECTED DIAGNOSTIC: the negative imports |
| `Failed to parse replacement theme: … larger than 256 KiB` | 1 | EXPECTED DIAGNOSTIC |
| `Update refused: format of … differs from …` | 1 | EXPECTED DIAGNOSTIC |
| `Installed theme file is not valid: planted-oversize.ectheme …` | 7 | EXPECTED DIAGNOSTIC. Logged once per catalog load while the planted file stays. |
| `Workbench role 'accent' is not readable …` | 2 | EXPECTED DIAGNOSTIC: package C admission warning |
| `Attribute 'source' scheme 'file' is not allowed …` | 11 | EXPECTED DIAGNOSTIC: package C, logged per load |
| `Unknown theme element '<warning>'; it was ignored` | 28 (4.40: 10) | EXPECTED DIAGNOSTIC, pre-existing: two bundled legacy themes contain `<warning>` (see `evidence.md` §3) |
| logback "Activated before the state location was initialized" / config file | 4 | PLATFORM/HARNESS (m2e logback, unrelated) |

No OUR DEFECT entries, no exceptions and no stack traces.

## Observations (not defects of this change)

- **Transient preview on import.** On the first import into an empty selection, `filterThemes()` briefly selects the first list row (the bundled *Atom One Dark*) before the imported theme is selected. That loads the bundled theme's remote preview once. This behaviour predates v1, and only a bundled legacy id is sent; no package data is.
- **Wording in the update confirmation.** The update confirmation says "Replace the installed XML content of '…'?" for packages too. This is cosmetic and was left unchanged, since a production change was not justified for runtime closure; it is a candidate for a later wording pass.
- **Harness limitation.** Programmatic switching of the file-type combo did not refresh the shell view; synthetic CBN_SELCHANGE and keyboard messages were both tried. The two narrower filters were therefore enumerated but their listings were not observed. Their patterns are fixed in `EclipseThemesPreferencePage.chooseThemeFile`.

## Not covered

- Linux and macOS were not exercised, consistent with the verified matrix.
- High-contrast mode was not exercised at runtime. It is covered by the existing R8 evidence and is unchanged by v1.
