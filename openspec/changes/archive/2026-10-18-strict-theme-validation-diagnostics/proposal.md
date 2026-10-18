# Proposal

## Why

`develop` at `c7c8404` has a safe import lifecycle but still accepts almost any XML document as a theme. Every
structural, metadata, token, colour and style problem is either logged through the plugin activator and dropped,
silently normalised to a fallback, or admitted with a value the author never wrote. The user sees the generic
string "The theme could not be validated or stored" when an import fails, and "Import Successful" when a document
was accepted with almost none of its tokens actually applied. Imported `website` metadata is pasted into SWT `Link`
markup unescaped and handed to `Program.launch` with no scheme check.

Re-verified against the merged tree (full detail in [evidence](evidence.md)):

- **Q10 (Medium) — still reproducible.** `XmlThemeParser` never checks the root element name; there is no
  metadata validation beyond reading attributes; invalid colours are `.warn`-logged and skipped; malformed
  booleans become `false` via `Boolean.parseBoolean`; absent or unknown `type` becomes `LIGHT`; and every
  document — including light themes — receives the hard-coded dark parser defaults `rgb(30,30,30)` /
  `rgb(212,212,212)` for background/foreground.
- **Q15 (Low) — fully satisfied inside this change's boundary.** Extension matching and `ThemeType.from` are
  already locale-stable in the admission paths. The only residual is `EclipseThemesPreferencePage`
  `matchesSearchCriteria`, which is catalog filtering/presentation and does not participate in validation or
  admission; it stays deferred with the other R4 localisation work. The audit's remaining `Constants`/formatting
  observations are cosmetic and are not converted into work.
- **U5 (Medium) — still reproducible.** The only non-fatal diagnostics are `Consumer<String>` mapping warnings
  that `ThemeRepository` sends straight to `log::warn` and never collects. No validation reason can reach the
  user. A document whose tokens are entirely unknown or unparseable is still admitted and reported as a success.
- **U8 (Medium) — still reproducible.** `website` is stored verbatim, concatenated into `"<a>" + website + "</a>"`
  with no escaping, and passed to `Program.launch` with no absoluteness or scheme check; the launch result is
  discarded.

Q10's identity half and its XML-security half were already closed by `stable-theme-identity-and-revisions`,
`atomic-deterministic-theme-import` and `secure-xml-theme-parsing` and are not reopened here.

The bundled corpus is already well-formed: all 33 files use root `colorTheme`, a non-blank `name`, `type` in
`{dark, light}`, `#RRGGBB` colours only, and only `"true"` as a style value. Only one defect exists in 33 files:
an unknown `<warning>` element in `vitesse_dark.xml` and `vitesse_dark_soft.xml`. The historical upstream Eclipse
Color Theme corpus is materially looser — **26/26 of its files omit `type` entirely**, 14/26 use `http://` for
`website`, and one file carries 13 custom palette elements plus a colour-less `<mappingOverrides>` container with
nested children. Any rule that ignores that corpus will reject themes users have been able to import for a
decade.

This change defines the smallest validation and diagnostics contract that makes the strictness intentional,
compatibility-aware and explainable. It deliberately stops at the validation boundary and prepares, without
performing, the immutable-model step that follows it.

## What Changes

- **One document-validity contract for every source.** Bundled load, installed reload, import and update all run
  the same validation. The rules are identical; only the *presentation* of the outcome differs.
- **Minimum valid document:** root element `colorTheme`, and a present non-blank display `name`.
- **Metadata policy is explicit per field:** `name` required; `type` optional-but-validated; `ect-id` optional and
  never identity; legacy `id` and `modified` recognised and ignored; `author`/`description` optional free text;
  `website` optional and launch-policy-checked.
- **Token policy is explicit per case:** known canonical and mapped-alias tokens keep working; unknown elements,
  colour-less tokens, invalid colours and invalid style booleans are *diagnosed*, never silently dropped; the
  already-approved canonical-over-alias precedence and mapping-conflict warning are untouched.
- **Sparse themes stay valid.** Deterministic parser defaults for missing tokens remain; nothing in this change
  requires a theme to define every token, or even `background`/`foreground`.
- **Diagnostics become data, not log strings.** Findings carry severity, a stable kind, a message, the source
  file, and the XML element/attribute involved. Errors abort admission; warnings do not.
- **Import and update refuse validation errors before any durable commit**, and the refusal carries the reason to
  the user instead of a generic sentence.
- **Website metadata is displayable but not blindly launchable.** The value is rendered as inert, escaped text;
  the link is activated only for an explicitly allowed absolute URI scheme.
- **Bundled/imported/reload consistency is fixed.** The blank-name admission gap between `loadPackedThemes` and
  `loadInstalledThemes` is closed by the shared validity rule.

Not in scope, explicitly: XSD/schema infrastructure, a generic validation framework, a visitor framework, a
diagnostics bus or OSGi service, a public API, a new bundle or dependency, revision/digest/history, an immutable
`Theme` refactor, package/archive formats, a problems view, background `Job`s, a preference-page redesign,
application-result or recovery behavior (R3), richer conflict/preview UX (R4), and the parser→activator layering
cleanup beyond the minimum this contract requires (R6).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `theme-parsing`: document-validity rules (root, metadata, tokens, colour, style), the diagnostics contract, and
  website launch eligibility. This is the primary delta.
- `theme-catalog`: reload admits only valid theme documents, using the same rule as import/update, and reports a
  diagnostic instead of admitting or dropping silently.
- `theme-import`: validation errors — not only XML parse failures — prevent admission before any durable commit,
  for both import and update; warnings alone do not.
- `preference-ui`: a failed import/update names the actionable validation reason; imported website metadata is
  rendered inert and is launchable only when the URI policy allows it.

`theme-definition` is deliberately **not** modified. The accepted sparse-theme and defaults contract stays exactly
as it is; nothing in this change makes a token mandatory in the definition model.

## Decision Gate

**Status: APPROVED for implementation.**

The maintainer resolved every product/compatibility question below. The resolutions are recorded in the table
first, because they are the contract; the option tables that follow are retained as decision context, not as an
implementation menu. Evidence-dependent questions were investigated before any option was offered (see
[evidence](evidence.md)).

| Gate | Resolution | Recorded refinement |
| --- | --- | --- |
| PM-V1 — minimum valid document | **Option A** | Root element `colorTheme`; `name` required and non-blank; no minimum token set. **`type` is optional and only validated when present:** an *absent* `type` SHALL NOT produce a diagnostic and keeps the documented `LIGHT` classification, while a *present* blank or unrecognised value produces a WARNING and keeps `LIGHT`. This refines the original sub-question ("absent or unrecognised → warning") because 26/26 historical files omit `type` entirely: warning on absence would emit a finding for the normal case and would turn the historical corpus's default state into noise. |
| PM-V2 — token invalidity policy | **Recommended severities as tabled** | Unknown element, known token with absent/blank `color`, known token with unaccepted `color`, non-boolean style value, and nested child elements are WARNING with the existing interpretable outcome unchanged. Unsupported token attributes produce no diagnostic. Canonical-over-alias precedence and mapping-conflict warnings are untouched. |
| PM-V3 — metadata and website policy | **Metadata policy as tabled; website Option B (`http` + `https`)** | Admission validity and launch eligibility remain independent. An unusable website is a WARNING and disables the link; it never rejects the theme. The allowed scheme set is exactly `http` and `https`, compared case-insensitively. |
| PM-V4 — warning commit semantics | **Option A** | Warnings do not block import/update. They are logged and carried on the operation outcome; the success dialog does not enumerate them in this change. |
| PM-V5 — diagnostic representation | **Option D (hybrid)** | Structured sink for all findings, error diagnostics on `ThemeParseException`, and a small outcome record at the import/update boundary shaped like `PreferenceReset.Result`. |
| PM-V6 — existing corpus compatibility | **Gate accepted** | With the resolutions above the bundled gate is 31/33 clean, 2/33 warnings only, 0/33 errors; the two warnings are the `<warning>` elements in `vitesse_dark.xml` and `vitesse_dark_soft.xml`. Bundled assets are not edited. |
| PM-V7 — dark parser defaults for light themes | **Deferred; known limitation** | The hard-coded dark root defaults are left exactly as they are. Fixing them is a visible behavior change that belongs to R2.4 `immutable-validated-theme-model` or a dedicated follow-up, not to a validation change. It is recorded in `docs/CURRENT_ARCHITECTURE.md` as accepted for now. |

No `BREAKING` compatibility behavior is approved. Errors are not a decision: any validation error prevents
admission on every source, and no approved rule promotes a historically admitted record to an error.

### PM-V1 — Minimum valid document (compatibility-affecting)

| Option | Rule set | Consequences |
| --- | --- | --- |
| **A — recommended** | Root element must be `colorTheme`; `name` must be present and non-blank. `type` optional; when present, a value other than `dark`/`light` (case-insensitive) is warned about rather than rejected, and an absent `type` produces no diagnostic. No minimum token set. | Bundled corpus: 0 failures. Historical corpus: 0 failures. Rejects arbitrary XML documents and nameless records, which is the concrete R1 target-spec scenario "Arbitrary XML root". Does not reject any historically importable theme. |
| **B — A, plus `type` required** | As A, and an absent `type` is an error. | Bundled corpus: 0 failures. **Historical corpus: 26/26 files rejected**, including Monokai and Solarized Dark. `type` is not part of the historical format at all; requiring it would remove most of the importable ecosystem this product exists to serve. Also conflicts with the "sparse themes remain valid" direction. |
| **C — A, plus minimum required visual tokens** (`background` and `foreground` must be authored) | As A, and no parser defaults. | Bundled corpus: 0 failures (all 33 define both). Historical corpus: 0 failures. **But** it directly contradicts the accepted `theme-definition` requirement "Known defaults may complete sparse themes", breaks the existing `XmlThemeParserCompatibilityTest.sparseDefaultsAndSourceEntryPoints` regression, and requires a `theme-definition` delta plus a repository-digest/fingerprint update. It converts a deterministic-fallback contract into a rejection contract. |
| **D — root-only** | Only the root element is validated; `name` optional. | Smallest change. But blank-name installed records remain admitted, `themeTypeLabel` and the catalog label render empty, and `loadPackedThemes` keeps a bundled-only gate that no other route shares — the source inconsistency this change exists to remove survives. |

Recommendation: **A**. It removes arbitrary XML acceptance without touching any evidence-backed historical input
and without disturbing the sparse-theme contract.

Additional sub-questions inside PM-V1, all answered by A unless noted:

- **Exact root name.** `colorTheme`, case-sensitively. 33/33 bundled and 26/26 historical files use exactly that
  name; there is no evidence for any alternative, and there is no namespace in any observed document. Allowing a
  namespace prefix or case variants would need evidence that does not exist.
- **Blank vs missing `name`.** Both are the same error. `Element.getAttribute` returns `""` for an absent
  attribute, so a separate "missing" branch would be unreachable code.
- **`type` case policy.** Case-insensitive compare against `dark`/`light` only. The bundled corpus uses exactly
  lowercase; `Boolean`-style case-insensitivity costs nothing and removes a locale trap. An **absent** `type` is
  the format's normal default state and produces **no diagnostic** (resolution recorded in the Decision Gate
  table). A **present but unrecognised or blank** value keeps the current `LIGHT` fallback **plus a warning**
  rather than becoming an error, because erroring would reject documents for a value the format never
  constrained.

### PM-V2 — Token invalidity policy

| Condition | Recommended severity | Evidence | Alternatives and consequences |
| --- | --- | --- | --- |
| Unknown/future token element | **WARNING + ignore** | Bundled: 2 occurrences (`<warning>` ×2). Historical: 14 occurrences in one file (`sl_*` palette + `mappingOverrides`), authored deliberately to be referenced by `themeKey`. | **ERROR** rejects 2 bundled files and at least one historically shipped theme, and breaks forward compatibility with any theme carrying a custom palette. **INFO** is what happens today and is what the audit calls out. **WARNING** is the only option that is honest without rejecting evidence-backed input. |
| Known token with missing/blank `color` | **WARNING + ignore** | Bundled: 0. Historical: the only colour-less element (`<mappingOverrides>`) is itself unknown, so this branch is never reached by known tokens. | **ERROR** has no evidence for or against; it is a pure policy choice. Erroring is defensible ("a token with no colour is meaningless") but buys nothing the warning does not, and would reject the one historical document that uses a colour-less container — unless the unknown-element warning already covers it, which it does. |
| Known token with invalid `color` | **WARNING + skip token** (keeps today's admission behavior) | Bundled: 0. Historical: 0. | **ERROR** is the intuitive strict answer and is what the audit implies. The counter-argument is concrete: `loadInstalledThemes` re-parses already-installed files on every reload, so promoting invalid colour to an error would make a theme the user successfully imported in an earlier plugin version silently **disappear from the catalog** after upgrade, while its file and binding remain. That is a data-visibility regression hidden inside a "validation" change. See PM-V6 for the maintainer's call. |
| Invalid style boolean value | **WARNING + treat as `false`** (keeps today's value) | Bundled: 0 (only `"true"`). Historical: 0 (only `"true"`/`"false"`). | **ERROR** has zero evidence backing it and would be the strictest rule with the least justification. The valuable part is that the mistake becomes *visible*, which a warning achieves. |
| Duplicate canonical element (same spelling) | **unchanged** — last valid occurrence wins, warn only when the candidates differ | Already the accepted `theme-parsing` contract. | Changing it would reopen `correct-editor-preference-mappings`. Not offered as an option. |
| Canonical + mapped legacy alias for one role | **unchanged** — approved deterministic winner + existing mapping-conflict warning | Accepted `theme-parsing` contract. | The only addition is that the same diagnostic record carries the location, so the finding is structured rather than a bare string. The mapping algorithm is **not** redesigned. |
| Several aliases for one role | **unchanged** — one warning per differing candidate | Accepted contract. | Severity stays warning; it never blocks admission. |
| Nested elements under a token element | **WARNING + ignore subtree** | Historical: only under `<mappingOverrides>`, which is itself unknown and already warned about. Bundled: 0. | **ERROR** would reject a historically shipped theme. The plugin does not implement `mappingOverrides` and must not claim to; warning plus ignore is truthful. |
| Unsupported token attribute | **ignored, no diagnostic** | Bundled: 0 occurrences. Historical: 0 occurrences. | Adding a diagnostic here is speculative and would only add noise. **Deferred** (see Deferred section). |

Note on style values: the audit's Q10 phrase "malformed booleans silently become false" is about *silence*, not
about `false` itself. Bundled and historical corpora both use `true`/`false`, which `Boolean.parseBoolean`
already handles. The fix is to make a non-boolean value visible, not to change the accepted value set. An
explicitly empty style attribute (`bold=""`) currently produces a *present* all-false `TokenOptions` rather than
absent options; that is pre-existing behaviour with no corpus evidence and is deliberately not changed here.

### PM-V3 — Metadata and website policy

Metadata field classification (recommended; each row is a separate maintainer decision but they are presented
together because they share one rule shape):

| Field | Recommended | Evidence / consequence |
| --- | --- | --- |
| `name` | **required, non-blank** | Bundled 33/33, historical 26/26. Blank records render an empty catalog label and empty detail header. |
| `type` | **optional; validated when present**; absent → no diagnostic, present blank/unrecognised → warning, value stays `LIGHT` | Required would reject 26/26 historical files. See PM-V1 B. |
| `ect-id` | **optional; never identity; never required** | Absent in 26/26 historical files and in imported records generally. R2.1 established plugin-owned identity independently. Making it required would directly contradict the archived identity contract. A blank/missing `ect-id` is not a defect and must not be diagnosed on the installed route. The curated `bundled:<ect-id>` gate for bundled resources is a *bundling* concern and is explicitly **not** changed. |
| legacy `id` | **recognised and ignored, no diagnostic** | Present in 33/33 bundled and 26/26 historical files. Warning on it would emit a diagnostic for every real theme. |
| `modified` | **recognised and ignored, no diagnostic** | Present in 33/33 bundled and 26/26 historical files. Same reason. |
| `author` | **optional free text** | Present 33/33 bundled; free text in both corpora. No structural constraint. |
| `description` | **optional free text** | Absent from 33/33 bundled and 26/26 historical files; parsed into the model and never displayed by the page. Still optional. |
| `website` | **optional; validated only for launch eligibility** | Absent 29/33 bundled. Absent or present in historical files without a single structural failure. |
| any other root attribute | **ignored, no diagnostic** | The recognised-and-ignored set is `ect-id`, `id`, `name`, `type`, `author`, `website`, `description`, `modified`. Anything else is ignored silently: this is a legacy format with unknown producers, and there is no evidence that any extra attribute is an error. |

Website launch eligibility — options:

| Option | Rule | Consequences |
| --- | --- | --- |
| **B — recommended: `https` + `http`** | Link is active only when the value is a syntactically absolute URI whose scheme is `https` or `http` (case-insensitive), after trimming surrounding whitespace and rejecting embedded control characters. Otherwise the value is rendered as escaped, non-clickable text. | Bundled corpus: 0 diagnostics (all four websites are `https`). Historical corpus: 14/26 files keep a working link, 0 are rejected for having a website. Matches what the format's producers actually wrote. `http` is not a security downgrade here because the plugin does not fetch the URI — it only asks the OS to open it, and OS/browser TLS policy applies. |
| **A — `https` only** | As above with `https`. | Bundled corpus: 0 diagnostics. **Historical corpus: 14/26 files lose their link**, including Monokai, Solarized Dark and Solarized Light. Import still succeeds; only the link is disabled. Safest, and the cheapest of the four to justify. |
| **C — `https` + `http` + explicitly enumerated extras** | Add `mailto`/`ftp`/… | No evidence in either corpus requires any additional scheme. Not recommended: every added scheme is new surface with no demonstrated need. |
| **D — display only, never launch** | Website is inert text in all cases | Fully removes the `Program.launch` boundary but removes a feature that currently works for the bundled corpus. |

Separation the maintainer must confirm: **theme admission validity vs launch eligibility are independent.** A
theme with no website is valid. A theme with an unusable website value is still **admitted** — the value produces
a warning diagnostic and the link is disabled. Under no option does a bad website reject the theme. If the
maintainer prefers rejection instead, that is PM-V3-A-ALT and it must be stated explicitly, because it converts a
display concern into an admission concern.

### PM-V4 — Warning commit semantics

| Option | Behavior | Consequences |
| --- | --- | --- |
| **A — recommended** | Warnings do not block import/update. They are logged and, for import/update, retained on the operation outcome. The success dialog does **not** enumerate them in R2.3. | Matches "warnings are advisory" and keeps R2.3 bounded. The user learns about warnings through the log; R4 adds acknowledgement UX. |
| **B — warn-and-continue with a confirm dialog** | Commit after an explicit user acknowledgement listing warnings | Better U5 closure, but it is a new modal interaction and squarely R4 `theme-import-conflict-and-diagnostic-ui` territory. |
| **C — warnings block commit** | Any warning refuses the operation | Would reject 2/33 bundled themes and every historical theme with a custom palette or `http` website. Rejected on evidence. |
| **D — warnings block *update* only** | Import commits, update requires acknowledgement | No principled basis; asymmetric handling of the same validator. Not recommended. |

Errors are not a decision: any validation **error** prevents import and update from committing, in every source.

### PM-V5 — Diagnostic representation (engineering, not product)

This is an engineering choice **provided the behaviour in PM-V1…PM-V4 is approved**. It is recorded here for
explicit acknowledgement rather than presented as a product decision.

Options evaluated (details and rationale in [design](design.md#diagnostic-representation-options)):

- **A — enriched `ThemeParseException` only.** Rejected: errors work, but there is nowhere for warnings to go on
  a successful parse, which is the case this change most needs to fix.
- **B — `ThemeParseResult { theme, diagnostics }` replacing `Theme parse(...)`.** Rejected as larger than needed:
  it adds a nullable-theme "empty" state, changes the `ThemeParser` interface, and forces every call site and
  test to handle a wrapper that most callers immediately unwrap.
- **C — caller-provided structured sink** (generalise the existing `Consumer<String> mappingWarning`). Viable
  but awkward: the repository would have to hand-collect into a local list it then has to attach somewhere else
  anyway.
- **D — recommended, evidence-backed hybrid:** one small `ThemeDiagnostic` record; the *existing* consumer seam
  changes its element type from `String` to `ThemeDiagnostic` and carries **all** findings (existing mapping
  conflicts plus new validation findings); errors additionally travel on `ThemeParseException` as a diagnostics
  list; and repository/manager import and update return a small outcome record shaped like the existing
  `PreferenceReset.Result`, carrying either the committed theme or the actionable reason.

`D` reuses two patterns the repository already has — the parser's injected sink and `PreferenceReset.Result` — so
it adds two small internal types and one additive exception field, and no framework. It also removes the two
activator `log` calls currently inside `XmlThemeParser`, which is a strict subset of the R6 parser-logging work and
is the minimum needed to make the contract expressible.

### PM-V6 — Existing corpus compatibility

Corpus gate result for the recommended severities: **31/33 bundled files clean, 2/33 warnings only, 0/33
failures. No bundled asset needs editing.** If the maintainer selects a stricter option, the affected assets are:

| Option | Files that would fail | Choices |
| --- | --- | --- |
| Unknown token = ERROR (PM-V2) | `bundles/com.github.eclipsethemes/themes/vitesse_dark.xml`, `.../vitesse_dark_soft.xml` (each has `<warning color="#d4976c"/>`) | (a) relax the rule to a warning — recommended; (b) edit the bundled assets; (c) grandfather bundled/imported legacy input differently from each other. |
| Invalid colour = ERROR (PM-V2) | none bundled; **already-installed user files** that were admitted under the permissive rule | (a) warning (recommended); (b) error and accept that those themes vanish from the catalog after upgrade; (c) a documented legacy-compatibility mode that relaxes colour validation for previously admitted records. |
| `type` required (PM-V1 B) | none bundled; 26/26 historical themes | (a) keep `type` optional (recommended); (b) require it. |
| Website `https`-only as error (PM-V3) | none bundled; 14/26 historical themes | (a) warning + disabled link (recommended); (b) reject the theme. |

Editing bundled theme assets is not authorized by this change and was not performed. Any asset edit that could
change a visible colour, style or metadata value requires its own justification and would need a separate
decision.

### PM-V7 — Dark parser defaults applied to light themes *(new decision surfaced by this discovery)*

The audit's Q10 ends with "Even light themes receive dark root fallbacks." This is confirmed: `XmlThemeParser`
lines 80–81 hard-code `rgb(30,30,30)`/`rgb(212,212,212)` for every document. A sparse *light* theme therefore
renders with a near-black background.

This was **not** in the original R2.3 scope, and fixing it is a visible behavior change, not a validation rule:

- it contradicts the accepted `theme-definition` requirement "Known defaults may complete sparse themes" and would
  require a `theme-definition` delta;
- it changes resolved values for any theme relying on the defaults, which is pinned by the
  `XmlThemeParserCompatibilityTest.bundledCorpusMatchesApprovedMappingInterpretation` corpus fingerprint;
- choosing new light-theme defaults is a product/visual decision, not an engineering one.

Resolution: **deferred to R2.4 `immutable-validated-theme-model` or a dedicated follow-up, and recorded as an
accepted-for-now known limitation.** The parser defaults are not changed by this change, so no
`theme-definition` delta and no corpus-digest update are required. If the maintainer later wants it here, it
becomes a BREAKING behavior change requiring explicit approval plus a corpus-digest update and a
`theme-definition` delta.

### Engineering (decided for the proposal)

- Validation is direct, format-specific code inside the existing parser. No XSD, no generic validator, no visitor.
- Diagnostics are internal to the runtime bundle. No exported package, no extension point, no new OSGi service.
- `ThemeParseException` keeps its checked-exception shape; only an additional diagnostics accessor is added.
- The `ThemeParser` interface signature is unchanged; only the sink element type and the additional validation
  behaviour change, so the single non-default constructor and `ParserFactory` overload absorb it.
- Existing mapping-conflict **message text** is preserved byte-for-byte so the approved `theme-parsing` mapping
  diagnostics contract and its tests stay meaningful; only the carrier type changes.
- Repository/manager import and update outcomes mirror the existing `PreferenceReset.Result` shape
  (`successful()` / `failures()` / `describeFailures()`) rather than inventing a new idiom.
- Tests extend the existing bundle test tree (`XmlThemeParser*`, `RepositoryMappingWarningTest`,
  `ThemeRepository*Test`). No new harness, no new module, no UI widget automation for message strings.
- Verification order: focused parser/repository tests, then `openspec validate --all --strict`, then
  `mvn clean verify -B` because production sources change.

### Deferred

| Item | Owner |
| --- | --- |
| Diagnostics panel, pre-commit preview, exact preview, conflict choices, multi-error presentation, warning acknowledgement, persistent problems view | **R4** `theme-import-conflict-and-diagnostic-ui` |
| `Program.launch` failure feedback for the "Download more themes" link and for a refused launch | **R4** (with the UI), not here |
| Locale-sensitive catalog search and `ThemeCapitalized` presentation | R4 localisation follow-up (presentation only; explicitly not validation) |
| Remaining activator/layering cleanup in the parser, adapters and repository | **R6** |
| Immutable/frozen validated `Theme`, snapshot accessors, `TokenKey` duplicate registration, thread contract | **R2.4** `immutable-validated-theme-model` |
| Application-result truth, rollback, recovery | **R3** |
| Contrast/high-contrast analysis of bundled themes; light-theme default colours | R4 / new decision (see PM-V7) |
| Editor-preview URL derivation from `ect-id` (community themes use `id`, so `getId()` is empty and the preview URL degenerates) | **R4** `offline-accurate-theme-preview` |
| `mappingOverrides` / `eclipseColorThemeMapping` semantic support | Not planned; out of the editor-theme scope of this product |
| Revision IDs, digests, history, content comparison | **R6/R9** |
| Externalizing UI strings | **R4** `externalize-ui-strings` |

## Compatibility and recovery

- **Imported themes:** a theme that imports today under the recommended rules keeps importing. The concrete
  already-importable cases that would newly fail are only: wrong root element, and blank/missing `name`. Every one
  of those is a record the product cannot present meaningfully.
- **Already-installed themes:** reload re-parses managed files with the same validator. A previously admitted file
  whose `name` is blank would stop being admitted. No file, binding, identity or bytes are deleted by this change;
  the record simply is not offered, and the reason is logged. This is the one place where "same bytes, different
  outcome between versions" can occur, and it is why PM-V6 treats already-installed files explicitly.
- **Bundled themes:** 0 changes under the recommended severities. No bundled asset is modified.
- **Third-party/historical themes:** all 26 sampled upstream themes remain importable; 14 keep a working website
  link under PM-V3 B and lose it under PM-V3 A; one produces warnings for its custom palette.
- **No data is destroyed by this change.** No preference node, managed file, binding or identity is written or
  removed on account of a diagnostic.
- **Rollback:** reverting the change restores exactly the previous permissive behavior; no migration, no persisted
  format change, no new preference keys, no new state files.

## Impact

Production changes are confined to `XmlThemeParser` (and possibly `ThemeType`/`ThemeRepository`/the preference
page for admission and presentation), `ThemeParseException`, `ThemeRepository`/`ThemeManager` operation results,
and the import/update/website rendering in `EclipseThemesPreferencePage`. Tests change in the existing bundle test
tree where they currently assert `Consumer<String>` mapping warnings or permissive parsing. No new dependency,
exported API, OSGi bundle, target-platform change, XSD, schema framework, migration, revision model, digest, or
public API is justified. `JavaSE-17` and the current platform scope are unchanged. Because production sources
change, `mvn clean verify -B` is the final verification step.