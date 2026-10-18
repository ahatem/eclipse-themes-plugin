# Source Evidence

Baseline: branch `feat/strict-theme-validation-diagnostics`, forked from `develop` at
`c7c8404cbb17425c5bb600546e247788df043f04` ("feat: make theme import deterministic (#26)", squash-merged,
GitHub Actions "Build and Deploy" run #35, `push` to `develop`, conclusion `success`). All findings below were
re-read against that tree. **No production or test file is modified by this planning change.** Where a claim is
from corpus inspection it names the corpus; where it is from an external format it is labelled historical.

Discovery scripts were temporary and read-only, kept outside the repository under the session temp directory.
They are not committed because they are one-off inspection tools, not regression fixtures.

---

## 1. Re-verification of the mapped audit findings

Roadmap mapping for this change: Q10, Q15, U5, U8. Original audit text:
`docs/audits/2026-10-complete-project-audit.md` lines 246, 251, 454, 457.

| Finding | Classification | Evidence on the current tree |
| --- | --- | --- |
| Q10 — "No root/schema/identity/name validation" | **still reproducible** | `XmlThemeParser.parse` never inspects `document.getDocumentElement().getTagName()`; there is no root check anywhere in the bundle. |
| Q10 — "Invalid colors are skipped" | **still reproducible** | `XmlThemeParser.java` lines 107–113: `catch (IllegalArgumentException e)` → `EclipseThemes.instance().getLogger().warn(...)` → `continue`. Only a log line. |
| Q10 — "malformed booleans silently become false" | **still reproducible** | `XmlThemeParser.java` lines 115–129 use `Boolean.parseBoolean` for `bold`/`italic`/`underline`/`strikethrough`. `"maybe"` becomes `false` with no diagnostic. |
| Q10 — "unknown/missing type becomes light" | **still reproducible** | `ThemeType.from` (line 13): `type.toLowerCase(Locale.ROOT).equals("dark") ? DARK : LIGHT`. Absent `type` yields `""` → `LIGHT`. |
| Q10 — "Even light themes receive dark root fallbacks" | **still reproducible** | `XmlThemeParser.java` lines 80–81 unconditionally pre-populate `BACKGROUND` = `rgb(30,30,30)` and `FOREGROUND` = `rgb(212,212,212)` for every document, regardless of `type`. |
| Q10 — "identity validation" | **already fixed (R2.1/R2.2)** | `Theme.logicalId` + `ThemeIdentityBindings` give plugin-owned identity independent of `ect-id`, name and basename. See `openspec/specs/theme-definition/spec.md` ("Established logical identity is independent of presentation and content"). |
| Q10 — XML security subset | **already fixed (R1)** | `SecureXmlBuilder` rejects DOCTYPE and disables external resolution; `openspec/specs/theme-parsing/spec.md` owns that contract. This change does not re-decide it. |
| Q15 — "Case-sensitive `.xml` lookup contradicts repository filtering" | **already fixed (R2.2)** | `ParserFactory.getParserFor` line 23 uses `toLowerCase(Locale.ROOT).endsWith(".xml")`; `ThemeRepository.managedTarget` line 241 and `loadInstalledThemes` line 376 do the same; `ThemeRepositoryImportTest.uppercaseSourceExtensionIsImportedAsAManagedXmlLocator` covers `.XML`. |
| Q15 — "`ThemeType` locale-sensitive" | **already fixed (R2.2)** | `ThemeType.from` uses `Locale.ROOT`. |
| Q15 — "Locale-sensitive case conversions affect searching/classification" | **residual, presentation-only — out of scope** | `EclipseThemesPreferencePage.matchesSearchCriteria` lines 365–367 still call `String.toLowerCase()` with the default locale. That code is catalog filtering/presentation; it does not participate in validation or admission. It was already deferred by `atomic-deterministic-theme-import` ("Locale-sensitive catalog search and `nameCapitalized` presentation: UI localization follow-up"). |
| Q15 — "Constant-holder interfaces and mixed formatting" | **obsolete / cosmetic** | `Constants` is a constant-holder interface and formatting is mixed. No behavior depends on it; not converted into work. |
| U5 — "Import errors are generic" | **still reproducible** | `EclipseThemesPreferencePage.importTheme` lines 575–577 show the fixed string "The theme could not be validated or stored. The existing catalog was not changed."; `updateSelectedTheme` lines 617–618 show "The theme file was not replaced. The installed theme is unchanged." No validation reason reaches the user. |
| U5 — "warnings are hidden" | **still reproducible** | The only non-fatal diagnostics are `Consumer<String> mappingWarning` messages. `ThemeRepository.supportedParser` passes `log::warn` (line 286) and never collects them, so no caller can observe them. |
| U5 — "success can be reported for partially accepted XML" | **still reproducible** | A document whose tokens are all unknown/missing-colour/invalid-colour still returns a `Theme` (`XmlThemeParser` line 151) and the page shows "Import Successful" (lines 589–590). |
| U5 — "Existing filters can hide the newly imported item" | **partially fixed (R2.2)** | `importTheme` now reloads and reselects by logical identity (lines 580–587), but `filterThemes()` still applies the active search/type filter to `themeViewer` immediately afterwards, so a theme that does not match the active filter is still not visible. Confirmation/reselection policy is R4 `theme-import-conflict-and-diagnostic-ui`. |
| U8 — "inserted into link markup" | **still reproducible** | `EclipseThemesPreferencePage.updateThemeDetails` line 484: `websiteLink.setText("<a>" + website + "</a>")` with no escaping. |
| U8 — "launched without scheme validation" | **still reproducible** | `EclipseThemesPreferencePage` lines 412–419: the selection listener calls `Program.launch(selectedTheme.getWebsite())` whenever the value is non-null. No scheme, absoluteness or whitespace check. |
| U8 — "launch failure is ignored" | **still reproducible** | `Program.launch` returns `boolean`; the return value is discarded (line 416). The `downloadMoreLink` listener (line 408) does the same. |
| U8 — escape display text / allow intended web schemes / validate URIs | **not started** | No URI handling exists anywhere in the bundle. |

---

## 2. Current parse-to-admission pipeline (exact)

All four loading routes use the same parser. There is one validation language by construction; only the
*admission consequence* differs.

```text
XML bytes
  -> ParserFactory.getParserFor(name, sink)          [Locale.ROOT ".xml" test; returns a NEW XmlThemeParser(sink)]
  -> XmlThemeParser.parse(stream, file)
       -> SecureXmlBuilder.create(...)               [R1 contract: DOCTYPE/external resolution refused]
       -> DocumentBuilder.parse(stream)               [SAXParseException -> ThemeParseException]
       -> document.getDocumentElement()               [ROOT NAME NEVER CHECKED]
       -> root.getAttribute(ect-id|name|author|website|description|type)
       -> ThemeType.from(type)                        [anything != "dark" -> LIGHT]
       -> new Theme(...)                             [id = raw ect-id; logicalId = null]
       -> unconditional parser defaults for BACKGROUND/FOREGROUND
       -> iterate DIRECT child elements only:
            element/comment/PIText nodes are skipped (node.getNodeType() != ELEMENT_NODE)
            LegacyEclipsePropertyMapper.mapProperty(tag)
            TokenKey.byId(mappedTag)
              null            -> activator log .info, continue
            color attribute
              absent/blank    -> continue (no diagnostic)
              IllegalArgument -> activator log .warn, continue
            bold/italic/underline/strikethrough -> Boolean.parseBoolean, no diagnostic
            -> candidate list per canonical key
       -> winner selection: canonical spelling wins; else last candidate wins
       -> mappingWarning.accept(...) for differing candidates only
       -> return Theme                                [logicalId still null: a "candidate"]
  -> repository route-specific admission
```

Route table:

| Route | Parser obtained via | Failure handling | Post-parse admission gate |
| --- | --- | --- | --- |
| Bundled load (`loadPackedThemes`) | `ParserFactory.getParserFor(path, log::warn)`, `parse(in)` with no source file | `catch (Exception) -> log.error`, continue | blank/`<null>` name → skip + `warn`; blank `<null>` `ect-id` → skip + `warn`; `logicalId = "bundled:" + ect-id`; duplicate `bundled:` id → `putIfAbsent` + `warn` |
| Installed reload (`loadInstalledThemes`) | `ParserFactory.getParserFor(name, log::warn)`, `parse(themeFile)` | `ThemeParseException` → `log.error`, continue; `IOException` → `log.error`, continue | **no name or identity gate**; `logicalId = identities.identityFor(fileName)` |
| Import (`importTheme`) | `ParserFactory.getParserFor(source.getName(), log::warn)`, `parse(staged, locator)` | `ThemeParseException` → `log.error`; returns `Optional.empty()` | `withLogicalId(local:…)`; `themes.put` |
| Update (`updateTheme`) | same as import, `parse(staged, target)` | `ThemeParseException` → `log.error`; returns `Optional.empty()` | `withLogicalId(selected.getLogicalId())`; `themes.put` |

Demonstrated inconsistency this creates: a bundled file and an installed file with **identical** blank-name
content are treated differently — bundled is skipped, installed is admitted. This is a real defect for §14, not a
hypothetical.

### Where malformed input currently ends up

| Condition | Current behavior |
| --- | --- |
| Malformed XML | **Rejected** — `ThemeParseException`; no theme; source untouched |
| DOCTYPE / external entity | **Rejected** — `ThemeParseException` via `SecureXmlBuilder` (R1 contract) |
| Wrong root element | **Allowed into a `Theme`** — never inspected |
| Nested/other root children | **Silently ignored** — only direct element children are visited |
| Comments, text, whitespace | **Silently ignored** — non-element nodes skipped |
| Unknown element name | **Logged only** at `.info` through the activator, then ignored |
| Known token with missing/blank `color` | **Silently ignored** — `continue` with no diagnostic |
| Known token with invalid `color` | **Logged only** at `.warn` through the activator, then skipped |
| Invalid `bold`/`italic`/`underline`/`strikethrough` value | **Silently normalized** to `false` |
| Missing `name` | **Allowed into a `Theme`** with `name == ""` (installed route admits it) |
| Missing/blank `type` | **Silently converted** to `LIGHT` |
| Unknown `type` value | **Silently converted** to `LIGHT` |
| Missing/blank `ect-id` | **Allowed**; `Theme.getId()` is `""`. Not used for installed identity |
| Legacy `id` attribute | **Ignored** — never read by the parser |
| `modified`, `description` | Read into the model / ignored; never displayed by the page |
| `website` | Stored verbatim, inserted into link markup, launched unvalidated |
| Duplicate canonical element | **Silently resolved** (last wins) unless a canonical-vs-alias conflict exists, then `mappingWarning` |
| Canonical + mapped legacy alias for one role | **Deterministic** winner + `mappingWarning` (R1 approved contract, unchanged) |
| Unsupported token attribute | **Silently ignored** |

---

## 3. Bundled corpus inventory (repository evidence)

Source: `bundles/com.github.eclipsethemes/themes/*.xml`. **33 files.** No DOCTYPE in any file.

Root element: `colorTheme` in **33/33**. No other root name anywhere.

Root attributes:

| Attribute | Present | Notes |
| --- | --- | --- |
| `ect-id` | 33/33 | curated UUID; never equal to `id` |
| `id` | 33/33 | legacy numeric source id (`3001`…`9011`); **never read by the parser** |
| `name` | 33/33 | never blank; one value contains a non-ASCII character (`Catppuccin Frappé`) |
| `type` | 33/33 | only `dark` (22) and `light` (11); no other value, no mixed case, no absence |
| `modified` | 33/33 | `2024-09-21` / `2025-09-22`; **never read by the parser** |
| `author` | 33/33 | `Eclipse Themes` or `Ahmed Hatem` |
| `website` | **4/33** | all four are `https://github.com/ahatem` |
| `description` | **0/33** | parsed into the model, never displayed |

Child token elements: 43–50 per file; **0** nested token elements; **0** tokens without a `color` attribute;
**0** documents relying on parser defaults for `background`/`foreground` (all 33 define both); **0** documents
with a duplicate semantic role.

Attributes used on token elements: `color` (1561), `italic` (177), `bold` (86), `strikethrough` (31),
`underline` (1). No other token attribute is used anywhere.

Color forms: **1561/1561 are `#RRGGBB`.** No `#RGB` shorthand and no other form in the bundled corpus.

Style attribute values: **only the literal `"true"`** (bold 86, italic 177, strikethrough 31, underline 1). No
`"false"` and no other value appears in the bundled corpus.

Tokens reachable only as canonical `TokenKey` names (not through the legacy mapper): `xmlDirective` (2 files),
`error` (2 files). Genuinely **unknown** elements: `<warning color="#d4976c"/>` in `vitesse_dark.xml` and
`vitesse_dark_soft.xml` — **2 occurrences in 2 files**. These are today `log.info`-ed and dropped.

### Bundled-corpus compatibility gate for the proposed rules

The recommended rule set in [proposal](proposal.md#decision-gate) was simulated against all 33 files:

```text
bundled files                    : 33
zero diagnostics (clean)         : 31
warnings only (still admitted)   : 2   (vitesse_dark.xml, vitesse_dark_soft.xml)
FAIL (errors, not admitted)      : 0
diagnostics: unknown token <warning> ignored  (1 each in the two files above)
```

Result: **no bundled theme asset needs to be edited** under the recommended severity choices. If the maintainer
instead chooses "unknown token = ERROR", the gate result changes to 2 failing bundled files, which is a Decision
Gate stop (see PM-V2/PM-V6).

---

## 4. Historical / upstream format evidence

External to this repository. Source: `eclipse-color-theme/eclipse-color-theme`,
`com.github.eclipsecolortheme/themes/*.xml` (26 files, the themes shipped by the original Eclipse Color Theme
plugin that this product's format derives from). This is evidence about *historically importable documents*, not
about this repository's behavior.

```text
root element `colorTheme`                       : 26/26
root attribute `id`                             : 26/26   (the historical source id)
root attribute `name`                           : 26/26
root attribute `modified`                       : 26/26
root attribute `author`                         : 26/26
root attribute `website`                        : 15/26   — 14 use http://, 1 uses https://
root attribute `type`                           :  0/26   — the historical format HAS NO type attribute
root attribute `description`                    :  0/26
root attribute `ect-id`                         :  0/26
DOCTYPE present                                 :  0/26
token elements without `color`                  :  1      (<mappingOverrides>, see below)
nested children under a token element           :  1      (same element)
unknown token elements                          : 14     (all in one file, see below)
color forms                                     : 1085/1085 `#RRGGBB` (mixed upper/lower case hex)
style attribute values                          : only "true" and "false" (no 1/0/yes/on)
missing background or foreground                :  0/26
```

Two decisive documents:

- `sublime-text-monokai-extended.xml` (importable, historically shipped) contains **13 custom palette elements**
  `sl_magenta`, `sl_green`, `sl_yellow`, `sl_purple`, `sl_blue`, `sl_orange`, `sl_grey`, `sl_comment`,
  `sl_lineNumber`, `sl_background`, `sl_white`, `sl_currentLineBackground`,
  `sl_currentSelectionBackground` — none of which is a known token or a mapped alias — plus a
  **`<mappingOverrides>` container with no `color` attribute and a three-level nested subtree**
  (`eclipseColorThemeMapping` → `mappings` → `mapping`).
- Every historical file omits `type`, so `ThemeType.from("")` classifies dark themes such as Monokai and
  Solarized Dark as `LIGHT`.

### Consequence for proposed rules (exact counts, historical corpus)

| Proposed rule | Bundled corpus impact | Historical corpus impact |
| --- | --- | --- |
| root must be `colorTheme` (ERROR) | 0 files | 0 files |
| nonblank `name` required (ERROR) | 0 files | 0 files |
| `type` required (ERROR when absent) | 0 files | **26/26 files** |
| unknown `type` value (ERROR) | 0 files | 0 files |
| unknown token element (ERROR) | **2 files** (`vitesse_dark`, `vitesse_dark_soft`) | **1 file** (`sublime-text-monokai-extended`) |
| known token without `color` (ERROR) | 0 files | 0 files *(the only colour-less element is itself unknown, so it never reaches the colour check)* |
| invalid `color` (ERROR) | 0 files | 0 files |
| invalid boolean style (ERROR) | 0 files | 0 files |
| website not `https` (ERROR or WARNING) | 0 files | **14/26 files** |

---

## 5. Current colour grammar (`Color.ofHex`)

`bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/models/Color.java` lines 21–46. This is the
authoritative grammar today; this change documents it and does **not** widen it.

1. `hex` must be non-null; the value is `trim()`-ed, so leading/trailing whitespace is accepted.
2. If the trimmed value has length 4 and starts with `#`, each remaining character is doubled (`#RGB` → `#RRGGBB`).
3. The value must start with `#` and have length exactly 7 after expansion.
4. Substrings 1–3, 3–5, 5–7 are parsed with `Integer.parseInt(.., 16)`. `Integer.parseInt` accepts a leading
   `+`/`-` sign, so `#-12345` and `#+12345` reach it; the resulting value is range-checked by the `Color`
   canonical constructor, which throws for out-of-range components.
5. Anything else throws `IllegalArgumentException("Invalid hex color format: ...")`.

Consequences: hex is case-insensitive; `#RGB` is supported; an `IllegalArgumentException` is the single signal
the parser uses to skip a token. `#RGB` shorthand is not used by any bundled or historical corpus file but **is
used by an existing repository test** (`XmlThemeParserCompatibilityTest.metadataStylesAliasesAndReferences` uses
`<background color='#123'/>`), so removing it is a compatibility change.

---

## 6. Website metadata path (U8)

```text
root.getAttribute("website")            XmlThemeParser line 72
  -> new Theme(..., website, ...)       Theme field, no validation, stored verbatim
  -> Theme.getWebsite()
  -> EclipseThemesPreferencePage.updateThemeDetails line 482-487
       websiteLink.setText("<a>" + website + "</a>")      // unescaped, SWT Link markup
  -> EclipseThemesPreferencePage.addListeners line 412-419
       Program.launch(selectedTheme.getWebsite())          // no scheme/absolute/whitespace check,
                                                          // boolean result discarded
```

The widget is a `org.eclipse.swt.widgets.Link` created with `SWT.WRAP`. It parses a limited HTML-like subset,
so an attacker-controlled value is inserted into that markup. `Link` is clickable regardless of the text set,
and the listener only checks `website != null`, so the `"N/A"` placeholder is also launchable (it launches the
literal string `"N/A"`).

`Program.launch(String)` delegates to the platform (Windows `ShellExecute`), so `file:`, `javascript:`,
custom-scheme and local-path values are handed to the operating system without any plugin-side decision.

---

## 7. Diagnostics seam as it exists today

- `XmlThemeParser` has one non-default constructor taking `Consumer<String> mappingWarning`, documented as
  "Internal loading-boundary sink; no per-parse state is retained on the parser."
- `ParserFactory.getParserFor(String, Consumer<String>)` returns `Optional<ThemeParser>` and constructs a **new**
  `XmlThemeParser` wrapping the caller's sink; the static no-arg `XML_PARSER` is only used by the single-argument
  overload.
- `ThemeRepository.supportedParser` (line 285-291) passes `log::warn`. Nothing accumulates.
- `ThemeRepositoryImportTest`, `ThemeRepositoryUpdateTest`, `ThemeRepositoryRemovalTest`,
  `RepositoryMappingWarningTest`, `XmlThemeMappingTest` assert the mapping-warning behaviour; those assertions are
  the approved `theme-parsing` mapping-diagnostic contract and must keep their observable content.

There is therefore **no** existing path by which a non-fatal validation finding can reach a caller as data.

---

## 8. Source-specific behaviour that must not diverge further

- `ThemeRepository.loadPackedThemes` skips a bundled theme whose parsed `name` is null/blank, and separately
  skips one whose `ect-id` is null/blank (both with `log.warn`). The curated-identity gate is a *bundling*
  concern (`bundled:<ect-id>` is the bundled logical id) and is **out of scope** for this change; `ect-id` must
  not become required for imported themes.
- `ThemeRepository.loadInstalledThemes` applies no such gate, so a blank-name installed file is admitted today.
- All four routes call the same `XmlThemeParser` through `ParserFactory`, so one parser change reaches all of
  them. Only the consequences differ, and those differences must be made consistent rather than multiplied.

## 9. Prior approved decisions this change must not reopen

- XML DOCTYPE/external-resolution policy — `secure-xml-theme-parsing`, accepted in `theme-parsing`.
- Canonical-over-alias precedence, last-valid-wins at equal rank, and the bounded mapping-conflict warning —
  `correct-editor-preference-mappings`, accepted in `theme-parsing`.
- Plugin-owned logical identity independent of `ect-id`/name/basename, and "source IDs do not establish
  equivalence" — `stable-theme-identity-and-revisions`, accepted in `theme-definition`/`theme-catalog`.
- Staged-byte validation before commit, import-always-new-record, explicit identity-preserving update, atomic
  replacement without fallback, truthful removal — `atomic-deterministic-theme-import`, accepted in
  `theme-import`/`theme-catalog`/`preference-ui`.
- Sparse themes and deterministic parser defaults — accepted `theme-definition` requirement "Known defaults may
  complete sparse themes", locked by `XmlThemeParserCompatibilityTest.sparseDefaultsAndSourceEntryPoints`.
- `SimpleDateFormat`-style locale presentation, preview fidelity, apply/OK/cancel semantics, diagnostics UX
  panels, problems view, and application-result truth belong to R3/R4/R6.