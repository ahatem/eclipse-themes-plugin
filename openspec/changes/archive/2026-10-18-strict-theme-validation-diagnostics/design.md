# Design

## Scope and invariants

This change defines a **validation and diagnostics contract at the theme-document boundary**. It is the last R2
change that is purely about accepting or rejecting input; the next one (`immutable-validated-theme-model`) changes
what a validated theme *is*.

Invariants that survive unchanged:

1. **Logical identity is plugin-owned.** Nothing here makes `ect-id`, `id`, display name, basename or content an
   identity input. `ect-id` remains optional legacy/source metadata.
2. **Import is always a new independent record; Update is always an explicit operation on the selected record.**
   Validation does not change which record an operation targets.
3. **Staged bytes are validated before any durable commit.** Validation happens on the same bytes that are
   committed, so a validation error and a commit can never disagree.
4. **Sparse themes remain valid.** Parser defaults for missing tokens stay. "Strict" means *honest about what was
   and was not understood*, not "must define everything".
5. **One validation language.** The same rules run for bundled load, installed reload, import and update. Source
   provenance never enables a relaxed mode (consistent with the accepted `theme-parsing` security requirement
   "Security policy is identical for every source and entry point").

## Where the validation boundary sits today, and after this change

```text
                          TODAY                              AFTER
bytes ─► SecureXmlBuilder ─► DOM ─► Theme (candidate,      ─► SecureXmlBuilder ─► DOM ─► structure
                                  logicalId == null)               + metadata ─► token mapping
                                                                          ─► colour/style
                                                                                │
                                                        ┌───────────────────────┴──────────────────────┐
                                                        │ candidate is complete: structural + metadata    │
                                                        │ + token + colour + style checks have run and   │
                                                        │ their findings are known                       │
                                                        ▼                                                │
                                       repository admission (route-specific                     validation boundary
                                       consequences: log / refuse / skip)          ═══════════╤══════════
                                                        ▼                          ║  R2.4 makes
                                        ThemeRepository / ThemeManager           ║  this immutable
                                                        ▼                          ║
                                        EclipseThemesPreferencePage               ║
```

**The validation boundary ends when the candidate `Theme` is complete** — i.e. after the last token candidate has
been resolved and every diagnostic has been produced. Today that boundary is invisible: `parse()` returns a
`Theme` and the diagnostics have already been logged and lost. This change makes the boundary explicit by making
diagnostics a value that accompanies the candidate rather than a side effect.

Deliberately **not** moved by this change:

- `Theme` stays mutable (`addToken`, live map view, `withLogicalId` copying tokens). `withLogicalId` remains the
  admission step that assigns identity.
- `TokenKey`'s static registry, its silent `put`, and the `IllegalStateException` reachable from `Theme.get()`
  when a chain is broken, remain as they are.
- The parser's documented design — stateless per parse, sink injected by the caller — is preserved rather than
  replaced by a retained result object.

## Diagnostic representation options

Options A–D from [proposal PM-V5](proposal.md#pm-v5--diagnostic-representation-engineering-not-product), assessed
against three concrete consumers that must be served:

- **repository rejection** (import/update must not admit, and must be able to say why);
- **import/update user messaging** (the page must show an actionable reason, not a fixed sentence);
- **logs for bundled/reloaded themes** (already the behaviour; must not regress);
- **tests** (must assert findings without parsing log text).

### Option A — enriched `ThemeParseException` only

`ThemeParseException` gains `List<Diagnostic>`.

- Errors: yes.
- Warnings on a **successful** parse: no home. The whole point of this change is that a document accepted with
  unknown tokens and invalid colours currently reports success silently. Option A leaves exactly that case
  unaddressed.
- Verdict: insufficient on its own.

### Option B — `ThemeParseResult { theme, diagnostics }` replaces the return value

- Warnings: yes, naturally.
- Costs: `ThemeParser` changes shape; the result has a "no theme" state that duplicates what the exception already
  expresses; every call site (`loadPackedThemes`, `loadInstalledThemes`, `importTheme`, `updateTheme`, plus ~10 test
  sites) must unwrap; the "no theme but no error" state invites a second, redundant failure channel.
- Verdict: workable, larger than needed.

### Option C — structured sink only

Generalise `Consumer<String>` to `Consumer<Diagnostic>` and let the caller accumulate.

- Warnings: yes. Errors: yes if the sink is also consulted before throwing, but then the exception and the sink
  carry the same error twice.
- Cost: the repository must hand-collect into a local list only to re-attach it to the value it returns to the UI;
  that re-attachment is unavoidable, so the sink alone does not close the requirement.
- Verdict: partial.

### Option D — structured sink for findings, exception for errors, outcome record at the operation boundary

(approved)

```text
core/parser/ThemeDiagnostic.java        new record, package-private-or-internal
      ( Severity severity, String kind, String message,
        String source, String element, String attribute )
core/parser/Severity.java              two constants: WARNING, ERROR   (or nested in ThemeDiagnostic)

XmlThemeParser(Consumer<ThemeDiagnostic>)  existing seam, element type widened
  - warnings  -> sink
  - first error -> ThemeParseException(message, cause, List<ThemeDiagnostic>)

ThemeParseException
  + diagnostics()                        additive accessor, existing constructors preserved

eclipse/ThemeRepository + ThemeManager
  importTheme/updateTheme -> ThemeOperationOutcome
      record( Theme theme, List<ThemeDiagnostic> diagnostics )  // theme == null == not committed
      successful() / describeFailure()
      -- deliberately shaped like the existing PreferenceReset.Result

preferences/EclipseThemesPreferencePage
  - on failure, describe the first actionable diagnostic
  - website rendered escaped; link activated only for an allowed absolute URI
```

Why this is the minimum:

- It reuses the two idioms already present (`Consumer<String>` sink, `PreferenceReset.Result`) instead of
  introducing a third.
- It adds **two** small internal types and **one** additive exception accessor.
- It is the only option that serves all four consumers without a second, redundant failure channel.
- It removes the two activator `log` calls that currently live inside `XmlThemeParser` (unknown tag `.info`,
  invalid colour `.warn`), which is a strict subset of R6's "extract parser diagnostics from activator dependency"
  and is the minimum required for findings to be data rather than log strings. No other layering change is made.

Severity semantics:

| Severity | Effect on admission | Destination |
| --- | --- | --- |
| `ERROR` | Parse fails immediately; no `Theme` is returned. Import/update do not commit. Reload does not admit. | `ThemeParseException.diagnostics()` → operation outcome → UI message |
| `WARNING` | Parse continues with the documented fallback. Import/update commit. Reload admits. | Sink → operation outcome (for import/update) or `log.warn` (bundled/reload) |

Fail-fast on the first error is chosen deliberately: it matches the existing parse model (any failure throws), it
keeps the failure reason precise, and multi-error presentation is R4 work. This is stated so it is not mistaken
for an oversight.

## Validation rule table (approved)

Approved via [PM-V1…PM-V3](proposal.md#decision-gate). The one refinement relative to the pre-approval draft is
V3/V4: an absent `type` is the format's normal default state and produces no finding, while a present blank or
unrecognised value does.

| # | Condition | Severity | Result |
| --- | --- | --- | --- |
| V1 | root element is not `colorTheme` | ERROR | parse fails |
| V2 | `name` absent or blank after the attribute is read | ERROR | parse fails |
| V3 | `type` absent | none | classified `LIGHT` (unchanged value); absence is the format's default |
| V4 | `type` present but not `dark`/`light` (case-insensitive) | WARNING | classified `LIGHT` (unchanged value) |
| V5 | `ect-id` absent or blank | none | accepted; never identity |
| V6 | legacy `id` present | none | recognised, ignored |
| V7 | `modified` present | none | recognised, ignored |
| V8 | root attribute outside the recognised set | none | ignored |
| V9 | `website` absent | none | accepted |
| V10 | `website` blank, not an absolute URI, or carries embedded control characters | WARNING | link not activated; value still displayed inert |
| V11 | `website` scheme not in the allowed set | WARNING | link not activated; value still displayed inert |
| V12 | unknown element name among direct children | WARNING | element ignored (unchanged outcome, now reported) |
| V13 | known token with absent/blank `color` | WARNING | token ignored (unchanged outcome, now reported) |
| V14 | known token with `color` rejected by `Color.ofHex` | WARNING | token ignored (unchanged outcome, now reported) |
| V15 | `bold`/`italic`/`underline`/`strikethrough` value not case-insensitive `true`/`false` | WARNING | treated as `false` (unchanged value, now reported) |
| V16 | nested child elements under a direct child | WARNING | subtree ignored (unchanged outcome, now reported) |
| V17 | unsupported attribute on a token element | none | ignored |
| V18 | duplicate canonical spelling / canonical-vs-alias conflict | WARNING | **unchanged** approved winner + existing message text |

Notes on rule shape:

- V8 and V17 deliberately emit **no** diagnostic. The recognised root attribute set covers every attribute present
  in 33/33 bundled and 26/26 historical files, and a legacy format with unknown producers should not reject or
  nag about extra attributes. Same reasoning for token attributes, which have zero non-standard occurrences in
  either corpus.
- V3 and V4 deliberately differ. Absent `type` is how 26/26 historical files are authored, so diagnosing it would
  flag the normal legacy case; a present value that is blank or unrecognised is a producer mistake worth surfacing.
  Both keep the existing `LIGHT` classification, so no resolved token value changes.
- V14 is the one rule where ERROR is a defensible alternative. It is escalated in [PM-V6](proposal.md#pm-v6--existing-corpus-compatibility)
  because `loadInstalledThemes` re-parses on every reload: promoting it to ERROR would make a theme that imported
  successfully under an earlier version disappear from the catalog after upgrade, while its bytes and binding
  remain on disk. The recommendation is WARNING precisely to avoid a silent visibility loss disguised as
  validation.
- V15's residual sub-case — `bold=""` currently yields a *present* all-false `TokenOptions` rather than absent
  options — is pre-existing behaviour with no corpus evidence and is not changed here.

## Website handling

Two independent questions, deliberately kept separate:

1. **Admission validity** — a theme is valid regardless of its `website` value (V9/V10/V11 are warnings).
2. **Launch eligibility** — the value is activated only if, after trimming surrounding whitespace, it is a
   syntactically absolute URI whose scheme is in the approved allowed set: exactly `http` and `https`, compared
   case-insensitively. Embedded control characters make the value unusable even if the scheme would otherwise be
   allowed.

The decision is a single small pure function over the raw string so both the parser and the page use the same
rule; the parser needs it to decide whether V11 applies, and the page needs it to decide whether to build anchor
markup at all.

Implementation shape in `EclipseThemesPreferencePage`:

- `updateThemeDetails` stops concatenating the raw value into `"<a>…</a>"`. It asks the parser/model for a
  launchable link decision and only emits anchor markup for that case; otherwise it sets escaped plain text on the
  same widget so the value stays readable.
- The `websiteLink` selection listener refuses to launch unless the same launchability check passes, so the
  `"N/A"` placeholder and any refused value become inert.
- No HTTP client, no fetch, no scheme registry, no browser/security framework is introduced. The plugin still only
  asks the OS to open an external URI.
- Escaping is limited to making the value inert as display text. The plugin does not attempt to sanitise arbitrary
  HTML.

The launchable-URI check is a small pure function so it can be tested without SWT. That is the only new logic and
it belongs to this change because U8 is part of this change's scope; nothing else in the page is touched.

## Source consistency (one language, three presentations)

| Source | On validation ERROR | On validation WARNING |
| --- | --- | --- |
| Bundled load | Not admitted; `log.error` naming the bundled resource and the diagnostic | Admitted; `log.warn` with the same diagnostic |
| Installed reload | Not admitted; `log.error` naming the managed file and the diagnostic; bytes and binding untouched | Admitted; `log.warn` |
| Import | Not admitted; no locator, binding, catalog record or selection change; reason carried on the operation outcome | Admitted and committed; warnings carried on the outcome |
| Update | Not admitted; prior bytes, binding, identity, catalog record and selection untouched | Admitted and committed; warnings carried on the outcome |

The single behavioural correction this produces: a blank-name installed file is no longer admitted while an
identical bundled file is skipped. Both are now skipped, with a diagnostic.

The curated-bundled-identity gate (`bundled:<ect-id>` requires a non-blank `ect-id`) is **not** a document-validity
rule and is **not** changed. It is a bundling concern; `ect-id` must not become required for imported themes.

## Relationship to R2.4 `immutable-validated-theme-model`

What R2.3 provides for the next change:

- an explicit, named boundary: "the candidate is complete" = structure + metadata + token + colour + style checks
  have all run and their findings are known;
- findings as values, so a future frozen model can be constructed from a validated result plus a diagnostic list
  rather than from a partially-logged parse;
- an error channel that is already distinct from the warning channel, which is what a frozen "validated or
  rejected" construction needs.

What remains intentionally mutable until R2.4, and must not be touched here:

- `Theme.addToken`, the live `HashMap` behind `getTokens()`, `withLogicalId`'s token copying;
- `TokenKey`'s static registry and its silent replacement;
- the parser's stateless-per-parse design and the mutability of a candidate `Theme` between `parse()` and
  `withLogicalId()`;
- `Theme`'s identity-unresolved equality semantics.

This change adds no immutable builder, snapshot, or builder-pattern `Theme`. The word "validated" in this change's
title means *the document passed the documented checks*, not *the object is frozen*.

## Relationship to R6 (parser logging / activator back-edge)

The accepted architecture note says the parser→activator→manager→repository→parser back-edge should be removed
by returning diagnostics or injecting a sink. This change necessarily performs the "inject a sink / return
diagnostics" half for `XmlThemeParser`: once findings are data, the two `EclipseThemes.instance().getLogger()`
calls inside the parser disappear, because that is what the contract requires. Nothing else is done here:

- no new logging abstraction, no logger port, no `ILog` interface in `core`;
- no changes to `ThemeRepository`'s or `ThemeManager`'s existing `ILog` use;
- no adapter-layering work, no registry/discovery work.

That is a strict subset of the R6 change, and it is documented here so the remaining R6 scope stays explicit.

## Notes on behavior deliberately preserved

- `Color.ofHex` grammar is documented, not widened. `#RGB` shorthand stays supported because
  `XmlThemeParserCompatibilityTest` uses it; hex stays case-insensitive; whitespace trimming stays.
- The existing mapping-conflict message text stays byte-identical. Only the carrier changes from `String` to
  `ThemeDiagnostic`, and the approved `theme-parsing` mapping-diagnostic scenarios keep asserting the same
  role/source/occurrence/winner content.
- The `ThemeParser` interface signature is unchanged. The single non-default `XmlThemeParser` constructor and the
  `ParserFactory.getParserFor(String, Consumer<…>)` overload absorb the new sink element type.
- `PreferenceReset`, adapters, preference keys, identity bindings, staging, and the update/removal orderings are
  untouched.
- Bundled and installed theme XML files are not modified.